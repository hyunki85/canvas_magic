package com.h2play.canvas_magic.util;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.h2play.canvas_magic.R;

/** Modal adapter for editing screens; the performance canvas uses the same controls inline. */
public class ColorPickerBottomSheet extends BottomSheetDialogFragment {
    public interface OnColorSelectedListener { void onColorSelected(int color, float thickness); }
    public interface OnLongPressListener { void onLongPress(); }
    private OnColorSelectedListener listener;
    private OnLongPressListener longPressListener;
    private ColorSettingsController controller;

    public static ColorPickerBottomSheet newInstance(int color, float thickness) {
        ColorPickerBottomSheet fragment = new ColorPickerBottomSheet();
        Bundle args = new Bundle();
        args.putInt("initial_color", color);
        args.putFloat("initial_thickness", thickness);
        fragment.setArguments(args);
        return fragment;
    }
    public void setOnColorSelectedListener(OnColorSelectedListener listener) { this.listener = listener; }
    public void setOnLongPressListener(OnLongPressListener listener) { longPressListener = listener; }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_color_picker, container, false);
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        Bundle args = getArguments() == null ? new Bundle() : getArguments();
        controller = new ColorSettingsController(view,
                args.getInt("initial_color", DrawingSettingsPreferences.DEFAULT_COLOR),
                args.getFloat("initial_thickness", DrawingSettingsPreferences.DEFAULT_THICKNESS),
                state == null ? null : state.getBundle("draft"), new ColorSettingsController.Listener() {
            @Override public void onApply(int color, float thickness) {
                if (listener != null) listener.onColorSelected(color, thickness);
                dismiss();
            }
            @Override public void onCancel() { dismiss(); }
            @Override public void onLongPress() {
                if (longPressListener != null) longPressListener.onLongPress();
            }
        });
    }
    @Override public void onStart() {
        super.onStart();
        View view = getView();
        if (view != null) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = Math.min(Math.round(370 * getResources().getDisplayMetrics().density),
                    Math.round(getResources().getDisplayMetrics().heightPixels * .75f));
            view.setLayoutParams(params);
        }
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null) {
            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(sheet);
                behavior.setDraggable(false);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }
    @Override public void onCancel(@NonNull DialogInterface dialog) {
        if (controller != null) controller.apply();
        super.onCancel(dialog);
    }
    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        if (controller != null) state.putBundle("draft", controller.saveState());
        super.onSaveInstanceState(state);
    }
    @Override public void onDestroyView() {
        controller = null;
        super.onDestroyView();
    }
}
