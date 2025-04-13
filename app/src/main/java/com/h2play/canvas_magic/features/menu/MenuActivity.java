package com.h2play.canvas_magic.features.menu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.h2play.canvas_magic.BuildConfig;
import com.h2play.canvas_magic.MvpStarterApplication;
import com.h2play.canvas_magic.R;
import com.h2play.canvas_magic.data.DataManager;
import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.features.base.BaseActivity;
import com.h2play.canvas_magic.features.common.ErrorView;
import com.h2play.canvas_magic.features.help.HelpActivity;
import com.h2play.canvas_magic.features.list.ShapeListActivity;
import com.h2play.canvas_magic.features.main.MainActivity;
import com.h2play.canvas_magic.features.share.ShareActivity;
import com.h2play.canvas_magic.injection.component.ActivityComponent;
import com.h2play.canvas_magic.util.AdDialog;
import com.vorlonsoft.android.rate.AppRate;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class MenuActivity extends BaseActivity implements MenuMvpView, ErrorView.ErrorListener {

    @Inject
    MenuPresenter menuPresenter;

    private ErrorView errorView;
    private ProgressBar progressBar;
    private Button startButton;
    private Button moreButton;
    private View btnChannel;
    private View btnHelp;
    private View imgRate;
    private View imgShare;
    private View btnShare;

    private FirebaseAuth mAuth;
    private AdView adView;
    private AdDialog mCustomDialog;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            signInAnonymously();
        }
    }

    @Override
    public void onBackPressed() {
        mCustomDialog.show();
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                        } else {
                        }
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Making sure Firebase is initialized before accessing FirebaseAuth
        if (!com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
            mAuth = FirebaseAuth.getInstance();
        } else {
            com.google.firebase.FirebaseApp.initializeApp(this);
            mAuth = FirebaseAuth.getInstance();
        }

        errorView = findViewById(R.id.view_error);
        progressBar = findViewById(R.id.progress);
        startButton = findViewById(R.id.btn_start);
        moreButton = findViewById(R.id.btn_more);
        btnChannel = findViewById(R.id.btn_channel);
        btnHelp = findViewById(R.id.btn_help);
        imgRate = findViewById(R.id.img_rate);
        imgShare = findViewById(R.id.img_share);
        btnShare = findViewById(R.id.btn_share);
        Button btnRestartTutorial = findViewById(R.id.btn_restart_tutorial);

        btnChannel.setOnClickListener(v -> onChannelClick());
        btnHelp.setOnClickListener(v -> onHelpClick());
        moreButton.setOnClickListener(v -> onMoreClick());
        imgRate.setOnClickListener(v -> onRateClick());
        imgShare.setOnClickListener(v -> onShareLinkClick());
        startButton.setOnClickListener(v -> onStartClick());
        btnShare.setOnClickListener(v -> onShareClick());
        btnRestartTutorial.setOnClickListener(v -> onRestartTutorialClick());

        errorView.setErrorListener(this);

        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        adView = new AdView(this);
        adView.setAdSize(AdSize.MEDIUM_RECTANGLE);
        adView.setAdUnitId("ca-app-pub-9937617798998725/1754825717");
        adView.loadAd(new AdRequest.Builder().build());

        mCustomDialog = new AdDialog(this,
                adView,
                view -> mCustomDialog.dismiss(),
                view -> {
                    mCustomDialog.dismiss();
                    finish();
                });

        Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(this, R.anim.blink);
        moreButton.startAnimation(hyperspaceJumpAnimation);

        AppRate.with(this)
                .setInstallDays((byte) 0)
                .setLaunchTimes((byte) 3)
                .setRemindInterval((byte) 1)
                .setRemindLaunchesNumber((byte) 1)
                .monitor();
        AppRate.showRateDialogIfMeetsConditions(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startButton.setText(R.string.start_trick);
        menuPresenter.checkNeedGuidE();
    }

    @Override
    public int getLayout() {
        return R.layout.activity_menu;
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

    public void onChannelClick() {
        Intent intent = new Intent(this, ShapeListActivity.class);
        startActivity(intent);
    }

    public void onHelpClick() {
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }

    public void onMoreClick() {
        String url = "https://play.google.com/store/apps/dev?id=8030976532724501230";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    public void onRateClick() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID));
        intent.setPackage("com.android.vending");
        startActivity(intent);
    }

    public void onShareLinkClick() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, R.string.app_name);
            String shareMessage = "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID;
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.choose_share_app)));
        } catch (Exception e) {
            // Handle exception
        }
    }

    public void onStartClick() {
        menuPresenter.getShapeList();
    }

    public void onShareClick() {
        Intent intent = ShareActivity.getStartIntent(MenuActivity.this);
        startActivity(intent);
    }

    /**
     * 튜토리얼 다시하기 버튼 클릭 핸들러
     * 확인 대화상자를 표시하고, 사용자가 확인하면 가이드를 다시 활성화한 후 튜토리얼 시작
     */
    public void onRestartTutorialClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.app_name)
                .setMessage(R.string.restart_tutorial_confirm)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    // 가이드 다시 활성화
                    menuPresenter.restartTutorial();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    @Override
    public void startTutorial() {
        Intent intent = MainActivity.getStartIntent(MenuActivity.this, 0);
        startActivity(intent);
    }

    @Override
    public void showShapeList(List<ShapeInfo> shapeInfos) {
        Observable.fromIterable(shapeInfos)
                .map(shapeInfo -> shapeInfo.name).toList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((strings, throwable) -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    final CharSequence[] cs = strings.toArray(new CharSequence[0]);
                    builder.setTitle(R.string.shape_list_title)
                            .setItems(cs, (dialog, which) -> {
                                Intent intent = MainActivity.getStartIntent(MenuActivity.this, which);
                                startActivity(intent);
                                DataManager dataManager = MvpStarterApplication.get(MenuActivity.this).getComponent().dataManager();
                                if (!dataManager.needGuide()) {
                                    finish();
                                }
                            });
                    builder.create().show();
                });
    }

    @Override
    public void showProgress(boolean show) {
        // Implementation
    }

    @Override
    public void showError(Throwable error) {
        errorView.setVisibility(View.VISIBLE);
        Timber.e(error, "There was an error retrieving the data");
    }

    @Override
    public void showTutorial() {
        startButton.setText(R.string.start_tutorial);
    }

    @Override
    public void onReloadData() {
        // Implementation
    }
}
