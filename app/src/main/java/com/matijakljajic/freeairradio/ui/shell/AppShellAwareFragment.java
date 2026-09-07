package com.matijakljajic.freeairradio.ui.shell;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

@SuppressWarnings("unused")
public abstract class AppShellAwareFragment extends Fragment {

    @Nullable
    private AppShellHost appShellHost;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof AppShellHost) {
            appShellHost = (AppShellHost) context;
        } else {
            throw new IllegalStateException("Host activity must implement AppShellHost");
        }
    }

    @Override
    public void onDetach() {
        appShellHost = null;
        super.onDetach();
    }

    @Nullable
    protected final AppShellController getAppShellController() {
        if (appShellHost == null) {
            return null;
        }
        return appShellHost.getAppShellController();
    }

    protected final void attachAppShellContentPadding(@NonNull View contentView, int topGapPx) {
        AppShellController appShellController = getAppShellController();
        if (appShellController != null) {
            appShellController.attachContentPaddingView(contentView, topGapPx);
        }
    }

    protected final void detachAppShellContentPadding(@NonNull View contentView) {
        AppShellController appShellController = getAppShellController();
        if (appShellController != null) {
            appShellController.detachContentPaddingView(contentView);
        }
    }
}
