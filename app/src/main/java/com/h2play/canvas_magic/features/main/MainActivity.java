package com.h2play.canvas_magic.features.main;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.f2prateek.dart.Dart;
import com.f2prateek.dart.InjectExtra;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
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
import com.h2play.canvas_magic.util.FabricView;
import com.h2play.canvas_magic.util.FileUtil;
import com.h2play.canvas_magic.util.GuideView;

public class MainActivity extends BaseActivity implements MainMvpView, ErrorView.ErrorListener {

    @InjectExtra
    Integer shapeIndex;

    private static final int REQUEST_CODE = 1001;
    @Inject
    MainPresenter mainPresenter;

    private FabricView fabricView;
    private ImageButton imageButton;
    private ImageButton thicknessButton;
    private ImageButton clearButton;
    private ImageButton eraseButton;

    private int selectedColor;
    private ShapeInfo selectedShape;
    private TextView guideTextView; // 추가된 변수

    private AdView adView;
    private AdDialog mCustomDialog;
    private GuideView activeGuideView; // 추가된 변수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Dart.inject(this);

        // Initialize views using findViewById instead of ButterKnife
        fabricView = findViewById(R.id.fabricView);
        imageButton = findViewById(R.id.btn_start);
        thicknessButton = findViewById(R.id.ib_thickness);
        clearButton = findViewById(R.id.ib_clear);
        eraseButton = findViewById(R.id.ib_erase);

        // Set click listeners (replacing @OnClick annotations)
        imageButton.setOnClickListener(v -> onStartClick());
        imageButton.setOnLongClickListener(v -> onStartLongClick());
        thicknessButton.setOnClickListener(v -> onThicknessClick());
        clearButton.setOnClickListener(v -> onClearClick());
        eraseButton.setOnClickListener(v -> onEraseClick());

        selectedColor = Color.RED;
        GradientDrawable drawable = (GradientDrawable) imageButton.getBackground();
        drawable.setColor(selectedColor);
        fabricView.setColor(selectedColor);

        mainPresenter.getShape(shapeIndex);

        mainPresenter.checkNeedGuide();

        adView = new AdView(this);
        adView.setAdSize(AdSize.MEDIUM_RECTANGLE);
        adView.setAdUnitId("ca-app-pub-9937617798998725/8292313909");
        adView.loadAd(new AdRequest.Builder().build());
        // [END load_banner_ad]

        mCustomDialog = new AdDialog(this,
                adView,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mCustomDialog.dismiss();
                    }
                },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mCustomDialog.dismiss();
                        finish();
                    }
                });

    }

    public void onStartClick() {
        ColorPickerDialogBuilder
                .with(this)
                .setTitle(getResources().getString(R.string.select_color))
                .initialColor(Color.RED)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setOnColorSelectedListener(new OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int selectedColor) {

                    }
                })
                .setPositiveButton("ok", new ColorPickerClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                        GradientDrawable drawable = (GradientDrawable) imageButton.getBackground();
                        drawable.setColor(selectedColor);
                        fabricView.setColor(selectedColor);
                        MainActivity.this.selectedColor = selectedColor;
                        fabricView.setSize(10);
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .build()
                .show();
    }

    @Override
    public void showLongPressGuide() {
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.rootView);
        
        // 기존 guideTextView 대신 새로운 GuideView 사용
        activeGuideView = new GuideView(this, layout);
        activeGuideView.setTitle(R.string.long_press);
        activeGuideView.setDescription(getResources().getString(R.string.long_press_description));
        activeGuideView.setAnimationType(true); // 롱프레스 애니메이션 사용
        
        // 화면 중앙에 가이드 뷰 배치 (0, 0은 자동으로 화면 중앙에 배치하도록 GuideView 코드 수정함)
        activeGuideView.updatePosition(0, 0);
        activeGuideView.show();
        
        // 버튼 위치를 강조하기 위해 버튼에 애니메이션 효과 추가
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(imageButton, "scaleX", 1f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(imageButton, "scaleY", 1f, 1.2f, 1f);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(1000);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.setStartDelay(1500); // 가이드 뷰가 표시된 후 시작
        
        // setRepeatCount가 AnimatorSet에 없으므로 ValueAnimator 사용 또는 다른 방식으로 반복
        ValueAnimator repeatAnimator = ValueAnimator.ofInt(0, 1);
        repeatAnimator.setDuration(2000); // 한 번의 애니메이션 + 딜레이 시간
        repeatAnimator.setRepeatCount(1);
        repeatAnimator.addUpdateListener(animation -> {
            if(animation.getAnimatedFraction() == 0f) {
                // 애니메이션 시작할 때마다 버튼 애니메이션 실행
                animatorSet.start();
            }
        });
        repeatAnimator.start();
        
        // 가이드 뷰 콜백 설정
        activeGuideView.setGuideListener(new GuideView.GuideListener() {
            @Override
            public void onGuideCompleted() {
                // 가이드 완료 후 작업이 필요하면 여기에 구현
            }
        });
    }

    public boolean onStartLongClick() {
        fabricView.setColor(selectedColor);
        
        // 가이드 텍스트 업데이트 대신 새로운 방식의 피드백 제공
        Toast.makeText(this, R.string.good_job, Toast.LENGTH_SHORT).show();

        Intent intent = PinActivity.getStartIntent(this, selectedShape.count);
        startActivityForResult(intent, REQUEST_CODE);
        return true;
    }

    public void onThicknessClick() {
        fabricView.setSize(fabricView.getSize() == 20 ? 10 : 20);
        fabricView.setColor(selectedColor);
    }

    public void onClearClick() {
        fabricView.cleanPage();
    }

    public void onEraseClick() {
        fabricView.setSize(50);
        fabricView.setColor(Color.WHITE);
    }

    @Override
    public void onBackPressed() {
        if (activeGuideView != null) {
            mCustomDialog.show();
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        fabricView.cleanPage();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        fabricView.cleanPage();

        int numPin = data.getIntExtra(PinActivity.PIN, 1);
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
