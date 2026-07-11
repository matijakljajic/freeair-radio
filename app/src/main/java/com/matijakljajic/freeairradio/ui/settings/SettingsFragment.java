package com.matijakljajic.freeairradio.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.serverselection.RadioBrowserServerDirectory;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.serverselection.RadioBrowserServerSettings;
import com.matijakljajic.freeairradio.ui.ShellChromeAwareFragment;
import com.matijakljajic.freeairradio.ui.util.UiDimensions;

import java.util.List;

public class SettingsFragment extends ShellChromeAwareFragment {

    @Nullable
    private View settingsRootView;
    @Nullable
    private RadioBrowserServerSettings serverSettings;
    @Nullable
    private RadioGroup serverSelectionGroup;
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
        serverStatusText = view.findViewById(R.id.server_status_text);
        serverSelectionGroup = view.findViewById(R.id.server_selection_group);
        resetButton = view.findViewById(R.id.server_reset_button);
        bindResetButton();
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
        if (resetButton != null) {
            resetButton.setOnClickListener(null);
        }
        serverSettings = null;
        serverSelectionGroup = null;
        serverStatusText = null;
        resetButton = null;
        settingsRootView = null;
        super.onDestroyView();
    }

    private void loadServerChoices() {
        if (serverSelectionGroup == null) {
            return;
        }

        int requestId = ++serverLoadRequestId;
        updateServerStatus(-1);
        new Thread(() -> {
            List<String> discoveredBaseUrls = RadioBrowserServerDirectory.discoverBaseUrls();
            View rootView = settingsRootView;
            if (rootView == null) {
                return;
            }
            rootView.post(() -> {
                if (isStaleServerLoad(requestId)) {
                    return;
                }
                populateServerChoices(discoveredBaseUrls);
                updateServerStatus(discoveredBaseUrls.size());
            });
        }, "RadioBrowserServerSettingsLoad").start();
    }

    private void populateServerChoices(@NonNull List<String> serverBaseUrls) {
        if (serverSelectionGroup == null) {
            return;
        }

        serverSelectionGroup.setOnCheckedChangeListener(null);
        while (serverSelectionGroup.getChildCount() > 1) {
            serverSelectionGroup.removeViewAt(1);
        }

        for (String baseUrl : serverBaseUrls) {
            RadioButton button = new RadioButton(requireContext());
            button.setId(View.generateViewId());
            button.setLayoutParams(new RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            button.setText(formatServerLabel(baseUrl));
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
            loadServerChoices();
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

    private boolean isStaleServerLoad(int requestId) {
        return requestId != serverLoadRequestId || !isAdded() || settingsRootView == null;
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
