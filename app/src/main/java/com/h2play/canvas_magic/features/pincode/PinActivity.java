package com.h2play.canvas_magic.features.pincode;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

import com.f2prateek.dart.Dart;
import com.f2prateek.dart.InjectExtra;
import com.h2play.canvas_magic.R;
import com.h2play.canvas_magic.data.model.response.ShapeOnline;
import com.h2play.canvas_magic.features.base.BaseActivity;
import com.h2play.canvas_magic.features.common.ErrorView;
import com.h2play.canvas_magic.features.preview.PreviewActivity;
import com.h2play.canvas_magic.injection.component.ActivityComponent;
import com.h2play.canvas_magic.util.GuideView; // GuideView 클래스 임포트 추가
import com.h2play.canvas_magic.util.ViewUtil;

import timber.log.Timber;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public class PinActivity extends BaseActivity implements PinMvpView, ErrorView.ErrorListener {

    public static final String PIN = "pin";
    @InjectExtra
    Integer count;
    @Inject
    PinPresenter pinPresenter;
    private long lastTouchTime;
    private int width;
    private int height;
    private int lastIndex;

    public static Intent getStartIntent(Context context, int count) {
        Intent intent = new Intent(context, PinActivity.class);
        intent.putExtra("count", count);
        return intent;
    }

    public boolean onMainClick(View view, MotionEvent motionEvent) {

        if (motionEvent.getAction() != MotionEvent.ACTION_UP)
            return true;

        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();

        int indexX = x / (width / 3);
        int indexY = y / (height / (count / 3));

        int index = indexY * 3 + indexX;

        if (System.currentTimeMillis() - lastTouchTime < 400) {

            if (lastIndex == index) {
                pinPresenter.noMoreGuide();
                Intent intent = new Intent();
                intent.putExtra(PIN, index + 1);
                setResult(RESULT_OK, intent);
                finish();
            }
        }

        lastIndex = index;
        lastTouchTime = System.currentTimeMillis();

        return true;

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Dart.inject(this);
        Display display = getWindowManager().getDefaultDisplay();

        Point point = new Point();
        display.getSize(point);

        width = point.x;
        height = point.y;

        findViewById(R.id.fl_main).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return onMainClick(v, event);
            }
        });

        pinPresenter.needGuide();

    }

    @Override
    public void onBackPressed() {

    }

    @Override
    public int getLayout() {
        return R.layout.activity_pin;
    }

    @Override
    protected void inject(ActivityComponent activityComponent) {
        activityComponent.inject(this);
    }

    @Override
    protected void attachView() {
        pinPresenter.attachView(this);
    }

    @Override
    protected void detachPresenter() {
        pinPresenter.detachView();
    }

    @Override
    public void showProgress(boolean show) {

    }

    @Override
    public void showError(Throwable error) {
        Timber.e(error, "There was an error retrieving the pokemon");
    }

    @Override
    public void showGuide() {
        FrameLayout layout = (FrameLayout) findViewById(R.id.fl_main);
        
        // 새로운 가이드 애니메이션 방식으로 변경
        // 첫 번째 가이드: 더블탭 애니메이션 가이드
        GuideView doubleTapGuide = new GuideView(this, layout);
        doubleTapGuide.setTitle(getResources().getString(R.string.double_tap));
        doubleTapGuide.setDescription(getResources().getString(R.string.tap_description));
        doubleTapGuide.setAnimationType(false); // 탭 애니메이션 사용
        
        // 화면 중앙에 위치
        doubleTapGuide.getView().post(() -> {
            int centerX = layout.getWidth() / 2;
            int centerY = layout.getHeight() / 3;
            doubleTapGuide.updatePosition(centerX, centerY);
        });
        
        doubleTapGuide.show();
        
        // 투명한 오버레이 생성하여 탭할 영역을 시각적으로 강조
        View overlayView = new View(this);
        overlayView.setBackgroundColor(Color.parseColor("#33000000")); // 반투명 검정색
        layout.addView(overlayView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        
        // 그리드 이미지 추가 (기존 코드 활용)
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(R.drawable.guide_grid);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        
        // 이미지 애니메이션 효과 추가
        imageView.setAlpha(0f);
        imageView.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(500)
                .start();
        
        // 탭 인식을 위한 OnTouchListener 설정 (기존 로직 유지)
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.MATCH_PARENT);
        
        layout.addView(imageView, params);
        
        // 번호를 탭하면 번호가 반짝이는 애니메이션 효과 추가
        // 각 번호 위치를 탭하면 해당 위치에 ripple 효과 생성
        layout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // 탭 위치에 ripple 효과 생성
                View rippleView = new View(PinActivity.this);
                rippleView.setBackgroundResource(R.drawable.circle_ripple);
                
                int size = dpToPx(80);
                FrameLayout.LayoutParams rippleParams = new FrameLayout.LayoutParams(size, size);
                rippleParams.leftMargin = (int)event.getX() - size/2;
                rippleParams.topMargin = (int)event.getY() - size/2;
                layout.addView(rippleView, rippleParams);
                
                // Ripple 애니메이션
                rippleView.animate()
                        .scaleX(1.5f)
                        .scaleY(1.5f)
                        .alpha(0f)
                        .setDuration(500)
                        .withEndAction(() -> layout.removeView(rippleView))
                        .start();
            }
            
            // 실제 터치 이벤트는 기존 onMainClick 메서드에서 처리
            return onMainClick(v, event);
        });
    }
    
    // dp를 px로 변환하는 유틸리티 메서드
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void onReloadData() {

    }
}
