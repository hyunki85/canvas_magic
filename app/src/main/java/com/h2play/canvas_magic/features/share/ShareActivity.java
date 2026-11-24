package com.h2play.canvas_magic.features.share;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

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

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

public class ShareActivity extends BaseActivity implements ShareMvpView, ErrorView.ErrorListener, ShapeAdapter.ClickListener {

    private static final String EXTRA_INITIAL_TAB = "initial_tab";
    private static final String EXTRA_DOCUMENT_ID = "document_id";

    @Inject
    SharePresenter menuPresenter;

    @Inject
    ShapeAdapter shapeAdapter;

    private ErrorView errorView;
    private ProgressBar progressBar;
    private TabLayout tabLayout;
    private RecyclerView mPokemonRecycler;
    private FloatingActionButton fabUpload;
    
    private final CompositeDisposable disposables = new CompositeDisposable();

    public static Intent getStartIntent(Context context) {
        Intent intent = new Intent(context, ShareActivity.class);
        return intent;
    }

    public static Intent getRecentStartIntent(Context context) {
        Intent intent = getStartIntent(context);
        intent.putExtra(EXTRA_INITIAL_TAB, SharePresenter.SORT_TYPE.RECENT.ordinal());
        return intent;
    }

    public static Intent getDocumentStartIntent(Context context, String documentId) {
        return getStartIntent(context)
                .putExtra(EXTRA_INITIAL_TAB, SharePresenter.SORT_TYPE.FEATURED.ordinal())
                .putExtra(EXTRA_DOCUMENT_ID, documentId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.h2play.canvas_magic.util.ScreenLayout.setup(this, R.string.discover_shapes);

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

        int initialTab = Math.max(0, Math.min(
                getIntent().getIntExtra(EXTRA_INITIAL_TAB, SharePresenter.SORT_TYPE.FEATURED.ordinal()),
                SharePresenter.SORT_TYPE.values().length - 1));
        TabLayout.Tab selectedTab = tabLayout.getTabAt(initialTab);
        if (selectedTab != null) {
            tabLayout.selectTab(selectedTab);
        }
        menuPresenter.getShapeOnline(SharePresenter.SORT_TYPE.values()[initialTab]);
        String documentId = getIntent().getStringExtra(EXTRA_DOCUMENT_ID);
        if (documentId != null) menuPresenter.getShapeById(documentId);

        Disposable nextPageDisposable = shapeAdapter.getNextPage()
                .subscribe(o -> {
                    menuPresenter.getShapeOnline(SharePresenter.SORT_TYPE.values()[tabLayout.getSelectedTabPosition()]);
                });
        disposables.add(nextPageDisposable);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                menuPresenter.resetArray();
                menuPresenter.getShapeOnline(SharePresenter.SORT_TYPE.values()[tab.getPosition()]);
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
    protected void onDestroy() {
        disposables.clear();
        super.onDestroy();
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
        errorView.setVisibility(View.GONE);
        mPokemonRecycler.setVisibility(View.VISIBLE);
        findViewById(R.id.community_empty).setVisibility(
                shapeOnlines == null || shapeOnlines.isEmpty() ? View.VISIBLE : View.GONE);
        shapeAdapter.setShapeOnlines(shapeOnlines);
        shapeAdapter.notifyDataSetChanged();

    }

    @Override
    public void showShapeList(List<ShapeInfo> shapeInfos) {
        if (shapeInfos == null || shapeInfos.isEmpty()) return;

        CharSequence[] names = new CharSequence[shapeInfos.size()];
        for (int index = 0; index < shapeInfos.size(); index++) {
            names[index] = shapeInfos.get(index).name;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.shape_list_title)
                .setItems(names, (dialog, which) -> menuPresenter.upload(ShareActivity.this, which))
                .show();
    }

    @Override
    public void onShareComplete() {
        com.h2play.canvas_magic.util.Analytics.log(this,
                com.h2play.canvas_magic.util.Analytics.COMMUNITY_UPLOAD);
        Toast.makeText(this, getResources().getString(R.string.upload_complete), Toast.LENGTH_SHORT).show();
        int recentIndex = SharePresenter.SORT_TYPE.RECENT.ordinal();
        if (tabLayout.getSelectedTabPosition() != recentIndex) {
            TabLayout.Tab recentTab = tabLayout.getTabAt(recentIndex);
            if (recentTab != null) {
                // 탭 리스너가 reset + 조회를 담당한다.
                tabLayout.selectTab(recentTab, true);
            }
        } else {
            menuPresenter.resetArray();
            menuPresenter.getShapeOnline(SharePresenter.SORT_TYPE.RECENT);
        }
    }

    @Override
    public void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        fabUpload.setEnabled(!show);
        if (show) {
            errorView.setVisibility(View.GONE);
            findViewById(R.id.community_empty).setVisibility(View.GONE);
        }
    }

    @Override
    public void showError(Throwable error) {
        progressBar.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
        Timber.e(error, "There was an error retrieving community shapes");
    }

    @Override
    public void updateShape(ShapeOnline shapeOnline) {
        shapeAdapter.updateItem(shapeOnline);
    }

    @Override public void openShapePreview(ShapeOnline shapeOnline) {
        startActivity(PreviewActivity.getStartIntent(this, shapeOnline));
    }

    @Override
    public void onReloadData() {
        errorView.setVisibility(View.GONE);
        menuPresenter.resetArray();
        menuPresenter.getShapeOnline(SharePresenter.SORT_TYPE.values()[tabLayout.getSelectedTabPosition()]);
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
