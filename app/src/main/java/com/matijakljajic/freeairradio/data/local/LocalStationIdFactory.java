package com.matijakljajic.freeairradio.data.local;

import androidx.annotation.NonNull;

import java.util.UUID;

public final class LocalStationIdFactory {

    private LocalStationIdFactory() {
    }

    @NonNull
    public static String create() {
        return "LOCAL:" + UUID.randomUUID();
    }
}
