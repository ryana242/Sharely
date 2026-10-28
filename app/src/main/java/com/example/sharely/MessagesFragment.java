package com.example.sharely;

import android.content.Intent;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MessagesFragment extends Fragment {

    private static final String ARG_OPEN_ITEM_ID = "sharely_open_item_id";
    private static final String ARG_OPEN_CONVERSATION_ID = "sharely_open_conversation_id";

    public static MessagesFragment forConversation(String conversationId) {
        MessagesFragment fragment = new MessagesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_OPEN_CONVERSATION_ID, conversationId);
        fragment.setArguments(args);
        return fragment;
    }

    public static MessagesFragment forItem(String itemId) {
        MessagesFragment fragment = new MessagesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_OPEN_ITEM_ID, itemId);
        fragment.setArguments(args);
        return fragment;
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final NotificationService notifications = new NotificationService();

    private ListenerRegistration inboxListener;
    private ListenerRegistration messageListener;
    private ListenerRegistration unreadNotificationListener;
    private final Map<String, Integer> unreadByConversation = new HashMap<>();
    private final Set<String> markingReadIds = new HashSet<>();
    private final List<DocumentSnapshot> inboxDocuments = new ArrayList<>();

    private FrameLayout body;
    private LinearLayout header;
    private String uid;
    private String conversationId;
    private String recipientId;
    private String itemTitle;
    private String currentItemId;
    private boolean sending;
    private boolean threadLoaded;

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
        root.setBackgroundResource(R.drawable.bg_sharely_page);

        header = new LinearLayout(requireContext());
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(12), dp(20), dp(12));
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
            currentItemId = state.getString("currentItemId");
        }

        if (uid == null) {
            showNotice("Sign in to read and send messages.");
        } else if (conversationId != null) {
            showThread();
        } else if (getArguments() != null
                && getArguments().getString(ARG_OPEN_CONVERSATION_ID) != null) {
            openLinkedConversation(getArguments().getString(ARG_OPEN_CONVERSATION_ID));
        } else if (getArguments() != null
                && getArguments().getString(ARG_OPEN_ITEM_ID) != null) {
            openListingFromItemDetails(
                    getArguments().getString(ARG_OPEN_ITEM_ID)
            );
        } else {
            showInbox();
        }

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (uid != null) {
            startUnreadNotificationListener();
            if (conversationId != null) markOpenThreadRead();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (conversationId != null) markOpenThreadRead();
    }

    @Override
    public void onStop() {
        // Don't mark messages as read when another Activity covers the chat.
        if (unreadNotificationListener != null) {
            unreadNotificationListener.remove();
            unreadNotificationListener = null;
        }
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("conversationId", conversationId);
        out.putString("recipientId", recipientId);
        out.putString("itemTitle", itemTitle);
        out.putString("currentItemId", currentItemId);
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
        if (unreadNotificationListener != null) {
            unreadNotificationListener.remove();
            unreadNotificationListener = null;
        }
        unreadByConversation.clear();
        markingReadIds.clear();
        inboxDocuments.clear();
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
        currentItemId = null;
        threadLoaded = false;

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

                    inboxDocuments.clear();
                    if (snapshot != null) {
                        inboxDocuments.addAll(snapshot.getDocuments());
                    }
                    renderInbox();
                });
    }

    private void renderInbox() {
        if (body == null || conversationId != null) return;
        LinearLayout list = verticalContent();
        if (inboxDocuments.isEmpty()) {
            TextView empty = text(
                    "No conversations yet.\nTap New chat to message an item owner.",
                    16, muted, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(14), dp(90), dp(14), 0);
            list.addView(empty);
            return;
        }
        List<DocumentSnapshot> docs = new ArrayList<>(inboxDocuments);
        Collections.sort(docs, (a, b) -> Long.compare(
                number(b, "updatedAt"), number(a, "updatedAt")));
        for (DocumentSnapshot doc : docs) addConversationCard(list, doc);
    }

    /** Incoming message notifications are the current source of unread state.
     * No message or conversation schema change is required. */
    private void startUnreadNotificationListener() {
        if (unreadNotificationListener != null || uid == null) return;
        unreadNotificationListener = db.collection("notifications")
                .whereEqualTo("recipientId", uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded() || body == null || error != null || snapshot == null) return;
                    Map<String, Integer> latest = new HashMap<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (!"NEW_MESSAGE".equals(doc.getString("type"))
                                || !Boolean.FALSE.equals(doc.getBoolean("isRead"))) continue;
                        String linkedConversation = doc.getString("relatedId");
                        if (linkedConversation == null || linkedConversation.isEmpty()) continue;
                        Integer count = latest.get(linkedConversation);
                        latest.put(linkedConversation, count == null ? 1 : count + 1);
                        if (linkedConversation.equals(conversationId) && threadLoaded && isResumed()) {
                            markIncomingNotificationRead(doc);
                        }
                    }
                    unreadByConversation.clear();
                    unreadByConversation.putAll(latest);
                    if (conversationId == null) renderInbox();
                });
    }

    private void markIncomingNotificationRead(DocumentSnapshot doc) {
        // Only the notification recipient can update its isRead field.
        if (!markingReadIds.add(doc.getId())) return;
        doc.getReference().update("isRead", true)
                .addOnCompleteListener(task -> markingReadIds.remove(doc.getId()));
    }

    private void markOpenThreadRead() {
        if (uid == null || conversationId == null || !threadLoaded || !isResumed()) return;
        // Refresh the unread records immediately when a thread opens;
        // the listener also handles notifications that arrive while it is open.
        final String openId = conversationId;
        db.collection("notifications")
                .whereEqualTo("recipientId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isResumed() || !openId.equals(conversationId)) return;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if ("NEW_MESSAGE".equals(doc.getString("type"))
                                && Boolean.FALSE.equals(doc.getBoolean("isRead"))
                                && openId.equals(doc.getString("relatedId"))) {
                            markIncomingNotificationRead(doc);
                        }
                    }
                });
    }

    private void openLinkedConversation(String linkedId) {
        if (linkedId == null || linkedId.trim().isEmpty()) {
            showInbox();
            return;
        }
        showNotice("Opening conversation...");
        db.collection("conversations").document(linkedId).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || body == null) return;
                    String owner = doc.getString("ownerId");
                    String borrower = doc.getString("borrowerId");
                    if (!doc.exists() || (!uid.equals(owner) && !uid.equals(borrower))) {
                        toast("Conversation not available.");
                        showInbox();
                        return;
                    }
                    String other = uid.equals(owner) ? borrower : owner;
                    openThread(doc.getId(), other, doc.getString("itemName"),
                            doc.getString("itemId"));
                })
                .addOnFailureListener(error -> {
                    if (isAdded() && body != null) {
                        toast("Unable to open conversation: " + error.getMessage());
                        showInbox();
                    }
                });
    }

    private void viewItem(String id) {
        if (id == null || id.trim().isEmpty() || !isAdded()) {
            toast("This listing is unavailable.");
            return;
        }
        Intent intent = new Intent(requireContext(), ItemDetailsActivity.class);
        intent.putExtra("itemId", id);
        startActivity(intent);
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
        final String linkedItemId = document.getString("itemId");
        int unseen = unreadByConversation.containsKey(document.getId())
                ? unreadByConversation.get(document.getId()) : 0;

        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(
                dp(16), dp(15), dp(16), dp(15)
        );
        card.setBackground(
                shape(
                        unseen > 0 ? pale : Color.WHITE,
                        Color.rgb(215, 232, 225),
                        14
                )
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(12);
        list.addView(card, params);

        TextView name = text("Sharely member", 16, ink, true);
        card.addView(name);

        TextView itemLink = text(conversationItem, 14, green, true);
        itemLink.setPadding(0, dp(5), 0, dp(6));
        itemLink.setContentDescription("Open item details: " + conversationItem);
        itemLink.setClickable(true);
        itemLink.setFocusable(true);
        itemLink.setContentDescription("View item " + conversationItem);
        itemLink.setOnClickListener(v -> viewItem(linkedItemId));
        card.addView(itemLink);

        if (unseen > 0) {
            TextView unread = text("●  " + unseen + " unread message"
                    + (unseen == 1 ? "" : "s"), 12, green, true);
            card.addView(unread);
        }

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
                        conversationItem,
                        linkedItemId
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
                                person.trim()
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

    private void openListingFromItemDetails(String linkedItemId) {
        if (linkedItemId == null || linkedItemId.trim().isEmpty()) {
            showInbox();
            return;
        }

        showNotice("Opening conversation...");

        db.collection("items")
                .document(linkedItemId)
                .get()
                .addOnSuccessListener(item -> {
                    if (!isAdded() || body == null) return;

                    String owner = item.getString("ownerId");
                    if (!item.exists()
                            || owner == null
                            || uid.equals(owner)
                            || !"ACTIVE".equals(item.getString("status"))) {
                        toast("This listing cannot start a conversation.");
                        showInbox();
                        return;
                    }

                    createOrOpen(item);
                })
                .addOnFailureListener(error -> {
                    if (!isAdded() || body == null) return;
                    toast("Unable to open chat: " + error.getMessage());
                    showInbox();
                });
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

        // New conversations may not be readable until the document exists.
        // Create first; when it already exists, a protected create attempt
        // fails and we then read the existing participant-only document.
        ref.set(data).addOnSuccessListener(unused -> {
            if (isAdded()) {
                openThread(id, owner, name, item.getId());
            }
        }).addOnFailureListener(createError ->
                ref.get()
                        .addOnSuccessListener(existing -> {
                            if (!isAdded()) return;

                            if (!existing.exists()
                                    || !uid.equals(existing.getString("borrowerId"))
                                    || !owner.equals(existing.getString("ownerId"))
                                    || !item.getId().equals(existing.getString("itemId"))) {
                                toast("Unable to open this conversation.");
                                showInbox();
                                return;
                            }

                            openThread(id, owner, name, item.getId());
                        })
                        .addOnFailureListener(error -> {
                            if (!isAdded()) return;
                            toast("Could not open chat: " + error.getMessage());
                            showInbox();
                        })
        );
    }

    private void openThread(
            String id,
            String peer,
            String label,
            String linkedItemId
    ) {
        stopListeners();
        conversationId = id;
        recipientId = peer;
        itemTitle = label;
        currentItemId = linkedItemId;
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
        threadLoaded = false;
        header.removeAllViews();

        android.widget.ImageButton back = new android.widget.ImageButton(requireContext());
        back.setBackgroundResource(R.drawable.bg_sharely_back);
        back.setBackgroundTintList(null);
        back.setImageResource(R.drawable.ic_sharely_back);
        back.setImageTintList(android.content.res.ColorStateList.valueOf(green));
        back.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        back.setPadding(dp(12), dp(12), dp(12), dp(12));
        back.setContentDescription("Back to messages");
        header.addView(
                back,
                new LinearLayout.LayoutParams(dp(48), dp(48))
        );
        back.setOnClickListener(v -> showInbox());

        TextView heading = text(
                itemTitle == null ? "Chat" : itemTitle,
                19, ink, true
        );
        heading.setSingleLine(true);
        heading.setEllipsize(TextUtils.TruncateAt.END);

        LinearLayout titleColumn = new LinearLayout(requireContext());
        titleColumn.setOrientation(LinearLayout.VERTICAL);
        heading.setTextColor(green);
        heading.setContentDescription("Open item details");
        heading.setClickable(true);
        heading.setFocusable(true);
        heading.setOnClickListener(v -> viewItem(currentItemId));
        titleColumn.addView(heading);
        TextView peerLabel = text("Conversation", 12, muted, false);
        titleColumn.addView(peerLabel);
        LinearLayout.LayoutParams h =
                new LinearLayout.LayoutParams(0, -2, 1);
        h.leftMargin = dp(12);
        header.addView(titleColumn, h);
        if (currentItemId == null) {
            db.collection("conversations").document(conversationId).get()
                    .addOnSuccessListener(doc -> {
                        if (!isAdded() || header == null || !doc.getId().equals(conversationId)) return;
                        currentItemId = doc.getString("itemId");
                    });
        }
        markOpenThreadRead();
        final String currentPeer = recipientId;
        final String currentConversation = conversationId;
        final String currentItem = itemTitle == null ? "Chat" : itemTitle;
        db.collection("users").document(currentPeer).get()
                .addOnSuccessListener(personDoc -> {
                    if (!isAdded() || header == null
                            || !currentConversation.equals(conversationId)) return;
                    String peerName = personDoc.getString("name");
                    if (peerName != null && !peerName.trim().isEmpty()) {
                        peerLabel.setText(peerName.trim());
                    }
                });

        body.removeAllViews();

        LinearLayout page = new LinearLayout(requireContext());
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundResource(R.drawable.bg_sharely_page);
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
        // Keep the message column within the visible screen width.
        scroll.addView(
                messages,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

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

                    // Only acknowledge unread notifications once this thread
                    // has been loaded successfully for the signed-in participant.
                    threadLoaded = true;
                    markOpenThreadRead();

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

        // A full-width row gives the bubble a real horizontal constraint.
        // Android otherwise may measure a WRAP_CONTENT bubble too wide and
        // clip the text instead of wrapping it onto subsequent lines.
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(mine ? Gravity.END : Gravity.START);

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        rowParams.bottomMargin = dp(10);
        list.addView(row, rowParams);

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
        bubble.setPadding(dp(13), dp(9), dp(13), dp(9));

        // Use MATCH_PARENT with a large side margin rather than a maximum
        // width on an unbounded TextView. This forces Android to measure
        // every message at a known width, regardless of message length.
        LinearLayout.LayoutParams bubbleParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        bubbleParams.leftMargin = mine ? dp(62) : 0;
        bubbleParams.rightMargin = mine ? 0 : dp(62);
        row.addView(bubble, bubbleParams);

        TextView content = text(
                message,
                15,
                mine ? Color.WHITE : ink,
                false
        );
        content.setSingleLine(false);
        content.setHorizontallyScrolling(false);
        content.setEllipsize(null);
        content.setMaxLines(Integer.MAX_VALUE);
        content.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);
        content.setTextIsSelectable(false);
        bubble.addView(
                content,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView time = text(
                new SimpleDateFormat(
                        "h:mm a",
                        Locale.getDefault()
                ).format(new Date(number(document, "createdAt"))),
                11,
                mine ? Color.rgb(221, 244, 234) : muted,
                false
        );

        LinearLayout.LayoutParams timeParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        timeParams.topMargin = dp(5);
        bubble.addView(time, timeParams);
    }

    private void sendMessage(EditText input, Button send) {
        if (sending
                || conversationId == null
                || recipientId == null
                || uid == null) {
            return;
        }

        // Read the complete composer contents (not just the visible line).
        final String value = input.getText().toString().trim();

        if (value.isEmpty()) return;

        if (value.length() > 1200) {
            input.setError("Maximum 1,200 characters");
            return;
        }

        sending = true;
        send.setEnabled(false);
        input.setEnabled(false);

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
                input.setEnabled(true);
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
                input.setEnabled(true);
                send.setEnabled(true);
                toast(
                        "Message not sent: " + error.getMessage()
                );
            }
        });
    }
}
