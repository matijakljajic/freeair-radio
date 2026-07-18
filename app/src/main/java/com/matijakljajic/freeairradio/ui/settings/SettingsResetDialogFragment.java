package com.matijakljajic.freeairradio.ui.settings;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.matijakljajic.freeairradio.R;

public class SettingsResetDialogFragment extends DialogFragment {

    public static final String REQUEST_KEY = "settings_reset_dialog_request";
    public static final String RESULT_KEY_ACTION = "result_action";

    public static final String ACTION_CLEAR_FAVORITES = "clear_favorites";
    public static final String ACTION_CLEAR_LOCAL_STATIONS = "clear_local_stations";
    public static final String ACTION_CLEAR_RECENTLY_PLAYED = "clear_recently_played";

    private static final String ARG_ACTION = "arg_action";
    private static final String ARG_TITLE_RES_ID = "arg_title_res_id";
    private static final String ARG_MESSAGE_RES_ID = "arg_message_res_id";

    @NonNull
    public static SettingsResetDialogFragment newInstance(@NonNull String action,
                                                          @StringRes int titleResId,
                                                          @StringRes int messageResId) {
        SettingsResetDialogFragment fragment = new SettingsResetDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ACTION, action);
        args.putInt(ARG_TITLE_RES_ID, titleResId);
        args.putInt(ARG_MESSAGE_RES_ID, messageResId);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View contentView = getLayoutInflater().inflate(R.layout.fragment_settings_reset_dialog, null, false);
        bindContent(contentView);
        return new MaterialAlertDialogBuilder(requireContext())
                .setView(contentView)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER_HORIZONTAL);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void bindContent(@NonNull View contentView) {
        Bundle args = requireArguments();
        ((TextView) contentView.findViewById(R.id.settings_reset_dialog_title))
                .setText(args.getInt(ARG_TITLE_RES_ID));
        ((TextView) contentView.findViewById(R.id.settings_reset_dialog_message))
                .setText(args.getInt(ARG_MESSAGE_RES_ID));

        contentView.findViewById(R.id.settings_reset_dialog_confirm_button).setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putString(RESULT_KEY_ACTION, requireAction());
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
            dismissAllowingStateLoss();
        });
        contentView.findViewById(R.id.settings_reset_dialog_close_button).setOnClickListener(v -> dismiss());
    }

    @NonNull
    private String requireAction() {
        Bundle args = requireArguments();
        String action = args.getString(ARG_ACTION);
        if (action == null) {
            throw new IllegalStateException("Reset dialog requires an action");
        }
        return action;
    }
}
