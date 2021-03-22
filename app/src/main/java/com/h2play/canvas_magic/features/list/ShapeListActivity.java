package com.h2play.canvas_magic.features.list;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.h2play.canvas_magic.R;
import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.features.base.BaseActivity;
import com.h2play.canvas_magic.features.common.ErrorView;
import com.h2play.canvas_magic.features.make.MakeActivity;
import com.h2play.canvas_magic.features.share.ShareActivity;
import com.h2play.canvas_magic.injection.component.ActivityComponent;
import com.h2play.canvas_magic.util.FabricView;
import com.h2play.canvas_magic.util.FileUtil;

import timber.log.Timber;

import static com.h2play.canvas_magic.util.FabricView.LOCKED_MODE;

public class ShapeListActivity extends BaseActivity implements ShapeListMvpView, ErrorView.ErrorListener {

    @Inject
    ShapeListPresenter listPresenter;

    ShapeInfo selectedShape;

    @BindView(R.id.btn_shape)
    Button shapeButton;

    @BindView(R.id.view_error)
    ErrorView errorView;

    @BindView(R.id.progress)
    ProgressBar progressBar;

    @BindViews({R.id.fabricView1,R.id.fabricView2,R.id.fabricView3,
            R.id.fabricView4,R.id.fabricView5,R.id.fabricView6,
            R.id.fabricView7,R.id.fabricView8,R.id.fabricView9})
    FabricView[] fabricViews;

    private List<ShapeInfo> shapeInfos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        errorView.setErrorListener(this);

        listPresenter.getShapes();

    }

    @Override
    protected void onResume() {

        super.onResume();

        if(selectedShape != null) {
            showShape(selectedShape.name,selectedShape.fileName);
        }

    }

    @Override
    public void showShape(String name, String fileName) {
        selectedShape = new ShapeInfo(fileName,name);
        shapeButton.setText(selectedShape.name);
        Observable.timer(100, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Long aLong) {

                        for (int i = 0; i < fabricViews.length; ++i) {
                            drawShape(fabricViews[i], fileName, i);
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

    public int[] resources = {R.id.fabricView1,R.id.fabricView2,R.id.fabricView3,
            R.id.fabricView4,R.id.fabricView5,R.id.fabricView6,
            R.id.fabricView7,R.id.fabricView8,R.id.fabricView9};

    public void onFabricClick(View view) {
        int index = Arrays.binarySearch(resources, view.getId());
        Intent intent = MakeActivity.getStartIntent(this,index,selectedShape.fileName);
        startActivity(intent);
    }

    @OnClick(R.id.fab_add)
    public void onAddClick() {


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
                        listPresenter.addNewItem(getBaseContext(),et.getText().toString());
                    } else {

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

    @OnClick(R.id.ib_more)
    public void onMoreClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                R.style.Theme_MaterialComponents_Light_Dialog_Alert);
        final CharSequence[] cs = getResources().getStringArray(R.array.menu_array);
        builder.setTitle(R.string.shape_list_title)
                .setItems(cs, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: {

                                showRenameDialog();
                                break;
                            }

                            case 1: {
                                listPresenter.deleteItem( selectedShape.name );
                                break;
                            }
                        }
                    }
                });
        builder.create().show();
    }

    private void showRenameDialog() {

        final EditText et = new EditText(this);
        FrameLayout container = new FrameLayout(this);

        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);

        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);

        et.setLayoutParams(params);


        container.addView(et);

        final AlertDialog.Builder alt_bld = new AlertDialog.Builder(this,R.style.Theme_MaterialComponents_Light_Dialog_Alert);

        alt_bld.setTitle(getResources().getString(R.string.rename))
                .setMessage(getResources()
                        .getString(R.string.insert_new_name)+"\n"+getResources().getString(R.string.before)+" : " + selectedShape.name)
                .setView(container).setPositiveButton(getResources().getString(android.R.string.ok),

                (dialog, id) -> {

                    if (et.getText().length() > 0) {
                        listPresenter.renameItem( selectedShape.name, et.getText().toString());
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


    @OnClick(R.id.btn_shape)
    public void onShapeClick() {

        if (shapeInfos == null) {
            return;
        }

        Observable.fromIterable(shapeInfos)
                .map( shapeInfo-> shapeInfo.name).toList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((strings, throwable) -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    final CharSequence[] cs = strings.toArray(new CharSequence[strings.size()]);
                    builder.setTitle(R.string.shape_list_title)
                            .setItems(cs, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    showShape(shapeInfos.get(which).name, shapeInfos.get(which).fileName);
                                }
                            });
                    builder.create().show();
                });
    }

    public void drawShape(FabricView fabricView, String fileName, int shapeIndex) {
        fabricView.cleanPage();
        fabricView.setColor(Color.BLACK);
        String jsonText = FileUtil.getJsonFromFile(this, fileName);

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
        fabricView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    onFabricClick(view);
                }

                return true;
            }
        });
    }


    @Override
    public int getLayout() {
        return R.layout.activity_list;
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
    public void showShapeList(List<ShapeInfo> shapeInfos) {

        this.shapeInfos = shapeInfos;
        selectedShape = shapeInfos.get(0);
        shapeButton.setText(selectedShape.name);
        showShape(selectedShape.name,selectedShape.fileName);
    }

    @Override
    public void setShapeList(List<ShapeInfo> shapeInfos) {
        this.shapeInfos = shapeInfos;
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
    public void showImpossibleDelete() {
        Toast.makeText(this, getResources().getString(R.string.need_one_item), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReloadData() {

    }
}
