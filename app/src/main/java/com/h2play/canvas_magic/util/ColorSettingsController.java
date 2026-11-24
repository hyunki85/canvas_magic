package com.h2play.canvas_magic.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import com.google.android.material.button.MaterialButton;
import android.view.LayoutInflater;
import com.google.android.material.slider.Slider;
import com.h2play.canvas_magic.R;
import com.madrapps.pikolo.HSLColorPicker;
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared draft/apply contract for the inline studio and the editor dialog. */
public final class ColorSettingsController {
    public interface Listener {
        void onApply(int color, float thickness);
        void onCancel();
        void onLongPress();
    }
    private final View root;
    private final Context context;
    private final Listener listener;
    private final HSLColorPicker picker;
    private final Slider opacity;
    private final List<MaterialButton> chips = new ArrayList<>();
    private final List<Integer> recent = new ArrayList<>();
    private static final int[] PRESETS = {Color.BLACK, 0xFFEF4444, 0xFFF59E0B, 0xFFFACC15,
            0xFF22C55E, 0xFF06B6D4, 0xFF3B82F6, 0xFF7C3AED, Color.WHITE};
    private int color;
    private float thickness;
    private boolean applied;
    private boolean syncing;
    private int longPressCount;

    @SuppressLint("ClickableViewAccessibility")
    public ColorSettingsController(View root, int initialColor, float initialThickness,
            Bundle state, Listener listener) {
        this.root = root;
        this.context = root.getContext();
        this.listener = listener;
        color = state == null ? initialColor : state.getInt("color", initialColor);
        thickness = state == null ? initialThickness : state.getFloat("thickness", initialThickness);
        thickness = Float.isNaN(thickness) ? 10f : Math.round(Math.max(5f, Math.min(50f, thickness)));
        picker = root.findViewById(R.id.color_picker);
        opacity = root.findViewById(R.id.slider_opacity);
        Slider width = root.findViewById(R.id.slider_thickness);
        picker.setColor(color);
        width.setValue(thickness);
        opacity.setValue(Color.alpha(color));
        opacity.setLabelFormatter(value -> String.format(Locale.getDefault(), "%d%%",
                Math.round(value * 100f / 255f)));
        width.addOnChangeListener((slider, value, fromUser) -> {
            thickness = value;
            refresh();
        });
        opacity.addOnChangeListener((slider, value, fromUser) -> {
            color = ColorUtils.setAlphaComponent(color, Math.round(value));
            refresh();
        });
        picker.setColorSelectionListener(new SimpleColorSelectionListener() {
            @Override public void onColorSelected(int selected) {
                if (syncing) return;
                color = ColorUtils.setAlphaComponent(selected, Color.alpha(color));
                refresh();
            }
        });
        // The wheel must own a drag even inside the scrollable settings body.
        picker.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            }
            return false;
        });
        picker.setOnLongClickListener(v -> {
            if (++longPressCount >= 3) setHint(context.getString(R.string.long_press_hint));
            return true;
        });
        View advanced = root.findViewById(R.id.advanced_color_container);
        advanced.setVisibility(state != null && state.getBoolean("advanced") ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.btn_custom_color).setOnClickListener(v -> {
            advanced.setVisibility(advanced.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            v.setSelected(advanced.getVisibility() == View.VISIBLE);
        });
        root.findViewById(R.id.btn_cancel).setOnClickListener(v -> listener.onCancel());
        root.findViewById(R.id.btn_apply).setOnClickListener(v -> apply());
        root.findViewById(R.id.btn_apply).setOnLongClickListener(v -> {
            apply();
            listener.onLongPress();
            return true;
        });
        LinearLayout presetRow = root.findViewById(R.id.preset_colors_container);
        for (int preset : PRESETS) addSwatch(presetRow, preset, false);
        String saved = context.getSharedPreferences("color_picker_prefs", Context.MODE_PRIVATE)
                .getString("recent_colors", "");
        if (saved != null) for (String item : saved.split(",")) {
            try {
                int entry = Integer.parseInt(item);
                if (!recent.contains(entry) && recent.size() < 6) recent.add(entry);
            } catch (NumberFormatException ignored) { }
        }
        LinearLayout recentRow = root.findViewById(R.id.recent_colors_container);
        for (int entry : recent) addSwatch(recentRow, entry, true);
        root.findViewById(R.id.tv_recent_empty).setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
        refresh();
    }

    private void addSwatch(LinearLayout row, int value, boolean restoreAlpha) {
        MaterialButton chip = (MaterialButton) LayoutInflater.from(context)
                .inflate(R.layout.view_color_swatch, row, false);
        chip.setCheckable(true);
        int displayed = ColorUtils.compositeColors(value, Color.WHITE);
        chip.setBackgroundTintList(ColorStateList.valueOf(displayed));
        chip.setIconTint(ColorStateList.valueOf(
                ColorUtils.calculateLuminance(displayed) > .5 ? 0xFF344054 : Color.WHITE));
        chip.setTag(value);
        chip.setTag(R.id.recent_colors_container, restoreAlpha);
        chip.setOnClickListener(v -> {
            color = restoreAlpha ? value : ColorUtils.setAlphaComponent(value, Color.alpha(color));
            syncing = true;
            picker.setColor(color);
            opacity.setValue(Color.alpha(color));
            syncing = false;
            refresh();
        });
        row.addView(chip);
        chips.add(chip);
    }

    private void refresh() {
        ((TextView) root.findViewById(R.id.tv_thickness_value)).setText(
                String.format(Locale.getDefault(), "%.0f", thickness));
        ((TextView) root.findViewById(R.id.tv_opacity_value)).setText(
                String.format(Locale.getDefault(), "%d%%", Math.round(Color.alpha(color) * 100f / 255)));
        ((StrokePreviewView) root.findViewById(R.id.stroke_preview)).setBrush(color, thickness);
        for (MaterialButton chip : chips) {
            int value = (int) chip.getTag();
            boolean restoresAlpha = Boolean.TRUE.equals(chip.getTag(R.id.recent_colors_container));
            int alpha = restoresAlpha ? Color.alpha(value) : Color.alpha(color);
            chip.setContentDescription(context.getString(R.string.named_color_opacity,
                    colorName(value), Math.round(alpha * 100f / 255f)));
            boolean checked = Boolean.TRUE.equals(chip.getTag(R.id.recent_colors_container))
                    ? (int) chip.getTag() == color
                    : ((int) chip.getTag() & 0xFFFFFF) == (color & 0xFFFFFF);
            chip.setChecked(checked);
            chip.setIcon(checked ? androidx.appcompat.content.res.AppCompatResources.getDrawable(
                    context, com.google.android.material.R.drawable.ic_mtrl_chip_checked_black) : null);
        }
    }
    public void setHint(String hint) {
        TextView view = root.findViewById(R.id.tv_hint);
        view.setText(hint);
        view.setVisibility(hint == null ? View.GONE : View.VISIBLE);
    }
    private String colorName(int value) {
        int rgb = value | 0xFF000000;
        String[] names = context.getResources().getStringArray(R.array.brush_color_names);
        for (int i = 0; i < PRESETS.length; i++) if (rgb == PRESETS[i]) return names[i];
        if (rgb == Color.RED) return names[1];
        if (rgb == Color.BLUE) return names[6];
        if (rgb == Color.GREEN) return names[4];
        return context.getString(R.string.custom_color_name, String.format(Locale.ROOT, "#%06X", value & 0xFFFFFF));
    }
    public Bundle saveState() {
        Bundle state = new Bundle();
        state.putInt("color", color);
        state.putFloat("thickness", thickness);
        state.putBoolean("advanced", root.findViewById(R.id.advanced_color_container).getVisibility() == View.VISIBLE);
        return state;
    }
    public void apply() {
        if (applied) return;
        applied = true;
        DrawingSettingsPreferences.save(context, color, thickness);
        recent.remove(Integer.valueOf(color));
        recent.add(0, color);
        while (recent.size() > 6) recent.remove(recent.size() - 1);
        StringBuilder values = new StringBuilder();
        for (int entry : recent) {
            if (values.length() > 0) values.append(',');
            values.append(entry);
        }
        context.getSharedPreferences("color_picker_prefs", Context.MODE_PRIVATE).edit()
                .putString("recent_colors", values.toString()).apply();
        listener.onApply(color, thickness);
    }
    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
