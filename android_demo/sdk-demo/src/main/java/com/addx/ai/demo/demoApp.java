package com.addx.ai.demo;

import android.util.Log;

import com.ai.addxbase.AddxVideoContextInitCallBack;
import com.ai.addxbase.AddxNode;
import com.ai.addxbase.A4xContext;
import com.ai.addxsettings.ADDXSettings;
//import com.ai.guard.vicohome.AddxInternalApp;
import androidx.multidex.MultiDexApplication;

import org.jetbrains.annotations.NotNull;

public class demoApp  extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        //请注意替换的字体要注意大写或者小写的区分，对于大写和小写相同的字体，是无法用肉眼区分出大小写的，会给用户带来困惑
        //此字体大写和小写相同，要注意实际项目中不要使用此字体，例如对于bind sdk中需要填写wifi密码的时候，会导致用户误识别
        A4xContext.getInstance().setEnableSetActivitystyle(true);//配置字体必须设置的参数
        ADDXSettings.setCallBack(new ADDXSettings.CallBack() {
            @Override
            public void onDeviceBeDeleteed(@NotNull String deviceSn, boolean isAdmin) {
                Log.d("ddd","onDeviceBeDeleteed-----");
            }
        });
    }

}
