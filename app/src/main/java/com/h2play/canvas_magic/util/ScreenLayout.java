package com.h2play.canvas_magic.util;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.h2play.canvas_magic.R;

/** Insets for ordinary menu screens only; never used by the drawing or secret screen. */
public final class ScreenLayout {
    private ScreenLayout() { }

    public static void setup(AppCompatActivity activity, int title) {
        if (activity.getSupportActionBar() != null) activity.getSupportActionBar().hide();
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        View root = ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
        root.setFitsSystemWindows(false);
        final int left = root.getPaddingLeft(), top = root.getPaddingTop();
        final int right = root.getPaddingRight(), bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(left + bars.left, top + bars.top,
                    right + bars.right, bottom + bars.bottom);
            return insets;
        });
        WindowCompat.getInsetsController(activity.getWindow(), root).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(activity.getWindow(), root).setAppearanceLightNavigationBars(true);
        ((TextView) activity.findViewById(R.id.screen_title)).setText(title);
        activity.findViewById(R.id.screen_back).setOnClickListener(v ->
                activity.getOnBackPressedDispatcher().onBackPressed());
        ViewCompat.requestApplyInsets(root);
    }
}
