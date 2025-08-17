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
import com.h2play.canvas_magic.util.SpotlightGuideOverlay;
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

    // 예전 방식 복구: 가이드 그리드 이미지를 전체 화면에 깔아주기 (오버레이 아래에 위치)
    final String GUIDE_GRID_TAG = "guide_grid_bg";
    if (layout.findViewWithTag(GUIDE_GRID_TAG) == null) {
        ImageView imageView = new ImageView(this);
        imageView.setTag(GUIDE_GRID_TAG);
        imageView.setImageResource(R.drawable.guide_grid);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        imageView.setOnTouchListener((v, event) -> false); // 터치 소비하지 않음
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        layout.addView(imageView, params);
    }

    // 시나리오: 상대가 6을 선택했다는 메시지와 함께, 숫자 6 위치를 강조
    SpotlightGuideOverlay overlay = new SpotlightGuideOverlay(this, layout);
    // 오버레이는 아직 attach되지 않았으므로 overlay.post가 아닌 layout.post로 지연 실행
    layout.post(() -> {
        // 레이아웃 실제 크기 기준으로 그리드 셀 계산 (터치 로직과 시각 가이드를 일치)
        int rows = Math.max(1, count / 3);
        int cellW = Math.max(1, layout.getWidth() / 3);
        int cellH = Math.max(1, layout.getHeight() / rows);

        int targetNumber = 6; // 1-based
        int targetIndex = Math.max(0, Math.min(count - 1, targetNumber - 1));
        int col = targetIndex % 3;          // 0..2
        int row = targetIndex / 3;          // 0..rows-1

        float left = col * cellW;
        float top = row * cellH;
        android.graphics.RectF rect = new android.graphics.RectF(
            left, top, left + cellW, top + cellH);

        String tip = getString(R.string.opponent_chose_number, targetNumber);
            overlay.highlightRect(rect, 10, tip, layout);
            overlay.show(); // 오버레이는 마지막에 addView되어 그리드 위에 표시됨
        });

    // 이 오버레이는 영역 내부 터치를 fl_main으로 전달 -> 더블탭 로직(onMainClick) 그대로 동작
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
