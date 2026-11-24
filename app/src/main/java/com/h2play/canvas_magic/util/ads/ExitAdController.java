package com.h2play.canvas_magic.util.ads;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.h2play.canvas_magic.BuildConfig;
import com.h2play.canvas_magic.util.AdDialog;

/** Restores the original Back confirmation banner, preserving UMP and debug/test isolation. */
public final class ExitAdController implements DefaultLifecycleObserver {
    public enum Placement {
        CANVAS("canvas_exit_banner", "ca-app-pub-9937617798998725/8292313909"),
        MENU("menu_exit_banner", "ca-app-pub-9937617798998725/1754825717");

        final String name;
        final String unitId;
        Placement(String name, String unitId) { this.name = name; this.unitId = unitId; }
    }

    private final Activity activity;
    private final Placement placement;
    private final View privacyOptions;
    private final AdsConsentManager consent;
    private final ExitAdState state = new ExitAdState();
    private final Runnable consentChanged = this::refresh;
    private AdView banner;
    private AdDialog dialog;

    public ExitAdController(@NonNull Activity activity, @NonNull Placement placement) {
        this(activity, placement, null);
    }

    public ExitAdController(@NonNull Activity activity, @NonNull Placement placement,
                            @Nullable View privacyOptions) {
        this.activity = activity;
        this.placement = placement;
        this.privacyOptions = privacyOptions;
        consent = AdsConsentManager.get(activity);
        consent.addListener(consentChanged);
        if (privacyOptions != null) {
            privacyOptions.setOnClickListener(v -> consent.showPrivacyOptions(activity));
        }
        ((LifecycleOwner) activity).getLifecycle().addObserver(this);
    }

    @Override public void onResume(@NonNull LifecycleOwner owner) {
        state.resume();
        consent.resume(activity);
        refresh();
        if (banner != null) banner.resume();
    }

    @Override public void onPause(@NonNull LifecycleOwner owner) {
        state.pause();
        consent.pause(activity);
        if (banner != null) banner.pause();
    }

    private void refresh() {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        if (privacyOptions != null) {
            privacyOptions.setVisibility(consent.privacyOptionsRequired() ? View.VISIBLE : View.GONE);
            privacyOptions.setEnabled(!consent.isBusy());
        }
        if (state.updateConsent(consent.canRequestAds(), consent.revision())) dispose();
        long now = SystemClock.elapsedRealtime();
        if (state.expire(now)) dispose();
        if (!state.needsRequest(now)) return;
        dispose();
        int token = state.request(now);
        AdView requested = new AdView(activity);
        banner = requested;
        requested.setAdSize(AdSize.MEDIUM_RECTANGLE);
        requested.setAdUnitId(BuildConfig.DEBUG ? "ca-app-pub-3940256099942544/6300978111" : placement.unitId);
        requested.setAdListener(new ExitListener(activity.getApplicationContext(), placement.name, state,
                token, () -> { if (dialog == null) requested.pause(); }, this::dispose));
        requested.setOnPaidEventListener(value -> {
            if (state.isCurrent(token)) AdTelemetry.paid(activity, placement.name, value);
        });
        AdTelemetry.event(activity, "monetization_ad_request", placement.name);
        requested.loadAd(new AdRequest.Builder().build());
    }

    public void onBack(@NonNull Runnable leave) {
        if (activity.isFinishing() || activity.isDestroyed() || dialog != null) return;
        View decor = activity.getWindow().getDecorView();
        float density = activity.getResources().getDisplayMetrics().density;
        boolean fits = decor.getWidth() / density >= 300 && decor.getHeight() / density >= 440;
        // Never trigger a late popup or hold Back while loading, offline, or awaiting consent.
        if (!fits || !consent.canRequestAds() || banner == null
                || !state.canShow(SystemClock.elapsedRealtime())) {
            AdTelemetry.event(activity, "monetization_exit_ad_skipped", placement.name);
            leave.run();
            return;
        }
        AdDialog shown = new AdDialog(activity, banner,
                v -> dismissDialog(), v -> { dismissDialog(); leave.run(); });
        dialog = shown;
        shown.setOnDismissListener(d -> {
            dialog = null;
            if (banner != null) {
                banner.pause();
                detach(banner);
            }
        });
        shown.show();
        banner.resume();
        AdTelemetry.event(activity, "monetization_exit_dialog_shown", placement.name);
    }

    private static void detach(AdView view) {
        if (view.getParent() instanceof ViewGroup) ((ViewGroup) view.getParent()).removeView(view);
    }

    private void dismissDialog() {
        if (dialog != null) dialog.dismiss();
    }

    private static final class ExitListener extends AdListener {
        private final Context context;
        private final String placement;
        private final ExitAdState state;
        private final int token;
        private final Runnable afterLoad;
        private final Runnable afterFailure;

        ExitListener(Context context, String placement, ExitAdState state, int token,
                     Runnable afterLoad, Runnable afterFailure) {
            this.context = context;
            this.placement = placement;
            this.state = state;
            this.token = token;
            this.afterLoad = afterLoad;
            this.afterFailure = afterFailure;
        }

        @Override public void onAdLoaded() {
            if (!state.isCurrent(token)) return;
            state.loaded(token, SystemClock.elapsedRealtime());
            afterLoad.run();
            AdTelemetry.event(context, "monetization_ad_loaded", placement);
        }

        @Override public void onAdFailedToLoad(@NonNull LoadAdError error) {
            if (!state.isCurrent(token)) return;
            state.failed(token, SystemClock.elapsedRealtime());
            afterFailure.run();
            AdTelemetry.failure(context, placement, error.getCode());
        }

        @Override public void onAdImpression() {
            if (state.isCurrent(token)) AdTelemetry.event(context, "monetization_ad_impression", placement);
        }
    }

    private void dispose() {
        if (dialog != null) {
            dialog.setOnDismissListener(null);
            dialog.dismiss();
            dialog = null;
        }
        if (banner == null) return;
        banner.setAdListener(null);
        banner.setOnPaidEventListener(null);
        detach(banner);
        banner.destroy();
        banner = null;
    }

    @Override public void onDestroy(@NonNull LifecycleOwner owner) {
        state.destroy();
        consent.pause(activity);
        consent.removeListener(consentChanged);
        if (privacyOptions != null) privacyOptions.setOnClickListener(null);
        dispose();
        owner.getLifecycle().removeObserver(this);
    }
}
