package com.h2play.canvas_magic.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.TextView;

import com.h2play.canvas_magic.R;

public class GuideView {

    private View guideView;
    private GuideAnimationView animationView;
    private TextView titleTextView;
    private TextView descriptionTextView;
    private ViewGroup parentView;
    private AnimatorSet showAnimator;
    private AnimatorSet hideAnimator;
    
    public interface GuideListener {
        void onGuideCompleted();
    }
    
    private GuideListener listener;

    public GuideView(Context context, ViewGroup parent) {
        this.parentView = parent;
        
        // 가이드 뷰 인플레이트
        LayoutInflater inflater = LayoutInflater.from(context);
        guideView = inflater.inflate(R.layout.layout_guide_view, parent, false);
        
        // 뷰 요소 찾기
        animationView = guideView.findViewById(R.id.guide_animation_view);
        titleTextView = guideView.findViewById(R.id.text_guide_title);
        descriptionTextView = guideView.findViewById(R.id.text_guide_description);
        
        // 초기 설정 - 숨김 상태로 시작
        guideView.setAlpha(0f);
        guideView.setScaleX(0.8f);
        guideView.setScaleY(0.8f);
        
        // 애니메이션 준비
        prepareAnimations();
    }
    
    private void prepareAnimations() {
        // 표시 애니메이션
        showAnimator = new AnimatorSet();
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(guideView, "alpha", 0f, 1f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(guideView, "scaleX", 0.8f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(guideView, "scaleY", 0.8f, 1f);
        
        showAnimator.playTogether(fadeIn, scaleX, scaleY);
        showAnimator.setDuration(500);
        showAnimator.setInterpolator(new AnticipateOvershootInterpolator(1.0f));
        
        // 숨김 애니메이션
        hideAnimator = new AnimatorSet();
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(guideView, "alpha", 1f, 0f);
        ObjectAnimator scaleXDown = ObjectAnimator.ofFloat(guideView, "scaleX", 1f, 0.8f);
        ObjectAnimator scaleYDown = ObjectAnimator.ofFloat(guideView, "scaleY", 1f, 0.8f);
        
        hideAnimator.playTogether(fadeOut, scaleXDown, scaleYDown);
        hideAnimator.setDuration(300);
        hideAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        hideAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (guideView.getParent() != null) {
                    parentView.removeView(guideView);
                }
                if (listener != null) {
                    listener.onGuideCompleted();
                }
            }
        });
    }
    
    public void setGuideListener(GuideListener listener) {
        this.listener = listener;
    }
    
    public void setTitle(String title) {
        titleTextView.setText(title);
    }
    
    public void setTitle(int titleResId) {
        titleTextView.setText(titleResId);
    }
    
    public void setDescription(String description) {
        descriptionTextView.setText(description);
    }
    
    public void setDescription(int descriptionResId) {
        descriptionTextView.setText(descriptionResId);
    }
    
    public void setAnimationType(boolean isLongPress) {
        animationView.setLongPressAnimation(isLongPress);
    }
    
    public void show() {
        if (guideView.getParent() == null) {
            parentView.addView(guideView);
        }
        showAnimator.start();
    }
    
    public void hide() {
        if (hideAnimator.isRunning()) {
            return;
        }
        hideAnimator.start();
    }
    
    public void updatePosition(float x, float y) {
        // 화면에 가이드뷰가 추가되기 전에는 크기를 알 수 없음
        if (guideView.getWidth() == 0) {
            // 처음 레이아웃 계산 후 위치를 조정하기 위해 post 사용
            guideView.post(() -> updatePosition(x, y));
            return;
        }
        
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) guideView.getLayoutParams();
        
        // x 위치: 가이드뷰를 화면 중앙에 배치하거나 지정된 x 좌표 중심에 배치
        if (x > 0) {
            params.leftMargin = Math.max(0, Math.round(x - guideView.getWidth() / 2));
        } else {
            // x가 지정되지 않은 경우 화면 중앙에 배치
            params.leftMargin = (parentView.getWidth() - guideView.getWidth()) / 2;
        }
        
        // y 위치: 화면 중앙 근처에 배치하거나 지정된 y 좌표에 배치
        if (y > 0) {
            // y 위치에 가이드뷰를 표시하되, 상단이 아닌 중앙에 배치
            params.topMargin = Math.max(0, Math.round(y - guideView.getHeight() / 2));
        } else {
            // y가 지정되지 않은 경우 화면 중앙보다 약간 위쪽에 배치
            params.topMargin = (parentView.getHeight() - guideView.getHeight()) / 3;
        }
        
        // 화면 경계 체크
        if (params.leftMargin + guideView.getWidth() > parentView.getWidth()) {
            params.leftMargin = parentView.getWidth() - guideView.getWidth();
        }
        
        if (params.topMargin + guideView.getHeight() > parentView.getHeight()) {
            params.topMargin = parentView.getHeight() - guideView.getHeight();
        }
        
        guideView.setLayoutParams(params);
    }
    
    public void updateToTargetPosition(View targetView, int offsetX, int offsetY) {
        // parentView 좌표계로 변환해서 정확히 정렬
        int[] parentLoc = new int[2];
        int[] targetLoc = new int[2];
        parentView.getLocationOnScreen(parentLoc);
        targetView.getLocationOnScreen(targetLoc);

        float centerXInParent = (targetLoc[0] - parentLoc[0]) + (targetView.getWidth() / 2f);
        float centerYInParent = (targetLoc[1] - parentLoc[1]) + (targetView.getHeight() / 2f);

        float x = centerXInParent + offsetX;
        float y = centerYInParent + offsetY;

        updatePosition(x, y);
    }
    
    public View getView() {
        return guideView;
    }
}