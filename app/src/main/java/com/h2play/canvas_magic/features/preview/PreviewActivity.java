package com.h2play.canvas_magic.features.preview;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.f2prateek.dart.Dart;
import com.f2prateek.dart.InjectExtra;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.h2play.canvas_magic.R;
import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.features.base.BaseActivity;
import com.h2play.canvas_magic.features.common.ErrorView;
import com.h2play.canvas_magic.features.detail.DetailActivity;
import com.h2play.canvas_magic.injection.component.ActivityComponent;
import com.h2play.canvas_magic.util.FabricView;
import com.h2play.canvas_magic.util.FileUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

import static com.h2play.canvas_magic.features.detail.DetailActivity.EXTRA_POKEMON_NAME;
import static com.h2play.canvas_magic.util.FabricView.LOCKED_MODE;

public class PreviewActivity extends BaseActivity implements PreviewMvpView, ErrorView.ErrorListener {


    @InjectExtra
    String title;

    @InjectExtra
    String jsonText;

    @Inject
    PreviewPresenter listPresenter;


    @BindView(R.id.txt_title)
    TextView titleTextView;

    @BindView(R.id.view_error)
    ErrorView errorView;

    @BindView(R.id.progress)
    ProgressBar progressBar;

    @BindViews({R.id.fabricView1,R.id.fabricView2,R.id.fabricView3,
            R.id.fabricView4,R.id.fabricView5,R.id.fabricView6,
            R.id.fabricView7,R.id.fabricView8,R.id.fabricView9})
    FabricView[] fabricViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        errorView.setErrorListener(this);

        Dart.inject(this);

        titleTextView.setText(title);
        showShape(title,jsonText);
    }

    @Override
    protected void onResume() {

        super.onResume();
    }

    public void showShape(String name, String jsonText) {

        Observable.timer(100, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Long aLong) {

                        for (int i = 0; i < fabricViews.length; ++i) {
                            drawShape(fabricViews[i], jsonText, i);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    public static Intent getStartIntent(Context context, String title, String jsonText) {
        Intent intent = new Intent(context, PreviewActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("jsonText", jsonText);
        return intent;
    }

    public void drawShape(FabricView fabricView, String jsonText, int shapeIndex) {
        fabricView.cleanPage();
        fabricView.setColor(Color.BLACK);

        JsonObject assetJsonObject = new Gson().fromJson(jsonText, JsonObject.class);
        JsonArray shapes = assetJsonObject.get("shapes").getAsJsonArray();
        JsonArray actions = shapes.get(shapeIndex).getAsJsonArray();

        List<JsonObject> jsonObjects = new ArrayList<>();
        for (int i = 0; i < actions.size(); ++i ) {
            jsonObjects.add(actions.get(i).getAsJsonObject());
        }
        ;
        for (JsonObject jsonObject :  jsonObjects) {
            switch (jsonObject.get("action").getAsString()) {
                case "down": {
                    fabricView.actionDown(jsonObject.get("x").getAsFloat()*fabricView.getWidth()
                            ,jsonObject.get("y").getAsFloat()*fabricView.getHeight());
                    break;
                }

                case "up": {
                    fabricView.actionUp(jsonObject.get("x").getAsFloat()*fabricView.getWidth()
                            ,jsonObject.get("y").getAsFloat()*fabricView.getHeight());
                    break;
                }

                case "move": {
                    fabricView.actionMove(jsonObject.get("x1").getAsFloat()*fabricView.getWidth()
                            ,jsonObject.get("y1").getAsFloat()*fabricView.getHeight(),
                            jsonObject.get("x2").getAsFloat()*fabricView.getWidth(),
                            jsonObject.get("y2").getAsFloat()*fabricView.getHeight());
                    break;
                }
            }
        }
        fabricView.setInteractionMode(LOCKED_MODE);
    }


    @Override
    public int getLayout() {
        return R.layout.activity_preview;
    }

    @Override
    protected void inject(ActivityComponent activityComponent) {
        activityComponent.inject(this);
    }

    @Override
    protected void attachView() {
        listPresenter.attachView(this);
    }

    @Override
    protected void detachPresenter() {
        listPresenter.detachView();
    }

    @Override
    public void showProgress(boolean show) {

    }

    @Override
    public void showError(Throwable error) {
        errorView.setVisibility(View.VISIBLE);
        Timber.e(error, "There was an error retrieving the pokemon");
    }

    @Override
    public void onSaveComplete() {
        Toast.makeText(this, getResources().getString(R.string.save_complete), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReloadData() {

    }

    @OnClick(R.id.fab_save)
    public void onSaveClick() {

        final EditText et = new EditText(this);
        FrameLayout container = new FrameLayout(this);

        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);

        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);

        et.setLayoutParams(params);


        container.addView(et);

        final AlertDialog.Builder alt_bld = new AlertDialog.Builder(this,R.style.Theme_MaterialComponents_Light_Dialog_Alert);

        alt_bld.setTitle(getResources().getString(R.string.add_new))
                .setMessage(getResources().getString(R.string.insert_new_name))
                .setIcon(R.drawable.ic_plus_24).setView(container).setPositiveButton(getResources().getString(android.R.string.ok),

                (dialog, id) -> {

                    if (et.getText().length() > 0) {
                        listPresenter.addNewItem(getBaseContext(),et.getText().toString(),jsonText);
                    }


                });

        AlertDialog alert = alt_bld.create();

        et.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {

                // Check if edittext is empty
                if (TextUtils.isEmpty(s)) {
                    // Disable ok button
                    ((AlertDialog) alert).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

                } else {
                    // Something into edit text. Enable the button.
                    ((AlertDialog) alert).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }

            }
        });

        alert.show();
        alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);



    }

}
