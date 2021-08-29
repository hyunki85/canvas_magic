package com.h2play.canvas_magic.data.model.response;


import java.io.Serializable;
import java.util.Date;
import java.util.List;


public class ShapeOnline implements Serializable {
    public String id;
    public String name;
    public int star;
    public int count = 9;
    public Date date;
    public String json;
    public boolean featured = false;
    public boolean alreadyStar = false;
}
