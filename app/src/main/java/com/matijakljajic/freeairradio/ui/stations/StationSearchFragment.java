package com.matijakljajic.freeairradio.ui.stations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.ui.MainActivity;
import com.google.android.material.card.MaterialCardView;

public class StationSearchFragment extends Fragment {

    private static final String STATE_QUERY = "state_query";
    private static final int SEARCH_TOP_GAP_DP = 10;
    private static final int SEARCH_LIST_GAP_DP = 12;
    private static final int SEARCH_LIST_BOTTOM_GAP_DP = 24;

    @Nullable
    private View rootView;
    @Nullable
    private View searchShellOverlayContainer;
    @Nullable
    private MaterialCardView searchShellView;
    @Nullable
    private EditText searchInput;
    @Nullable
    private StationListFragment stationListFragment;
    @NonNull
    private String currentQuery = "";
    private final View.OnLayoutChangeListener searchShellLayoutChangeListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateSearchListPadding();

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
        searchShellOverlayContainer = requireActivity().findViewById(R.id.search_shell_overlay_container);
        attachSearchShell();

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

        ensureStationListFragment();

        if (rootView != null && searchShellView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                applySearchShellTopMargin(systemBars.top);
                updateSearchListPadding();
                return insets;
            });
            ViewCompat.requestApplyInsets(rootView);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_QUERY, currentQuery);
    }

    @Override
    public void onDestroyView() {
        if (searchShellView != null) {
            searchShellView.removeOnLayoutChangeListener(searchShellLayoutChangeListener);
            if (searchShellOverlayContainer instanceof ViewGroup) {
                ((ViewGroup) searchShellOverlayContainer).removeView(searchShellView);
            }
        }
        resetTopContentFilterHeight();
        rootView = null;
        searchShellOverlayContainer = null;
        searchShellView = null;
        searchInput = null;
        stationListFragment = null;
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

        if (fragment instanceof StationListFragment) {
            stationListFragment = (StationListFragment) fragment;
            updateSearchListPadding();
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

    private void updateSearchListPadding() {
        if (searchShellView == null || stationListFragment == null) {
            return;
        }
        int desiredTopPaddingPx = searchShellView.getBottom() + dpToPx(SEARCH_LIST_GAP_DP);
        stationListFragment.setSearchTopPaddingPx(desiredTopPaddingPx);
        stationListFragment.setBottomRecyclerGapPx(dpToPx(SEARCH_LIST_BOTTOM_GAP_DP));
        setTopContentFilterHeightPx(desiredTopPaddingPx);
    }

    private void applySearchShellTopMargin(int statusBarInsetPx) {
        if (searchShellView == null) {
            return;
        }

        ViewGroup.LayoutParams layoutParams = searchShellView.getLayoutParams();
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }

        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
        int desiredTopMarginPx = statusBarInsetPx + dpToPx(SEARCH_TOP_GAP_DP);
        if (marginLayoutParams.topMargin != desiredTopMarginPx) {
            marginLayoutParams.topMargin = desiredTopMarginPx;
            searchShellView.setLayoutParams(marginLayoutParams);
        }
    }

    @NonNull
    private String getSearchQuery() {
        if (searchInput == null || searchInput.getText() == null) {
            return "";
        }
        return searchInput.getText().toString().trim();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private void setTopContentFilterHeightPx(int heightPx) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setTopContentFilterHeightPx(heightPx);
        }
    }

    private void resetTopContentFilterHeight() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).resetTopContentFilterHeight();
        }
    }

    private void attachSearchShell() {
        if (!(searchShellOverlayContainer instanceof ViewGroup)) {
            return;
        }

        ViewGroup overlayContainer = (ViewGroup) searchShellOverlayContainer;
        View shellView = LayoutInflater.from(requireContext())
                .inflate(R.layout.view_station_search_shell, overlayContainer, false);
        overlayContainer.addView(shellView);
        searchShellView = (MaterialCardView) shellView;
        searchInput = searchShellView.findViewById(R.id.station_search_input);
        View searchButton = searchShellView.findViewById(R.id.station_search_button);

        if (searchShellView != null) {
            searchShellView.addOnLayoutChangeListener(searchShellLayoutChangeListener);
        }
        if (searchButton != null) {
            searchButton.setOnClickListener(v -> submitSearch());
        }
    }
}
