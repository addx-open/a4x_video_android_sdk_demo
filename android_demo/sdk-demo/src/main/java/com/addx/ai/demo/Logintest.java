package com.addx.ai.demo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.addx.common.utils.LogUtils;
import com.ai.addx.model.UserBean;
import com.ai.addxbase.AddxOauth;
import com.blankj.rxbus.RxBus;

public class Logintest extends AppCompatActivity {

    private ProgressBar pb_loading;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logintest);
        pb_loading = findViewById(R.id.pb_loading);
        findViewById(R.id.back).setEnabled(false);
        findViewById(R.id.back).setOnClickListener((v)->{
            onBackPressed();
        });
        pb_loading.setVisibility(View.VISIBLE);
        AddxOauth.startLogin(this, "124063031@qq.com", "Yuan2983", "", new AddxOauth.Callback() {
            @Override
            public void callback(boolean ret, Object msg) {
                findViewById(R.id.name).post(() -> {
                    pb_loading.setVisibility(View.INVISIBLE);
                    LogUtils.d("dd", "startLogin=======doOnNext");
                    findViewById(R.id.back).setEnabled(true);
                    if(ret){
                        if (Logintest.this.getLifecycle().getCurrentState() == Lifecycle.State.RESUMED) {
                            ((TextView)findViewById(R.id.name)).setText("name:"+((UserBean)msg).getName());
                            ((TextView)findViewById(R.id.id)).setText("id:"+((UserBean)msg).getId());
                            ((TextView)findViewById(R.id.email)).setText("email:"+((UserBean)msg).getEmail());
                            ((TextView)findViewById(R.id.account)).setText("account:"+((UserBean)msg).getAccount());
                            ((TextView)findViewById(R.id.token)).setText("token:"+((UserBean)msg).getToken().getToken());

                            Toast.makeText(getApplicationContext(),"login success",Toast.LENGTH_LONG).show();
                        }
                    }else{
                        Toast.makeText(getApplicationContext(),"login fail",Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

}
