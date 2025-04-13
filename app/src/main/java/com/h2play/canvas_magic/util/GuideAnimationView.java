package com.h2play.canvas_magic.util;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import androidx.core.content.ContextCompat;

import com.h2play.canvas_magic.R;

public class GuideAnimationView extends View {

    private Paint circlePaint;
    private Paint fingerPaint;
    private Paint ripplePaint;
    
    private Path fingerPath;
    private RectF fingerOvalRect;
    
    private PointF fingerPosition = new PointF(0f, 0f);
    private float rippleRadius = 0f;
    private float rippleAlpha = 1f;
    private float scale = 0f;
    
    private AnimatorSet animatorSet;
    private ValueAnimator fingerAnimator; // 새로 추가된 ValueAnimator
    private boolean isLongPressAnimation = true;

    public GuideAnimationView(Context context) {
        super(context);
        init();
    }

    public GuideAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GuideAnimationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.parseColor("#22FF0000"));
        circlePaint.setStyle(Paint.Style.FILL);
        
        ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ripplePaint.setColor(Color.parseColor("#33FF0000"));
        ripplePaint.setStyle(Paint.Style.FILL);
        
        fingerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fingerPaint.setColor(Color.WHITE);
        fingerPaint.setStyle(Paint.Style.FILL);
        fingerPaint.setShadowLayer(8, 0, 4, Color.parseColor("#80000000"));
        
        fingerPath = new Path();
        fingerOvalRect = new RectF();
        
        setLayerType(LAYER_TYPE_SOFTWARE, null); // Needed for shadow
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animatorSet != null) {
            animatorSet.cancel();
        }
        if (fingerAnimator != null) {
            fingerAnimator.cancel();
        }
    }
    
    public void setLongPressAnimation(boolean isLongPress) {
        this.isLongPressAnimation = isLongPress;
        if (animatorSet != null) {
            animatorSet.cancel();
        }
        if (fingerAnimator != null) {
            fingerAnimator.cancel();
        }
        startAnimation();
    }

    private void startAnimation() {
        if (animatorSet != null) {
            animatorSet.cancel();
        }
        if (fingerAnimator != null) {
            fingerAnimator.cancel();
        }
        
        animatorSet = new AnimatorSet();
        
        if (isLongPressAnimation) {
            // 롱프레스 애니메이션
            ObjectAnimator scaleUpAnim = ObjectAnimator.ofFloat(this, "scale", 0f, 1f);
            scaleUpAnim.setDuration(400);
            scaleUpAnim.setInterpolator(new OvershootInterpolator());
            
            ValueAnimator rippleAnim = ValueAnimator.ofFloat(0f, 80f);
            rippleAnim.setDuration(1500);
            rippleAnim.setRepeatCount(ValueAnimator.INFINITE);
            rippleAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            rippleAnim.addUpdateListener(animation -> {
                rippleRadius = (float) animation.getAnimatedValue();
                rippleAlpha = 1f - animation.getAnimatedFraction();
                invalidate();
            });
            
            animatorSet.playSequentially(scaleUpAnim, rippleAnim);
        } else {
            // 탭 애니메이션
            ObjectAnimator scaleUpAnim = ObjectAnimator.ofFloat(this, "scale", 0f, 1f);
            scaleUpAnim.setDuration(300);
            scaleUpAnim.setInterpolator(new OvershootInterpolator());
            
            // 손가락 애니메이션을 별도의 ValueAnimator로 관리
            fingerAnimator = ValueAnimator.ofFloat(0f, 20f, 0f, 20f, 0f);
            fingerAnimator.setDuration(2000);
            fingerAnimator.setInterpolator(new LinearInterpolator());
            fingerAnimator.setRepeatCount(ValueAnimator.INFINITE);
            fingerAnimator.setRepeatMode(ValueAnimator.RESTART);
            fingerAnimator.addUpdateListener(animation -> {
                fingerPosition.y = (float) animation.getAnimatedValue();
                invalidate();
            });
            
            // 첫 애니메이션은 스케일업만 진행
            animatorSet.play(scaleUpAnim);
            animatorSet.setStartDelay(500);
            
            // 스케일업 애니메이션 후 손가락 애니메이션 시작
            scaleUpAnim.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    fingerAnimator.start();
                }
            });
        }
        
        animatorSet.start();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        
        // 스케일에 따른 크기 조정
        float fingerSize = 40 * scale;
        
        // 물결 효과 (롱프레스 애니메이션에만 표시)
        if (isLongPressAnimation && rippleRadius > 0) {
            ripplePaint.setAlpha((int) (rippleAlpha * 255));
            canvas.drawCircle(centerX, centerY, rippleRadius, ripplePaint);
        }
        
        // 터치할 위치 표시 원
        canvas.drawCircle(centerX, centerY, 30 * scale, circlePaint);
        
        // 손가락 그리기
        if (fingerSize > 0) {
            float fingerOffsetY = isLongPressAnimation ? 0f : fingerPosition.y;
            
            // 손가락 경로 생성
            fingerPath.reset();
            
            // 손가락 타원형 상단
            fingerOvalRect.set(centerX - fingerSize, centerY - fingerSize * 2 + fingerOffsetY, 
                               centerX + fingerSize, centerY + fingerOffsetY);
            fingerPath.addOval(fingerOvalRect, Path.Direction.CW);
            
            canvas.drawPath(fingerPath, fingerPaint);
        }
    }
    
    // 애니메이터를 위한 setter 메서드들
    public void setScale(float scale) {
        this.scale = scale;
        invalidate();
    }
    
    public float getScale() {
        return scale;
    }
    
    public void setFingerPosition(float position) {
        this.fingerPosition.y = position;
        invalidate();
    }
    
    public float getFingerPosition() {
        return fingerPosition.y;
    }
}