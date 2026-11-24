package com.h2play.canvas_magic.util;

/** Bundled, offline drawing content. Points are normalized within a square, not the display. */
public final class DrawingCatalog {
    private static final String[] IDS = {"little_surprises", "celebration", "good_luck", "night_sky"};
    private DrawingCatalog() {}
    public static int size() { return IDS.length; }
    public static String id(int index) { return IDS[index]; }
    public static int index(String id) {
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
        return -1;
    }
    // Each pack has three choices: left, centre, right on the secret screen.
    public static float[][] strokes(int pack, int choice) {
        if (pack < 0 || pack >= size() || choice < 0 || choice >= 3)
            throw new IllegalArgumentException("Unknown artwork");
        switch (pack * 3 + choice) {
            case 0: return new float[][] {heart()};
            case 1: return new float[][] {star(.5f,.48f,.33f,.14f,5)};
            case 2: return new float[][] {
                ellipse(.5f,.39f,.09f,.09f), ellipse(.5f,.22f,.10f,.12f),
                ellipse(.67f,.39f,.12f,.10f), ellipse(.5f,.56f,.10f,.12f),
                ellipse(.33f,.39f,.12f,.10f), p(.5f,.65f,.5f,.9f),
                p(.5f,.79f,.34f,.69f,.31f,.8f,.5f,.86f)};
            case 3: return new float[][] {
                p(.22f,.48f,.78f,.48f,.78f,.79f,.22f,.79f,.22f,.48f),
                p(.22f,.58f,.32f,.64f,.42f,.58f,.52f,.64f,.62f,.58f,.78f,.63f),
                p(.36f,.48f,.36f,.30f),p(.64f,.48f,.64f,.30f),
                ellipse(.36f,.23f,.025f,.055f),ellipse(.64f,.23f,.025f,.055f),
                p(.16f,.83f,.84f,.83f)};
            case 4: return new float[][] {ellipse(.5f,.36f,.23f,.29f),
                p(.5f,.65f,.45f,.70f,.55f,.70f,.5f,.65f),
                p(.5f,.70f,.58f,.77f,.43f,.84f,.50f,.93f)};
            case 5: return new float[][] {
                p(.22f,.43f,.78f,.43f,.78f,.82f,.22f,.82f,.22f,.43f),
                p(.18f,.35f,.82f,.35f,.82f,.43f,.18f,.43f,.18f,.35f),
                p(.47f,.35f,.47f,.82f),p(.55f,.35f,.55f,.82f),
                p(.5f,.35f,.30f,.29f,.27f,.18f,.36f,.15f,.5f,.35f,.64f,.15f,.73f,.18f,.70f,.29f,.5f,.35f)};
            case 6: return new float[][] {ellipse(.38f,.29f,.13f,.16f),
                ellipse(.62f,.29f,.13f,.16f),ellipse(.33f,.53f,.16f,.13f),
                ellipse(.64f,.53f,.16f,.13f),p(.49f,.58f,.54f,.73f,.45f,.9f)};
            case 7: return new float[][] {
                p(.2f,.3f,.38f,.5f,.5f,.2f,.62f,.5f,.8f,.3f,.73f,.76f,.27f,.76f,.2f,.3f),
                p(.27f,.65f,.73f,.65f),ellipse(.5f,.57f,.035f,.045f)};
            case 8: return new float[][] {
                p(.38f,.65f,.36f,.4f,.42f,.23f,.5f,.12f,.58f,.23f,.64f,.4f,.62f,.65f,.38f,.65f),
                ellipse(.5f,.38f,.075f,.075f),p(.37f,.48f,.23f,.64f,.22f,.77f,.38f,.65f),
                p(.63f,.48f,.77f,.64f,.78f,.77f,.62f,.65f),
                p(.42f,.7f,.5f,.91f,.58f,.7f)};
            case 9: return new float[][] {moon(),star(.76f,.25f,.07f,.03f,5)};
            case 10: return new float[][] {ellipse(.5f,.48f,.24f,.24f),
                p(.27f,.4f,.1f,.49f,.13f,.58f,.36f,.62f,.63f,.54f,.87f,.4f,.89f,.33f,.72f,.35f),
                p(.38f,.30f,.55f,.28f),p(.38f,.68f,.53f,.69f)};
            default: return new float[][] {star(.36f,.61f,.19f,.085f,5),
                p(.48f,.47f,.80f,.16f),p(.54f,.55f,.9f,.23f),p(.56f,.66f,.84f,.42f)};
        }
    }
    private static float[] p(float... points) { return points; }
    private static float[] ellipse(float x, float y, float rx, float ry) {
        float[] points = new float[98];
        for (int i=0;i<=48;i++) {
            double t=i*Math.PI*2/48;
            points[i*2]=x+rx*(float)Math.cos(t);
            points[i*2+1]=y+ry*(float)Math.sin(t);
        }
        return points;
    }
    private static float[] star(float x,float y,float outer,float inner,int tips) {
        float[] points=new float[(tips*2+1)*2];
        for(int i=0;i<=tips*2;i++) {
            double t=-Math.PI/2+i*Math.PI/tips;
            float r=i%2==0?outer:inner;
            points[2*i]=x+r*(float)Math.cos(t);
            points[2*i+1]=y+r*(float)Math.sin(t);
        }
        return points;
    }
    private static float[] heart() {
        float[] points=new float[194];
        for(int i=0;i<=96;i++) {
            double t=i*Math.PI*2/96;
            points[2*i]=.5f+(float)(16*Math.pow(Math.sin(t),3))*.021f;
            points[2*i+1]=.46f-(float)(13*Math.cos(t)-5*Math.cos(2*t)-2*Math.cos(3*t)-Math.cos(4*t))*.021f;
        }
        return points;
    }
    private static float[] moon() {
        float[] points=new float[130];
        // Continuous outer crescent and indented inner edge.
        for(int i=0;i<=32;i++) {
            double t=-Math.PI/2-i*Math.PI/32;
            points[i*2]=.56f+.33f*(float)Math.cos(t);
            points[i*2+1]=.5f+.33f*(float)Math.sin(t);
        }
        for(int i=1;i<=32;i++) {
            double t=Math.PI/2+i*Math.PI/32;
            points[(32+i)*2]=.56f+.16f*(float)Math.cos(t);
            points[(32+i)*2+1]=.5f+.33f*(float)Math.sin(t);
        }
        return points;
    }
}
