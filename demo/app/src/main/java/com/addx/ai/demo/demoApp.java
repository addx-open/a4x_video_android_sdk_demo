package com.addx.ai.demo;

import android.app.Application;

import com.ai.addxvideo.addxvideoplay.AddxVideoContext;
import com.ai.guard.vicohome.AddxInternalApp;
import com.ai.guard.vicohome.MyApp;
import androidx.multidex.MultiDexApplication;

public class demoApp  extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        AddxVideoContext.setmContext(this);
        AddxInternalApp.getInstance().installAddxVideo(this);
        AddxVideoContext.init(getApplicationContext(), "staging", BuildConfig.FLAVOR, "zh", "US");
    }

}
