package com.h2play.canvas_magic.util.ads;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.ads.AdValue;
import com.h2play.canvas_magic.util.Analytics;

final class AdTelemetry {
    private AdTelemetry() { }

    private static Bundle parameters(String placement) {
        Bundle params = new Bundle();
        params.putString("placement", placement);
        params.putString("ad_format", "banner");
        return params;
    }

    static void event(Context context, String event, String placement) {
        Analytics.log(context, event, parameters(placement));
    }

    static void failure(Context context, String placement, int code) {
        Bundle params = parameters(placement);
        params.putLong("error_code", code);
        Analytics.log(context, "monetization_ad_load_failed", params);
    }

    static void paid(Context context, String placement, AdValue value) {
        Bundle params = parameters(placement);
        params.putLong("value_micros", value.getValueMicros());
        params.putDouble("value", value.getValueMicros() / 1_000_000.0);
        params.putString("currency", value.getCurrencyCode());
        params.putLong("precision_type", value.getPrecisionType());
        // A custom event avoids double-counting Firebase's automatic ad_impression revenue.
        Analytics.log(context, "monetization_ad_paid", params);
    }
}
