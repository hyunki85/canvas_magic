package com.h2play.canvas_magic.features.pincode;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

import butterknife.OnTouch;

import com.f2prateek.dart.Dart;
import com.f2prateek.dart.InjectExtra;
import com.h2play.canvas_magic.R;
import com.h2play.canvas_magic.data.model.response.ShapeOnline;
import com.h2play.canvas_magic.features.base.BaseActivity;
import com.h2play.canvas_magic.features.common.ErrorView;
import com.h2play.canvas_magic.features.preview.PreviewActivity;
import com.h2play.canvas_magic.injection.component.ActivityComponent;
import com.h2play.canvas_magic.util.ViewUtil;

import kotlin.Unit;
import timber.log.Timber;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public class PinActivity extends BaseActivity implements PinMvpView, ErrorView.ErrorListener {

    public static final String PIN = "pin";
    @InjectExtra
    Integer count;
    @Inject
    PinPresenter pinPresenter;
    private long lastTouchTime;
    private int width;
    private int height;
    private int lastIndex;

    public static Intent getStartIntent(Context context, int count) {
        Intent intent = new Intent(context, PinActivity.class);
        intent.putExtra("count", count);
        return intent;
    }
    

    @OnTouch(R.id.fl_main)
    public boolean onMainClick(View view, MotionEvent motionEvent) {

        if(motionEvent.getAction() != MotionEvent.ACTION_UP)
            return true;

        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();

        int indexX = x / (width/3);
        int indexY = y / (height/(count/3));

        int index = indexY*3 + indexX;

        if(System.currentTimeMillis() - lastTouchTime < 400) {

            if(lastIndex == index) {
                pinPresenter.noMoreGuide();
                Intent intent = new Intent();
                intent.putExtra(PIN,index+1);
                setResult(RESULT_OK,intent);
                finish();
            }
        }

        lastIndex = index;
        lastTouchTime =  System.currentTimeMillis();


        return true;

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Dart.inject(this);
        Display display = getWindowManager().getDefaultDisplay();

        Point point = new Point();
         display.getSize(point);

        width = point.x;
        height = point.y;

        pinPresenter.needGuide();

    }

    @Override
    public void onBackPressed() {

    }

    @Override
    public int getLayout() {
        return R.layout.activity_pin;
    }

    @Override
    protected void inject(ActivityComponent activityComponent) {
        activityComponent.inject(this);
    }

    @Override
    protected void attachView() {
        pinPresenter.attachView(this);
    }

    @Override
    protected void detachPresenter() {
        pinPresenter.detachView();
    }

    @Override
    public void showProgress(boolean show) {

    }

    @Override
    public void showError(Throwable error) {
        Timber.e(error, "There was an error retrieving the pokemon");
    }

    @Override
    public void showGuide() {

        FrameLayout layout = (FrameLayout) findViewById(R.id.fl_main);
        TextView guideTextView = new TextView(this);
        guideTextView.setText(getResources().getString(R.string.double_tap));
        guideTextView.setTextSize(COMPLEX_UNIT_DIP,25);
        guideTextView.setTextColor(Color.WHITE);
        guideTextView.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        layout.addView(guideTextView,params);

        ImageView imageView = new ImageView(this);
        imageView.setImageResource(R.drawable.guide_grid);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });

        params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        layout.addView(imageView,params);

    }

    @Override
    public void onReloadData() {

    }
}
