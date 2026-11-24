package com.h2play.canvas_magic.util;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** Passive usage dates only; never schedules content, challenges or notifications. */
public final class DrawingUsagePolicy {
    private DrawingUsagePolicy() {}
    public static String dayKey(long time, TimeZone zone) {
        Calendar local=Calendar.getInstance(zone, Locale.ROOT);
        local.setTimeInMillis(time);
        return String.format(Locale.ROOT,"%04d-%02d-%02d",local.get(Calendar.YEAR),
                local.get(Calendar.MONTH)+1,local.get(Calendar.DAY_OF_MONTH));
    }
    public static boolean newRevealDay(String today,String last) {
        return today!=null && today.matches("\\d{4}-\\d{2}-\\d{2}")
                && (last==null || today.compareTo(last)>0);
    }
}
