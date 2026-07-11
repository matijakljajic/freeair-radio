package com.matijakljajic.freeairradio.ui;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public abstract class ShellChromeAwareFragment extends Fragment {

    @Nullable
    private ShellChromeHost shellChromeHost;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ShellChromeHost) {
            shellChromeHost = (ShellChromeHost) context;
        } else {
            throw new IllegalStateException("Host activity must implement ShellChromeHost");
        }
    }

    @Override
    public void onDetach() {
        shellChromeHost = null;
        super.onDetach();
    }

    @Nullable
    protected final ShellChromeController getShellChromeController() {
        if (shellChromeHost == null) {
            return null;
        }
        return shellChromeHost.getShellChromeController();
    }

    protected final void attachShellContentPadding(@NonNull View contentView, int topGapPx) {
        ShellChromeController shellChromeController = getShellChromeController();
        if (shellChromeController != null) {
            shellChromeController.attachContentPaddingView(contentView, topGapPx);
        }
    }

    protected final void detachShellContentPadding(@NonNull View contentView) {
        ShellChromeController shellChromeController = getShellChromeController();
        if (shellChromeController != null) {
            shellChromeController.detachContentPaddingView(contentView);
        }
    }
}
