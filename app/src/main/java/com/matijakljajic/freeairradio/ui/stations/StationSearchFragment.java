package com.matijakljajic.freeairradio.ui.stations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.matijakljajic.freeairradio.R;

public class StationSearchFragment extends Fragment {

    private static final String STATE_QUERY = "state_query";

    @Nullable
    private View rootView;
    @Nullable
    private EditText searchInput;
    @NonNull
    private String currentQuery = "";

    @Nullable
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
        rootView = view.findViewById(R.id.station_search_root);
        searchInput = view.findViewById(R.id.station_search_input);
        Button searchButton = view.findViewById(R.id.station_search_button);

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

        searchButton.setOnClickListener(v -> submitSearch());

        ensureStationListFragment();

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            rootView.setPadding(
                    rootView.getPaddingLeft(),
                    systemBars.top,
                    rootView.getPaddingRight(),
                    rootView.getPaddingBottom()
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(rootView);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_QUERY, currentQuery);
    }

    @Override
    public void onDestroyView() {
        rootView = null;
        searchInput = null;
        super.onDestroyView();
    }

    private void ensureStationListFragment() {
        FragmentManager childFragmentManager = getChildFragmentManager();
        Fragment fragment = childFragmentManager.findFragmentById(R.id.station_search_list_container);
        if (fragment == null) {
            fragment = StationListFragment.newSearchInstance();
            childFragmentManager.beginTransaction()
                    .replace(R.id.station_search_list_container, fragment)
                    .commitNow();
        }

        if (!currentQuery.isEmpty() && fragment instanceof StationListFragment) {
            ((StationListFragment) fragment).submitQuery(currentQuery);
        }
    }

    private void submitSearch() {
        currentQuery = getSearchQuery();
        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.station_search_list_container);
        if (fragment instanceof StationListFragment) {
            ((StationListFragment) fragment).submitQuery(currentQuery);
        }
    }

    @NonNull
    private String getSearchQuery() {
        if (searchInput == null || searchInput.getText() == null) {
            return "";
        }
        return searchInput.getText().toString().trim();
    }
}
