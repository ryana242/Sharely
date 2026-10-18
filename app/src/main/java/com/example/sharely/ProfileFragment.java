
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Locale;

public class ProfileFragment extends Fragment {

    private FirebaseAuth firebaseAuth;

    public ProfileFragment() {
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_profile,
                container,
                false
        );
    }

    // =========================================================
    // FINISH EDITING
    // =========================================================

    private void finishNameEditing(
            View profileContent,
            EditText nameField
    ) {

        // Give focus to the page instead of the input.
        nameField.clearFocus();

        profileContent.setFocusableInTouchMode(true);
        profileContent.requestFocus();

        // Hide the keyboard.
        InputMethodManager keyboard =
                (InputMethodManager)
                        requireContext().getSystemService(
                                Context.INPUT_METHOD_SERVICE
                        );

        if (keyboard != null) {
            keyboard.hideSoftInputFromWindow(
                    nameField.getWindowToken(),
                    0
            );
        }
    }

    // =========================================================
    // DISMISS EDITING WHEN TAPPING OUTSIDE THE INPUT
    // =========================================================

    private void setupBackgroundDismiss(
            View target,
            View profileContent,
            EditText nameField
    ) {

        // Do not interfere with typing or action buttons.
        if (target == nameField
                || target.getId() == R.id.buttonSaveProfileName
                || target.getId() == R.id.buttonLogout) {
            return;
        }

        target.setOnTouchListener((v, event) -> {

            if (event.getAction() == MotionEvent.ACTION_DOWN
                    && nameField.hasFocus()) {

                finishNameEditing(
                        profileContent,
                        nameField
                );
            }

            // Allow scrolling and other touches normally.
            return false;
        });

        // Attach the listener to non-interactive child views too.
        if (target instanceof ViewGroup) {

            ViewGroup group = (ViewGroup) target;

            for (int i = 0; i < group.getChildCount(); i++) {

                setupBackgroundDismiss(
                        group.getChildAt(i),
                        profileContent,
                        nameField
                );
            }
        }
    }

    // =========================================================
    // DISPLAY PROFILE NAME AND AVATAR
    // =========================================================

    private void updateProfileDisplay(
            String displayName,
            TextView profileName,
            TextView profileAvatar
    ) {

        String cleanName = displayName == null
                ? ""
                : displayName.trim();

        String nameToShow = cleanName.isEmpty()
                ? "Sharely user"
                : cleanName;

        profileName.setText(nameToShow);

        String initial = new String(
                Character.toChars(
                        nameToShow.codePointAt(0)
                )
        );

        profileAvatar.setText(
                initial.toUpperCase(Locale.getDefault())
        );
    }

    // =========================================================
    // VIEW CREATED
    // =========================================================

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {

        super.onViewCreated(view, savedInstanceState);

        // -----------------------------------------------------
        // VIEWS
        // -----------------------------------------------------

        View profileContent =
                view.findViewById(R.id.profileContent);

        TextView profileName =
                view.findViewById(R.id.textProfileName);

        TextView profileAvatar =
                view.findViewById(R.id.textProfileAvatar);

        TextView profileEmail =
                view.findViewById(R.id.textProfileEmail);

        EditText editProfileName =
                view.findViewById(R.id.editProfileName);

        TextView saveProfileButton =
                view.findViewById(R.id.buttonSaveProfileName);

        TextView logoutButton =
                view.findViewById(R.id.buttonLogout);

        // -----------------------------------------------------
        // INITIAL FOCUS
        // -----------------------------------------------------

        profileContent.setFocusableInTouchMode(true);
        profileContent.requestFocus();

        // Tapping outside the input ends editing.
        setupBackgroundDismiss(
                view,
                profileContent,
                editProfileName
        );

        // Keyboard Done key ends editing.
        editProfileName.setOnEditorActionListener(
                (v, actionId, event) -> {

                    if (actionId == EditorInfo.IME_ACTION_DONE) {

                        finishNameEditing(
                                profileContent,
                                editProfileName
                        );

                        return true;
                    }

                    return false;
                }
        );

        // -----------------------------------------------------
        // FIREBASE AUTHENTICATION
        // -----------------------------------------------------

        firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        // -----------------------------------------------------
        // DISPLAY CURRENT USER
        // -----------------------------------------------------

        if (currentUser != null) {

            String email = currentUser.getEmail();

            profileEmail.setText(
                    email != null ? email : ""
            );

            String displayName =
                    currentUser.getDisplayName();

            updateProfileDisplay(
                    displayName,
                    profileName,
                    profileAvatar
            );

            editProfileName.setText(
                    displayName != null
                            ? displayName.trim()
                            : ""
            );

        } else {

            profileEmail.setText("Not signed in");

            updateProfileDisplay(
                    null,
                    profileName,
                    profileAvatar
            );
        }

        // -----------------------------------------------------
        // SAVE PROFILE NAME
        // -----------------------------------------------------

        saveProfileButton.setOnClickListener(v -> {

            FirebaseUser user =
                    firebaseAuth.getCurrentUser();

            if (user == null) {

                Toast.makeText(
                        requireContext(),
                        "Please log in first",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            String newName = editProfileName
                    .getText()
                    .toString()
                    .trim();

            if (newName.isEmpty()) {

                editProfileName.setError(
                        "Name cannot be empty"
                );

                return;
            }

            editProfileName.setError(null);

            // End editing immediately, before Firebase responds.
            finishNameEditing(
                    profileContent,
                    editProfileName
            );

            UserProfileChangeRequest updates =
                    new UserProfileChangeRequest.Builder()
                            .setDisplayName(newName)
                            .build();

            saveProfileButton.setEnabled(false);

            saveProfileButton.setText("Saving...");

            user.updateProfile(updates)
                    .addOnCompleteListener(task -> {

                        if (!isAdded() || getView() != view) {
                            return;
                        }

                        saveProfileButton.setEnabled(true);
                        saveProfileButton.setText("Save changes");

                        if (task.isSuccessful()) {

                            updateProfileDisplay(
                                    newName,
                                    profileName,
                                    profileAvatar
                            );

                            editProfileName.setText(newName);

                            Toast.makeText(
                                    requireContext(),
                                    "Profile updated successfully",
                                    Toast.LENGTH_SHORT
                            ).show();

                        } else {

                            Toast.makeText(
                                    requireContext(),
                                    "Could not update profile",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
        });

        // -----------------------------------------------------
        // LOG OUT
        // -----------------------------------------------------

        logoutButton.setOnClickListener(v -> {

            firebaseAuth.signOut();

            Intent intent = new Intent(
                    requireContext(),
                    MainActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
        });
    }
}
