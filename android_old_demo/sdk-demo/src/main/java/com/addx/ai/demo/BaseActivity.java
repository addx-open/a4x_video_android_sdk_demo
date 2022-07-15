package com.addx.ai.demo;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.ai.addxbase.LanguageUtils;
import com.base.resmodule.view.LoadingDialog;

import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {

    protected Handler mBaseMainHandler = new Handler(Looper.getMainLooper());
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResid());
    }
    protected abstract int getResid();

    @Override
    protected void attachBaseContext(Context base) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            super.attachBaseContext(base);
            Resources res = super.getResources();
            Configuration config = new Configuration();
            config.setToDefaults();
            config.setLocale(Locale.US);
            res.updateConfiguration(config, res.getDisplayMetrics());
        } else {
            super.attachBaseContext(base);
        }
    }

    protected void showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = new LoadingDialog(this);
        }
        loadingDialog.show();
    }

    protected void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    protected void addFragment(@IdRes int containerViewId, Fragment fragment, String tag) {
        if (fragment == null) {
            return;
        }
        FragmentManager sfm = getSupportFragmentManager();
        FragmentTransaction ft = sfm.beginTransaction();
        ft.add(containerViewId, fragment, tag);
        ft.commitNow();
    }
}
