package com.h2play.canvas_magic.features.share;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.h2play.canvas_magic.R;
import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.data.model.response.ShapeOnline;
import com.h2play.canvas_magic.features.base.BaseActivity;
import com.h2play.canvas_magic.features.common.ErrorView;
import com.h2play.canvas_magic.features.preview.PreviewActivity;
import com.h2play.canvas_magic.injection.component.ActivityComponent;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class ShareActivity extends BaseActivity implements ShareMvpView, ErrorView.ErrorListener, ShapeAdapter.ClickListener {

    @Inject
    SharePresenter menuPresenter;

    @Inject
    ShapeAdapter shapeAdapter;

    private ErrorView errorView;
    private ProgressBar progressBar;
    private TabLayout tabLayout;
    private RecyclerView mPokemonRecycler;
    private FloatingActionButton fabUpload;

    public static Intent getStartIntent(Context context) {
        Intent intent = new Intent(context, ShareActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize views using findViewById instead of ButterKnife
        errorView = findViewById(R.id.view_error);
        progressBar = findViewById(R.id.progress);
        tabLayout = findViewById(R.id.tab_category);
        mPokemonRecycler = findViewById(R.id.rv_shape_online);
        fabUpload = findViewById(R.id.fab_upload);
        
        // Set click listeners (replacing @OnClick annotations)
        fabUpload.setOnClickListener(v -> onUploadClick());

        errorView.setErrorListener(this);

        shapeAdapter.setClickListener(this);
        mPokemonRecycler.setLayoutManager(new LinearLayoutManager(this));
        mPokemonRecycler.setAdapter(shapeAdapter);
        mPokemonRecycler.addItemDecoration(new DividerItemDecoration(this,LinearLayoutManager.VERTICAL));

        menuPresenter.getShapeOnline(SharePresenter.SORT_TYPE.FEATURED, null);

        shapeAdapter.getNextPage()
                .subscribe(o -> {
                    menuPresenter.getShapeOnline( SharePresenter.SORT_TYPE.values()[tabLayout.getSelectedTabPosition()], o );
                });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                menuPresenter.resetArray();
                menuPresenter.getShapeOnline(SharePresenter.SORT_TYPE.values()[tab.getPosition()], null);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    public int getLayout() {
        return R.layout.activity_share;
    }

    @Override
    protected void inject(ActivityComponent activityComponent) {
        activityComponent.inject(this);
    }

    @Override
    protected void attachView() {
        menuPresenter.attachView(this);
    }

    @Override
    protected void detachPresenter() {
        menuPresenter.detachView();
    }

    @Override
    public void showShapes(List<ShapeOnline> shapeOnlines) {

        shapeAdapter.setShapeOnlines(shapeOnlines);
        shapeAdapter.notifyDataSetChanged();

    }

    @Override
    public void showShapeList(List<ShapeInfo> shapeInfos) {

        Observable.fromIterable(shapeInfos)
                .map( shapeInfo-> shapeInfo.name).toList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((strings, throwable) -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    final CharSequence[] cs = strings.toArray(new CharSequence[strings.size()]);
                    builder.setTitle(R.string.shape_list_title)
                            .setItems(cs, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    menuPresenter.upload(ShareActivity.this, which);
                                }
                            });
                    builder.create().show();
                });
    }

    @Override
    public void onShareComplete() {
        Toast.makeText(this, getResources().getString(R.string.upload_complete), Toast.LENGTH_SHORT).show();
        menuPresenter.resetArray();
        tabLayout.selectTab(tabLayout.getTabAt(2),true);
        menuPresenter.getShapeOnline(SharePresenter.SORT_TYPE.RECENT,null);
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
    public void updateShape(ShapeOnline shapeOnline) {
        shapeAdapter.updateItem(shapeOnline);
    }

    @Override
    public void onReloadData() {

    }

    @Override
    public void onShapeClick(ShapeOnline shapeOnline) {

        startActivity(PreviewActivity.getStartIntent(this,shapeOnline));

    }

    @Override
    public void onLikeClick(ShapeOnline shapeOnline) {
        menuPresenter.addLike(shapeOnline);
    }

    public void onUploadClick() {
        menuPresenter.getShapeList();
    }

}
