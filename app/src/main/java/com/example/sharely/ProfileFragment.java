package com.example.sharely;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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


    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {

        super.onViewCreated(
                view,
                savedInstanceState
        );


        TextView profileName =
                view.findViewById(
                        R.id.textProfileName
                );

        TextView profileEmail =
                view.findViewById(
                        R.id.textProfileEmail
                );

        TextView logoutButton =
                view.findViewById(
                        R.id.buttonLogout
                );


        firebaseAuth =
                FirebaseAuth.getInstance();


        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();


        // -----------------------------------------------------
        // CURRENT FIREBASE USER
        // -----------------------------------------------------

        if (currentUser != null) {

            String email =
                    currentUser.getEmail();

            if (email != null) {

                profileEmail.setText(
                        email
                );
            }

            String displayName =
                    currentUser.getDisplayName();

            if (displayName != null
                    && !displayName.trim().isEmpty()) {

                profileName.setText(
                        displayName
                );
            }
        }


        // -----------------------------------------------------
        // LOG OUT
        // -----------------------------------------------------

        logoutButton.setOnClickListener(v -> {

            firebaseAuth.signOut();

            Intent intent =
                    new Intent(
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