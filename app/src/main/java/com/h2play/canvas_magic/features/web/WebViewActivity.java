package com.h2play.canvas_magic.features.web;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.f2prateek.dart.Dart;
import com.f2prateek.dart.InjectExtra;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.h2play.canvas_magic.R;

public class WebViewActivity extends AppCompatActivity {


    @InjectExtra String url;
    @InjectExtra String title;

    @BindView(R.id.webview) WebView webView;
    @BindView(R.id.txt_title) TextView titleTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        ButterKnife.bind(this);
        Dart.inject(this);


        webView.setWebViewClient(new MyCustomWebViewClient(this));
        webView.clearCache(true);
        webView.clearHistory();
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.loadUrl(url);

        titleTextView.setText(title);
    }

    @OnClick(R.id.btn_cancel)
    public void onCancelClick() {
        finish();
    }

    private class MyCustomWebViewClient extends WebViewClient {
        public MyCustomWebViewClient(WebViewActivity webViewActivity) {
        }
    }
}
