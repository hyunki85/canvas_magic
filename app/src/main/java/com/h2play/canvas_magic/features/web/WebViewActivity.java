package com.h2play.canvas_magic.features.web;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Button;

import com.f2prateek.dart.Dart;
import com.f2prateek.dart.InjectExtra;

import com.h2play.canvas_magic.R;

public class WebViewActivity extends AppCompatActivity {

    @InjectExtra String url;
    @InjectExtra String title;

    private WebView webView;
    private TextView titleTextView;
    private Button cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        Dart.inject(this);

        // Initialize views using findViewById instead of ButterKnife
        webView = findViewById(R.id.webview);
        titleTextView = findViewById(R.id.txt_title);
        cancelButton = findViewById(R.id.btn_cancel);
        
        // Set click listener instead of using @OnClick
        cancelButton.setOnClickListener(v -> onCancelClick());

        webView.setWebViewClient(new MyCustomWebViewClient(this));
        webView.clearCache(true);
        webView.clearHistory();
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.loadUrl(url);

        titleTextView.setText(title);
    }

    public void onCancelClick() {
        finish();
    }

    private class MyCustomWebViewClient extends WebViewClient {
        public MyCustomWebViewClient(WebViewActivity webViewActivity) {
        }
    }
}
