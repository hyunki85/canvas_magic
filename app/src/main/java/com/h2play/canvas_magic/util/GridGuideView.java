package com.h2play.canvas_magic.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * 동적으로 그리드와 숫자를 그리는 가이드 뷰
 * 화면 비율에 맞게 자동으로 조정됨
 */
public class GridGuideView extends View {

    private Paint linePaint;
    private Paint textPaint;
    private int gridCount = 9; // 기본값: 9칸 (3x3)
    private int columns = 3;

    // 색상 및 스타일 설정
    private int lineColor = Color.parseColor("#E0E0E0");
    private int textColor = Color.parseColor("#FFEB3B"); // 노란색
    private float lineWidth = 2f;

    public GridGuideView(Context context) {
        super(context);
        init();
    }

    public GridGuideView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GridGuideView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 라인 페인트 설정
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(lineColor);
        linePaint.setStrokeWidth(lineWidth);
        linePaint.setStyle(Paint.Style.STROKE);

        // 텍스트 페인트 설정
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));
    }

    /**
     * 그리드 개수 설정 (예: 9, 12, 15 등)
     * @param count 총 그리드 칸 수 (3의 배수)
     */
    public void setGridCount(int count) {
        this.gridCount = count;
        invalidate();
    }

    /**
     * 열 개수 설정 (기본: 3)
     */
    public void setColumns(int columns) {
        this.columns = columns;
        invalidate();
    }

    /**
     * 라인 색상 설정
     */
    public void setLineColor(int color) {
        this.lineColor = color;
        linePaint.setColor(color);
        invalidate();
    }

    /**
     * 숫자 색상 설정
     */
    public void setTextColor(int color) {
        this.textColor = color;
        textPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        if (width == 0 || height == 0) return;

        int rows = gridCount / columns;
        float cellWidth = (float) width / columns;
        float cellHeight = (float) height / rows;

        // 텍스트 크기 동적 계산 (셀 크기의 약 40%)
        float textSize = Math.min(cellWidth, cellHeight) * 0.4f;
        textPaint.setTextSize(textSize);

        // 각 셀에 숫자 그리기
        for (int i = 0; i < gridCount; i++) {
            int col = i % columns;
            int row = i / columns;

            float centerX = col * cellWidth + cellWidth / 2;
            float centerY = row * cellHeight + cellHeight / 2;

            // 텍스트를 수직 중앙에 맞추기 위한 조정
            Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
            float textY = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2;

            // 숫자 그리기
            canvas.drawText(String.valueOf(i + 1), centerX, textY, textPaint);
        }

        // 세로선 그리기
        for (int i = 1; i < columns; i++) {
            float x = i * cellWidth;
            canvas.drawLine(x, 0, x, height, linePaint);
        }

        // 가로선 그리기
        for (int i = 1; i < rows; i++) {
            float y = i * cellHeight;
            canvas.drawLine(0, y, width, y, linePaint);
        }
    }
}
