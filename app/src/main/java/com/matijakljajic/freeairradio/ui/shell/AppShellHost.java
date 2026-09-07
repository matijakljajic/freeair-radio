package com.matijakljajic.freeairradio.ui.shell;

import androidx.annotation.Nullable;

public interface AppShellHost {
    @Nullable
    AppShellController getAppShellController();
}
