package com.h2play.canvas_magic.data.model;

import com.h2play.canvas_magic.data.model.response.ShapeInfo;

import java.util.List;
import java.util.Objects;

/** Stable reference to either an included drawing set or a user-owned file. */
public final class PackRef {
    public enum Kind { BUILT_IN, CUSTOM }

    public final Kind kind;
    public final String id;

    private PackRef(Kind kind, String id) {
        this.kind = kind;
        this.id = id == null ? "" : id;
    }

    public static PackRef builtIn(String catalogId) {
        return new PackRef(Kind.BUILT_IN, catalogId);
    }

    public static PackRef custom(String fileName) {
        return new PackRef(Kind.CUSTOM, fileName);
    }

    public static PackRef restore(String kind, String id) {
        if (id == null || id.isEmpty()) return null;
        try {
            return new PackRef(Kind.valueOf(kind), id);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public int findCustomIndex(List<ShapeInfo> shapes) {
        if (kind != Kind.CUSTOM || shapes == null) return -1;
        for (int index = 0; index < shapes.size(); index++) {
            ShapeInfo shape = shapes.get(index);
            if (shape != null && id.equals(shape.fileName)) return index;
        }
        return -1;
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PackRef)) return false;
        PackRef that = (PackRef) other;
        return kind == that.kind && id.equals(that.id);
    }

    @Override public int hashCode() {
        return Objects.hash(kind, id);
    }
}
