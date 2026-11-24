package com.h2play.canvas_magic.util;

import android.graphics.Color;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class DrawingPreviewRenderer {
    private DrawingPreviewRenderer() { }
    public static boolean draw(FabricView view, String json, int index) {
        view.cleanPage();
        view.setColor(Color.BLACK);
        view.setInteractionMode(FabricView.LOCKED_MODE);
        if (!DrawingPreviewData.hasInk(json, index)) return false;
        try {
            for (JsonElement element : DrawingPreviewData.actions(json, index)) {
                JsonObject action = element.getAsJsonObject();
                switch (action.get("action").getAsString()) {
                    case "down":
                        view.actionDown(x(view, action, "x"), y(view, action, "y")); break;
                    case "up":
                        view.actionUp(x(view, action, "x"), y(view, action, "y")); break;
                    case "move":
                        view.actionMove(x(view, action, "x1"), y(view, action, "y1"),
                                x(view, action, "x2"), y(view, action, "y2")); break;
                }
            }
            view.invalidate();
            return true;
        } catch (RuntimeException invalid) { view.cleanPage(); return false; }
    }
    private static float x(FabricView view, JsonObject action, String key) {
        return DrawingPreviewData.coordinate(action, key) * view.getWidth();
    }
    private static float y(FabricView view, JsonObject action, String key) {
        return DrawingPreviewData.coordinate(action, key) * view.getHeight();
    }
}
