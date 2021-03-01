package com.addx.ai.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.addx.common.utils.CommonUtil;

public class VideoViewTest extends NocontrolVideoViewTest {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CommonUtil.hideNavKey(this);
    }
    @Override
    protected int getResid(){
        return R.layout.activity_video_view_test2;
    }

    @Override
    protected void beginAutoPlay(){

    }
}
