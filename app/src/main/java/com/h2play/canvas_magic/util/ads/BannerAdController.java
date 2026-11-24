package com.h2play.canvas_magic.util.ads;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

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

/** Owns a visible, width-aware banner and its consent/lifecycle. No exit or performance ads. */
public final class BannerAdController implements DefaultLifecycleObserver {
    public static final String HOME = "home_banner";
    public static final String DRAWING_PACK = "drawing_pack_banner";
    private static final String TEST_BANNER_ID = "ca-app-pub-3940256099942544/9214589741";

    private final Activity activity;
    private final FrameLayout container;
    private final String placement;
    private final View privacyOptions;
    private final AdsConsentManager consent;
    private final BannerRequestState state = new BannerRequestState();
    private final Runnable consentChanged = this::refresh;
    private final View.OnLayoutChangeListener layoutChanged = (v, l, t, r, b, ol, ot, or, ob) -> refresh();
    private AdView adView;

    public BannerAdController(@NonNull Activity activity, @NonNull FrameLayout container,
                              @NonNull String placement, @Nullable View privacyOptions) {
        if (!HOME.equals(placement) && !DRAWING_PACK.equals(placement)) {
            throw new IllegalArgumentException("Unknown banner placement");
        }
        this.activity = activity;
        this.container = container;
        this.placement = placement;
        this.privacyOptions = privacyOptions;
        consent = AdsConsentManager.get(activity);
        consent.addListener(consentChanged);
        container.addOnLayoutChangeListener(layoutChanged);
        if (privacyOptions != null) privacyOptions.setOnClickListener(v -> consent.showPrivacyOptions(activity));
        ((LifecycleOwner) activity).getLifecycle().addObserver(this);
    }

    @Override public void onResume(@NonNull LifecycleOwner owner) {
        state.resume();
        consent.resume(activity);
        if (adView != null) adView.resume();
        refresh();
    }

    @Override public void onPause(@NonNull LifecycleOwner owner) {
        state.pause();
        consent.pause(activity);
        if (adView != null) adView.pause();
    }

    private void refresh() {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        if (privacyOptions != null) {
            privacyOptions.setVisibility(consent.privacyOptionsRequired() ? View.VISIBLE : View.GONE);
            privacyOptions.setEnabled(!consent.isBusy());
        }
        if (state.updateConsent(consent.canRequestAds(), consent.revision())) disposeBanner();
        int pixels = container.getWidth() - container.getPaddingLeft() - container.getPaddingRight();
        int width = (int) (pixels / activity.getResources().getDisplayMetrics().density);
        if (!container.isShown() || !state.needsRequest(width)) return;
        disposeBanner();
        int token = state.request(width);
        AdView banner = new AdView(activity);
        adView = banner;
        banner.setAdUnitId(BuildConfig.DEBUG ? TEST_BANNER_ID : productionUnitId());
        // The existing v24 SDK supports standard anchored adaptive banners (50–90dp).
        AdSize size = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, width);
        banner.setAdSize(size);
        container.addView(banner, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        banner.setAdListener(new TelemetryListener(activity.getApplicationContext(), placement, state, token));
        banner.setOnPaidEventListener(value -> {
            if (state.isCurrent(token)) AdTelemetry.paid(activity, placement, value);
        });
        AdTelemetry.event(activity, "monetization_ad_request", placement);
        banner.loadAd(new AdRequest.Builder().build());
    }

    private static final class TelemetryListener extends AdListener {
        private final Context context;
        private final String placement;
        private final BannerRequestState state;
        private final int token;

        TelemetryListener(Context context, String placement, BannerRequestState state, int token) {
            this.context = context;
            this.placement = placement;
            this.state = state;
            this.token = token;
        }

        @Override public void onAdLoaded() {
            if (state.isCurrent(token)) AdTelemetry.event(context, "monetization_ad_loaded", placement);
        }

        @Override public void onAdFailedToLoad(@NonNull LoadAdError error) {
            if (state.isCurrent(token)) AdTelemetry.failure(context, placement, error.getCode());
            // AdMob's configured automatic refresh handles retries while the banner is visible.
        }

        @Override public void onAdImpression() {
            if (state.isCurrent(token)) AdTelemetry.event(context, "monetization_ad_impression", placement);
        }
    }

    private String productionUnitId() {
        return HOME.equals(placement) ? "ca-app-pub-9937617798998725/6628693119"
                : "ca-app-pub-9937617798998725/8292313909";
    }

    private void disposeBanner() {
        if (adView == null) return;
        adView.setAdListener(null);
        adView.setOnPaidEventListener(null);
        container.removeView(adView);
        adView.destroy();
        adView = null;
    }

    @Override public void onDestroy(@NonNull LifecycleOwner owner) {
        state.destroy();
        consent.pause(activity);
        consent.removeListener(consentChanged);
        container.removeOnLayoutChangeListener(layoutChanged);
        disposeBanner();
        owner.getLifecycle().removeObserver(this);
    }
}
