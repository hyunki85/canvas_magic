package com.h2play.canvas_magic.util;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * A lightweight spotlight overlay that dims the screen and highlights a target view with a cutout
 * and ripple. Touches inside the spotlight are forwarded to the target view, so users can interact
 * with it while the guide is shown.
 */
public class SpotlightGuideOverlay extends View {

    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final ViewGroup parent;
    private View targetView;
    private final RectF targetRect = new RectF();
    private RectF customRect = null;
    private float paddingPx;

    private ValueAnimator rippleAnimator;
    private float rippleProgress = 0f; // 0..1

    private CharSequence guideText;
    private View touchForwardView;
    // Reentrancy guard to avoid infinite recursion when forwarding events
    private boolean isForwarding = false;

    public interface Listener {
        void onDismissed();
    }
    private Listener listener;

    public SpotlightGuideOverlay(Context context, ViewGroup parent) {
        super(context);
        this.parent = parent;
        init();
    }

    public SpotlightGuideOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.parent = null;
        init();
    }

    private void init() {
    // Make overlay non-interactive by default; we'll selectively consume outside the hole
    setClickable(false);
    setFocusable(false);

        // Dim background
        dimPaint.setColor(Color.parseColor("#99000000"));

        // Clear paint to cut a hole
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        // Ripple ring
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dp(2));
        ringPaint.setColor(Color.WHITE);

        // Text
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dp(14));
        textPaint.setShadowLayer(dp(2), 0, dp(1), 0x80000000);

        setLayerType(LAYER_TYPE_HARDWARE, null);

        // Window insets (status/navigation bars)
        ViewCompat.setOnApplyWindowInsetsListener(this, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            insetTop = sys.top;
            insetBottom = sys.bottom;
            computeTargetRect();
            invalidate();
            return insets;
        });
        ViewCompat.requestApplyInsets(this);
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    public void show() {
        if (getParent() == null && parent != null) {
            setPadding(0, 0, 0, 0);
            setFitsSystemWindows(false);
            // elevation이 있는 뷰(FAB, CardView) 위에 표시하기 위해
            // DecorView의 content 영역(android.R.id.content)에 추가
            ViewGroup decor = (ViewGroup) parent.getRootView().findViewById(android.R.id.content);
            if (decor != null) {
                decor.addView(this, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
            } else {
                parent.addView(this, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
            }
        }
        // 레이아웃 완료 후 좌표 재계산
        post(() -> {
            computeTargetRect();
            invalidate();
        });
        startRipple();
    }

    public void hide() {
        stopRipple();
        if (getParent() != null && getParent() instanceof ViewGroup) {
            ((ViewGroup) getParent()).removeView(this);
        }
        if (listener != null) listener.onDismissed();
    }

    public void highlight(View target, int paddingDp, @Nullable CharSequence text) {
        this.targetView = target;
        this.paddingPx = dp(paddingDp);
        this.guideText = text;
        this.customRect = null;
        this.touchForwardView = target;
        // show() 이후 post()에서 computeTargetRect()를 호출하므로 여기선 생략 가능
        // 단, 이미 화면에 붙어있는 경우에만 즉시 계산
        if (getParent() != null && getWidth() > 0 && getHeight() > 0) {
            computeTargetRect();
            invalidate();
        }
        // 아직 화면에 없으면 show()의 post()에서 처리됨
    }
        private int insetTop = 0;
        private int insetBottom = 0;

    public void highlightRect(RectF rectInParent, int paddingDp, @Nullable CharSequence text, @Nullable View touchDelegate) {
        this.targetView = null;
        this.touchForwardView = touchDelegate;
        this.paddingPx = dp(paddingDp);
        this.guideText = text;
        if (rectInParent != null) {
            this.customRect = new RectF(
                    rectInParent.left - paddingPx,
                    rectInParent.top - paddingPx,
                    rectInParent.right + paddingPx,
                    rectInParent.bottom + paddingPx
            );
        } else {
            this.customRect = null;
        }
        invalidate();
    }

    private void computeTargetRect() {
        if (targetView == null) return;
        // 타깃 뷰와 오버레이 모두 getLocationOnScreen으로 절대 좌표 구한 뒤 변환
        int[] targetLoc = new int[2];
        targetView.getLocationOnScreen(targetLoc);
        int[] overlayLoc = new int[2];
        getLocationOnScreen(overlayLoc);

        float left = targetLoc[0] - overlayLoc[0] - paddingPx;
        float top = targetLoc[1] - overlayLoc[1] - paddingPx;
        float right = left + targetView.getWidth() + paddingPx * 2;
        float bottom = top + targetView.getHeight() + paddingPx * 2;
        targetRect.set(left, top, right, bottom);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeTargetRect();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Prepare layer for CLEAR mode
    int save = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);

    // Dim 전체 오버레이 영역 (오버레이는 이미 콘텐츠 영역에 배치됨)
    canvas.drawRect(0, 0, getWidth(), getHeight(), dimPaint);

    // Determine rect to highlight
    RectF hole = (customRect != null) ? customRect : targetRect;
    // Cut a round-rect hole over the target/custom area
    float radius = Math.min(hole.width(), hole.height()) / 2f;
    canvas.drawRoundRect(hole, radius, radius, clearPaint);

        // Ripple ring around the target rect center
        if (!((customRect != null) ? customRect : targetRect).isEmpty()) {
            RectF src = (customRect != null) ? customRect : targetRect;
            float cx = src.centerX();
            float cy = src.centerY();
            float baseR = Math.max(src.width(), src.height()) / 2f + dp(6);
            float ringR = baseR + dp(12) * rippleProgress;
            int alpha = (int) ((1f - rippleProgress) * 255);
            ringPaint.setAlpha(alpha);
            canvas.drawCircle(cx, cy, ringR, ringPaint);
        }

        // Optional helper text just above the target
        if (guideText != null && guideText.length() > 0) {
            RectF src = (customRect != null) ? customRect : targetRect;
            float textWidth = textPaint.measureText(guideText.toString());
            float margin = dp(12);
            float x = Math.max(margin, Math.min(getWidth() - textWidth - margin, src.centerX() - textWidth / 2));
            float lineHeight = textPaint.getTextSize();
            float availableBottom = getHeight() - insetBottom - margin;
            float availableTop = insetTop + margin + lineHeight;
            // 기본: 타깃 위에 표시, 공간 부족하면 아래로 이동
            float desiredY = src.top - margin;
            float y = desiredY;
            if (y < availableTop) {
                y = Math.min(availableBottom, src.bottom + margin + lineHeight);
            }
            textPaint.setAlpha(230);
            canvas.drawText(guideText.toString(), x, y, textPaint);
        }

        canvas.restoreToCount(save);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Pass-through inside the spotlight hole: return false so parent can dispatch to underlying child
        RectF hole = (customRect != null) ? customRect : targetRect;
        if (hole.contains(event.getX(), event.getY())) {
            return false;
        }
        // Outside the hole: consume to block background interaction
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Align onTouchEvent with dispatchTouchEvent behavior
        RectF hole = (customRect != null) ? customRect : targetRect;
        if (hole.contains(event.getX(), event.getY())) {
            return false;
        }
        return true;
    }

    private void startRipple() {
        stopRipple();
        rippleAnimator = ValueAnimator.ofFloat(0f, 1f);
        rippleAnimator.setDuration(1200);
        rippleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rippleAnimator.addUpdateListener(a -> {
            rippleProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        rippleAnimator.start();
    }

    private void stopRipple() {
        if (rippleAnimator != null) {
            rippleAnimator.cancel();
            rippleAnimator = null;
        }
    }

    private float dp(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
