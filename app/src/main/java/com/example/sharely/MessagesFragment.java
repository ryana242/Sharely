package com.example.sharely;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.sharely.data.NotificationRepository;
import com.example.sharely.service.NotificationService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MessagesFragment extends Fragment {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final NotificationService notifications = new NotificationService();

    private ListenerRegistration inboxListener;
    private ListenerRegistration messageListener;

    private FrameLayout body;
    private LinearLayout header;
    private String uid;
    private String conversationId;
    private String recipientId;
    private String itemTitle;
    private boolean sending;

    private final int green = Color.rgb(23, 107, 82);
    private final int ink = Color.rgb(18, 61, 49);
    private final int muted = Color.rgb(90, 125, 114);
    private final int pale = Color.rgb(237, 247, 243);

    public MessagesFragment() {
        super();
    }

    private int dp(float value) {
        return Math.round(
                value * requireContext()
                        .getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private GradientDrawable shape(int fill, int stroke, int radius) {
        GradientDrawable result = new GradientDrawable();
        result.setColor(fill);
        result.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) {
            result.setStroke(dp(1), stroke);
        }
        return result;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(requireContext());
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private Button button(String title, boolean filled) {
        Button view = new Button(requireContext());
        view.setText(title);
        view.setAllCaps(false);
        view.setTextSize(14);
        view.setTextColor(filled ? Color.WHITE : green);
        view.setBackgroundTintList(
                ColorStateList.valueOf(filled ? green : pale)
        );
        view.setMinHeight(dp(48));
        view.setMinimumHeight(dp(48));
        return view;
    }

    private void toast(String message) {
        if (isAdded()) {
            Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull android.view.LayoutInflater inflater,
            @Nullable ViewGroup parent,
            @Nullable Bundle state
    ) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user == null ? null : user.getUid();

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        header = new LinearLayout(requireContext());
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(10), dp(18), dp(10));
        root.addView(
                header,
                new LinearLayout.LayoutParams(-1, dp(72))
        );

        body = new FrameLayout(requireContext());
        root.addView(
                body,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

        if (state != null) {
            conversationId = state.getString("conversationId");
            recipientId = state.getString("recipientId");
            itemTitle = state.getString("itemTitle");
        }

        if (uid == null) {
            showNotice("Sign in to read and send messages.");
        } else if (conversationId == null) {
            showInbox();
        } else {
            showThread();
        }

        return root;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("conversationId", conversationId);
        out.putString("recipientId", recipientId);
        out.putString("itemTitle", itemTitle);
    }

    private void stopListeners() {
        if (inboxListener != null) {
            inboxListener.remove();
            inboxListener = null;
        }
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        stopListeners();
        body = null;
        header = null;
        super.onDestroyView();
    }

    private void showNotice(String notice) {
        if (body == null) return;

        body.removeAllViews();

        TextView label = text(notice, 15, muted, false);
        label.setGravity(Gravity.CENTER);
        body.addView(
                label,
                new FrameLayout.LayoutParams(-1, -1)
        );
    }

    private LinearLayout verticalContent() {
        body.removeAllViews();

        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        body.addView(
                scroll,
                new FrameLayout.LayoutParams(-1, -1)
        );

        LinearLayout list = new LinearLayout(requireContext());
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), dp(12), dp(16), dp(20));
        scroll.addView(list);

        return list;
    }

    private void showInbox() {
        stopListeners();

        conversationId = null;
        recipientId = null;
        itemTitle = null;

        header.removeAllViews();

        TextView title = text("Messages", 23, ink, true);
        header.addView(
                title,
                new LinearLayout.LayoutParams(0, -2, 1)
        );

        Button newChat = button("New chat", true);
        header.addView(
                newChat,
                new LinearLayout.LayoutParams(dp(112), dp(48))
        );

        newChat.setOnClickListener(v -> chooseListing());

        showNotice("Loading conversations...");

        inboxListener = db.collection("conversations")
                .whereArrayContains("participantIds", uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()
                            || body == null
                            || conversationId != null) {
                        return;
                    }

                    if (error != null) {
                        showNotice(
                                "Could not load messages. Check chat permissions.\n"
                                        + error.getMessage()
                        );
                        return;
                    }

                    LinearLayout list = verticalContent();

                    if (snapshot == null || snapshot.isEmpty()) {
                        TextView empty = text(
                                "No conversations yet.\n"
                                        + "Tap New chat to message an item owner.",
                                16,
                                muted,
                                false
                        );
                        empty.setGravity(Gravity.CENTER);
                        empty.setPadding(
                                dp(14), dp(90), dp(14), 0
                        );
                        list.addView(empty);
                        return;
                    }

                    List<DocumentSnapshot> docs =
                            new ArrayList<>(snapshot.getDocuments());

                    Collections.sort(
                            docs,
                            (a, b) -> Long.compare(
                                    number(b, "updatedAt"),
                                    number(a, "updatedAt")
                            )
                    );

                    for (DocumentSnapshot doc : docs) {
                        addConversationCard(list, doc);
                    }
                });
    }

    private long number(DocumentSnapshot document, String key) {
        Long value = document.getLong(key);
        return value == null ? 0 : value;
    }

    private void addConversationCard(
            LinearLayout list,
            DocumentSnapshot document
    ) {
        String owner = document.getString("ownerId");
        String borrower = document.getString("borrowerId");

        if (!uid.equals(owner) && !uid.equals(borrower)) return;

        String peer = uid.equals(owner) ? borrower : owner;
        if (peer == null || peer.isEmpty()) return;

        String label = document.getString("itemName");
        String preview = document.getString("lastMessage");

        if (label == null || label.isEmpty()) {
            label = "Item conversation";
        }
        if (preview == null || preview.isEmpty()) {
            preview = "Start your conversation";
        }

        final String conversationItem = label;

        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(
                dp(16), dp(15), dp(16), dp(15)
        );
        card.setBackground(
                shape(
                        Color.WHITE,
                        Color.rgb(215, 232, 225),
                        14
                )
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(12);
        list.addView(card, params);

        TextView name = text(
                "Sharely member · " + conversationItem,
                16, ink, true
        );
        card.addView(name);

        TextView snippet = text(preview, 14, muted, false);
        snippet.setMaxLines(2);
        snippet.setEllipsize(TextUtils.TruncateAt.END);

        LinearLayout.LayoutParams s =
                new LinearLayout.LayoutParams(-1, -2);
        s.topMargin = dp(6);
        card.addView(snippet, s);

        card.setOnClickListener(
                v -> openThread(
                        document.getId(),
                        peer,
                        conversationItem
                )
        );

        db.collection("users")
                .document(peer)
                .get()
                .addOnSuccessListener(user -> {
                    if (name.getParent() == null) return;

                    String person = user.getString("name");
                    if (person != null && !person.trim().isEmpty()) {
                        name.setText(
                                person.trim() + " · " + conversationItem
                        );
                    }
                });
    }

    private void chooseListing() {
        db.collection("items")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;

                    List<DocumentSnapshot> choices =
                            new ArrayList<>();
                    List<String> titles = new ArrayList<>();

                    for (DocumentSnapshot item : snapshot.getDocuments()) {
                        String owner = item.getString("ownerId");
                        String name = item.getString("itemName");
                        String status = item.getString("status");

                        if (owner == null
                                || owner.equals(uid)
                                || name == null
                                || !"ACTIVE".equals(status)) {
                            continue;
                        }

                        choices.add(item);
                        titles.add(
                                name + " · "
                                        + String.valueOf(
                                        item.getString("location")
                                )
                        );
                    }

                    if (choices.isEmpty()) {
                        toast(
                                "No available listings from other users."
                        );
                        return;
                    }

                    new AlertDialog.Builder(requireContext())
                            .setTitle("Message an item owner")
                            .setItems(
                                    titles.toArray(new String[0]),
                                    (dialog, which) ->
                                            createOrOpen(
                                                    choices.get(which)
                                            )
                            )
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .addOnFailureListener(
                        error -> toast(
                                "Can't load listings: "
                                        + error.getMessage()
                        )
                );
    }

    private void createOrOpen(DocumentSnapshot item) {
        String owner = item.getString("ownerId");
        String name = item.getString("itemName");

        if (uid == null
                || owner == null
                || uid.equals(owner)
                || name == null) {
            return;
        }

        String id = "item_" + UUID.nameUUIDFromBytes(
                (item.getId() + ":" + uid)
                        .getBytes(StandardCharsets.UTF_8)
        );

        DocumentReference ref =
                db.collection("conversations").document(id);

        long now = System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", id);
        data.put("itemId", item.getId());
        data.put("itemName", name);
        data.put("ownerId", owner);
        data.put("borrowerId", uid);
        data.put("participantIds", Arrays.asList(owner, uid));
        data.put("createdAt", now);
        data.put("updatedAt", now);
        data.put("lastMessage", "");
        data.put("lastSenderId", "");

        db.runTransaction(tx -> {
            DocumentSnapshot existing = tx.get(ref);

            if (!existing.exists()) {
                tx.set(ref, data);
            } else if (!uid.equals(existing.getString("borrowerId"))
                    || !owner.equals(existing.getString("ownerId"))
                    || !item.getId().equals(
                    existing.getString("itemId")
            )) {
                throw new IllegalStateException(
                        "Conversation details do not match"
                );
            }

            return null;
        }).addOnSuccessListener(unused -> {
            if (isAdded()) {
                openThread(id, owner, name);
            }
        }).addOnFailureListener(
                error -> toast(
                        "Could not start chat: " + error.getMessage()
                )
        );
    }

    private void openThread(
            String id,
            String peer,
            String label
    ) {
        stopListeners();
        conversationId = id;
        recipientId = peer;
        itemTitle = label;
        showThread();
    }

    private void showThread() {
        if (body == null
                || conversationId == null
                || recipientId == null) {
            showInbox();
            return;
        }

        stopListeners();
        header.removeAllViews();

        Button back = button("‹ Inbox", false);
        header.addView(
                back,
                new LinearLayout.LayoutParams(dp(98), dp(48))
        );
        back.setOnClickListener(v -> showInbox());

        TextView heading = text(
                itemTitle == null ? "Chat" : itemTitle,
                19, ink, true
        );
        heading.setSingleLine(true);
        heading.setEllipsize(TextUtils.TruncateAt.END);

        LinearLayout.LayoutParams h =
                new LinearLayout.LayoutParams(0, -2, 1);
        h.leftMargin = dp(12);
        header.addView(heading, h);

        body.removeAllViews();

        LinearLayout page = new LinearLayout(requireContext());
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.rgb(249, 252, 251));
        body.addView(
                page,
                new FrameLayout.LayoutParams(-1, -1)
        );

        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        page.addView(
                scroll,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

        LinearLayout messages = new LinearLayout(requireContext());
        messages.setPadding(
                dp(12), dp(16), dp(12), dp(14)
        );
        messages.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(messages);

        LinearLayout composer = new LinearLayout(requireContext());
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(
                dp(10), dp(8), dp(10), dp(8)
        );
        composer.setBackgroundColor(Color.WHITE);
        page.addView(
                composer,
                new LinearLayout.LayoutParams(-1, -2)
        );

        EditText input = new EditText(requireContext());
        input.setTextSize(15);
        input.setSingleLine(false);
        input.setMaxLines(4);
        input.setHint("Write a message...");
        input.setPadding(
                dp(12), dp(9), dp(12), dp(9)
        );
        input.setBackground(
                shape(pale, Color.TRANSPARENT, 12)
        );
        composer.addView(
                input,
                new LinearLayout.LayoutParams(0, -2, 1)
        );

        Button send = button("Send", true);
        LinearLayout.LayoutParams sp =
                new LinearLayout.LayoutParams(dp(76), dp(48));
        sp.leftMargin = dp(8);
        composer.addView(send, sp);

        send.setOnClickListener(
                v -> sendMessage(input, send)
        );

        final String showing = conversationId;

        messageListener = db.collection("conversations")
                .document(showing)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()
                            || body == null
                            || !showing.equals(conversationId)) {
                        return;
                    }

                    messages.removeAllViews();

                    if (error != null) {
                        messages.addView(
                                text(
                                        "Unable to load chat: "
                                                + error.getMessage(),
                                        14, muted, false
                                )
                        );
                        return;
                    }

                    if (snapshot == null || snapshot.isEmpty()) {
                        TextView empty = text(
                                "Say hello! Keep conversations respectful "
                                        + "and relevant to the item.",
                                14, muted, false
                        );
                        empty.setGravity(Gravity.CENTER);
                        empty.setPadding(
                                dp(8), dp(70), dp(8), 0
                        );
                        messages.addView(empty);
                    } else {
                        for (DocumentSnapshot doc
                                : snapshot.getDocuments()) {
                            addBubble(messages, doc);
                        }
                    }

                    scroll.post(
                            () -> scroll.fullScroll(View.FOCUS_DOWN)
                    );
                });
    }

    private void addBubble(
            LinearLayout list,
            DocumentSnapshot document
    ) {
        String message = document.getString("text");
        String sender = document.getString("senderId");

        if (message == null || sender == null) return;

        boolean mine = uid.equals(sender);

        LinearLayout bubble = new LinearLayout(requireContext());
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setBackground(
                shape(
                        mine ? green : Color.WHITE,
                        mine
                                ? Color.TRANSPARENT
                                : Color.rgb(215, 232, 225),
                        16
                )
        );
        bubble.setPadding(
                dp(13), dp(9), dp(13), dp(9)
        );

        TextView content = text(
                message,
                15,
                mine ? Color.WHITE : ink,
                false
        );
        bubble.addView(content);

        TextView time = text(
                new SimpleDateFormat(
                        "h:mm a",
                        Locale.getDefault()
                ).format(
                        new Date(number(document, "createdAt"))
                ),
                11,
                mine
                        ? Color.rgb(221, 244, 234)
                        : muted,
                false
        );

        LinearLayout.LayoutParams timeParams =
                new LinearLayout.LayoutParams(-2, -2);
        timeParams.topMargin = dp(5);
        bubble.addView(time, timeParams);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-2, -2);
        params.gravity = mine ? Gravity.END : Gravity.START;
        params.bottomMargin = dp(10);
        params.leftMargin = mine ? dp(50) : 0;
        params.rightMargin = mine ? 0 : dp(50);

        list.addView(bubble, params);
    }

    private void sendMessage(EditText input, Button send) {
        if (sending
                || conversationId == null
                || recipientId == null
                || uid == null) {
            return;
        }

        String value = input.getText().toString().trim();

        if (value.isEmpty()) return;

        if (value.length() > 1200) {
            input.setError("Maximum 1,200 characters");
            return;
        }

        sending = true;
        send.setEnabled(false);

        final String id = conversationId;
        final String recipient = recipientId;

        final DocumentReference convRef =
                db.collection("conversations").document(id);

        final DocumentReference msgRef =
                convRef.collection("messages").document();

        long now = System.currentTimeMillis();

        Map<String, Object> message = new HashMap<>();
        message.put("messageId", msgRef.getId());
        message.put("senderId", uid);
        message.put("recipientId", recipient);
        message.put("text", value);
        message.put("createdAt", now);

        WriteBatch batch = db.batch();
        batch.set(msgRef, message);
        batch.update(
                convRef,
                "lastMessage", value,
                "lastSenderId", uid,
                "updatedAt", now
        );

        batch.commit().addOnSuccessListener(unused -> {
            sending = false;

            if (isAdded() && id.equals(conversationId)) {
                input.setText("");
                send.setEnabled(true);
            }

            notifications.notifyNewMessage(
                    id,
                    uid,
                    recipient,
                    new NotificationRepository.OnNotificationSavedListener() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onFailure(Exception error) {
                            android.util.Log.w(
                                    "SharelyChat",
                                    "Message notification not saved",
                                    error
                            );
                        }
                    }
            );
        }).addOnFailureListener(error -> {
            sending = false;

            if (isAdded()) {
                send.setEnabled(true);
                toast(
                        "Message not sent: " + error.getMessage()
                );
            }
        });
    }
}
