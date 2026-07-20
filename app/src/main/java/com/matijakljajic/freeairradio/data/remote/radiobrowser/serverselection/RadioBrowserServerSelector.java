package com.matijakljajic.freeairradio.data.remote.radiobrowser.serverselection;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.util.AppLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public final class RadioBrowserServerSelector {

    private static final String TAG = "RadioBrowserServerSelector";
    interface ServerDiscovery {
        @NonNull
        List<String> refresh();
    }

    @NonNull
    private final Object lock = new Object();
    @Nullable
    private final String preferredBaseUrl;
    @NonNull
    private final ServerDiscovery serverDiscovery;
    @NonNull
    private List<String> baseUrls;
    private int selectedIndex;

    public RadioBrowserServerSelector(@NonNull Context context) {
        this(
                new RadioBrowserServerSettings(context).getPreferredBaseUrl(),
                true,
                RadioBrowserServerDirectory.getCachedServers(),
                RadioBrowserServerDirectory::refresh
        );
    }

    RadioBrowserServerSelector(@NonNull List<String> baseUrls) {
        this(null, false, baseUrls, () -> baseUrls);
    }

    RadioBrowserServerSelector(@Nullable String preferredBaseUrl,
                               boolean refreshAsync,
                               @NonNull List<String> cachedBaseUrls,
                               @NonNull ServerDiscovery serverDiscovery) {
        this.preferredBaseUrl = normalizeBaseUrl(preferredBaseUrl);
        this.serverDiscovery = serverDiscovery;
        this.baseUrls = buildStartupBaseUrls(cachedBaseUrls, this.preferredBaseUrl);
        this.selectedIndex = 0;
        AppLog.d(TAG, "Initialized server selector"
                + " preferred=" + AppLog.value(this.preferredBaseUrl)
                + " startupSource=" + (cachedBaseUrls.isEmpty() ? "bootstrap" : "cache")
                + " serverCount=" + this.baseUrls.size()
                + " selected=" + AppLog.value(getSelectedBaseUrl()));
        if (refreshAsync) {
            refreshAsync();
        }
    }

    @Nullable
    public String getSelectedBaseUrl() {
        synchronized (lock) {
            if (baseUrls.isEmpty()) {
                return null;
            }
            return baseUrls.get(selectedIndex);
        }
    }

    public void rememberSuccess(@NonNull String baseUrl) {
        synchronized (lock) {
            int index = indexOf(baseUrl);
            if (index >= 0) {
                selectedIndex = index;
                AppLog.d(TAG, "Remembered successful server"
                        + " selected=" + baseUrls.get(selectedIndex)
                        + " index=" + selectedIndex);
            }
        }
    }

    public boolean rotateToNextServer(@Nullable String failedBaseUrl) {
        synchronized (lock) {
            if (baseUrls.size() < 2) {
                return false;
            }

            int failedIndex = indexOf(failedBaseUrl);
            if (failedIndex < 0) {
                failedIndex = selectedIndex;
            }
            selectedIndex = (failedIndex + 1) % baseUrls.size();
            AppLog.w(TAG, "Rotated Radio Browser server after failure"
                    + " failed=" + AppLog.value(failedBaseUrl)
                    + " next=" + baseUrls.get(selectedIndex)
                    + " serverCount=" + baseUrls.size());
            return true;
        }
    }

    public boolean refreshAndRotate(@Nullable String failedBaseUrl) {
        List<String> discoveredBaseUrls = serverDiscovery.refresh();
        synchronized (lock) {
            List<String> refreshedBaseUrls = buildInitialBaseUrls(discoveredBaseUrls, preferredBaseUrl);
            if (refreshedBaseUrls.isEmpty()) {
                AppLog.w(TAG, "Server refresh returned no usable base urls after failure"
                        + " failed=" + AppLog.value(failedBaseUrl));
                return false;
            }

            String normalizedFailedBaseUrl = normalizeBaseUrl(failedBaseUrl);
            int failedIndex = normalizedFailedBaseUrl == null
                    ? -1
                    : refreshedBaseUrls.indexOf(normalizedFailedBaseUrl);
            if (failedIndex >= 0 && refreshedBaseUrls.size() < 2) {
                AppLog.w(TAG, "Server refresh returned only the failed base url"
                        + " failed=" + AppLog.value(failedBaseUrl));
                return false;
            }

            baseUrls = refreshedBaseUrls;
            selectedIndex = failedIndex >= 0
                    ? (failedIndex + 1) % refreshedBaseUrls.size()
                    : 0;
            AppLog.d(TAG, "Refreshed and selected Radio Browser server after failure"
                    + " failed=" + AppLog.value(failedBaseUrl)
                    + " serverCount=" + baseUrls.size()
                    + " selected=" + baseUrls.get(selectedIndex));
            return true;
        }
    }

    private void refreshAsync() {
        AppLog.d(TAG, "Refreshing Radio Browser servers in background");
        Thread refreshThread = new Thread(() -> {
            List<String> discoveredBaseUrls = serverDiscovery.refresh();
            synchronized (lock) {
                List<String> refreshedBaseUrls = buildInitialBaseUrls(discoveredBaseUrls, preferredBaseUrl);
                if (!refreshedBaseUrls.isEmpty()) {
                    baseUrls = refreshedBaseUrls;
                    selectedIndex = 0;
                    AppLog.d(TAG, "Refreshed Radio Browser servers"
                            + " serverCount=" + baseUrls.size()
                            + " selected=" + baseUrls.get(selectedIndex));
                } else {
                    AppLog.w(TAG, "Server refresh returned no base urls");
                }
            }
        }, "RadioBrowserServerRefresh");
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    @NonNull
    static List<String> buildStartupBaseUrls(@NonNull List<String> cachedBaseUrls,
                                             @Nullable String preferredBaseUrl) {
        List<String> startupBaseUrls = buildInitialBaseUrls(cachedBaseUrls, preferredBaseUrl);
        if (!startupBaseUrls.isEmpty()) {
            return startupBaseUrls;
        }

        String fallbackBaseUrl = preferredBaseUrl != null
                ? preferredBaseUrl
                : RadioBrowserServerDirectory.getBootstrapBaseUrl();
        return new ArrayList<>(Collections.singletonList(fallbackBaseUrl));
    }

    @NonNull
    private static List<String> buildInitialBaseUrls(@NonNull List<String> baseUrls,
                                                     @Nullable String preferredBaseUrl) {
        Set<String> deduplicatedBaseUrls = new LinkedHashSet<>(baseUrls);
        List<String> normalizedBaseUrls = new ArrayList<>(deduplicatedBaseUrls);
        if (preferredBaseUrl == null) {
            return normalizedBaseUrls;
        }

        int preferredIndex = normalizedBaseUrls.indexOf(preferredBaseUrl);
        if (preferredIndex > 0) {
            normalizedBaseUrls.remove(preferredIndex);
            normalizedBaseUrls.add(0, preferredBaseUrl);
        }
        return normalizedBaseUrls;
    }

    @Nullable
    private static String normalizeBaseUrl(@Nullable String baseUrl) {
        if (baseUrl == null) {
            return null;
        }

        String trimmedValue = baseUrl.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private int indexOf(@Nullable String baseUrl) {
        if (baseUrl == null) {
            return -1;
        }
        for (int i = 0; i < baseUrls.size(); i++) {
            if (baseUrls.get(i).equals(baseUrl)) {
                return i;
            }
        }
        return -1;
    }

}
