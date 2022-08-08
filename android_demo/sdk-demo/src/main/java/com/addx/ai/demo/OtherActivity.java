package com.addx.ai.demo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.addx.common.Const;

public class OtherActivity extends BaseActivity {

    public static final String TYPE = "TYPE";
    public static final String LIB_TYPE = "LIB_TYPE";
    public static final String DEVICE_SETTING_TYPE = "DEVICE_SETTING_TYPE";
    public static final String USER_SETTING_TYPE = "USER_SETTING_TYPE";
    ImageView descimg;
    TextView desctext;
    Button interfacedoc;
    String interfaceUrl = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        descimg = findViewById(R.id.descimg);
        desctext = findViewById(R.id.desctext);
        interfacedoc = findViewById(R.id.interfacedoc);
        interfacedoc.setOnClickListener((v)->{
            if(interfaceUrl.isEmpty()){
                Toast.makeText(getApplicationContext(),"接口文档不存在",Toast.LENGTH_LONG).show();
                return;
            }
            Intent intent = new Intent(this, WebViewActivity.class);
            intent.putExtra(Const.Extra.WEB_VIEW_URL,interfaceUrl);
            startActivity(intent);
        });
        String type = getIntent().getStringExtra(TYPE);
        if(type.equals(LIB_TYPE)){
            interfaceUrl = "";
            descimg.setImageResource(R.mipmap.lib);
        }else if(type.equals(DEVICE_SETTING_TYPE)){
            interfaceUrl = "";
            descimg.setImageResource(R.mipmap.devicesetting);
        }else if(type.equals(USER_SETTING_TYPE)){
            interfaceUrl = "";
            descimg.setImageResource(R.mipmap.usersetting);
        }
        desctext.setText("详情具体接口描述见接口文档：");
    }

    @Override
    protected int getResid() {
        return R.layout.activity_other;
    }
}
