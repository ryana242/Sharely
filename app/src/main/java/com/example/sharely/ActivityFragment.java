package com.example.sharely;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class ActivityFragment extends Fragment {

    public ActivityFragment() {
    }


    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        return inflater.inflate(
                R.layout.fragment_activity,
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


        TextView borrowingTab =
                view.findViewById(
                        R.id.tabBorrowing
                );

        TextView listingsTab =
                view.findViewById(
                        R.id.tabListings
                );

        LinearLayout borrowingsContent =
                view.findViewById(
                        R.id.borrowingsContent
                );

        LinearLayout listingsContent =
                view.findViewById(
                        R.id.listingsContent
                );


        // Borrowing is selected initially
        showBorrowing(
                borrowingTab,
                listingsTab,
                borrowingsContent,
                listingsContent
        );


        borrowingTab.setOnClickListener(v -> {

            showBorrowing(
                    borrowingTab,
                    listingsTab,
                    borrowingsContent,
                    listingsContent
            );
        });


        listingsTab.setOnClickListener(v -> {

            showListings(
                    borrowingTab,
                    listingsTab,
                    borrowingsContent,
                    listingsContent
            );
        });
    }


    private void showBorrowing(
            TextView borrowingTab,
            TextView listingsTab,
            LinearLayout borrowingsContent,
            LinearLayout listingsContent
    ) {

        borrowingsContent.setVisibility(
                View.VISIBLE
        );

        listingsContent.setVisibility(
                View.GONE
        );


        borrowingTab.setBackgroundResource(
                R.drawable.button_primary
        );

        borrowingTab.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        android.R.color.white
                )
        );


        listingsTab.setBackgroundResource(
                R.drawable.button_secondary
        );

        listingsTab.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        R.color.sharely_primary
                )
        );
    }


    private void showListings(
            TextView borrowingTab,
            TextView listingsTab,
            LinearLayout borrowingsContent,
            LinearLayout listingsContent
    ) {

        borrowingsContent.setVisibility(
                View.GONE
        );

        listingsContent.setVisibility(
                View.VISIBLE
        );


        listingsTab.setBackgroundResource(
                R.drawable.button_primary
        );

        listingsTab.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        android.R.color.white
                )
        );


        borrowingTab.setBackgroundResource(
                R.drawable.button_secondary
        );

        borrowingTab.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        R.color.sharely_primary
                )
        );
    }
}