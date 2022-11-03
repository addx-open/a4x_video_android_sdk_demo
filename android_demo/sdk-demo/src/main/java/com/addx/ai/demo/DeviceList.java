package com.addx.ai.demo;

import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.LinearLayoutCompat;

import com.addx.common.Const;
import com.addx.common.utils.LogUtils;
import com.addx.common.utils.SizeUtils;
import com.ai.addx.model.UserConfigBean;
import com.ai.addxbase.DeviceClicent;
import com.ai.addxbase.IDeviceClient;
import com.ai.addxbase.model.OtaStateResponse;
import com.ai.addxbase.mvvm.RxViewModel;
import com.ai.addxbase.util.ToastUtils;
import com.ai.addx.model.DeviceBean;
import com.ai.addxbase.mvvm.BaseActivity;
import com.ai.addxbase.ADDXBind;
import com.ai.addxsettings.ADDXSettings;
import com.ai.addxvideo.addxvideoplay.AddxBaseVideoView;
import com.ai.addxvideo.addxvideoplay.IAddxView;
import com.ai.addxvideo.addxvideoplay.IAddxViewCallback;
import com.ai.addxvideo.addxvideoplay.LiveAddxVideoView;
import com.ai.addxvideo.addxvideoplay.addxplayer.webrtcplayer.DataChannelCommand;
import com.alibaba.fastjson.JSON;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;


public class DeviceList extends BaseActivity {

    public HashMap<String, WeakReference<AddxBaseVideoView>> allPlayers = new HashMap<>();//key:snsplit
    @Override
    protected int getLayoutId() {
        return R.layout.activity_device_list;
    }

    LinearLayoutCompat container;

    @Override
    protected void initView() {
        super.initView();
        container = findViewById(R.id.list_device);
        if(DemoGlobal.isSDKInited){
            reloadPage();
        }else{
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void reloadPage(){
        container.removeAllViews();
        listDeviceInfo();
    }

    public void stopVideoViews( ){
        Collection<WeakReference<AddxBaseVideoView>> addxallPlayers = allPlayers.values();
        AddxBaseVideoView tmpplayer = null;
        if (addxallPlayers != null) {
            for (WeakReference<AddxBaseVideoView> player : addxallPlayers) {
                tmpplayer = player.get();
                LogUtils.w(TAG, "---------releaseAllVideoViewRender-----------");
                if (tmpplayer != null) {
                    if(tmpplayer.isPlaying() || tmpplayer.isPrepareing()){
                        tmpplayer.stopPlay();
                    }
                }
            }
        }
    }
    void listDeviceInfo() {
        showLoadingDialog();
        DeviceClicent.getInstance().queryDeviceListAsync(new IDeviceClient.ResultListener<List<DeviceBean>>() {
            @Override
            public void onResult(@NotNull IDeviceClient.ResponseMessage responseMessage, @Nullable List<DeviceBean> result) {
                dismissLoadingDialog();
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

                for (DeviceBean bean :result) {
//                            View root = LayoutInflater.from(DeviceList.this).inflate(R.layout.item_device_demo, null, false);
//                            ((TextView)root.findViewById(R.id.item_device_name)).setText(bean.getDeviceName());
//                            root.findViewById(R.id.item_setting).setOnClickListener(new View.OnClickListener() {
//                                @Override
//                                public void onClick(View v) {
//                                    ADDXSettings.Companion.startSetting(DeviceList.this, bean);
//                                }
//                            });

                    LogUtils.d(TAG, "name : " + bean.getDeviceName());
                    LiveAddxVideoView liveAddxVideoView = new LiveAddxVideoView(DeviceList.this);
                    allPlayers.put(bean.getSerialNumber(), new WeakReference<>(liveAddxVideoView));
                    liveAddxVideoView.init(DeviceList.this, bean, new IAddxViewCallback(){
                        @Override
                        public void onFullScreenStateChange(boolean b, @NotNull DeviceBean deviceBean) {

                        }

                        @Override
                        public void toConnectApDevice(@NotNull String s) {

                        }

                        @Override
                        public void onGetMicPremissionSuccess() {

                        }

                        @Override
                        public boolean onClickErrorTip(@Nullable View view, @NotNull DeviceBean deviceBean) {
                            return false;
                        }

                        @Override
                        public boolean onClickUnderline(@Nullable View v, @NotNull DeviceBean sn) {
                            LogUtils.d(TAG, "liveAddxVideoView----onClickUnderline");
                            return true;
                        }

                        @Override
                        public boolean onClickStart(@Nullable View v, @NotNull DeviceBean sn) {
                            LogUtils.d(TAG, "liveAddxVideoView----onClickStart");
                            return true;
                        }

                        @Override
                        public void onClickRefresh(@Nullable View v) {
                            LogUtils.d(TAG, "liveAddxVideoView----onClickRefresh");

                        }


                        @Override
                        public void toLibrary(@NotNull DeviceBean bean) {
                            LogUtils.d(TAG, "liveAddxVideoView----toLibrary");

                        }

                        @Override
                        public void toShare(@NotNull DeviceBean bean) {
                            LogUtils.d(TAG, "liveAddxVideoView----toShare");

                        }

                        @Override
                        public void toLimitMaxPlayerCount(boolean isSplit, @NotNull IAddxView addxView) {
                            LogUtils.d(TAG, "liveAddxVideoView----toLimitMaxPlayerCount");

                        }

                        @Override
                        public void isSupportGuide() {
                            LogUtils.d(TAG, "liveAddxVideoView----isSupportGuide");

                        }

                        @Override
                        public void onToSetting(@NotNull DeviceBean bean) {
                            LogUtils.d(TAG, "liveAddxVideoView----onToSetting");

                        }

                        @Override
                        public void onBackPressed() {
                            LogUtils.d(TAG, "liveAddxVideoView----onBackPressed");
                            liveAddxVideoView.backToNormal();

                        }

                        @Override
                        public void onPlayStateChanged(int currentState, int oldState) {
                            LogUtils.d(TAG, "liveAddxVideoView----onPlayStateChanged");

                        }

                        @Override
                        public void onMicFrame(@Nullable byte[] data) {
                            LogUtils.d(TAG, "liveAddxVideoView----onMicFrame");

                        }

                        @Override
                        public void onStopPlay() {
                            LogUtils.d(TAG, "liveAddxVideoView----onStopPlay");

                        }

                        @Override
                        public void onStartPlay() {
                            LogUtils.d(TAG, "liveAddxVideoView----onStartPlay");
                        }

                        @Override
                        public void onError(int errorCode) {
                            LogUtils.d(TAG, "liveAddxVideoView----onError");
                        }
                    });
                    container.addView(liveAddxVideoView);
                    container.addView(addItem(bean, "Click To SD Card Video", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            DeviceClicent.getInstance().queryDeviceInfo(bean.getSerialNumber(), new IDeviceClient.ResultListener<DeviceBean>(){
                                @Override
                                public void onResult(@NotNull IDeviceClient.ResponseMessage responseMessage, @Nullable DeviceBean result) {
                                    if (result.isSdCardNormal()) {
//                                      ADDXSettings.launchPlaybackPage(DeviceList.this, bean);
                                        Intent devicelistintent = new Intent(DeviceList.this, SdcardPlayActivity.class);
                                        devicelistintent.putExtra("sn", bean);
                                        startActivity(devicelistintent);
                                    } else {
                                        ToastUtils.showShort("no sdcard or not support sdcard");
                                    }
                                }
                            });
                        }
                    }));
                    container.addView(addItem(bean, "Click To Device Info Page", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                                ADDXSettings.launchOTAInfoPage(DeviceList.this, bean);
                        }
                    }));
                    container.addView(addItem(bean, "Click To OTA Info Page", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // input what you want to test
                            ADDXSettings.startNeedOtaCheck(DeviceList.this, bean);
                        }
                    }));
                    container.addView(addItem(bean, "Click To Delete Device", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showLoadingDialog();
                            DeviceClicent.getInstance().deleteDevice(bean.getSerialNumber(), new IDeviceClient.ResultListener<Object>() {
                                @Override
                                public void onResult(@NotNull IDeviceClient.ResponseMessage responseMessage, @Nullable Object result) {
                                    dismissLoadingDialog();
                                    if (responseMessage.getResponseCode() == Const.ResponseCode.CODE_OK) {
                                        reloadPage();
                                    } else {
                                        ToastUtils.showShort(R.string.network_error);
                                    }
                                }
                            });
                        }
                    }));
                    container.addView(addItem(bean, "Click To Log Device Info", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showLoadingDialog();
                            DeviceClicent.getInstance().queryDeviceInfo(bean.getSerialNumber(), new IDeviceClient.ResultListener<DeviceBean>() {
                                @Override
                                public void onResult(@NotNull IDeviceClient.ResponseMessage responseMessage, @Nullable DeviceBean result) {
                                    dismissLoadingDialog();
                                    if (responseMessage.getResponseCode() == Const.ResponseCode.CODE_OK) {
                                        ToastUtils.showShort("Success");
                                        LogUtils.d(TAG, JSON.toJSONString(result));
                                    } else {
                                        ToastUtils.showShort(R.string.network_error);
                                    }
                                }
                            });
                        }
                    }));
                    container.addView(addItem(bean, "Click To Log Device Config", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showLoadingDialog();
                            DeviceClicent.getInstance().queryDeviceConfigAsync(bean.getSerialNumber(), new IDeviceClient.ResultListener<UserConfigBean>() {
                                @Override
                                public void onResult(@NotNull IDeviceClient.ResponseMessage responseMessage, @Nullable UserConfigBean result) {
                                    dismissLoadingDialog();
                                    if (responseMessage.getResponseCode() == Const.ResponseCode.CODE_OK) {
                                        ToastUtils.showShort("Success");
                                        LogUtils.d(TAG, JSON.toJSONString(result));
                                    } else {
                                        ToastUtils.showShort(R.string.network_error);
                                    }
                                }
                            });
                        }
                    }));
                    container.addView(addItem(bean, "check ota state", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!bean.isFirmwareUpdateing()){
                                ToastUtils.showShort("device it not otaing");
                                return;
                            }
                            showLoadingDialog();
                            DeviceClicent.getInstance().checkOtaStatus(bean.getSerialNumber(), new IDeviceClient.ResultListener<OtaStateResponse>() {
                                @Override
                                public void onResult(@NotNull IDeviceClient.ResponseMessage responseMessage, @Nullable OtaStateResponse result) {
                                    dismissLoadingDialog();
                                    if (responseMessage.getResponseCode()!=Const.ResponseCode.CODE_OK){
                                        // you can catch detail code info @Const.ResponseCode
                                        return;
                                    }
                                    if (result!=null){
                                        RxViewModel.OtaState otaState = result.getOtaState();
                                        switch (otaState){
                                            case ERROR:
                                            case NET_ERROR:

                                                ToastUtils.showShort("query failed");
                                                break;
                                            case TIME_OUT:
                                                ToastUtils.showShort("ota time out");
                                                break;
                                            case UPDATE:
                                                ToastUtils.showShort("otaing percent:" + result.getTransferredSize() / result.getTotalSize());
                                                break;
                                            case SUCCESS:
                                                ToastUtils.showShort("OTA SUCCESS");
                                                break;
                                            case INSTALLING:
                                                ToastUtils.showShort("OTA fireware installing");
                                                break;
                                            case SKIP:
                                            case START:
                                            case DEVICE_SLEEP:
                                            case START_SUCCESS:
                                                //do not catch
                                                break;
                                        }
                                    }
                                }
                            });
                        }
                    }));

                    container.addView(addItem(bean, getOtaString(bean), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (bean.isFirmwareUpdateing()) {
                                ToastUtils.showShort(R.string.fireware_is_updating);
                            } else if (bean.needOta()) {
                                DeviceClicent.getInstance().startOta(bean.getSerialNumber(), false, new IDeviceClient.ResultListener<Object>() {
                                    @Override
                                    public void onResult(@NotNull IDeviceClient.ResponseMessage responseMessage, @Nullable Object result) {
                                        if (responseMessage.getResponseCode() == Const.ResponseCode.CODE_OK) {
                                            bean.setFirmwareStatus(1 << 2);
                                            ToastUtils.showShort("start ota");
                                        } else {
                                            ToastUtils.showShort("start ota failed");
                                        }
                                    }
                                });
                            } else {
                                ToastUtils.showShort(R.string.device_update_scuess);
                            }
                        }
                    }));
                    container.addView(addItem(bean, "get thumb image", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String thumbImgPath = liveAddxVideoView.getThumbImagePath(DeviceList.this, bean.getSerialNumber());
                            LogUtils.d(TAG, "thumbImgPath====thumbImgPath："+(thumbImgPath != null? thumbImgPath: ""));
                        }
                    }));
                }
            }
        });
    }

    private String getOtaString(DeviceBean bean) {
        if (bean.isFirmwareUpdateing()) {
            return "Device OTAing";
        } else if (bean.needForceOta()) {
            return "Device Need Force Update";
        } else if (bean.needOta()) {
            return "Device Need Update";
        } else {
            return "Device OTA State Normal";
        }
    }

    private TextView addItem(DeviceBean bean, String text, View.OnClickListener listener) {
        TextView toSDCardVideoPage = new TextView(DeviceList.this);
        toSDCardVideoPage.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SizeUtils.sp2px(40f)));
        toSDCardVideoPage.setBackgroundResource(R.color.theme_color);
        toSDCardVideoPage.setText(text);
        toSDCardVideoPage.setGravity(Gravity.CENTER);
        toSDCardVideoPage.setOnClickListener(listener);
        return toSDCardVideoPage;
    }

    public void clickAddDevice(View v){
        ADDXBind.lanchBind(this,new ADDXBind.Builder().withBindCallback(new ADDXBind.BindInterface() {
            @Override
            public void onBindCancel() {

            }

            @Override
            public void onBindSccess(@NotNull String sn) {
                listDeviceInfo();
            }

            @Override
            public void onBindStart(@NotNull String callBackUrl) {

            }
        }));
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopVideoViews();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        ToastUtils.init(getApplication());
    }
}
