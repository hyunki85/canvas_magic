package com.h2play.canvas_magic.features.make;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.f2prateek.dart.Dart;
import com.f2prateek.dart.InjectExtra;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import com.h2play.canvas_magic.R;
import com.h2play.canvas_magic.features.base.BaseActivity;
import com.h2play.canvas_magic.features.common.ErrorView;
import com.h2play.canvas_magic.features.detail.DetailActivity;
import com.h2play.canvas_magic.injection.component.ActivityComponent;
import com.h2play.canvas_magic.util.FabricView;
import com.h2play.canvas_magic.util.FileUtil;

import org.json.JSONObject;

public class MakeActivity extends BaseActivity implements MakeMvpView, ErrorView.ErrorListener {


    @InjectExtra
    int shapeIndex;

    @InjectExtra
    String fileName;

    @Inject
    MakePresenter mainPresenter;

    @BindView(R.id.fabricView)
    FabricView fabricView;

    private boolean needSave = false;

    private int selectedColor;
    private JsonObject assetJsonObject;
    private JsonArray actions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Dart.inject(this);

        selectedColor = Color.BLACK;
        fabricView.setColor(selectedColor);

        loadJson();

        showShape();

        fabricView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                needSave = true;
                return false;
            }
        });

    }

    @Override
    public void onBackPressed() {

        if(needSave) {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(getResources().getString(R.string.app_name));
            alertDialog.setMessage(getResources().getString(R.string.need_save));
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
                    getResources().getString(android.R.string.no),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });

            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,
                    getResources().getString(android.R.string.yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });

            alertDialog.show();
        } else {
            super.onBackPressed();
        }


    }

    @OnClick(R.id.btn_save)
    public void onSaveClick() {
        needSave = false;
        saveJson();
    }

    @OnClick(R.id.ib_clear)
    public void onClearClick() {
        fabricView.cleanPage();
        actions = new JsonArray();
    }

    public void saveJson() {

        JsonArray shapeArray = assetJsonObject.get("shapes").getAsJsonArray();

        List<JsonObject> jsonObjects = new ArrayList<>();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        for ( JsonObject jsonObject : fabricView.getJsonArrays()) {
            JsonObject newjsonObject = jsonObject.deepCopy();
            switch (newjsonObject.get("action").getAsString()) {
                case "up":
                case "down": {
                    newjsonObject.addProperty("x", newjsonObject.get("x").getAsFloat()/displayMetrics.widthPixels);
                    newjsonObject.addProperty("y", newjsonObject.get("y").getAsFloat()/displayMetrics.heightPixels);
                    break;
                }
                case "move": {
                    newjsonObject.addProperty("x1", newjsonObject.get("x1").getAsFloat()/displayMetrics.widthPixels);
                    newjsonObject.addProperty("y1", newjsonObject.get("y1").getAsFloat()/displayMetrics.heightPixels);
                    newjsonObject.addProperty("x2", newjsonObject.get("x2").getAsFloat()/displayMetrics.widthPixels);
                    newjsonObject.addProperty("y2", newjsonObject.get("y2").getAsFloat()/displayMetrics.heightPixels);
                    break;
                }
            }
            jsonObjects.add(newjsonObject);
        }

        for (int i = 0; i < actions.size(); ++i ) {
            jsonObjects.add(actions.get(i).getAsJsonObject());
        }

        Gson gson = new Gson();
        String str = gson.toJson(jsonObjects);

        shapeArray.set(shapeIndex, gson.fromJson(str,JsonArray.class));

        assetJsonObject.add("shapes",shapeArray);

        FileUtil.writeFile(this,fileName, new Gson().toJson(assetJsonObject));
    }

    public void loadJson() {

        File fl = new File(getFilesDir(), "file.txt");

        String jsonText = null;
        if(fl.exists()) {
            jsonText = FileUtil.getJsonFromFile(this,fileName);
        } else {
            InputStream inputStream = getResources().openRawResource(R.raw.base_pattern);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            try {
                int i = inputStream.read();
                while (i != -1) {
                    byteArrayOutputStream.write(i);
                    i = inputStream.read();
                }

                jsonText = new String(byteArrayOutputStream.toByteArray());
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        assetJsonObject = new Gson().fromJson(jsonText,JsonObject.class);
    }

    public void showShape() {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        fabricView.cleanPage();
        {

            JsonArray shapes = assetJsonObject.get("shapes").getAsJsonArray();
            actions = shapes.get(shapeIndex).getAsJsonArray();

            List<JsonObject> jsonObjects = new ArrayList<>();
            for (int i = 0; i < actions.size(); ++i ) {
                jsonObjects.add(actions.get(i).getAsJsonObject());
            }

            for (JsonObject jsonObject :  jsonObjects) {
                switch (jsonObject.get("action").getAsString()) {
                    case "down": {
                        fabricView.actionDown(jsonObject.get("x").getAsFloat()*displayMetrics.widthPixels
                                ,jsonObject.get("y").getAsFloat()*displayMetrics.heightPixels);
                        break;
                    }

                    case "up": {
                        fabricView.actionUp(jsonObject.get("x").getAsFloat()*displayMetrics.widthPixels
                                ,jsonObject.get("y").getAsFloat()*displayMetrics.heightPixels);
                        break;
                    }

                    case "move": {
                        fabricView.actionMove(jsonObject.get("x1").getAsFloat()*displayMetrics.widthPixels
                                ,jsonObject.get("y1").getAsFloat()*displayMetrics.heightPixels,
                                jsonObject.get("x2").getAsFloat()*displayMetrics.widthPixels,
                                jsonObject.get("y2").getAsFloat()*displayMetrics.heightPixels);
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


    }

    @Override
    public int getLayout() {
        return R.layout.activity_make;
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
    public void showPokemon(List<String> pokemon) {

    }

    @Override
    public void showProgress(boolean show) {

    }

    @Override
    public void showError(Throwable error) {
    }

    @Override
    public void onReloadData() {
    }

    public static Intent getStartIntent(Context context, int index, String fileName) {
        Intent intent = new Intent(context, MakeActivity.class);
        intent.putExtra("shapeIndex",index);
        intent.putExtra("fileName",fileName);
        return intent;
    }
}
