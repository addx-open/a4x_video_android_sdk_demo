package com.addx.ai.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.ai.guard.vicohome.modules.home.CameraFragment;

public class DeviceListActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addFragment(R.id.container, new CameraFragment(), "");
    }

    @Override
    protected int getResid() {
        return R.layout.activity_device_list;
    }
}
