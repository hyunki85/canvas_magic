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
import androidx.activity.OnBackPressedCallback;

import com.h2play.canvas_magic.util.GridGuideView;
import com.h2play.canvas_magic.util.GuideView; // GuideView 클래스 임포트 추가
import com.h2play.canvas_magic.util.SpotlightGuideOverlay;
import com.h2play.canvas_magic.util.ViewUtil;

import timber.log.Timber;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public class PinActivity extends BaseActivity implements PinMvpView, ErrorView.ErrorListener {

    public static final String PIN = "pin";
    public static final String EXTRA_NO_GUIDE = "no_guide";
    public static final String EXTRA_PRACTICE_TARGET = "practice_target";
    public static final String EXTRA_SHOW_GRID = "show_grid";
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

        // 몰입형 전체화면이라 fl_main의 실제 높이가 display.getSize()보다 클 수 있다.
        // 클램프하지 않으면 화면 끝 터치에서 index가 count를 넘어
        // MainActivity의 shapes.get(numPin-1)이 IndexOutOfBounds로 죽는다(공연 도중 크래시).
        int rows = Math.max(1, count / 3);
        int indexX = clamp(x / Math.max(1, width / 3), 0, 2);
        int indexY = clamp(y / Math.max(1, height / rows), 0, rows - 1);

        int index = indexY * 3 + indexX;
        if (index >= count) {
            index = count - 1;
        }

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

        int practiceTarget = getIntent().getIntExtra(EXTRA_PRACTICE_TARGET, -1);
        if (practiceTarget >= 0) {
            showPracticeBubble(practiceTarget);
        } else if (!getIntent().getBooleanExtra(EXTRA_NO_GUIDE, false)) {
            pinPresenter.needGuide();
        }

        // Block back button - user must tap a pin to proceed
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // intentionally empty — back is disabled
            }
        });

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

    // 프로그래밍 방식으로 그리드 가이드 표시 (이미지 대신 동적 뷰 사용)
    final String GUIDE_GRID_TAG = "guide_grid_bg";
    if (layout.findViewWithTag(GUIDE_GRID_TAG) == null) {
        GridGuideView gridGuideView = new GridGuideView(this);
        gridGuideView.setTag(GUIDE_GRID_TAG);
        gridGuideView.setGridCount(count); // count에 맞게 그리드 개수 설정
        gridGuideView.setOnTouchListener((v, event) -> false); // 터치 소비하지 않음
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        layout.addView(gridGuideView, params);
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
    
    private void showPracticeBubble(int targetNumber) {
        FrameLayout layout = findViewById(R.id.fl_main);

        // 그리드 가이드: EXTRA_SHOW_GRID가 true일 때만 표시
        if (getIntent().getBooleanExtra(EXTRA_SHOW_GRID, false)) {
            GridGuideView gridGuideView = new GridGuideView(this);
            gridGuideView.setGridCount(count);
            gridGuideView.setOnTouchListener((v, event) -> false);
            layout.addView(gridGuideView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }

        // 말풍선 텍스트
        TextView bubble = new TextView(this);
        bubble.setText(getString(R.string.tutorial_practice_bubble, targetNumber));
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(COMPLEX_UNIT_DIP, 18);
        bubble.setBackground(getResources().getDrawable(R.drawable.speech_bubble_bg));
        bubble.setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(16));
        bubble.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        params.topMargin = dpToPx(60);
        params.leftMargin = dpToPx(24);
        params.rightMargin = dpToPx(24);
        layout.addView(bubble, params);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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
