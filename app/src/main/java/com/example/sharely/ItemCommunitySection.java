package com.example.sharely;

import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.HashMap;
import java.util.Map;

/** Public item questions and private user/listing reports integrated into Item Details. */
final class ItemCommunitySection {
    private final Context context;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final LinearLayout root;
    private final LinearLayout questions;
    private String itemId;
    private String ownerId;
    private final int green = Color.rgb(23,107,82);
    ItemCommunitySection(Context context) {
        this.context = context;
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, dp(18), 0, dp(18));
        TextView heading = label("Community questions", 19, true);
        root.addView(heading);
        TextView description = label("Ask about the item. Questions and answers are public.",14,false);
        root.addView(description);
        questions = new LinearLayout(context);
        questions.setOrientation(LinearLayout.VERTICAL);
        root.addView(questions);
        Button ask = button("Ask a question");
        root.addView(ask);
        ask.setOnClickListener(v -> askQuestion());
        Button report = button("Report this listing or owner");
        root.addView(report);
        report.setOnClickListener(v -> chooseReport());
    }
    View getView() { return root; }
    void showItem(String itemId, String ownerId) {
        if (itemId == null || ownerId == null) return;
        this.itemId = itemId;
        this.ownerId = ownerId;
        loadQuestions();
    }
    private String uid() {
        return FirebaseAuth.getInstance().getCurrentUser() == null ? null
                : FirebaseAuth.getInstance().getCurrentUser().getUid();
    }
    private int dp(int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
    private TextView label(String text, int sp, boolean bold) {
        TextView v = new TextView(context); v.setText(text); v.setTextSize(sp);
        v.setTextColor(Color.rgb(18,61,49)); v.setPadding(0,dp(6),0,dp(6));
        if(bold) v.setTypeface(null, android.graphics.Typeface.BOLD);
        return v;
    }
    private Button button(String text) {
        Button b = new Button(context); b.setText(text); b.setAllCaps(false);
        b.setTextColor(Color.WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(green));
        return b;
    }
    private void notice(String message) { Toast.makeText(context,message,Toast.LENGTH_LONG).show(); }
    private void loadQuestions() {
        final String displayed = itemId;
        questions.removeAllViews(); questions.addView(label("Loading questions...",14,false));
        db.collection("itemQuestions").whereEqualTo("itemId",displayed)
          .orderBy("createdAt",Query.Direction.ASCENDING).limit(50).get()
          .addOnSuccessListener(result -> {
            if(!displayed.equals(itemId)) return;
            questions.removeAllViews();
            if(result.isEmpty()) { questions.addView(label("No questions yet.",14,false)); return; }
            for (DocumentSnapshot doc : result) {
                String question = doc.getString("question");
                String answer = doc.getString("answer");
                if(question == null) continue;
                questions.addView(label("Q: " + question, 15, true));
                questions.addView(label(answer == null || answer.isEmpty()
                        ? "Awaiting owner response" : "A: " + answer,14,false));
                if(uid() != null && uid().equals(ownerId) && (answer == null || answer.isEmpty())) {
                    Button reply = button("Answer"); questions.addView(reply);
                    reply.setOnClickListener(v -> answerQuestion(doc.getId()));
                }
            }
          }).addOnFailureListener(e -> { questions.removeAllViews();
              questions.addView(label("Unable to load questions: " + e.getMessage(),14,false)); });
    }
    private void askQuestion() {
        if(uid() == null || itemId == null) { notice("Please sign in first"); return; }
        if(uid().equals(ownerId)) { notice("Owners can answer questions, not ask their own listing"); return; }
        EditText field = new EditText(context); field.setHint("What would you like to know?");
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        new AlertDialog.Builder(context).setTitle("Ask the owner").setView(field)
          .setNegativeButton("Cancel",null).setPositiveButton("Post",(d,w)->{
            String message = field.getText().toString().trim();
            if(message.length()<3 || message.length()>500) { notice("Enter 3–500 characters"); return; }
            String id = db.collection("itemQuestions").document().getId();
            Map<String,Object> data = new HashMap<>();
            data.put("questionId",id); data.put("itemId",itemId);
            data.put("authorId",uid()); data.put("question",message);
            data.put("answer",""); data.put("createdAt",System.currentTimeMillis());
            db.collection("itemQuestions").document(id).set(data)
              .addOnSuccessListener(x->loadQuestions()).addOnFailureListener(e->notice(e.getMessage()));
          }).show();
    }
    private void answerQuestion(String id) {
        EditText field = new EditText(context); field.setHint("Your answer");
        new AlertDialog.Builder(context).setTitle("Reply to question").setView(field)
          .setNegativeButton("Cancel",null).setPositiveButton("Post",(d,w)->{
            String answer = field.getText().toString().trim();
            if(answer.length()<2 || answer.length()>1000) { notice("Enter 2–1000 characters"); return; }
            db.collection("itemQuestions").document(id).update("answer",answer)
              .addOnSuccessListener(x->loadQuestions()).addOnFailureListener(e->notice(e.getMessage()));
          }).show();
    }
    private void chooseReport() {
        if(uid()==null || itemId==null) {notice("Please sign in first");return;}
        String[] targets={"Report listing","Report owner account"};
        new AlertDialog.Builder(context).setTitle("What would you like to report?")
          .setItems(targets,(d,w)->reasonReport(w==0?"LISTING":"ACCOUNT",w==0?itemId:ownerId)).show();
    }
    private void reasonReport(String targetType,String targetId) {
        if(targetId.equals(uid())) {notice("You cannot report your own account");return;}
        String[] reasons={"Illegal or prohibited item","Misleading information","Harassment","Other"};
        new AlertDialog.Builder(context).setTitle("Reason for report")
          .setItems(reasons,(d,which)->{
            String reason=reasons[which];
            new AlertDialog.Builder(context).setTitle("Submit report?")
              .setMessage("This report is private and will be reviewed. No automatic action will be taken.")
              .setNegativeButton("Cancel",null).setPositiveButton("Submit",(dialog,n)->{
                String id=db.collection("reports").document().getId();
                Map<String,Object> data=new HashMap<>();
                data.put("reportId",id);data.put("reporterId",uid());
                data.put("targetType",targetType);data.put("targetId",targetId);
                data.put("reason",reason);data.put("createdAt",System.currentTimeMillis());
                db.collection("reports").document(id).set(data)
                  .addOnSuccessListener(v->notice("Report submitted"))
                  .addOnFailureListener(e->notice("Could not submit: "+e.getMessage()));
              }).show();
          }).show();
    }
}
