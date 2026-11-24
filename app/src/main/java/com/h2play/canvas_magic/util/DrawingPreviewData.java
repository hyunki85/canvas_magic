package com.h2play.canvas_magic.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Read-only validation for community previews; never modifies saved drawing data. */
public final class DrawingPreviewData {
    private DrawingPreviewData() { }
    public static JsonArray actions(String json, int index) {
        return JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("shapes").get(index).getAsJsonArray();
    }
    public static int pageCount(String json, int declaredCount) {
        try {
            return Math.max(0, Math.min(declaredCount,
                    JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("shapes").size()));
        } catch (RuntimeException invalid) { return 0; }
    }
    public static boolean hasInk(String json, int index) {
        try {
            boolean started = false, ink = false;
            for (JsonElement element : actions(json, index)) {
                JsonObject action = element.getAsJsonObject();
                switch (action.get("action").getAsString()) {
                    case "down":
                        coordinate(action, "x"); coordinate(action, "y");
                        started = true; break;
                    case "move":
                        coordinate(action, "x1"); coordinate(action, "y1");
                        coordinate(action, "x2"); coordinate(action, "y2");
                        if (!started) return false;
                        ink = true; break;
                    case "up":
                        coordinate(action, "x"); coordinate(action, "y");
                        if (!started) return false;
                        ink = true; started = false; break;
                    default: return false;
                }
            }
            return ink;
        } catch (RuntimeException invalid) { return false; }
    }
    public static int firstDrawable(String json, int count) {
        int pages = pageCount(json, count);
        for (int i = 0; i < pages; i++) if (hasInk(json, i)) return i;
        return -1;
    }
    static float coordinate(JsonObject action, String key) {
        float value = action.get(key).getAsFloat();
        if (Float.isNaN(value) || Float.isInfinite(value)) throw new IllegalArgumentException("Invalid coordinate");
        return value;
    }
}
