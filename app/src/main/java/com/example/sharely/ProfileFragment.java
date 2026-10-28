package com.example.sharely;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sharely.data.ReviewRepository;
import com.example.sharely.enums.ReviewRole;
import com.example.sharely.model.Review;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final ReviewRepository reviews = new ReviewRepository();

    private TextView profileName, avatar, profileEmail;
    private TextView borrowerRating, ownerRating, borrowerCount, ownerCount;
    private EditText nameInput;
    private TextView saveButton;
    private View profileContent;
    private String displayedName = "";
    private boolean savingName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup parent, Bundle state) {
        return inflater.inflate(R.layout.fragment_profile, parent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        profileContent = view.findViewById(R.id.profileContent);
        profileName = view.findViewById(R.id.textProfileName);
        avatar = view.findViewById(R.id.textProfileAvatar);
        profileEmail = view.findViewById(R.id.textProfileEmail);
        nameInput = view.findViewById(R.id.editProfileName);
        saveButton = view.findViewById(R.id.buttonSaveProfileName);
        borrowerRating = view.findViewById(R.id.textBorrowerRating);
        ownerRating = view.findViewById(R.id.textOwnerRating);
        borrowerCount = view.findViewById(R.id.textBorrowerRatingCount);
        ownerCount = view.findViewById(R.id.textOwnerRatingCount);

        profileContent.setFocusableInTouchMode(true);
        profileContent.requestFocus();
        setupBackgroundDismiss(view);

        nameInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                endEditing();
                return true;
            }
            return false;
        });
        saveButton.setOnClickListener(v -> saveName());
        view.findViewById(R.id.buttonLogout).setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh whenever returning to Me, including after a review was received.
        loadProfile();
        loadRatings();
    }

    private void showName(String name) {
        String clean = name == null ? "" : name.trim();
        displayedName = clean;
        String shown = clean.isEmpty() ? "Sharely user" : clean;
        profileName.setText(shown);
        avatar.setText(shown.substring(0, 1).toUpperCase(Locale.getDefault()));
        if (!savingName && !nameInput.hasFocus()) nameInput.setText(clean);
    }

    private void loadProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            showName("");
            profileEmail.setText("Not signed in");
            return;
        }
        final String uid = user.getUid();
        profileEmail.setText(user.getEmail() == null ? "" : user.getEmail());
        showName(user.getDisplayName());

        // The name entered at registration is stored in users/{uid}.name.
        // This is also what Messages displays for other participants.
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || auth.getCurrentUser() == null
                            || !uid.equals(auth.getCurrentUser().getUid())) return;
                    String firestoreName = doc.getString("name");
                    if (firestoreName != null && !firestoreName.trim().isEmpty()) {
                        showName(firestoreName);
                    }
                })
                .addOnFailureListener(error -> {
                    if (isAdded()) android.util.Log.w("SharelyProfile",
                            "Unable to fetch display name", error);
                });
    }

    private void loadRatings() {
        FirebaseUser user = auth.getCurrentUser();
        borrowerRating.setText("—");
        ownerRating.setText("—");
        borrowerCount.setText("No reviews yet");
        ownerCount.setText("No reviews yet");
        if (user == null) return;

        final String uid = user.getUid();
        reviews.getReviewsByUser(uid, new ReviewRepository.OnReviewsLoadedListener() {
            @Override
            public void onSuccess(List<Review> received) {
                if (!isAdded() || auth.getCurrentUser() == null
                        || !uid.equals(auth.getCurrentUser().getUid())) return;
                int borrowerTotal = 0, borrowerN = 0, ownerTotal = 0, ownerN = 0;
                for (Review review : received) {
                    if (review.getRevieweeRole() == ReviewRole.BORROWER) {
                        borrowerTotal += review.getRating();
                        borrowerN++;
                    } else if (review.getRevieweeRole() == ReviewRole.OWNER) {
                        ownerTotal += review.getRating();
                        ownerN++;
                    }
                }
                showRating(borrowerRating, borrowerCount, borrowerTotal, borrowerN);
                showRating(ownerRating, ownerCount, ownerTotal, ownerN);
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    borrowerCount.setText("Could not load ratings");
                    ownerCount.setText("Could not load ratings");
                }
            }
        });
    }

    private void showRating(TextView score, TextView count, int total, int n) {
        if (n == 0) {
            score.setText("—");
            count.setText("No reviews yet");
        } else {
            score.setText(String.format(Locale.getDefault(), "★ %.1f", total / (double) n));
            count.setText(n + (n == 1 ? " review" : " reviews"));
        }
    }

    private void saveName() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || savingName) return;
        String newName = nameInput.getText().toString().trim();
        if (newName.isEmpty() || newName.length() > 40) {
            nameInput.setError("Enter a name of 1–40 characters");
            return;
        }
        nameInput.setError(null);
        endEditing();
        savingName = true;
        saveButton.setEnabled(false);
        saveButton.setText("Saving...");
        final String uid = user.getUid();

        // Save the canonical name first so chat sees the change.
        db.collection("users").document(uid).update("name", newName)
                .addOnSuccessListener(unused -> {
                    UserProfileChangeRequest update =
                            new UserProfileChangeRequest.Builder()
                                    .setDisplayName(newName).build();
                    user.updateProfile(update).addOnCompleteListener(task -> {
                        completeSave();
                        if (!isAdded()) return;
                        showName(newName);
                        nameInput.setText(newName);
                        if (task.isSuccessful()) {
                            Toast.makeText(requireContext(),
                                    "Name updated in Profile and Messages",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Name saved in Sharely; Auth sync will need retrying",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .addOnFailureListener(error -> {
                    completeSave();
                    if (isAdded()) Toast.makeText(requireContext(),
                            "Name not saved: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void completeSave() {
        savingName = false;
        if (isAdded()) {
            saveButton.setEnabled(true);
            saveButton.setText("Save changes");
        }
    }

    private void endEditing() {
        nameInput.clearFocus();
        profileContent.setFocusableInTouchMode(true);
        profileContent.requestFocus();
        InputMethodManager keyboard = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(nameInput.getWindowToken(), 0);
    }

    private void setupBackgroundDismiss(View target) {
        if (target == nameInput || target.getId() == R.id.buttonSaveProfileName
                || target.getId() == R.id.buttonLogout) return;
        target.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN && nameInput.hasFocus())
                endEditing();
            return false;
        });
        if (target instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) target;
            for (int i = 0; i < group.getChildCount(); i++)
                setupBackgroundDismiss(group.getChildAt(i));
        }
    }
}
