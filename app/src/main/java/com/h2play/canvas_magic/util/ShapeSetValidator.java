package com.h2play.canvas_magic.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;

/** Strict boundary validation for drawing sets received from or sent to the community. */
public final class ShapeSetValidator {
    public static final int MAX_BYTES = 512 * 1024;
    public static final int MAX_ACTIONS_PER_DRAWING = 5_000;
    public static final int MAX_ACTIONS_TOTAL = 20_000;

    private ShapeSetValidator() { }

    public static boolean isValidCommunitySet(String json, int declaredCount) {
        if (!isAllowedCount(declaredCount) || json == null
                || json.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) return false;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray shapes = root.getAsJsonArray("shapes");
            if (shapes == null || shapes.size() != declaredCount) return false;
            int total = 0;
            boolean anyInk = false;
            for (JsonElement shapeElement : shapes) {
                JsonArray actions = shapeElement.getAsJsonArray();
                if (actions.size() > MAX_ACTIONS_PER_DRAWING) return false;
                total += actions.size();
                if (total > MAX_ACTIONS_TOTAL) return false;
                boolean strokeStarted = false;
                for (JsonElement actionElement : actions) {
                    JsonObject action = actionElement.getAsJsonObject();
                    String type = action.get("action").getAsString();
                    switch (type) {
                        case "down":
                            coordinate(action, "x"); coordinate(action, "y");
                            strokeStarted = true;
                            break;
                        case "move":
                            if (!strokeStarted) return false;
                            coordinate(action, "x1"); coordinate(action, "y1");
                            coordinate(action, "x2"); coordinate(action, "y2");
                            anyInk = true;
                            break;
                        case "up":
                            if (!strokeStarted) return false;
                            coordinate(action, "x"); coordinate(action, "y");
                            strokeStarted = false;
                            anyInk = true;
                            break;
                        default:
                            return false;
                    }
                }
                if (strokeStarted) return false;
            }
            return anyInk;
        } catch (RuntimeException invalid) {
            return false;
        }
    }

    public static boolean isAllowedCount(int count) {
        return count == 6 || count == 9 || count == 12 || count == 15;
    }

    private static float coordinate(JsonObject action, String key) {
        float value = action.get(key).getAsFloat();
        if (!Float.isFinite(value) || value < 0f || value > 1f) {
            throw new IllegalArgumentException("Coordinate outside normalized drawing space");
        }
        return value;
    }
}
