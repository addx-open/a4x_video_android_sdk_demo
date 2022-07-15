package com.addx.ai.demo;

import android.content.Intent;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.LinearLayoutCompat;

import com.addx.ai.demo.videoview.KotlinDemoVideoView;
import com.addx.common.utils.LogUtils;
import com.addx.common.utils.SizeUtils;
import com.ai.addx.model.DeviceBean;
import com.ai.addxbase.DeviceClicent;
import com.ai.addxbase.IDeviceClient;
import com.ai.addxbase.mvvm.BaseActivity;
import com.ai.addxbase.util.ToastUtils;
import com.ai.addxvideo.addxvideoplay.SimpleAddxViewCallBack;
import com.ai.addxvideo.addxvideoplay.addxplayer.AddxPlayerManager;
import com.ai.addxvideo.addxvideoplay.addxplayer.IVideoPlayer;
import com.ai.addxvideo.addxvideoplay.addxplayer.webrtcplayer.AddxVideoWebRtcPlayer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;


public class PlayerDeviceList extends BaseActivity {
    DeviceBean firstDevice;
    int positionId = 0;
    String coordinate = "";
    @Override
    protected int getLayoutId() {
        return R.layout.activity_customer_player_list;
    }

    LinearLayoutCompat container;

    @Override
    protected void initView() {
        super.initView();
        container = findViewById(R.id.list_device);
        if(DemoGlobal.isSDKInited){
            listDeviceInfo();
        }else{
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }

    void listDeviceInfo() {
        DeviceClicent.getInstance().queryDeviceListAsync(new IDeviceClient.ResultListener<List<DeviceBean>>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onResult(@NotNull IDeviceClient.ResponseMessage responseMessage, @Nullable List<DeviceBean> result) {
                if (responseMessage.getResponseCode()<0) {
                    ToastUtils.showShort("response error code = " + responseMessage.getResponseCode());
                    return;
                }
                if (result==null||result.isEmpty()){
                    findViewById(R.id.no_device).setVisibility(View.VISIBLE);
                    return;
                }else {
                    findViewById(R.id.no_device).setVisibility(View.INVISIBLE);
                }
                if(result != null && !result.isEmpty()){
                    firstDevice = result.get(0);
                    for (DeviceBean bean :result) {
                        LogUtils.d(TAG, "name : " + bean.getDeviceName());
                        KotlinDemoVideoView demoVideoView = new KotlinDemoVideoView(PlayerDeviceList.this);
                        demoVideoView.init(PlayerDeviceList.this, bean, new SimpleAddxViewCallBack(){
                            @Override
                            public void onStartPlay() {
                                super.onStartPlay();
                            }

                            @Override
                            public void onError(int errorCode) {
                                super.onError(errorCode);
                            }
                        });
                        container.addView(demoVideoView);
                        container.addView(addItem(bean, "add preposition", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                demoVideoView.getIAddxPlayer().addPreLocationPoint("position1", bean.getSerialNumber(), (responseMessage1, result1) -> {
                                    LogUtils.d(TAG, "addPreLocationPoint-----");
                                });
                            }
                        }));
                        container.addView(addItem(bean, "delete preposition", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                demoVideoView.getIAddxPlayer().deletePreLocationPoint(bean.getSerialNumber(), positionId, (responseMessage12, result12) -> {
                                    LogUtils.d(TAG, "deletePreLocationPoint-----");
                                });
                            }
                        }));
                        container.addView(addItem(bean, "get prepositions", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                demoVideoView.getIAddxPlayer().getPreLocationPoints(bean.getSerialNumber(), (responseMessage13, result13) -> {
                                    LogUtils.d(TAG, "getPreLocationPoints-----postion size:"+result13.size());
                                    if(result13 != null && !result13.isEmpty()){
                                        positionId = result13.get(0).id;
                                        coordinate = result13.get(0).coordinate;
                                        LogUtils.d(TAG, "getPreLocationPoints-----postion id:"+result13.get(0));
                                    }
                                });
                            }
                        }));
                        container.addView(addItem(bean, "move to preposition", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                demoVideoView.getIAddxPlayer().moveToPreLocationPoint(coordinate);
                                LogUtils.d(TAG, "moveToPreLocationPoint-----");
                            }
                        }));
                        IVideoPlayer iVideoPlayer = AddxPlayerManager.getInstance().getPlayer(bean);
                        LogUtils.d(TAG, "AddxPlayerManager---------getPlayer---iVideoPlayer:%s",(iVideoPlayer== null));
                        AddxVideoWebRtcPlayer addxVideoWebRtcPlayer = AddxPlayerManager.getInstance().getWebRTCPlayer(bean.getSerialNumber());
                        LogUtils.d(TAG, "AddxPlayerManager---------getPlayer---addxVideoWebRtcPlayer:%s",(addxVideoWebRtcPlayer== null));
                    }
                    Collection<IVideoPlayer> collection = AddxPlayerManager.getInstance().getAllPlayer();
                    LogUtils.d(TAG, "AddxPlayerManager---------getAllPlayer---size:%s",(collection == null?"0":collection.size()));
                }
            }
        });
    }
    public void releasePlayer(View v){
        if(firstDevice == null){
            return;
        }
        IVideoPlayer iVideoPlayer = AddxPlayerManager.getInstance().getPlayer(firstDevice);
        LogUtils.d(TAG, "AddxPlayerManager---releasePlayer------getPlayer---iVideoPlayer:%s",(iVideoPlayer== null));

        AddxPlayerManager.getInstance().releasePlayer(firstDevice);
        Collection<IVideoPlayer> collection1 = AddxPlayerManager.getInstance().getAllPlayer();
        LogUtils.d(TAG, "AddxPlayerManager----releasePlayer-----getAllPlayer---size:%s",(collection1 == null?"0":collection1.size()));

        AddxPlayerManager.getInstance().releaseAll();
        Collection<IVideoPlayer> collection2 = AddxPlayerManager.getInstance().getAllPlayer();
        LogUtils.d(TAG, "AddxPlayerManager----releasePlayer-----getAllPlayer---size:%s",(collection2 == null?"0":collection2.size()));
    }

    public void stopPlayExcludeOne(View v){
        LogUtils.d(TAG, "AddxPlayerManager---stopOther---");
        AddxPlayerManager.getInstance().stopOther(firstDevice.getSerialNumber());
    }

    public void clickAddDevice(View v){
//        ADDXBind.lanchBind(this,new ADDXBind.Builder().withBindCallback(new ADDXBind.BindInterface() {
//            @Override
//            public void onBindCancel() {
//
//            }
//
//            @Override
//            public void onBindSccess(@NotNull String sn) {
//                listDeviceInfo();
//            }
//
//            @Override
//            public void onBindStart(@NotNull String callBackUrl) {
//
//            }
//        }));
    }
    public TextView addItem(DeviceBean bean, String text, View.OnClickListener listener) {
        TextView toSDCardVideoPage = new TextView(this);
        toSDCardVideoPage.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SizeUtils.sp2px(40f)));
        toSDCardVideoPage.setBackgroundResource(com.ai.addxbase.R.color.theme_color);
        toSDCardVideoPage.setText(text);
        toSDCardVideoPage.setGravity(Gravity.CENTER);
        toSDCardVideoPage.setOnClickListener(listener);
        return toSDCardVideoPage;
    }
}
