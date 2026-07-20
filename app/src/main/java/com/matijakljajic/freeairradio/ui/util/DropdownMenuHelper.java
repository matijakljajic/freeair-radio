package com.matijakljajic.freeairradio.ui.util;

import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.matijakljajic.freeairradio.R;

import java.util.List;

public final class DropdownMenuHelper {

    private DropdownMenuHelper() {
    }

    public static void bindOptions(@NonNull MaterialAutoCompleteTextView inputView,
                                   @NonNull List<String> labels) {
        inputView.setAdapter(new ArrayAdapter<>(
                inputView.getContext(),
                R.layout.item_dialog_dropdown_option,
                labels
        ));
        inputView.setOnClickListener(v -> inputView.showDropDown());
        inputView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                inputView.showDropDown();
            }
        });
    }
}
