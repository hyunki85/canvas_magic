package com.h2play.canvas_magic.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import java.util.TimeZone;

/** Saved selection and passive usage metrics. No calendar content, goals, cohorts or reminders. */
public final class DrawingExperience {
    // Keep the wire key and preferences readable for installs of the earlier test build.
    public static final String EXTRA_PACK = "weekly_pack_id";
    private DrawingExperience() {}
    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("canvas_weekly_v1",Context.MODE_PRIVATE);
    }
    public static String lastPack(Context c) {
        String id = prefs(c).getString("last_drawing_pack", null);
        return DrawingCatalog.index(id) >= 0 ? id : null;
    }
    public static void rememberPack(Context c, String id) {
        SharedPreferences.Editor edit = prefs(c).edit();
        if (DrawingCatalog.index(id) >= 0) edit.putString("last_drawing_pack", id);
        else edit.remove("last_drawing_pack");
        edit.apply();
    }
    public static void log(Context c, String name, String id) {
        Bundle b=new Bundle();
        if (DrawingCatalog.index(id) >= 0) b.putString("pack_id",id);
        Analytics.log(c,name,b);
    }
    /** Only actual, non-tutorial rendering reaches here. Existing historical state is preserved. */
    public static synchronized void recordReveal(Context c,String id) {
        SharedPreferences p=prefs(c);
        if (!p.contains("zone")) p.edit().putString("zone",TimeZone.getDefault().getID()).apply();
        TimeZone zone=TimeZone.getTimeZone(p.getString("zone",TimeZone.getDefault().getID()));
        String today=DrawingUsagePolicy.dayKey(System.currentTimeMillis(),zone);
        String last=p.getString("last_reveal_day","");
        if(DrawingUsagePolicy.newRevealDay(today,last)) {
            SharedPreferences.Editor edit=p.edit().putString("last_reveal_day",today);
            if(!p.contains("first_reveal_day")) {
                edit.putString("first_reveal_day",today);
                log(c,"first_observed_reveal",id);
            } else {
                log(c,"return_day_reveal",id);
                if(!p.getBoolean("second_day_recorded",false)) {
                    edit.putBoolean("second_day_recorded",true);
                    log(c,"second_day_reveal",id);
                }
            }
            edit.apply();
        }
        if(DrawingCatalog.index(id)>=0) log(c,"drawing_set_reveal",id);
    }
}
