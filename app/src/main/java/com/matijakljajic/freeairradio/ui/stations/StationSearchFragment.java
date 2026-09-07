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
import com.matijakljajic.freeairradio.ui.shell.AppShellAwareFragment;
import com.matijakljajic.freeairradio.ui.shell.AppShellController;

@SuppressWarnings("unused")
public class StationSearchFragment extends AppShellAwareFragment {

    private static final String STATE_QUERY = "state_query";
    @Nullable
    private EditText searchInput;
    @Nullable
    private View searchButton;
    @Nullable
    private StationSearchResultsFragment searchResultsFragment;
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
        bindSearchResultsFragment();
        bindSearchShell();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_QUERY, currentQuery);
    }

    @Override
    public void onDestroyView() {
        AppShellController appShellController = getAppShellController();
        if (appShellController != null) {
            appShellController.setSearchResultsFragment(null);
        }
        if (searchInput != null) {
            searchInput.setOnEditorActionListener(null);
        }
        if (searchButton != null) {
            searchButton.setOnClickListener(null);
        }
        searchInput = null;
        searchButton = null;
        searchResultsFragment = null;
        super.onDestroyView();
    }

    private void bindSearchResultsFragment() {
        searchResultsFragment = findOrCreateSearchResultsFragment();
        if (searchResultsFragment == null) {
            return;
        }

        AppShellController appShellController = getAppShellController();
        if (appShellController != null) {
            appShellController.setSearchResultsFragment(searchResultsFragment);
        }
        submitCurrentQueryIfNeeded();
    }

    @Nullable
    private StationSearchResultsFragment findOrCreateSearchResultsFragment() {
        FragmentManager childFragmentManager = getChildFragmentManager();
        Fragment fragment = childFragmentManager.findFragmentById(R.id.station_search_results_container);
        if (fragment == null) {
            fragment = new StationSearchResultsFragment();
            childFragmentManager.beginTransaction()
                    .replace(R.id.station_search_results_container, fragment)
                    .commitNow();
        }

        if (fragment instanceof StationSearchResultsFragment) {
            return (StationSearchResultsFragment) fragment;
        }
        return null;
    }

    private void submitSearch() {
        currentQuery = getSearchQuery();
        submitCurrentQueryIfNeeded();
        closeKeyboard();
    }

    private void submitCurrentQueryIfNeeded() {
        if (searchResultsFragment != null) {
            searchResultsFragment.submitQuery(currentQuery);
        }
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

    private void bindSearchShell() {
        AppShellController appShellController = getAppShellController();
        if (appShellController == null) {
            return;
        }

        searchInput = appShellController.getSearchInput();
        searchButton = appShellController.getSearchButton();
        bindSearchInput();
        bindSearchButton();
    }

    private void bindSearchInput() {
        if (searchInput == null) {
            return;
        }
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

    private void bindSearchButton() {
        if (searchButton != null) {
            searchButton.setOnClickListener(v -> submitSearch());
        }
    }
}
