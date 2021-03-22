package com.h2play.canvas_magic.features.menu;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialog;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
import com.h2play.canvas_magic.features.web.WebViewActivity;
import com.h2play.canvas_magic.injection.component.ActivityComponent;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class MenuActivity extends BaseActivity implements MenuMvpView, ErrorView.ErrorListener {

    @Inject
    MenuPresenter menuPresenter;

    @BindView(R.id.view_error)
    ErrorView errorView;

    @BindView(R.id.progress)
    ProgressBar progressBar;

    private FirebaseAuth mAuth;


    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser == null) {
            signInAnonymously();
        }

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

        errorView.setErrorListener(this);


        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("6EC902AB7F41DABAA64DD42BF1FF0711")
                .build();
        mAdView.loadAd(adRequest);


        mAuth = FirebaseAuth.getInstance();

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

    @Override
    public void showPokemon(List<String> pokemon) {

    }


    @OnClick(R.id.btn_channel)
    public void onChannelClick() {
        Intent intent = new Intent(this, ShapeListActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.btn_help)
    public void onHelpClick() {
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);

    }


    @OnClick(R.id.img_rate)
    public void onRateClick() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(
                "https://play.google.com/store/apps/details?id="+ BuildConfig.APPLICATION_ID));
        intent.setPackage("com.android.vending");
        startActivity(intent);

    }

    @OnClick(R.id.img_share)
    public void onShareLinkClick() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Canvas Trick");
            String shareMessage = "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID ;
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, "choose one"));
        } catch(Exception e) {
            //e.toString();
        }

    }

    @OnClick(R.id.btn_start)
    public void onStartClick() {
        menuPresenter.getShapeList();
    }


    @OnClick(R.id.btn_share)
    public void onShareClick() {
        Intent intent = ShareActivity.getStartIntent(MenuActivity.this);
        startActivity(intent);
    }


    @Override
    public void showShapeList(List<ShapeInfo> shapeInfos) {

        Observable.fromIterable(shapeInfos)
                .map( shapeInfo-> shapeInfo.name).toList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((strings, throwable) -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this,
                            R.style.Theme_MaterialComponents_Light_Dialog_Alert);
                    final CharSequence[] cs = strings.toArray(new CharSequence[strings.size()]);
                    builder.setTitle(R.string.shape_list_title)
                            .setItems(cs, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = MainActivity.getStartIntent(MenuActivity.this,which);
                                    startActivity(intent);
                                    DataManager dataManager = MvpStarterApplication.get(MenuActivity.this).getComponent().dataManager();
                                    if(!dataManager.needGuide()) {
                                        finish();
                                    }
                                }
                            });
                    builder.create().show();
                });
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
    public void onReloadData() {

    }
}
