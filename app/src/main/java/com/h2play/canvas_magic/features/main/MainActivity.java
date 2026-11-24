package com.h2play.canvas_magic.features.main;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;

import com.f2prateek.dart.Dart;
import com.f2prateek.dart.InjectExtra;
import com.google.android.material.button.MaterialButton;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import com.h2play.canvas_magic.R;
import com.h2play.canvas_magic.data.model.TutorialProgress;
import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.features.base.BaseActivity;
import com.h2play.canvas_magic.features.common.ErrorView;
import com.h2play.canvas_magic.features.pincode.PinActivity;
import com.h2play.canvas_magic.injection.component.ActivityComponent;
import com.h2play.canvas_magic.util.Analytics;
import com.h2play.canvas_magic.util.ColorSettingsController;
import com.h2play.canvas_magic.util.DrawingSettingsPreferences;
import com.h2play.canvas_magic.util.FabricView;
import com.h2play.canvas_magic.util.DrawingCatalog;
import com.h2play.canvas_magic.util.DrawingExperience;
import com.h2play.canvas_magic.util.DrawingArtworkView;
import com.h2play.canvas_magic.util.FileUtil;
import com.h2play.canvas_magic.util.LongPressProgressView;
import com.h2play.canvas_magic.util.NotificationPermissionManager;
import com.h2play.canvas_magic.util.ads.ExitAdController;

public class MainActivity extends BaseActivity implements MainMvpView, ErrorView.ErrorListener {

    public static final String EXTRA_CUSTOM_PACK_ID = "custom_pack_id";
    public static final String EXTRA_ENTRY_POINT = "entry_point";
    private static final String TUTORIAL_FILE = "__tutorial_builtin__";

    @InjectExtra
    Integer shapeIndex;

    private static final int REQUEST_CODE = 1001;
    @Inject
    MainPresenter mainPresenter;

    private FabricView fabricView;
    private MaterialButton colorButton;
    private View undoButton;
    private View clearButton;
    private View eraseButton;

    private int selectedColor;
    private float selectedThickness;
    private ShapeInfo selectedShape;
    private int drawingPack = -1;

    private ColorSettingsController colorSettings;
    private FrameLayout settingsPanel;
    private View settingsShield;
    private int guideStage = 0;
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

        settingsPanel = findViewById(R.id.inline_color_settings);
        settingsShield = findViewById(R.id.settings_shield);
        settingsShield.setOnClickListener(v -> applyOpenSettings());
        findViewById(R.id.coach_skip).setOnClickListener(v -> skipGuide());

        // 롱프레스 프로그레스 완료 시 콜백
        longPressProgress.setOnCompleteListener(this::onLongPressComplete);

        // 클릭 리스너 설정
        colorButton.setOnClickListener(v -> onStartClick());
        setupLongPressWithProgress();
        undoButton.setOnClickListener(v -> { applyOpenSettings(); fabricView.undo(); });
        clearButton.setOnClickListener(v -> onClearClick());
        eraseButton.setOnClickListener(v -> onEraseClick());

        // 마지막으로 적용한 그리기 설정 복원
        selectedColor = DrawingSettingsPreferences.getColor(this);
        selectedThickness = DrawingSettingsPreferences.getThickness(this);
        applyDrawingSettings();

        TutorialProgress tutorialProgress = mainPresenter.getTutorialProgress();
        // Tutorial drawings always come from the immutable raw resource, never user storage.
        drawingPack = tutorialProgress.isComplete()
                ? DrawingCatalog.index(getIntent().getStringExtra(DrawingExperience.EXTRA_PACK)) : -1;
        if (!tutorialProgress.isComplete()) {
            selectedShape = new ShapeInfo(TUTORIAL_FILE,
                    getString(R.string.default_shape_set_name), 9);
        } else if (drawingPack >= 0) {
            selectedShape = new ShapeInfo("builtin:" + DrawingCatalog.id(drawingPack),
                    getResources().getStringArray(R.array.drawing_pack_titles)[drawingPack], 3);
            DrawingExperience.rememberPack(this, DrawingCatalog.id(drawingPack));
            mainPresenter.rememberBuiltInPack(DrawingCatalog.id(drawingPack));
        } else {
            DrawingExperience.rememberPack(this, null);
            String customPackId = getIntent().getStringExtra(EXTRA_CUSTOM_PACK_ID);
            if (customPackId == null || customPackId.isEmpty()) mainPresenter.getShape(shapeIndex);
            else mainPresenter.getShape(customPackId);
        }
        if (savedInstanceState == null) {
            mainPresenter.checkNeedGuide();
        } else {
            guideStage = savedInstanceState.getInt("guide_stage");
            isTutorialMode = guideStage == 1 || guideStage == 2;
            wasGuideMode = savedInstanceState.getBoolean("was_guide");
            practiceAttempts = savedInstanceState.getInt("practice_attempts");
            updateCoach();
            Bundle draft = savedInstanceState.getBundle("brush_draft");
            if (draft != null) showColorPicker(draft);
        }

        ExitAdController exitAd = mainPresenter.isGuideMode() ? null
                : new ExitAdController(this, ExitAdController.Placement.CANVAS);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (colorSettings != null) {
                    applyOpenSettings();
                    return;
                }
                if (exitAd != null && !isTutorialMode) exitAd.onBack(MainActivity.this::finish);
                else finish();
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            View decor = getWindow().getDecorView();
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decor.setSystemUiVisibility(flags);
        }
    }

    public void onStartClick() {
        if (colorSettings != null) applyOpenSettings();
        else showColorPicker(null);
    }

    private void showColorPicker(Bundle draft) {
        View content = getLayoutInflater().inflate(R.layout.bottom_sheet_color_picker, settingsPanel, false);
        settingsPanel.removeAllViews();
        settingsPanel.addView(content);
        colorSettings = new ColorSettingsController(content, selectedColor, selectedThickness, draft,
                new ColorSettingsController.Listener() {
            @Override public void onApply(int color, float thickness) {
                selectedColor = color;
                selectedThickness = thickness;
                applyDrawingSettings();
                if (isTutorialMode && guideStage == 1) {
                    guideStage = 2;
                    mainPresenter.setTutorialProgress(TutorialProgress.SECRET_SELECTION_GUIDE);
                }
                closeColorPicker();
            }
            @Override public void onCancel() { closeColorPicker(); }
            @Override public void onLongPress() { onStartLongClick(); }
        });
        if (isTutorialMode) colorSettings.setHint(getString(R.string.coach_adjust));
        settingsPanel.setVisibility(View.VISIBLE);
        settingsShield.setVisibility(View.VISIBLE);
        findViewById(R.id.tutorial_coach).setVisibility(View.GONE);
        // Fit small/folded windows without resizing the drawing coordinate space.
        View root = findViewById(R.id.rootView);
        root.post(() -> {
            if (isFinishing() || settingsPanel.getVisibility() != View.VISIBLE) return;
            ViewGroup.LayoutParams params = settingsPanel.getLayoutParams();
            int preferred = dp(isTutorialMode ? 390 : 320);
            params.height = Math.min(preferred, Math.max(dp(160), root.getHeight() - dp(120)));
            settingsPanel.setLayoutParams(params);
        });
    }

    private void applyOpenSettings() {
        if (colorSettings != null) colorSettings.apply();
    }

    private void closeColorPicker() {
        colorSettings = null;
        settingsPanel.setVisibility(View.GONE);
        settingsShield.setVisibility(View.GONE);
        settingsPanel.removeAllViews();
        updateCoach();
    }

    private void applyDrawingSettings() {
        eraseButton.setSelected(false);
        colorButton.setSelected(true);
        findViewById(R.id.current_color_indicator).setBackgroundTintList(
                ColorStateList.valueOf(selectedColor));
        findViewById(R.id.current_color_indicator).setBackgroundColor(selectedColor);
        fabricView.setColor(selectedColor);
        fabricView.setSize(selectedThickness);
    }

    @Override public void showLongPressGuide() {
        TutorialProgress progress = mainPresenter.getTutorialProgress();
        guideStage = progress == TutorialProgress.NOT_STARTED
                || progress == TutorialProgress.DRAWING_GUIDE ? 1 : 2;
        if (progress == TutorialProgress.NOT_STARTED) {
            mainPresenter.setTutorialProgress(TutorialProgress.DRAWING_GUIDE);
        }
        isTutorialMode = true;
        updateCoach();
        Analytics.log(this, Analytics.TUTORIAL_GUIDE_SHOWN);
    }

    private void updateCoach() {
        View coach = findViewById(R.id.tutorial_coach);
        boolean visible = isTutorialMode && colorSettings == null;
        coach.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) return;
        ((TextView) findViewById(R.id.coach_step)).setText(
                guideStage == 1 ? R.string.coach_colors_title : R.string.coach_hold_title);
        ((TextView) findViewById(R.id.coach_message)).setText(
                guideStage == 1 ? R.string.coach_colors : R.string.coach_hold);
    }

    private void skipGuide() {
        longPressProgress.hide();
        Analytics.log(this, Analytics.TUTORIAL_EXIT, "source", "canvas_skip");
        finish();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        if (colorSettings != null) state.putBundle("brush_draft", colorSettings.saveState());
        state.putInt("guide_stage", guideStage);
        state.putBoolean("was_guide", wasGuideMode);
        state.putInt("practice_attempts", practiceAttempts);
        super.onSaveInstanceState(state);
    }

    @Override protected void onStop() {
        longPressProgress.hide();
        super.onStop();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
                    if (isTutorialMode && guideStage == 2 && colorSettings == null) {
                        longPressProgress.setProgressColor(getColor(R.color.primary_color));
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
            if (guideStage == 1) onStartClick();
            // In step 2 the completed hold ring advances the guide.
            return true;
        });
    }

    /**
     * 롱프레스 프로그레스 완료 시 호출
     */
    private void onLongPressComplete() {
        longPressProgress.hide();
        isTutorialMode = false;
        guideStage = 3;
        updateCoach();
        Analytics.log(this, Analytics.TUTORIAL_LONGPRESS_DONE);
        onStartLongClick();
    }

    private static final int REQUEST_TUTORIAL_PRE_PIN = 2002;
    private static final int REQUEST_PRACTICE_PIN = 2005;
    private static final int PRACTICE_TARGET_PIN = 8;
    private boolean wasGuideMode = false;
    private int practiceAttempts = 0;
    private boolean pendingTutorialCompletion = false;

    public boolean onStartLongClick() {
        applyOpenSettings();
        if (selectedShape == null) return true;
        isTutorialMode = false;
        updateCoach();
        // 지우개가 임시로 바꾼 흰색/두께 50 대신 사용자가 저장한 설정을 사용한다.
        applyDrawingSettings();

        TutorialProgress tutorialProgress = mainPresenter.getTutorialProgress();
        wasGuideMode = !tutorialProgress.isComplete();
        if (wasGuideMode) {
            guideStage = 3;
            if (tutorialProgress == TutorialProgress.PRACTICE) {
                launchPractice(false);
                return true;
            }
            // 칭찬 토스트는 튜토리얼 진행 중에만 노출한다.
            // (튜토리얼 완료 후에는 실제 마술 중이므로 토스트가 뜨면 트릭이 노출된다)
            Toast.makeText(this, R.string.good_job, Toast.LENGTH_SHORT).show();

            Intent intent = com.h2play.canvas_magic.features.tutorial.TutorialDialogueActivity
                    .getStartIntent(this, com.h2play.canvas_magic.features.tutorial.TutorialDialogueActivity.PHASE_PRE_PIN);
            startActivityForResult(intent, REQUEST_TUTORIAL_PRE_PIN);
        } else {
            Bundle performance = new Bundle();
            performance.putString("pack_kind", drawingPack >= 0 ? "built_in" : "custom");
            performance.putString("entry_point", getIntent().getStringExtra(EXTRA_ENTRY_POINT) == null
                    ? "canvas" : getIntent().getStringExtra(EXTRA_ENTRY_POINT));
            performance.putLong("choice_count", selectedShape.count);
            Analytics.log(this, Analytics.PERFORMANCE_START, performance);
            Intent intent = PinActivity.getStartIntent(this, selectedShape.count);
            startActivityForResult(intent, REQUEST_CODE);
        }
        return true;
    }


    public void onClearClick() {
        applyOpenSettings();
        fabricView.cleanPage();
    }

    public void onEraseClick() {
        applyOpenSettings();
        colorButton.setSelected(false);
        eraseButton.setSelected(true);
        fabricView.setSize(50);
        fabricView.setColor(Color.WHITE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (selectedShape == null) return;
        if (resultCode != RESULT_OK && (requestCode == REQUEST_TUTORIAL_PRE_PIN
                || requestCode == REQUEST_PRACTICE_PIN || (requestCode == REQUEST_CODE && wasGuideMode))) {
            mainPresenter.setTutorialProgress(requestCode == REQUEST_PRACTICE_PIN
                    ? TutorialProgress.PRACTICE : TutorialProgress.SECRET_SELECTION_GUIDE);
            guideStage = 2;
            isTutorialMode = true;
            updateCoach();
            return;
        }

        if (requestCode == REQUEST_TUTORIAL_PRE_PIN && resultCode == RESULT_OK) {
            mainPresenter.setTutorialProgress(TutorialProgress.SECRET_SELECTION_GUIDE);
            Intent intent = PinActivity.getStartIntent(this, 9);
            startActivityForResult(intent, REQUEST_CODE);
            return;
        }

        // 연습용 PinActivity에서 돌아옴 → 8번을 탭했는지 검증
        if (requestCode == REQUEST_PRACTICE_PIN && resultCode == RESULT_OK && data != null) {
            int tappedPin = data.getIntExtra(PinActivity.PIN, -1);
            practiceAttempts++;
            if (tappedPin != PRACTICE_TARGET_PIN) {
                // 오답 → 다시 시도
                Bundle wrong = new Bundle();
                wrong.putString("result", "wrong");
                wrong.putLong("attempt", practiceAttempts);
                wrong.putString("tutorial_stage", TutorialProgress.PRACTICE.name().toLowerCase(Locale.ROOT));
                Analytics.log(this, Analytics.TUTORIAL_PRACTICE, wrong);

                Toast.makeText(this, R.string.tutorial_practice_wrong, Toast.LENGTH_SHORT).show();
                launchPractice(true);
                return;
            }
            // Completion is committed only after the immutable practice drawing renders below.
            Bundle ok = new Bundle();
            ok.putString("result", "correct");
            ok.putLong("attempt", practiceAttempts);
            ok.putString("tutorial_stage", TutorialProgress.PRACTICE.name().toLowerCase(Locale.ROOT));
            Analytics.log(this, Analytics.TUTORIAL_PRACTICE, ok);
            pendingTutorialCompletion = true;
            wasGuideMode = false;
        }

        // 튜토리얼 첫 PinActivity 완료 → 말풍선 연습 PinActivity로 이동
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && wasGuideMode) {
            mainPresenter.setTutorialProgress(TutorialProgress.PRACTICE);
            launchPractice(false);
            return;
        }

        // 튜토리얼 대화/핀 화면을 뒤로가기로 빠져나오면 data가 null이다.
        // 그대로 진행하면 아래 getIntExtra에서 NPE로 죽는다.
        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        if (requestCode != REQUEST_CODE && requestCode != REQUEST_PRACTICE_PIN) return;
        int numPin = data.getIntExtra(PinActivity.PIN, -1);
        if (numPin < 1 || numPin > selectedShape.count) return;
        fabricView.cleanPage();
        boolean rendered = false;
        try {
            if (drawingPack >= 0) {
                rendered = DrawingArtworkView.reveal(fabricView, drawingPack, numPin - 1);
            } else {
                // Preserve the original display-coordinate format for existing user drawings.
                DisplayMetrics metrics = new DisplayMetrics();
                ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
                String drawingJson = TUTORIAL_FILE.equals(selectedShape.fileName)
                        ? FileUtil.getJsonFromRaw(this, R.raw.base_pattern)
                        : FileUtil.getJsonFromFile(this, selectedShape.fileName);
                JsonObject asset = new Gson().fromJson(drawingJson, JsonObject.class);
                JsonArray actions = asset.getAsJsonArray("shapes").get(numPin - 1).getAsJsonArray();
                boolean strokeStarted = false;
                for (int i = 0; i < actions.size(); i++) {
                    JsonObject action = actions.get(i).getAsJsonObject();
                    switch (action.get("action").getAsString()) {
                        case "down":
                            fabricView.actionDown(action.get("x").getAsFloat()*metrics.widthPixels,
                                    action.get("y").getAsFloat()*metrics.heightPixels);
                            strokeStarted = true;
                            break;
                        case "up":
                            if (strokeStarted) {
                                fabricView.actionUp(action.get("x").getAsFloat()*metrics.widthPixels,
                                        action.get("y").getAsFloat()*metrics.heightPixels);
                                rendered = true;
                            }
                            break;
                        case "move":
                            if (strokeStarted) {
                                fabricView.actionMove(action.get("x1").getAsFloat()*metrics.widthPixels,
                                        action.get("y1").getAsFloat()*metrics.heightPixels,
                                        action.get("x2").getAsFloat()*metrics.widthPixels,
                                        action.get("y2").getAsFloat()*metrics.heightPixels);
                                rendered = true;
                            }
                            break;
                    }
                }
            }
        } catch (RuntimeException invalidDrawing) {
            rendered = false;
            fabricView.cleanPage();
            timber.log.Timber.w(invalidDrawing, "Drawing could not be rendered");
        }
        if (rendered && pendingTutorialCompletion) {
            mainPresenter.completeTutorial();
            Analytics.log(this, Analytics.TUTORIAL_COMPLETE,
                    "tutorial_stage", TutorialProgress.COMPLETED.name().toLowerCase(Locale.ROOT));
            Analytics.setTutorialDone(this, true);
            pendingTutorialCompletion = false;
            guideStage = 0;
            isTutorialMode = false;
            updateCoach();
            Toast.makeText(this, R.string.tutorial_practice_correct, Toast.LENGTH_SHORT).show();
        } else if (rendered) {
            recordSuccessfulReveal();
        } else if (pendingTutorialCompletion) {
            pendingTutorialCompletion = false;
            mainPresenter.setTutorialProgress(TutorialProgress.PRACTICE);
            guideStage = 2;
            isTutorialMode = true;
            updateCoach();
        }
    }

    private void launchPractice(boolean showGrid) {
        mainPresenter.setTutorialProgress(TutorialProgress.PRACTICE);
        Intent practiceIntent = PinActivity.getStartIntent(this, 9);
        practiceIntent.putExtra(PinActivity.EXTRA_PRACTICE_TARGET, PRACTICE_TARGET_PIN);
        practiceIntent.putExtra(PinActivity.EXTRA_SHOW_GRID, showGrid);
        startActivityForResult(practiceIntent, REQUEST_PRACTICE_PIN);
    }

    private void recordSuccessfulReveal() {
        int trickCount = mainPresenter.recordTrickPerformed();
        Bundle trick = new Bundle();
        trick.putString("pack_kind", drawingPack >= 0 ? "built_in" : "custom");
        trick.putString("entry_point", getIntent().getStringExtra(EXTRA_ENTRY_POINT) == null
                ? "canvas" : getIntent().getStringExtra(EXTRA_ENTRY_POINT));
        trick.putLong("choice_count", selectedShape.count);
        Analytics.log(this, Analytics.TRICK_PERFORMED, trick);
        Analytics.setTrickCount(this, trickCount);
        NotificationPermissionManager.markEligible(this);
        DrawingExperience.recordReveal(this, drawingPack >= 0 ? DrawingCatalog.id(drawingPack) : null);
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
        if (shapeInfo != null) {
            Analytics.log(this, Analytics.PACK_OPENED, "pack_kind", "custom");
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

    public static Intent getCustomPackIntent(Context context, String fileName, String entryPoint) {
        return getStartIntent(context, 0)
                .putExtra(EXTRA_CUSTOM_PACK_ID, fileName)
                .putExtra(EXTRA_ENTRY_POINT, entryPoint);
    }
}
