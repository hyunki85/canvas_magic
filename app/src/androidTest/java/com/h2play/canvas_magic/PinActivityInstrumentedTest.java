package com.h2play.canvas_magic;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.h2play.canvas_magic.features.pincode.PinActivity;
import com.h2play.canvas_magic.util.SecretSelectionGesture;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PinActivityInstrumentedTest {
    @Test public void secretScreenKeepsDisplayAwake() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = PinActivity.getStartIntent(context, 9)
                .putExtra(PinActivity.EXTRA_PRACTICE_TARGET, 8)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try (ActivityScenario<PinActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> assertTrue(
                    (activity.getWindow().getAttributes().flags
                            & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0));
        }
    }

    @Test public void twoFingerHoldSilentlyClosesPerformanceScreen() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = PinActivity.getStartIntent(context, 9)
                .putExtra(PinActivity.EXTRA_NO_GUIDE, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        AtomicReference<PinActivity> launched = new AtomicReference<>();
        try (ActivityScenario<PinActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                launched.set(activity);
                View target = activity.findViewById(com.h2play.canvas_magic.R.id.fl_main);
                long downTime = SystemClock.uptimeMillis();
                dispatch(activity, target, downTime, downTime, MotionEvent.ACTION_DOWN, 1);
                dispatch(activity, target, downTime, downTime,
                        MotionEvent.ACTION_POINTER_DOWN
                                | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2);
            });
            Thread.sleep(SecretSelectionGesture.ABORT_HOLD_MS + 250);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertTrue(launched.get().isFinishing()
                    || scenario.getState() == androidx.lifecycle.Lifecycle.State.DESTROYED);
        }
    }

    private static void dispatch(PinActivity activity, View target, long downTime, long eventTime,
                                 int action, int pointerCount) {
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[pointerCount];
        MotionEvent.PointerCoords[] coordinates = new MotionEvent.PointerCoords[pointerCount];
        for (int index = 0; index < pointerCount; index++) {
            MotionEvent.PointerProperties property = new MotionEvent.PointerProperties();
            property.id = index;
            property.toolType = MotionEvent.TOOL_TYPE_FINGER;
            properties[index] = property;
            MotionEvent.PointerCoords coordinate = new MotionEvent.PointerCoords();
            coordinate.x = 180 + (index * 220);
            coordinate.y = 420;
            coordinate.pressure = 1;
            coordinate.size = 1;
            coordinates[index] = coordinate;
        }
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, pointerCount,
                properties, coordinates, 0, 0, 1, 1, 0, 0,
                InputDevice.SOURCE_TOUCHSCREEN, 0);
        activity.onMainClick(target, event);
        event.recycle();
    }
}
