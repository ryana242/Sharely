package com.example.sharely;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        // Required empty constructor
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        return inflater.inflate(
                R.layout.fragment_home,
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


        // -----------------------------------------------------
        // FIND VIEWS
        // -----------------------------------------------------

        LinearLayout browseButton =
                view.findViewById(
                        R.id.buttonBrowseHome
                );

        LinearLayout listButton =
                view.findViewById(
                        R.id.buttonListHome
                );

        View searchHome =
                view.findViewById(
                        R.id.searchHome
                );

        View seeAll =
                view.findViewById(
                        R.id.textSeeAll
                );

        View profileButton =
                view.findViewById(
                        R.id.buttonProfileHome
                );


        // -----------------------------------------------------
        // FIND SOMETHING → BROWSE TAB
        // -----------------------------------------------------

        browseButton.setOnClickListener(v -> {

            MainAppActivity mainActivity =
                    (MainAppActivity) requireActivity();

            mainActivity.openBrowse(
                    false
            );
        });


        // -----------------------------------------------------
        // SEARCH → BROWSE TAB
        // -----------------------------------------------------

        searchHome.setOnClickListener(v -> {

            MainAppActivity mainActivity =
                    (MainAppActivity) requireActivity();

            mainActivity.openBrowse(
                    true
            );
        });


        // -----------------------------------------------------
        // SEE ALL → BROWSE TAB
        // -----------------------------------------------------

        seeAll.setOnClickListener(v -> {

            MainAppActivity mainActivity =
                    (MainAppActivity) requireActivity();

            mainActivity.openBrowse(
                    false
            );
        });


        // -----------------------------------------------------
        // LIST ITEM → ADD ITEM ACTIVITY
        // -----------------------------------------------------

        listButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            requireContext(),
                            AddItemActivity.class
                    );

            startActivity(intent);
        });


        // -----------------------------------------------------
        // PROFILE → ME TAB
        // -----------------------------------------------------

        profileButton.setOnClickListener(v -> {

            MainAppActivity mainActivity =
                    (MainAppActivity) requireActivity();

            mainActivity.selectTab(
                    R.id.navProfile
            );
        });
    }
}