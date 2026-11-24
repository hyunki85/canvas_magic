package com.h2play.canvas_magic.util;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

/** Same source strokes in the preview and actual reveal; no network/video dependency. */
public final class DrawingArtworkView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path drawingPath=new Path();
    private float[][] artwork=DrawingCatalog.strokes(0,0);
    private int pack,choice;
    private float progress=1;
    private ValueAnimator animator;
    public DrawingArtworkView(Context c) { this(c,null); }
    public DrawingArtworkView(Context c, AttributeSet a) {
        super(c,a);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(0xff7457df);
        paint.setStrokeWidth(3*c.getResources().getDisplayMetrics().density);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    public void show(int pack,int choice) {
        this.pack=pack; this.choice=choice;
        artwork=DrawingCatalog.strokes(pack,choice);
        progress=1; invalidate();
    }
    public void replay() {
        if(animator!=null) animator.cancel();
        if(Build.VERSION.SDK_INT>=26 && !ValueAnimator.areAnimatorsEnabled()) {
            progress=1; invalidate(); return;
        }
        animator=ValueAnimator.ofFloat(0,1);
        animator.setDuration(1600);
        animator.addUpdateListener(v -> { progress=(float)v.getAnimatedValue(); invalidate(); });
        animator.start();
    }
    @Override protected void onDetachedFromWindow() {
        if(animator!=null) animator.cancel();
        super.onDetachedFromWindow();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size=Math.min(getWidth(),getHeight())*.92f;
        float x=(getWidth()-size)/2,y=(getHeight()-size)/2;
        float[][] strokes=artwork;
        int total=0; for(float[] s:strokes) total+=s.length/2;
        int remaining=Math.round(progress*total);
        for(float[] s:strokes) {
            Path path=drawingPath;
            path.reset(); path.moveTo(x+s[0]*size,y+s[1]*size);
            int count=Math.min(s.length/2,remaining);
            for(int i=1;i<count;i++) path.lineTo(x+s[i*2]*size,y+s[i*2+1]*size);
            if(count==s.length/2 && closed(s)) path.close();
            canvas.drawPath(path,paint);
            remaining-=count;
            if(remaining<=0) break;
        }
    }
    public static boolean reveal(FabricView view,int pack,int choice) {
        if(view.getWidth()<=0 || view.getHeight()<=0) return false;
        float size=Math.min(view.getWidth()*.78f,view.getHeight()*.56f);
        float x=(view.getWidth()-size)/2,y=(view.getHeight()-size)/2;
        for(float[] s:DrawingCatalog.strokes(pack,choice)) {
            view.actionDown(x+s[0]*size,y+s[1]*size);
            for(int i=2;i<s.length;i+=2) view.actionUp(x+s[i]*size,y+s[i+1]*size);
            com.h2play.canvas_magic.util.DrawableObjects.CDrawable stroke =
                    view.getDrawables().get(view.getDrawables().size()-1);
            stroke.getPaint().setStrokeCap(Paint.Cap.ROUND);
            if(closed(s) && stroke instanceof com.h2play.canvas_magic.util.DrawableObjects.CPath) {
                ((com.h2play.canvas_magic.util.DrawableObjects.CPath)stroke).getPath().close();
            }
        }
        view.invalidate();
        return true;
    }
    private static boolean closed(float[] stroke) {
        return Math.abs(stroke[0]-stroke[stroke.length-2])<.0001f
                && Math.abs(stroke[1]-stroke[stroke.length-1])<.0001f;
    }
}
