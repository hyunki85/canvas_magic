package com.h2play.canvas_magic.data.model.response;

import java.util.List;

public class ShapeInfo {
    public ShapeInfo(String fileName, String name, int count) {
        this.fileName = fileName;
        this.name = name;
        this.count = count;
    }

    public String fileName;
    public String name;
    public int count = 9;
}
