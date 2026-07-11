package com.matijakljajic.freeairradio.data.remote.radiobrowser.serverselection;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class RadioBrowserServerSelector {

    @NonNull
    private final Object lock = new Object();
    @Nullable
    private final String preferredBaseUrl;
    @NonNull
    private List<String> baseUrls;
    private int selectedIndex;
    @NonNull
    private final CountDownLatch readyLatch;

    public RadioBrowserServerSelector() {
        this(null, true);
    }

    public RadioBrowserServerSelector(@NonNull Context context) {
        this(new RadioBrowserServerSettings(context).getPreferredBaseUrl(), true);
    }

    RadioBrowserServerSelector(@NonNull List<String> baseUrls) {
        this(null, false);
        synchronized (lock) {
            this.baseUrls = buildInitialBaseUrls(baseUrls, null);
            this.selectedIndex = 0;
        }
    }

    private RadioBrowserServerSelector(@Nullable String preferredBaseUrl,
                                       boolean refreshAsync) {
        this.preferredBaseUrl = normalizeBaseUrl(preferredBaseUrl);
        this.baseUrls = new ArrayList<>();
        this.selectedIndex = 0;
        this.readyLatch = new CountDownLatch(1);
        if (refreshAsync) {
            refreshAsync();
        } else {
            readyLatch.countDown();
        }
    }

    public boolean hasServers() {
        synchronized (lock) {
            return !baseUrls.isEmpty();
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
            return true;
        }
    }

    public boolean isReadyWithin(long timeoutMillis) {
        try {
            return !readyLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        }
    }

    private void refreshAsync() {
        Thread refreshThread = new Thread(() -> {
            try {
                List<String> discoveredBaseUrls = RadioBrowserServerDirectory.discoverBaseUrls();
                synchronized (lock) {
                    baseUrls = buildInitialBaseUrls(discoveredBaseUrls, preferredBaseUrl);
                    selectedIndex = 0;
                }
            } finally {
                readyLatch.countDown();
            }
        }, "RadioBrowserServerRefresh");
        refreshThread.setDaemon(true);
        refreshThread.start();
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
