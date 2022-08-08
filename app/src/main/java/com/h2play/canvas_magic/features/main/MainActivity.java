package com.h2play.canvas_magic.features.main;

import android.content.ClipData;
import android.content.ClipboardManager;
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

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnLongClick;
import com.h2play.canvas_magic.R;
import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.features.base.BaseActivity;
import com.h2play.canvas_magic.features.common.ErrorView;
import com.h2play.canvas_magic.features.pincode.PinActivity;
import com.h2play.canvas_magic.injection.component.ActivityComponent;
import com.h2play.canvas_magic.util.AdDialog;
import com.h2play.canvas_magic.util.FabricView;
import com.h2play.canvas_magic.util.FileUtil;

public class MainActivity extends BaseActivity implements MainMvpView, ErrorView.ErrorListener {

    @InjectExtra
    Integer shapeIndex;

    private static final int REQUEST_CODE = 1001;
    @Inject
    MainPresenter mainPresenter;

    @BindView(R.id.fabricView)
    FabricView fabricView;

    @BindView(R.id.btn_start)
    ImageButton imageButton;

    private int selectedColor;
    private ShapeInfo selectedShape;
    private TextView guideTextView;

    private AdView adView;
    private AdDialog mCustomDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Dart.inject(this);

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

    @OnClick(R.id.btn_start)
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

        guideTextView = new TextView(this);
        guideTextView.setBackgroundResource(R.drawable.bubble);
        guideTextView.setText(getResources().getString(R.string.long_press));

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_LEFT, R.id.btn_start);
        params.addRule(RelativeLayout.ABOVE, R.id.btn_start);

        RelativeLayout layout = (RelativeLayout) findViewById(R.id.rootView);
        layout.addView(guideTextView,params);

    }

    @OnLongClick(R.id.btn_start)
    public boolean onStartLongClick() {
        fabricView.setColor(selectedColor);
        if(guideTextView != null) {
            guideTextView.setText(R.string.good_job);
        }

        Intent intent = PinActivity.getStartIntent(this,selectedShape.count);
        startActivityForResult(intent, REQUEST_CODE);
        return true;
    }

    @OnClick(R.id.ib_thickness)
    public void onThicknessClick() {
        fabricView.setSize(fabricView.getSize()==20?10:20);
        fabricView.setColor(selectedColor);
    }

    @OnClick(R.id.ib_clear)
    public void onClearClick() {
        fabricView.cleanPage();
    }

    @OnClick(R.id.ib_erase)
    public void onEraseClick() {
        fabricView.setSize(50);
        fabricView.setColor(Color.WHITE);

    }

    @Override
    public void onBackPressed() {
        if(guideTextView == null) {
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
