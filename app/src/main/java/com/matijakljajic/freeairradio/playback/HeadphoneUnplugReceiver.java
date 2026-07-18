package com.matijakljajic.freeairradio.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public final class HeadphoneUnplugReceiver extends BroadcastReceiver {

    public interface Listener {
        void onAudioOutputBecameNoisy();
    }

    @NonNull
    private final Listener listener;
    private boolean registered;

    public HeadphoneUnplugReceiver(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void register(@NonNull Context context) {
        if (registered) {
            return;
        }

        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        ContextCompat.registerReceiver(
                context.getApplicationContext(),
                this,
                intentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
        registered = true;
    }

    public void unregister(@NonNull Context context) {
        if (!registered) {
            return;
        }

        context.getApplicationContext().unregisterReceiver(this);
        registered = false;
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
            listener.onAudioOutputBecameNoisy();
        }
    }
}
