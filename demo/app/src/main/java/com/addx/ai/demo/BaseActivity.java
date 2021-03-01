package com.addx.ai.demo;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public abstract class BaseActivity extends AppCompatActivity {

    protected Handler mBaseMainHandler = new Handler(Looper.getMainLooper());
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResid());
    }
    protected abstract int getResid();


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
