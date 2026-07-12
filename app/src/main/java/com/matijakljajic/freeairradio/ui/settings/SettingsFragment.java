package com.matijakljajic.freeairradio.ui.settings;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.android.material.radiobutton.MaterialRadioButton;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.serverselection.RadioBrowserServerDirectory;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.serverselection.RadioBrowserServerSettings;
import com.matijakljajic.freeairradio.ui.shell.ShellChromeAwareFragment;
import com.matijakljajic.freeairradio.ui.util.UiDimensions;

import java.util.List;

@SuppressWarnings("unused")
public class SettingsFragment extends ShellChromeAwareFragment {

    @Nullable
    private View settingsRootView;
    @Nullable
    private RadioBrowserServerSettings serverSettings;
    @Nullable
    private AppThemeSettings appThemeSettings;
    @Nullable
    private RadioGroup serverSelectionGroup;
    @Nullable
    private RadioGroup themeSelectionGroup;
    @Nullable
    private TextView serverStatusText;
    @Nullable
    private Button resetButton;
    private int serverLoadRequestId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        settingsRootView = view.findViewById(R.id.settings_root);
        if (settingsRootView != null) {
            attachShellContentPadding(
                    settingsRootView,
                    UiDimensions.px(requireContext(), R.dimen.top_content_gap)
            );
        }
        serverSettings = new RadioBrowserServerSettings(requireContext());
        appThemeSettings = new AppThemeSettings(requireContext());
        serverStatusText = view.findViewById(R.id.server_status_text);
        serverSelectionGroup = view.findViewById(R.id.server_selection_group);
        themeSelectionGroup = view.findViewById(R.id.theme_selection_group);
        resetButton = view.findViewById(R.id.server_reset_button);
        bindResetButton();
        bindThemeSelection();
        loadServerChoices();
    }

    @Override
    public void onDestroyView() {
        serverLoadRequestId++;
        if (settingsRootView != null) {
            detachShellContentPadding(settingsRootView);
        }
        if (serverSelectionGroup != null) {
            serverSelectionGroup.setOnCheckedChangeListener(null);
        }
        if (themeSelectionGroup != null) {
            themeSelectionGroup.setOnCheckedChangeListener(null);
        }
        if (resetButton != null) {
            resetButton.setOnClickListener(null);
        }
        serverSettings = null;
        appThemeSettings = null;
        serverSelectionGroup = null;
        themeSelectionGroup = null;
        serverStatusText = null;
        resetButton = null;
        settingsRootView = null;
        super.onDestroyView();
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
            List<String> discoveredBaseUrls = RadioBrowserServerDirectory.getCachedServers();
            if (forceRefresh || discoveredBaseUrls.isEmpty()) {
                RadioBrowserServerDirectory.refresh();
                discoveredBaseUrls =
                        RadioBrowserServerDirectory.getCachedServers();
            }
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

        ColorStateList serverButtonTint = createRadioButtonTintList();
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

    private void bindResetButton() {
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

    private boolean isStaleServerLoad(int requestId) {
        return requestId != serverLoadRequestId || !isAdded() || settingsRootView == null;
    }

    @NonNull
    private ColorStateList createRadioButtonTintList() {
        int checkedColor = resolveThemeColor(androidx.appcompat.R.attr.colorPrimary);
        int uncheckedColor = resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant);
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_enabled, android.R.attr.state_checked},
                        new int[]{android.R.attr.state_enabled, -android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_enabled}
                },
                new int[]{
                        checkedColor,
                        uncheckedColor,
                        uncheckedColor
                }
        );
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
