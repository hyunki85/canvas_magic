package com.h2play.canvas_magic.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

/**
 * 롱프레스 진행 상황을 보여주는 원형 프로그레스 뷰
 * 
 * 사용법:
 * - show()로 표시하고 애니메이션 시작
 * - 완료 시 OnCompleteListener 콜백 호출
 * - hide()로 숨기거나 취소
 */
public class LongPressProgressView extends View {

    private static final int DEFAULT_DURATION = 1500; // 1.5초
    private static final int STROKE_WIDTH_DP = 4;
    private static final int SIZE_DP = 80;

    private Paint backgroundPaint;
    private Paint progressPaint;
    private Paint iconPaint;
    private RectF arcRect;
    
    private float progress = 0f; // 0 ~ 1
    private int duration = DEFAULT_DURATION;
    private ValueAnimator animator;
    
    private OnCompleteListener completeListener;
    private int progressColor = Color.parseColor("#4CAF50"); // Green
    private int backgroundColor = Color.parseColor("#E0E0E0");

    public interface OnCompleteListener {
        void onComplete();
    }

    public LongPressProgressView(Context context) {
        super(context);
        init();
    }

    public LongPressProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LongPressProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        float strokeWidth = STROKE_WIDTH_DP * density;

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(strokeWidth);
        backgroundPaint.setColor(backgroundColor);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setColor(progressColor);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setStyle(Paint.Style.FILL);
        iconPaint.setColor(Color.WHITE);
        iconPaint.setTextSize(24 * density);
        iconPaint.setTextAlign(Paint.Align.CENTER);

        arcRect = new RectF();
        
        setVisibility(GONE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float density = getResources().getDisplayMetrics().density;
        float strokeWidth = STROKE_WIDTH_DP * density;
        float padding = strokeWidth / 2 + 4 * density;
        arcRect.set(padding, padding, w - padding, h - padding);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 배경 원
        canvas.drawArc(arcRect, 0, 360, false, backgroundPaint);

        // 진행 호
        float sweepAngle = 360 * progress;
        canvas.drawArc(arcRect, -90, sweepAngle, false, progressPaint);

        // 중앙 아이콘 (손가락 누르기)
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        
        // 진행률 텍스트
        String text = String.format("%.0f%%", progress * 100);
        iconPaint.setTextSize(16 * getResources().getDisplayMetrics().density);
        canvas.drawText(text, centerX, centerY + iconPaint.getTextSize() / 3, iconPaint);
    }

    /**
     * 프로그레스 표시 시작
     */
    public void show() {
        setVisibility(VISIBLE);
        setAlpha(0f);
        animate().alpha(1f).setDuration(150).start();
        startProgress();
    }

    /**
     * 프로그레스 숨기기 및 취소
     */
    public void hide() {
        if (animator != null) {
            animator.cancel();
        }
        animate().alpha(0f).setDuration(150).withEndAction(() -> {
            setVisibility(GONE);
            progress = 0f;
            invalidate();
        }).start();
    }

    /**
     * 프로그레스 리셋
     */
    public void reset() {
        if (animator != null) {
            animator.cancel();
        }
        progress = 0f;
        invalidate();
    }

    private void startProgress() {
        if (animator != null) {
            animator.cancel();
        }

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(duration);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (progress >= 1f && completeListener != null) {
                    completeListener.onComplete();
                }
            }
        });
        animator.start();
    }

    public void setOnCompleteListener(OnCompleteListener listener) {
        this.completeListener = listener;
    }

    public void setProgressColor(int color) {
        this.progressColor = color;
        progressPaint.setColor(color);
        invalidate();
    }

    public void setDuration(int durationMs) {
        this.duration = durationMs;
    }

    public boolean isAnimating() {
        return animator != null && animator.isRunning();
    }
}
