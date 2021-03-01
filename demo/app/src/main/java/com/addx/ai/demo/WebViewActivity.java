package com.addx.ai.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.addx.common.Const;
import com.addx.common.utils.LogUtils;

public class WebViewActivity extends BaseActivity {

    private WebView webView;
    private String title;
    private String url;
    private WebViewClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
    }

    @Override
    protected int getResid() {
        return R.layout.activity_web_view;
    }

    protected void initView() {
        url = getIntent().getStringExtra(Const.Extra.WEB_VIEW_URL);
        title = getIntent().getStringExtra(Const.Extra.EXTRA_ACTIVITY_TITLE);
        webView = findViewById(R.id.webview_);
        WebSettings mWebSettings = webView.getSettings();
        mWebSettings.setUseWideViewPort(true);
        mWebSettings.setLoadWithOverviewMode(true);
        mWebSettings.setAppCacheEnabled(true);
        mWebSettings.setJavaScriptEnabled(true);
        webView.setHorizontalScrollBarEnabled(false);

        client = new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return super.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override

            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    LogUtils.e("WebResourceError", error.getDescription());
                }

            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                LogUtils.e("onPageFinished", url);
            }

        };
        webView.setWebViewClient(client);
        webView.loadUrl(url);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        webView.stopLoading();
        webView.pauseTimers();
        webView.destroy();

    }
}
