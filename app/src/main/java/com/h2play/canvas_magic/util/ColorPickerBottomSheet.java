package com.h2play.canvas_magic.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.h2play.canvas_magic.R;
import com.madrapps.pikolo.HSLColorPicker;
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener;

import java.util.ArrayList;
import java.util.List;

/**
 * 현대적인 색상 선택기 BottomSheet
 * - Pikolo HSL 컬러 피커 사용
 * - 선 굵기/투명도 조절
 * - 최근 사용 색상 저장
 * - 롱프레스 숨은 기능 유지
 */
public class ColorPickerBottomSheet extends BottomSheetDialogFragment {

    private static final String PREFS_NAME = "color_picker_prefs";
    private static final String KEY_RECENT_COLORS = "recent_colors";
    private static final int MAX_RECENT_COLORS = 6;

    private HSLColorPicker colorPicker;
    private View colorPreview;
    private Slider sliderThickness;
    private Slider sliderOpacity;
    private LinearLayout recentColorsContainer;
    private TextView tvHint;

    private int currentColor = Color.RED;
    private float currentThickness = 10f;
    private int currentAlpha = 255;

    private OnColorSelectedListener listener;
    private OnLongPressListener longPressListener;

    private List<Integer> recentColors = new ArrayList<>();
    private int longPressCount = 0;

    public interface OnColorSelectedListener {
        void onColorSelected(int color, float thickness);
    }

    public interface OnLongPressListener {
        void onLongPress();
    }

    public static ColorPickerBottomSheet newInstance(int initialColor, float initialThickness) {
        ColorPickerBottomSheet fragment = new ColorPickerBottomSheet();
        Bundle args = new Bundle();
        args.putInt("initial_color", initialColor);
        args.putFloat("initial_thickness", initialThickness);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
        this.listener = listener;
    }

    public void setOnLongPressListener(OnLongPressListener listener) {
        this.longPressListener = listener;
    }

    @Override
    public void onStart() {
        super.onStart();
        // BottomSheet 드래그로 인한 색상 피커 터치 충돌 방지
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setDraggable(false);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_color_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 인자에서 초기값 가져오기
        if (getArguments() != null) {
            currentColor = getArguments().getInt("initial_color", Color.RED);
            currentThickness = getArguments().getFloat("initial_thickness", 10f);
        }

        initViews(view);
        loadRecentColors();
        setupColorPicker();
        setupSliders();
        setupButtons(view);
        setupRecentColors();
        updateColorPreview();
    }

    private void initViews(View view) {
        colorPicker = view.findViewById(R.id.color_picker);
        colorPreview = view.findViewById(R.id.color_preview);
        sliderThickness = view.findViewById(R.id.slider_thickness);
        sliderOpacity = view.findViewById(R.id.slider_opacity);
        recentColorsContainer = view.findViewById(R.id.recent_colors_container);
        tvHint = view.findViewById(R.id.tv_hint);
    }

    private void setupColorPicker() {
        colorPicker.setColor(currentColor);
        colorPicker.setColorSelectionListener(new SimpleColorSelectionListener() {
            @Override
            public void onColorSelected(int color) {
                // 투명도 유지
                currentColor = Color.argb(currentAlpha, Color.red(color), Color.green(color), Color.blue(color));
                updateColorPreview();
            }
        });

        // 컬러 피커 롱프레스로 숨은 기능 활성화 (기존 롱프레스 기능 유지)
        colorPicker.setOnLongClickListener(v -> {
            longPressCount++;
            if (longPressCount >= 3) {
                // 3번 롱프레스하면 힌트 표시
                tvHint.setVisibility(View.VISIBLE);
            }
            return true;
        });
    }

    private void setupSliders() {
        // 선 굵기 슬라이더
        sliderThickness.setValue(currentThickness);
        sliderThickness.addOnChangeListener((slider, value, fromUser) -> {
            currentThickness = value;
        });

        // 투명도 슬라이더
        sliderOpacity.setValue(currentAlpha);
        sliderOpacity.addOnChangeListener((slider, value, fromUser) -> {
            currentAlpha = (int) value;
            currentColor = Color.argb(currentAlpha, 
                    Color.red(currentColor), 
                    Color.green(currentColor), 
                    Color.blue(currentColor));
            updateColorPreview();
        });
    }

    private void setupButtons(View view) {
        MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);
        MaterialButton btnApply = view.findViewById(R.id.btn_apply);

        btnCancel.setOnClickListener(v -> dismiss());

        btnApply.setOnClickListener(v -> {
            saveRecentColor(currentColor);
            if (listener != null) {
                listener.onColorSelected(currentColor, currentThickness);
            }
            dismiss();
        });

        // Apply 버튼 롱프레스 = 숨은 기능 (기존 onStartLongClick 동작)
        btnApply.setOnLongClickListener(v -> {
            saveRecentColor(currentColor);
            if (listener != null) {
                listener.onColorSelected(currentColor, currentThickness);
            }
            if (longPressListener != null) {
                longPressListener.onLongPress();
            }
            dismiss();
            return true;
        });
    }

    private void setupRecentColors() {
        recentColorsContainer.removeAllViews();

        int size = (int) (36 * getResources().getDisplayMetrics().density);
        int margin = (int) (8 * getResources().getDisplayMetrics().density);

        for (int color : recentColors) {
            View colorView = new View(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            colorView.setLayoutParams(params);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(color);
            drawable.setStroke((int) (1 * getResources().getDisplayMetrics().density), Color.parseColor("#CCCCCC"));
            colorView.setBackground(drawable);

            colorView.setOnClickListener(v -> {
                currentColor = color;
                currentAlpha = Color.alpha(color);
                colorPicker.setColor(color);
                sliderOpacity.setValue(currentAlpha);
                updateColorPreview();
            });

            recentColorsContainer.addView(colorView);
        }
    }

    private void updateColorPreview() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(currentColor);
        drawable.setStroke((int) (2 * getResources().getDisplayMetrics().density), Color.parseColor("#33000000"));
        colorPreview.setBackground(drawable);
    }

    private void loadRecentColors() {
        recentColors.clear();
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String colorsStr = prefs.getString(KEY_RECENT_COLORS, "");
        
        if (!colorsStr.isEmpty()) {
            String[] parts = colorsStr.split(",");
            for (String part : parts) {
                try {
                    recentColors.add(Integer.parseInt(part));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
    }

    private void saveRecentColor(int color) {
        // 중복 제거 후 맨 앞에 추가
        recentColors.remove(Integer.valueOf(color));
        recentColors.add(0, color);
        
        // 최대 개수 유지
        while (recentColors.size() > MAX_RECENT_COLORS) {
            recentColors.remove(recentColors.size() - 1);
        }

        // 저장
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < recentColors.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(recentColors.get(i));
        }

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_RECENT_COLORS, sb.toString()).apply();
    }
}
