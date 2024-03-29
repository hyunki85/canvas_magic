package com.h2play.canvas_magic.features.list;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.Inflater;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
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
import com.h2play.canvas_magic.features.preview.PreviewActivity;
import com.h2play.canvas_magic.features.share.ShareActivity;
import com.h2play.canvas_magic.injection.component.ActivityComponent;
import com.h2play.canvas_magic.util.FabricView;
import com.h2play.canvas_magic.util.FileUtil;
import com.h2play.canvas_magic.util.ViewUtil;

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

    @BindView(R.id.rv_preview)
    RecyclerView recyclerView;

    private List<ShapeInfo> shapeInfos;
    private RecyclerView.Adapter<PreviewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        errorView.setErrorListener(this);

        listPresenter.getShapes();


        recyclerView.setLayoutManager(new GridLayoutManager(this,3));

        int spacingInPixels = ViewUtil.dpToPx(16);
        recyclerView.addItemDecoration(new SpacesItemDecoration(spacingInPixels));
        adapter = new RecyclerView.Adapter<PreviewHolder>() {
            @NonNull
            @Override
            public PreviewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                View view = LayoutInflater
                        .from(parent.getContext())
                        .inflate(R.layout.item_preview, parent, false);
                return new PreviewHolder(view);
            }

            @Override
            public void onBindViewHolder(PreviewHolder holder, int position) {

                holder.itemView.post(new Runnable() {
                    @Override
                    public void run() {
                        drawShape(holder.fabricView, selectedShape.fileName, position);
                    }
                });
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = MakeActivity.getStartIntent(ShapeListActivity.this,position,selectedShape.fileName);
                        startActivity(intent);
                    }
                });


            }

            @Override
            public int getItemCount() {
                return selectedShape==null?0:selectedShape.count;
            }
        };
        recyclerView.setAdapter(adapter);

    }

    @Override
    protected void onResume() {

        super.onResume();

        if(selectedShape != null) {
            showShape(selectedShape.name,selectedShape.fileName, selectedShape.count);
        }
    }

    @Override
    public void showShape(String name, String fileName, int count) {
        selectedShape = new ShapeInfo(fileName,name,count);
        adapter.notifyDataSetChanged();
        shapeButton.setText(selectedShape.name);
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

        final AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialogTheme);

        View view = getLayoutInflater().inflate(R.layout.dialog_new,null);
        String items[] = new String[]{"6", "9", "12", "15"};
        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this, R.layout.item_list, items);
        final AutoCompleteTextView textview = view.findViewById(R.id.tv_count);
        textview.setAdapter(adapter2);
        EditText editText = view.findViewById(R.id.et_new_name);

        builder.setView(view);

        final AlertDialog alert = builder.create();

        final Button button = view.findViewById(R.id.btn_create);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listPresenter.addNewItem(getBaseContext(),editText.getText().toString(),Integer.parseInt(textview.getText().toString()));
                alert.dismiss();
            }
        });
        editText.addTextChangedListener(new TextWatcher() {

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
                    button.setEnabled(false);
                } else {
                    // Something into edit text. Enable the button.
                    button.setEnabled(true);
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

        final AlertDialog.Builder alt_bld = new AlertDialog.Builder(this,R.style.AlertDialogTheme);

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
                                    showShape(shapeInfos.get(which).name, shapeInfos.get(which).fileName,shapeInfos.get(which).count);
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
        showShape(selectedShape.name,selectedShape.fileName,selectedShape.count);
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

    class PreviewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.fabricView)
        FabricView fabricView;

        PreviewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private int space;

        public SpacesItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view,
                                   RecyclerView parent, RecyclerView.State state) {
            outRect.left = space;
            outRect.right = space;
            outRect.bottom = space;

            outRect.top = space;
        }
    }
}
