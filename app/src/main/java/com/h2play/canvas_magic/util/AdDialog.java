package com.h2play.canvas_magic.util;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatDialog;

import com.google.android.gms.ads.AdView;
import com.h2play.canvas_magic.R;

public class AdDialog extends AppCompatDialog {

    private final AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowManager.LayoutParams lpWindow = new WindowManager.LayoutParams();
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lpWindow.dimAmount = 0.8f;
        getWindow().setAttributes(lpWindow);

        setContentView(R.layout.ad_dialog);

        setLayout();
        setClickListener(mLeftClickListener , mRightClickListener);

        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.container_adview);
        frameLayout.addView(adView);
    }

    public AdDialog(Context context , AdView adView, View.OnClickListener leftListener ,  View.OnClickListener rightListener) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        this.mLeftClickListener = leftListener;
        this.mRightClickListener = rightListener;
        this.adView = adView;
    }
     
    private void setClickListener(View.OnClickListener left , View.OnClickListener right){
        if(left!=null && right!=null ){
            mLeftButton.setOnClickListener(left);
            mRightButton.setOnClickListener(right);
        }else if(left!=null && right==null){
            mLeftButton.setOnClickListener(left);
        }else {
             
        }
    }

    private Button mLeftButton;
    private Button mRightButton;
     
    private View.OnClickListener mLeftClickListener;
    private View.OnClickListener mRightClickListener;

    /*
     * Layout
     */
    private void setLayout(){
        mLeftButton = (Button) findViewById(R.id.btn_cancel);
        mRightButton = (Button) findViewById(R.id.btn_exit);
    }
     
}