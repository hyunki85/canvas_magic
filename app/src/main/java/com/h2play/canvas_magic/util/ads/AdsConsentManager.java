package com.h2play.canvas_magic.util.ads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.ads.MobileAds;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.h2play.canvas_magic.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import timber.log.Timber;

/** One successful consent refresh per process, shared by visible placements. Main thread only. */
final class AdsConsentManager {
    private static AdsConsentManager instance;
    private final Context context;
    private final ConsentInformation consent;
    private final Set<Runnable> listeners = new HashSet<>();
    private WeakReference<Activity> foreground = new WeakReference<>(null);
    private boolean updateStarted;
    private boolean updating;
    private boolean formShowing;
    private boolean resolved;
    private boolean sdkInitializing;
    private boolean sdkReady;
    private long retryAfter;
    private int revision;

    private AdsConsentManager(Context context) {
        this.context = context.getApplicationContext();
        consent = UserMessagingPlatform.getConsentInformation(this.context);
    }

    static AdsConsentManager get(Context context) {
        if (instance == null) instance = new AdsConsentManager(context);
        return instance;
    }

    void addListener(Runnable listener) { listeners.add(listener); }
    void removeListener(Runnable listener) { listeners.remove(listener); }
    int revision() { return revision; }

    boolean canRequestAds() {
        return resolved && !formShowing && consent.canRequestAds() && sdkReady;
    }

    boolean privacyOptionsRequired() {
        return consent.getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    boolean isBusy() { return updating || formShowing; }

    void resume(Activity activity) {
        foreground = new WeakReference<>(activity);
        if (!updateStarted && SystemClock.elapsedRealtime() >= retryAfter) {
            updateStarted = true;
            updating = true;
            resolved = false;
            consent.requestConsentInfoUpdate(activity, new ConsentRequestParameters.Builder().build(),
                    () -> {
                        updating = false;
                        showRequiredForm();
                    }, error -> {
                        updating = false;
                        resolved = true;
                        Timber.w("Consent refresh failed: %d", error.getErrorCode());
                        // UMP decides whether a previous session's consent is still usable.
                        allowLaterRetryIfNeeded();
                        publish();
                    });
        } else if (!updating && !resolved && !formShowing) {
            showRequiredForm();
        }
    }

    void pause(Activity activity) {
        if (foreground.get() == activity) foreground.clear();
    }

    private void showRequiredForm() {
        Activity activity = foreground.get();
        if (!isResumed(activity)) return; // Resume the flow when a visible screen is available.
        formShowing = true;
        publish();
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, error -> {
            formShowing = false;
            resolved = true;
            revision++;
            if (error != null) {
                Timber.w("Consent form failed: %d", error.getErrorCode());
                allowLaterRetryIfNeeded();
            }
            publish();
        });
    }

    void showPrivacyOptions(Activity activity) {
        if (!isResumed(activity) || isBusy() || !privacyOptionsRequired()) return;
        formShowing = true;
        revision++;
        publish(); // Dispose every previously loaded banner before a consent change.
        UserMessagingPlatform.showPrivacyOptionsForm(activity, error -> {
            formShowing = false;
            resolved = true;
            revision++;
            if (error != null) {
                Timber.w("Privacy options failed: %d", error.getErrorCode());
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    Toast.makeText(activity, R.string.ad_privacy_options_unavailable, Toast.LENGTH_SHORT).show();
                }
            }
            publish();
        });
    }

    private void allowLaterRetryIfNeeded() {
        if (!consent.canRequestAds()) {
            updateStarted = false;
            // Retry on a later screen resume; never loop or interrupt the performance screen.
            retryAfter = SystemClock.elapsedRealtime() + 60_000L;
        }
    }

    private static boolean isResumed(Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed()
                && activity instanceof LifecycleOwner
                && ((LifecycleOwner) activity).getLifecycle().getCurrentState()
                .isAtLeast(Lifecycle.State.RESUMED);
    }

    private void publish() {
        if (resolved && !formShowing && consent.canRequestAds() && !sdkReady && !sdkInitializing) {
            sdkInitializing = true;
            Handler main = new Handler(Looper.getMainLooper());
            new Thread(() -> MobileAds.initialize(context, status -> main.post(() -> {
                sdkReady = true;
                publish();
            })), "canvas-mobile-ads-init").start();
        }
        for (Runnable listener : new ArrayList<>(listeners)) listener.run();
    }
}
