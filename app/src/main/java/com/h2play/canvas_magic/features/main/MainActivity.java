package com.h2play.canvas_magic.features.main;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.activity.OnBackPressedCallback;

import com.f2prateek.dart.Dart;
import com.f2prateek.dart.InjectExtra;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.h2play.canvas_magic.R;
import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.features.base.BaseActivity;
import com.h2play.canvas_magic.features.common.ErrorView;
import com.h2play.canvas_magic.features.pincode.PinActivity;
import com.h2play.canvas_magic.injection.component.ActivityComponent;
import com.h2play.canvas_magic.util.AdDialog;
import com.h2play.canvas_magic.util.Analytics;
import com.h2play.canvas_magic.util.ColorPickerBottomSheet;
import com.h2play.canvas_magic.util.FabricView;
import com.h2play.canvas_magic.util.FileUtil;
import com.h2play.canvas_magic.util.LongPressProgressView;
import com.h2play.canvas_magic.util.SpotlightGuideOverlay;

public class MainActivity extends BaseActivity implements MainMvpView, ErrorView.ErrorListener {

    @InjectExtra
    Integer shapeIndex;

    private static final int REQUEST_CODE = 1001;
    @Inject
    MainPresenter mainPresenter;

    private FabricView fabricView;
    private FloatingActionButton colorButton;
    private View undoButton;
    private View clearButton;
    private View eraseButton;

    private int selectedColor;
    private ShapeInfo selectedShape;

    private AdView adView;
    private AdDialog mCustomDialog;
    private SpotlightGuideOverlay spotlightOverlay;
    private LongPressProgressView longPressProgress;
    private boolean isTutorialMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Dart.inject(this);

        // 뷰 초기화
        fabricView = findViewById(R.id.fabricView);
        colorButton = findViewById(R.id.btn_start);
        undoButton = findViewById(R.id.ib_undo);
        clearButton = findViewById(R.id.ib_clear);
        eraseButton = findViewById(R.id.ib_erase);
        longPressProgress = findViewById(R.id.long_press_progress);

        // 롱프레스 프로그레스 완료 시 콜백
        longPressProgress.setOnCompleteListener(this::onLongPressComplete);

        // 클릭 리스너 설정
        colorButton.setOnClickListener(v -> onStartClick());
        setupLongPressWithProgress();
        undoButton.setOnClickListener(v -> fabricView.undo());
        clearButton.setOnClickListener(v -> onClearClick());
        eraseButton.setOnClickListener(v -> onEraseClick());

        // 초기 색상 설정
        selectedColor = Color.RED;
        colorButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(selectedColor));
        fabricView.setColor(selectedColor);
        fabricView.setSize(10);

        mainPresenter.getShape(shapeIndex);
        mainPresenter.checkNeedGuide();

        // 광고 초기화
        adView = new AdView(this);
        adView.setAdSize(AdSize.MEDIUM_RECTANGLE);
        adView.setAdUnitId("ca-app-pub-9937617798998725/8292313909");
        adView.loadAd(new AdRequest.Builder().build());

        mCustomDialog = new AdDialog(this,
                adView,
                view -> mCustomDialog.dismiss(),
                view -> {
                    mCustomDialog.dismiss();
                    finish();
                });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (spotlightOverlay != null) {
                    spotlightOverlay.hide();
                    spotlightOverlay = null;
                }
                mCustomDialog.show();
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    public void onStartClick() {
        if (isTutorialMode) return;
        showColorPicker();
    }

    /**
     * 현대적인 BottomSheet 색상 선택기 표시
     * - Pikolo HSL 컬러 피커
     * - 선 굵기/투명도 조절 가능
     * - 최근 사용 색상 저장
     * - OK 버튼 롱프레스 = 숨은 기능 (자동 그리기 모드)
     */
    private void showColorPicker() {
        ColorPickerBottomSheet bottomSheet = ColorPickerBottomSheet.newInstance(
                selectedColor, 
                fabricView.getSize()
        );

        bottomSheet.setOnColorSelectedListener((color, thickness) -> {
            // 색상 적용
            selectedColor = color;
            colorButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(selectedColor));
            fabricView.setColor(selectedColor);
            fabricView.setSize(thickness);
        });

        // 숨은 기능: OK 버튼 롱프레스 시 자동 그리기 모드
        // onStartLongClick()에 위임해야 가이드 모드 판정(wasGuideMode)이 항상 최신 값으로 갱신된다.
        bottomSheet.setOnLongPressListener(() -> onStartLongClick());

        bottomSheet.show(getSupportFragmentManager(), "color_picker");
    }

    @Override
    public void showLongPressGuide() {
        ViewGroup layout = findViewById(R.id.rootView);
        layout.post(() -> {
            if (isFinishing() || isDestroyed()) return;
            spotlightOverlay = new SpotlightGuideOverlay(this, layout);
            String tip = getString(R.string.long_press_description);
            spotlightOverlay.highlight(colorButton, 8, tip);
            spotlightOverlay.setListener(() -> spotlightOverlay = null);
            spotlightOverlay.show();
        });
        
        // 버튼 강조 애니메이션
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(colorButton, "scaleX", 1f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(colorButton, "scaleY", 1f, 1.2f, 1f);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(1000);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.setStartDelay(1500);
        
        ValueAnimator repeatAnimator = ValueAnimator.ofInt(0, 1);
        repeatAnimator.setDuration(2000);
        repeatAnimator.setRepeatCount(1);
        repeatAnimator.addUpdateListener(animation -> {
            if (animation.getAnimatedFraction() == 0f) {
                animatorSet.start();
            }
        });
        repeatAnimator.start();
        
        // 튜토리얼 모드 활성화
        isTutorialMode = true;
        Analytics.log(this, Analytics.TUTORIAL_GUIDE_SHOWN);

        // longPressProgress를 android.R.id.content로 옮겨서 스포트라이트 오버레이 위에 표시
        layout.post(() -> {
            ViewGroup contentRoot = findViewById(android.R.id.content);
            if (contentRoot != null && longPressProgress.getParent() != contentRoot) {
                ViewGroup oldParent = (ViewGroup) longPressProgress.getParent();
                if (oldParent != null) oldParent.removeView(longPressProgress);
                android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                        (int) (80 * getResources().getDisplayMetrics().density),
                        (int) (80 * getResources().getDisplayMetrics().density),
                        android.view.Gravity.CENTER
                );
                contentRoot.addView(longPressProgress, params);
            }
        });
    }

    /**
     * 롱프레스 + 프로그레스바 설정
     * 튜토리얼 모드에서는 프로그레스바가 표시되며, 꽉 채우면 다음 단계로 진행
     */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void setupLongPressWithProgress() {
        colorButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    // 롱프레스 시작 - 튜토리얼 모드일 때만 프로그레스 표시
                    if (isTutorialMode) {
                        longPressProgress.setProgressColor(selectedColor);
                        longPressProgress.show();
                    }
                    return false; // 기본 롱클릭 이벤트도 처리되도록
                    
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    // 터치 종료 - 프로그레스 숨기기
                    if (longPressProgress.isAnimating()) {
                        longPressProgress.hide();
                    }
                    return false;
            }
            return false;
        });
        
        // 기존 롱클릭 리스너 (튜토리얼 모드가 아닐 때 동작)
        colorButton.setOnLongClickListener(v -> {
            if (!isTutorialMode) {
                return onStartLongClick();
            }
            // 튜토리얼 모드에서는 프로그레스 완료 시 처리
            return true;
        });
    }

    /**
     * 롱프레스 프로그레스 완료 시 호출
     */
    private void onLongPressComplete() {
        longPressProgress.hide();
        isTutorialMode = false; // 튜토리얼 완료
        Analytics.log(this, Analytics.TUTORIAL_LONGPRESS_DONE);
        onStartLongClick();
    }

    private static final int REQUEST_TUTORIAL_PRE_PIN = 2002;
    private static final int REQUEST_PRACTICE_PIN = 2005;
    private static final int PRACTICE_TARGET_PIN = 8;
    private Intent pendingPinData = null;
    private boolean wasGuideMode = false;
    private int practiceAttempts = 0;
    private boolean justFinishedTutorial = false;

    public boolean onStartLongClick() {
        fabricView.setColor(selectedColor);

        if (spotlightOverlay != null) {
            spotlightOverlay.hide();
            spotlightOverlay = null;
        }

        wasGuideMode = mainPresenter.isGuideMode();
        if (wasGuideMode) {
            // 칭찬 토스트는 튜토리얼 진행 중에만 노출한다.
            // (튜토리얼 완료 후에는 실제 마술 중이므로 토스트가 뜨면 트릭이 노출된다)
            Toast.makeText(this, R.string.good_job, Toast.LENGTH_SHORT).show();

            Intent intent = com.h2play.canvas_magic.features.tutorial.TutorialDialogueActivity
                    .getStartIntent(this, com.h2play.canvas_magic.features.tutorial.TutorialDialogueActivity.PHASE_PRE_PIN);
            startActivityForResult(intent, REQUEST_TUTORIAL_PRE_PIN);
        } else {
            Intent intent = PinActivity.getStartIntent(this, selectedShape.count);
            startActivityForResult(intent, REQUEST_CODE);
        }
        return true;
    }


    public void onClearClick() {
        fabricView.cleanPage();
    }

    public void onEraseClick() {
        fabricView.setSize(50);
        fabricView.setColor(Color.WHITE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_TUTORIAL_PRE_PIN && resultCode == RESULT_OK) {
            Intent intent = PinActivity.getStartIntent(this, selectedShape.count);
            startActivityForResult(intent, REQUEST_CODE);
            return;
        }

        // 연습용 PinActivity에서 돌아옴 → 8번을 탭했는지 검증
        if (requestCode == REQUEST_PRACTICE_PIN && resultCode == RESULT_OK) {
            int tappedPin = data.getIntExtra(PinActivity.PIN, -1);
            practiceAttempts++;
            if (tappedPin != PRACTICE_TARGET_PIN) {
                // 오답 → 다시 시도
                Bundle wrong = new Bundle();
                wrong.putString("result", "wrong");
                wrong.putLong("attempt", practiceAttempts);
                wrong.putLong("tapped_pin", tappedPin);
                Analytics.log(this, Analytics.TUTORIAL_PRACTICE, wrong);

                Toast.makeText(this, R.string.tutorial_practice_wrong, Toast.LENGTH_SHORT).show();
                Intent retry = PinActivity.getStartIntent(this, selectedShape.count);
                retry.putExtra(PinActivity.EXTRA_PRACTICE_TARGET, PRACTICE_TARGET_PIN);
                retry.putExtra(PinActivity.EXTRA_SHOW_GRID, true);
                startActivityForResult(retry, REQUEST_PRACTICE_PIN);
                return;
            }
            // 정답 → 축하 후 8번 그리기 진행
            Bundle ok = new Bundle();
            ok.putString("result", "correct");
            ok.putLong("attempt", practiceAttempts);
            Analytics.log(this, Analytics.TUTORIAL_PRACTICE, ok);
            Analytics.log(this, Analytics.TUTORIAL_COMPLETE);
            Analytics.setTutorialDone(this, true);
            justFinishedTutorial = true;

            Toast.makeText(this, R.string.tutorial_practice_correct, Toast.LENGTH_SHORT).show();
            pendingPinData = null;
            // 튜토리얼 종료. 여기서 내리지 않으면 이후 일반 롱프레스마다
            // 연습 PinActivity가 다시 뜨면서 "8번이 아니에요" 토스트가 계속 반복된다.
            wasGuideMode = false;
            // data는 이미 PIN=8이므로 그대로 아래 그리기 로직으로 진행
        }

        // 튜토리얼 첫 PinActivity 완료 → 말풍선 연습 PinActivity로 이동
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && wasGuideMode) {
            pendingPinData = data;
            Intent practiceIntent = PinActivity.getStartIntent(this, selectedShape.count);
            practiceIntent.putExtra(PinActivity.EXTRA_PRACTICE_TARGET, PRACTICE_TARGET_PIN);
            startActivityForResult(practiceIntent, REQUEST_PRACTICE_PIN);
            return;
        }

        // 튜토리얼 대화/핀 화면을 뒤로가기로 빠져나오면 data가 null이다.
        // 그대로 진행하면 아래 getIntExtra에서 NPE로 죽는다.
        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        fabricView.cleanPage();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        fabricView.cleanPage();

        int numPin = data.getIntExtra(PinActivity.PIN, 1);

        // ★ 핵심 활성화 이벤트: 여기까지 왔다는 건 실제로 마술 1회를 수행했다는 뜻이다.
        // 첫 수행까지의 이탈과, 첫 수행 이후의 재수행이 이 앱 리텐션의 전부다.
        int trickCount = mainPresenter.recordTrickPerformed();
        Bundle trick = new Bundle();
        trick.putLong("pin", numPin);
        trick.putLong("lifetime_count", trickCount);
        trick.putString("is_first", String.valueOf(trickCount == 1));
        trick.putString("from_tutorial", String.valueOf(justFinishedTutorial));
        if (selectedShape != null && selectedShape.name != null) {
            trick.putString("pack_name", selectedShape.name);
        }
        Analytics.log(this, Analytics.TRICK_PERFORMED, trick);
        Analytics.setTrickCount(this, trickCount);
        justFinishedTutorial = false;

        {
            String jsonText = FileUtil.getJsonFromFile(this, selectedShape.fileName);

            JsonObject assetJsonObject = new Gson().fromJson(jsonText, JsonObject.class);
            JsonArray shapes = assetJsonObject.get("shapes").getAsJsonArray();
            JsonArray actions = shapes.get(numPin - 1).getAsJsonArray();

            List<JsonObject> jsonObjects = new ArrayList<>();
            for (int i = 0; i < actions.size(); ++i) {
                jsonObjects.add(actions.get(i).getAsJsonObject());
            }
            ;
            for (JsonObject jsonObject : jsonObjects) {
                switch (jsonObject.get("action").getAsString()) {
                    case "down": {
                        fabricView.actionDown(jsonObject.get("x").getAsFloat() * displayMetrics.widthPixels
                                , jsonObject.get("y").getAsFloat() * displayMetrics.heightPixels);
                        break;
                    }

                    case "up": {
                        fabricView.actionUp(jsonObject.get("x").getAsFloat() * displayMetrics.widthPixels
                                , jsonObject.get("y").getAsFloat() * displayMetrics.heightPixels);
                        break;
                    }

                    case "move": {
                        fabricView.actionMove(jsonObject.get("x1").getAsFloat() * displayMetrics.widthPixels
                                , jsonObject.get("y1").getAsFloat() * displayMetrics.heightPixels,
                                jsonObject.get("x2").getAsFloat() * displayMetrics.widthPixels,
                                jsonObject.get("y2").getAsFloat() * displayMetrics.heightPixels);
                        break;
                    }
                }
            }
        }

    }

    @Override
    public int getLayout() {
        return R.layout.activity_main;
    }

    @Override
    protected void inject(ActivityComponent activityComponent) {
        activityComponent.inject(this);
    }

    @Override
    protected void attachView() {
        mainPresenter.attachView(this);
    }

    @Override
    protected void detachPresenter() {
        mainPresenter.detachView();
    }

    @Override
    public void showProgress(boolean show) {

    }

    @Override
    public void showError(Throwable error) {
    }

    @Override
    public void setShapeFileName(ShapeInfo shapeInfo) {
        this.selectedShape = shapeInfo;
        if (shapeInfo != null && shapeInfo.name != null) {
            Analytics.log(this, Analytics.PACK_OPENED, "pack_name", shapeInfo.name);
        }
    }

    @Override
    public void onReloadData() {
    }

    public static Intent getStartIntent(Context context, int shapeIndex) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("shapeIndex", shapeIndex);
        return intent;
    }
}
