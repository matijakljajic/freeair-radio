package com.matijakljajic.freeairradio.ui.stations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.ui.ShellChromeAwareFragment;
import com.matijakljajic.freeairradio.ui.ShellChromeController;

public class StationSearchFragment extends ShellChromeAwareFragment {

    private static final String STATE_QUERY = "state_query";
    @Nullable
    private EditText searchInput;
    @Nullable
    private View searchButton;
    @Nullable
    private StationListFragment stationListFragment;
    @NonNull
    private String currentQuery = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_station_search, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            currentQuery = savedInstanceState.getString(STATE_QUERY, "");
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ensureStationListFragment();
        bindSearchControls();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_QUERY, currentQuery);
    }

    @Override
    public void onDestroyView() {
        ShellChromeController shellChromeController = getShellChromeController();
        if (shellChromeController != null) {
            shellChromeController.setFloaterStationListFragment(null);
        }
        if (searchInput != null) {
            searchInput.setOnEditorActionListener(null);
        }
        if (searchButton != null) {
            searchButton.setOnClickListener(null);
        }
        searchInput = null;
        searchButton = null;
        stationListFragment = null;
        super.onDestroyView();
    }

    private void ensureStationListFragment() {
        FragmentManager childFragmentManager = getChildFragmentManager();
        Fragment fragment = childFragmentManager.findFragmentById(R.id.station_search_list_container);
        if (fragment == null) {
            fragment = new StationListFragment();
            childFragmentManager.beginTransaction()
                    .replace(R.id.station_search_list_container, fragment)
                    .commitNow();
        }

        if (fragment instanceof StationListFragment) {
            stationListFragment = (StationListFragment) fragment;
            ShellChromeController shellChromeController = getShellChromeController();
            if (shellChromeController != null) {
                shellChromeController.setFloaterStationListFragment(stationListFragment);
            }
            if (!currentQuery.isEmpty()) {
                stationListFragment.submitQuery(currentQuery);
            }
        }
    }

    private void submitSearch() {
        currentQuery = getSearchQuery();
        if (stationListFragment != null) {
            stationListFragment.submitQuery(currentQuery);
        }
        closeKeyboard();
    }

    private void closeKeyboard() {
        if (searchInput == null) {
            return;
        }

        searchInput.clearFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) requireContext()
                .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }
    }

    @NonNull
    private String getSearchQuery() {
        if (searchInput == null || searchInput.getText() == null) {
            return "";
        }
        return searchInput.getText().toString().trim();
    }

    private void bindSearchControls() {
        ShellChromeController shellChromeController = getShellChromeController();
        if (shellChromeController == null) {
            return;
        }

        searchInput = shellChromeController.getSearchInput();
        searchButton = shellChromeController.getSearchButton();

        if (searchInput != null) {
            searchInput.setText(currentQuery);
            searchInput.setSelection(searchInput.getText() != null ? searchInput.getText().length() : 0);
            searchInput.setOnEditorActionListener((textView, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    submitSearch();
                    return true;
                }
                return false;
            });
        }

        if (searchButton != null) {
            searchButton.setOnClickListener(v -> submitSearch());
        }
    }
}
