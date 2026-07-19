package com.matijakljajic.freeairradio.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.AttrRes;
import androidx.annotation.StringRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.android.material.radiobutton.MaterialRadioButton;
import com.matijakljajic.freeairradio.BuildConfig;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.serverselection.RadioBrowserServerDirectory;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.serverselection.RadioBrowserServerSettings;
import com.matijakljajic.freeairradio.data.repository.LibraryRepository;
import com.matijakljajic.freeairradio.playback.AudioInterruptionSettings;
import com.matijakljajic.freeairradio.ui.homepage.HomePageSource;
import com.matijakljajic.freeairradio.ui.shell.ShellChromeAwareFragment;
import com.matijakljajic.freeairradio.ui.util.UiDimensions;

import java.util.List;

@SuppressWarnings("unused")
public class SettingsFragment extends ShellChromeAwareFragment {

    private static final String SETTINGS_RESET_DIALOG_TAG = "settings_reset_dialog";
    private static final String PROJECT_GITHUB_URL = "https://github.com/matijakljajic/freeair-radio";
    private static final String PROJECT_GITHUB_ISSUES_URL = PROJECT_GITHUB_URL + "/issues";
    private static final String SUPPORT_EMAIL_ADDRESS = "freeair-radio@matijakljajic.com";
    private static final String SUPPORT_EMAIL_SUBJECT_PREFIX = "[FreeAir Radio] ";

    @Nullable
    private View settingsRootView;
    @Nullable
    private RadioBrowserServerSettings serverSettings;
    @Nullable
    private LibraryRepository libraryRepository;
    @Nullable
    private AppThemeSettings appThemeSettings;
    @Nullable
    private HomePageSettings homePageSettings;
    @Nullable
    private AudioInterruptionSettings audioInterruptionSettings;
    @Nullable
    private RadioGroup serverSelectionGroup;
    @Nullable
    private RadioGroup themeSelectionGroup;
    @Nullable
    private RadioGroup homePageDefaultSelectionGroup;
    @Nullable
    private RadioGroup audioInterruptionSelectionGroup;
    @Nullable
    private TextView serverStatusText;
    @Nullable
    private TextView supportEmailText;
    @Nullable
    private TextView versionText;
    @Nullable
    private Button supportStarButton;
    @Nullable
    private Button supportIssueButton;
    @Nullable
    private Button resetButton;
    @Nullable
    private Button clearFavoritesButton;
    @Nullable
    private Button clearLocalStationsButton;
    @Nullable
    private Button clearRecentlyPlayedButton;
    private int serverLoadRequestId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindRootPadding(view);
        initDependencies();
        bindViews(view);
        bindResetDialogResults();
        bindSupportSection();
        bindThemeSection();
        bindPlaybackSection();
        bindHomePageSection();
        bindServerSection();
        bindResetSection();
    }

    @Override
    public void onDestroyView() {
        serverLoadRequestId++;
        detachRootPadding();
        clearListeners();
        clearReferences();
        super.onDestroyView();
    }

    private void bindRootPadding(@NonNull View view) {
        settingsRootView = view.findViewById(R.id.settings_root);
        if (settingsRootView != null) {
            attachShellContentPadding(
                    settingsRootView,
                    UiDimensions.px(requireContext(), R.dimen.top_content_gap)
            );
        }
    }

    private void detachRootPadding() {
        if (settingsRootView != null) {
            detachShellContentPadding(settingsRootView);
        }
    }

    private void initDependencies() {
        serverSettings = new RadioBrowserServerSettings(requireContext());
        libraryRepository = LibraryRepository.getInstance(requireContext());
        appThemeSettings = new AppThemeSettings(requireContext());
        homePageSettings = new HomePageSettings(requireContext());
        audioInterruptionSettings = new AudioInterruptionSettings(requireContext());
    }

    private void bindViews(@NonNull View view) {
        serverStatusText = view.findViewById(R.id.server_status_text);
        serverSelectionGroup = view.findViewById(R.id.server_selection_group);
        themeSelectionGroup = view.findViewById(R.id.theme_selection_group);
        homePageDefaultSelectionGroup = view.findViewById(R.id.homepage_default_selection_group);
        audioInterruptionSelectionGroup = view.findViewById(R.id.audio_interruption_selection_group);
        versionText = view.findViewById(R.id.settings_version_text);
        supportEmailText = view.findViewById(R.id.settings_support_email_text);
        supportStarButton = view.findViewById(R.id.settings_support_star_button);
        supportIssueButton = view.findViewById(R.id.settings_support_issue_button);
        resetButton = view.findViewById(R.id.server_reset_button);
        clearFavoritesButton = view.findViewById(R.id.settings_clear_favorites_button);
        clearLocalStationsButton = view.findViewById(R.id.settings_clear_local_stations_button);
        clearRecentlyPlayedButton = view.findViewById(R.id.settings_clear_recently_played_button);
        if (versionText != null) {
            versionText.setText(BuildConfig.VERSION_NAME);
        }
    }

    private void bindResetDialogResults() {
        getChildFragmentManager().setFragmentResultListener(
                SettingsResetDialogFragment.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> onResetConfirmed(result.getString(SettingsResetDialogFragment.RESULT_KEY_ACTION))
        );
    }

    private void clearListeners() {
        if (serverSelectionGroup != null) {
            serverSelectionGroup.setOnCheckedChangeListener(null);
        }
        if (themeSelectionGroup != null) {
            themeSelectionGroup.setOnCheckedChangeListener(null);
        }
        if (homePageDefaultSelectionGroup != null) {
            homePageDefaultSelectionGroup.setOnCheckedChangeListener(null);
        }
        if (audioInterruptionSelectionGroup != null) {
            audioInterruptionSelectionGroup.setOnCheckedChangeListener(null);
        }
        if (supportEmailText != null) {
            supportEmailText.setOnClickListener(null);
        }
        if (supportStarButton != null) {
            supportStarButton.setOnClickListener(null);
        }
        if (supportIssueButton != null) {
            supportIssueButton.setOnClickListener(null);
        }
        if (resetButton != null) {
            resetButton.setOnClickListener(null);
        }
        if (clearFavoritesButton != null) {
            clearFavoritesButton.setOnClickListener(null);
        }
        if (clearLocalStationsButton != null) {
            clearLocalStationsButton.setOnClickListener(null);
        }
        if (clearRecentlyPlayedButton != null) {
            clearRecentlyPlayedButton.setOnClickListener(null);
        }
    }

    private void clearReferences() {
        serverSettings = null;
        libraryRepository = null;
        appThemeSettings = null;
        homePageSettings = null;
        audioInterruptionSettings = null;
        serverSelectionGroup = null;
        themeSelectionGroup = null;
        homePageDefaultSelectionGroup = null;
        audioInterruptionSelectionGroup = null;
        serverStatusText = null;
        supportEmailText = null;
        versionText = null;
        supportStarButton = null;
        supportIssueButton = null;
        resetButton = null;
        clearFavoritesButton = null;
        clearLocalStationsButton = null;
        clearRecentlyPlayedButton = null;
        settingsRootView = null;
    }

    private void bindSupportSection() {
        if (supportEmailText != null) {
            supportEmailText.setOnClickListener(v -> openEmailComposer());
        }
        if (supportStarButton != null) {
            supportStarButton.setOnClickListener(v -> openExternalUrl(PROJECT_GITHUB_URL));
        }
        if (supportIssueButton != null) {
            supportIssueButton.setOnClickListener(v -> openExternalUrl(PROJECT_GITHUB_ISSUES_URL));
        }
    }

    private void bindThemeSection() {
        bindThemeSelection();
    }

    private void bindPlaybackSection() {
        bindAudioInterruptionSelection();
    }

    private void bindHomePageSection() {
        bindHomePageDefaultSelection();
    }

    private void bindServerSection() {
        bindServerResetButton();
        loadServerChoices();
    }

    private void bindResetSection() {
        bindLibraryResetButton(
                clearFavoritesButton,
                SettingsResetDialogFragment.ACTION_CLEAR_FAVORITES,
                R.string.settings_clear_favorites_title,
                R.string.settings_clear_favorites_message
        );
        bindLibraryResetButton(
                clearLocalStationsButton,
                SettingsResetDialogFragment.ACTION_CLEAR_LOCAL_STATIONS,
                R.string.settings_clear_local_stations_title,
                R.string.settings_clear_local_stations_message
        );
        bindLibraryResetButton(
                clearRecentlyPlayedButton,
                SettingsResetDialogFragment.ACTION_CLEAR_RECENTLY_PLAYED,
                R.string.settings_clear_recently_played_title,
                R.string.settings_clear_recently_played_message
        );
    }

    private void bindHomePageDefaultSelection() {
        if (homePageDefaultSelectionGroup == null || homePageSettings == null) {
            return;
        }

        homePageDefaultSelectionGroup.setOnCheckedChangeListener(null);
        syncSelectedHomePageDefault();
        homePageDefaultSelectionGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (homePageSettings == null) {
                return;
            }

            HomePageSource source = homePageSourceForCheckedId(checkedId);
            if (source == null || homePageSettings.getDefaultSource() == source) {
                return;
            }

            homePageSettings.setDefaultSource(source);
        });
    }

    private void bindAudioInterruptionSelection() {
        if (audioInterruptionSelectionGroup == null || audioInterruptionSettings == null) {
            return;
        }

        audioInterruptionSelectionGroup.setOnCheckedChangeListener(null);
        syncSelectedAudioInterruptionMode();
        audioInterruptionSelectionGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (audioInterruptionSettings == null) {
                return;
            }

            Boolean respectAudioInterruptions = respectAudioInterruptionsForCheckedId(checkedId);
            if (respectAudioInterruptions == null
                    || audioInterruptionSettings.shouldRespectAudioInterruptions()
                    == respectAudioInterruptions) {
                return;
            }

            audioInterruptionSettings.setRespectAudioInterruptions(respectAudioInterruptions);
        });
    }

    private void bindThemeSelection() {
        if (themeSelectionGroup == null || appThemeSettings == null) {
            return;
        }

        themeSelectionGroup.setOnCheckedChangeListener(null);
        syncSelectedTheme();
        themeSelectionGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (appThemeSettings == null) {
                return;
            }

            int desiredNightMode = nightModeForCheckedId(checkedId);
            if (desiredNightMode == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) {
                return;
            }

            if (appThemeSettings.getNightMode() == desiredNightMode) {
                return;
            }

            appThemeSettings.setNightMode(desiredNightMode);
            AppCompatDelegate.setDefaultNightMode(desiredNightMode);
        });
    }

    private void syncSelectedTheme() {
        if (themeSelectionGroup == null || appThemeSettings == null) {
            return;
        }

        switch (appThemeSettings.getNightMode()) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                themeSelectionGroup.check(R.id.theme_selection_light);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                themeSelectionGroup.check(R.id.theme_selection_dark);
                break;
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
            default:
                themeSelectionGroup.check(R.id.theme_selection_auto);
                break;
        }
    }

    private void syncSelectedHomePageDefault() {
        if (homePageDefaultSelectionGroup == null || homePageSettings == null) {
            return;
        }

        switch (homePageSettings.getDefaultSource()) {
            case FAVORITES:
                homePageDefaultSelectionGroup.check(R.id.homepage_default_selection_favorites);
                break;
            case LOCAL_STATIONS:
                homePageDefaultSelectionGroup.check(R.id.homepage_default_selection_local_stations);
                break;
            case NOW_POPULAR:
            default:
                homePageDefaultSelectionGroup.check(R.id.homepage_default_selection_now_popular);
                break;
        }
    }

    private void syncSelectedAudioInterruptionMode() {
        if (audioInterruptionSelectionGroup == null || audioInterruptionSettings == null) {
            return;
        }

        audioInterruptionSelectionGroup.check(audioInterruptionSettings.shouldRespectAudioInterruptions()
                ? R.id.audio_interruption_selection_respect
                : R.id.audio_interruption_selection_keep_playing);
    }

    private void loadServerChoices() {
        if (serverSelectionGroup == null) {
            return;
        }

        List<String> cachedBaseUrls =
                RadioBrowserServerDirectory.getCachedServers();

        if (!cachedBaseUrls.isEmpty()) {
            populateServerChoices(cachedBaseUrls);
            updateServerStatus(cachedBaseUrls.size());
            return;
        }

        loadServerChoicesAsync(false);
    }

    private void loadServerChoicesAsync(boolean forceRefresh) {
        int requestId = ++serverLoadRequestId;
        updateServerStatus(-1);

        new Thread(() -> {
            List<String> discoveredBaseUrls = RadioBrowserServerDirectory.loadServers(forceRefresh);
            postServerChoices(requestId, discoveredBaseUrls);
        }, forceRefresh ? "RadioBrowserServerSettingsRefresh" : "RadioBrowserServerSettingsLoad").start();
    }

    private void populateServerChoices(@NonNull List<String> serverBaseUrls) {
        if (serverSelectionGroup == null) {
            return;
        }

        serverSelectionGroup.setOnCheckedChangeListener(null);
        while (serverSelectionGroup.getChildCount() > 1) {
            serverSelectionGroup.removeViewAt(1);
        }

        ColorStateList serverButtonTint =
                ColorStateList.valueOf(resolveThemeColor(androidx.appcompat.R.attr.colorPrimary));
        int serverButtonTextColor = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface);

        for (String baseUrl : serverBaseUrls) {
            MaterialRadioButton button = new MaterialRadioButton(requireContext());
            button.setId(View.generateViewId());
            button.setLayoutParams(new RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            button.setText(formatServerLabel(baseUrl));
            button.setTextColor(serverButtonTextColor);
            button.setButtonTintList(serverButtonTint);
            button.setTag(baseUrl);
            serverSelectionGroup.addView(button);
        }

        serverSelectionGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (serverSettings == null) {
                return;
            }

            View checkedView = group.findViewById(checkedId);
            if (checkedView == null) {
                return;
            }

            Object serverTag = checkedView.getTag();
            if (checkedId == R.id.server_selection_auto || serverTag == null) {
                serverSettings.setPreferredBaseUrl(null);
            } else {
                serverSettings.setPreferredBaseUrl(serverTag.toString());
            }
        });

        syncSelectedServer();
    }

    private void bindServerResetButton() {
        if (resetButton == null) {
            return;
        }

        resetButton.setOnClickListener(v -> {
            if (serverSettings != null) {
                serverSettings.setPreferredBaseUrl(null);
            }

            loadServerChoicesAsync(true);
        });
    }

    private void bindLibraryResetButton(@Nullable Button button,
                                        @NonNull String action,
                                        @StringRes int titleResId,
                                        @StringRes int messageResId) {
        if (button == null) {
            return;
        }

        button.setOnClickListener(v -> SettingsResetDialogFragment
                .newInstance(action, titleResId, messageResId)
                .show(getChildFragmentManager(), SETTINGS_RESET_DIALOG_TAG));
    }

    private void onResetConfirmed(@Nullable String action) {
        if (action == null || libraryRepository == null) {
            return;
        }

        switch (action) {
            case SettingsResetDialogFragment.ACTION_CLEAR_FAVORITES:
                runLibraryReset(
                        callback -> libraryRepository.clearFavoriteStations(callback),
                        R.string.settings_clear_favorites_success,
                        R.string.settings_clear_favorites_failure
                );
                return;
            case SettingsResetDialogFragment.ACTION_CLEAR_LOCAL_STATIONS:
                runLibraryReset(
                        callback -> libraryRepository.clearLocalStations(callback),
                        R.string.settings_clear_local_stations_success,
                        R.string.settings_clear_local_stations_failure
                );
                return;
            case SettingsResetDialogFragment.ACTION_CLEAR_RECENTLY_PLAYED:
                runLibraryReset(
                        callback -> libraryRepository.clearRecentlyPlayedStations(callback),
                        R.string.settings_clear_recently_played_success,
                        R.string.settings_clear_recently_played_failure
                );
                return;
            default:
        }
    }

    private void runLibraryReset(@NonNull ResetAction action,
                                 @StringRes int successResId,
                                 @StringRes int failureResId) {
        action.run(new LibraryRepository.WriteCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) {
                    return;
                }
                showToast(successResId);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                showToast(failureResId);
            }
        });
    }

    private void postServerChoices(int requestId, @NonNull List<String> serverBaseUrls) {
        View rootView = settingsRootView;
        if (rootView == null) {
            return;
        }

        rootView.post(() -> {
            if (isStaleServerLoad(requestId)) {
                return;
            }

            populateServerChoices(serverBaseUrls);
            updateServerStatus(serverBaseUrls.size());
        });
    }

    private void syncSelectedServer() {
        if (serverSelectionGroup == null || serverSettings == null) {
            return;
        }

        String preferredBaseUrl = serverSettings.getPreferredBaseUrl();
        if (preferredBaseUrl == null) {
            serverSelectionGroup.check(R.id.server_selection_auto);
            return;
        }

        for (int i = 0; i < serverSelectionGroup.getChildCount(); i++) {
            View child = serverSelectionGroup.getChildAt(i);
            Object serverTag = child.getTag();
            if (preferredBaseUrl.equals(serverTag)) {
                serverSelectionGroup.check(child.getId());
                return;
            }
        }

        serverSelectionGroup.check(R.id.server_selection_auto);
    }

    private void updateServerStatus(int serverCount) {
        if (serverStatusText == null) {
            return;
        }

        if (serverCount < 0) {
            serverStatusText.setText(R.string.settings_server_status_loading);
        } else if (serverCount == 0) {
            serverStatusText.setText(R.string.settings_server_status_empty);
        } else {
            serverStatusText.setText(getResources().getQuantityString(
                    R.plurals.settings_server_status_count,
                    serverCount,
                    serverCount
            ));
        }
    }

    private int nightModeForCheckedId(int checkedId) {
        if (checkedId == R.id.theme_selection_light) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if (checkedId == R.id.theme_selection_dark) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        if (checkedId == R.id.theme_selection_auto) {
            return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        return AppCompatDelegate.MODE_NIGHT_UNSPECIFIED;
    }

    @Nullable
    private HomePageSource homePageSourceForCheckedId(int checkedId) {
        if (checkedId == R.id.homepage_default_selection_now_popular) {
            return HomePageSource.NOW_POPULAR;
        }
        if (checkedId == R.id.homepage_default_selection_favorites) {
            return HomePageSource.FAVORITES;
        }
        if (checkedId == R.id.homepage_default_selection_local_stations) {
            return HomePageSource.LOCAL_STATIONS;
        }
        return null;
    }

    @Nullable
    private Boolean respectAudioInterruptionsForCheckedId(int checkedId) {
        if (checkedId == R.id.audio_interruption_selection_respect) {
            return Boolean.TRUE;
        }
        if (checkedId == R.id.audio_interruption_selection_keep_playing) {
            return Boolean.FALSE;
        }
        return null;
    }

    private boolean isStaleServerLoad(int requestId) {
        return requestId != serverLoadRequestId || !isAdded() || settingsRootView == null;
    }

    private interface ResetAction {
        void run(@NonNull LibraryRepository.WriteCallback callback);
    }

    private void showToast(@StringRes int messageResId) {
        Toast.makeText(requireContext(), messageResId, Toast.LENGTH_SHORT).show();
    }

    private void openExternalUrl(@NonNull String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void openEmailComposer() {
        startActivity(new Intent(Intent.ACTION_SENDTO)
                .setData(Uri.parse(
                        "mailto:" + SUPPORT_EMAIL_ADDRESS
                                + "?subject=" + Uri.encode(SUPPORT_EMAIL_SUBJECT_PREFIX)))
                .putExtra(Intent.EXTRA_SUBJECT, SUPPORT_EMAIL_SUBJECT_PREFIX));
    }

    private int resolveThemeColor(@AttrRes int attrResId) {
        TypedValue typedValue = new TypedValue();
        if (!requireContext().getTheme().resolveAttribute(attrResId, typedValue, true)) {
            throw new IllegalStateException("Missing theme color attribute: " + attrResId);
        }
        if (typedValue.resourceId != 0) {
            return ContextCompat.getColor(requireContext(), typedValue.resourceId);
        }
        return typedValue.data;
    }

    @NonNull
    private String formatServerLabel(@NonNull String baseUrl) {
        String label = baseUrl;
        if (label.startsWith("https://")) {
            label = label.substring("https://".length());
        } else if (label.startsWith("http://")) {
            label = label.substring("http://".length());
        }
        if (label.endsWith("/")) {
            label = label.substring(0, label.length() - 1);
        }
        return label;
    }
}
