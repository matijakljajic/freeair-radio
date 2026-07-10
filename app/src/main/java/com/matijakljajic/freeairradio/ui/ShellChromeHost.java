package com.matijakljajic.freeairradio.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface ShellChromeHost {
    @Nullable
    ShellChromeController getShellChromeController();
}
