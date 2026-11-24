package com.h2play.canvas_magic.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/** A live sample of the actual brush, including its alpha and pixel width. */
public class StrokePreviewView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    public StrokePreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    public void setBrush(int color, float width) {
        paint.setColor(color);
        paint.setStrokeWidth(width);
        invalidate();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float pad = Math.max(paint.getStrokeWidth() / 2 + 2, getWidth() * .08f);
        float mid = getHeight() / 2f;
        Path path = new Path();
        path.moveTo(pad, mid);
        path.cubicTo(getWidth() * .35f, mid - getHeight() * .35f,
                getWidth() * .65f, mid + getHeight() * .35f, getWidth() - pad, mid);
        canvas.drawPath(path, paint);
    }
}
