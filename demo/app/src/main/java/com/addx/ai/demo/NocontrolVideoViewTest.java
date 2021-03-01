package com.addx.ai.demo;

import androidx.lifecycle.Lifecycle;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import com.addx.common.Const;
import com.addx.common.utils.LogUtils;
import com.addx.common.utils.ToastUtils;
import com.ai.addx.model.DeviceBean;
import com.ai.addx.model.request.BaseEntry;
import com.ai.addx.model.response.AllDeviceResponse;
import com.ai.addxnet.ApiClient;
import com.ai.addxnet.HttpSubscriber;
import com.ai.addxvideo.addxvideoplay.AddxBaseVideoView;
import com.ai.addxvideo.addxvideoplay.DeviceManager;
import com.ai.addxvideo.addxvideoplay.SimpleAddxViewCallBack;

import java.util.List;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class NocontrolVideoViewTest extends BaseActivity{
    private static final String TAG = "NocontrolVideoViewTest";
    private AddxBaseVideoView mNoControlAddxVideoView;
    private List<DeviceBean> allDevice;
    private ProgressBar loadding;
    private CompositeSubscription mSubscription = new CompositeSubscription();
    @Override
    protected int getResid(){
        return R.layout.activity_video_view_test;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResid());
        loadding = findViewById(R.id.loadding);
        mNoControlAddxVideoView = findViewById(R.id.setup_videoview);
        listDevice();
        loadding.setVisibility(View.VISIBLE);
    }
    private Runnable autoPlayRunnable = new Runnable() {
        @Override
        public void run() {
            LogUtils.d(TAG,"initPlayer========autoPlayRunnable");
            mNoControlAddxVideoView.startPlay();
        }
    };
    void listDevice() {
        Subscription subscribe = ApiClient.getInstance()
                .listDevice(new BaseEntry())
                .subscribeOn(Schedulers.io())
                .subscribe(new HttpSubscriber<AllDeviceResponse>() {
                    @Override
                    public void doOnNext(AllDeviceResponse allDeviceResponse) {
                        mNoControlAddxVideoView.post(()->{
                            LogUtils.d(TAG,"initPlayer========doOnNext");
                            loadding.setVisibility(View.INVISIBLE);
                            if (allDeviceResponse.getResult() < Const.ResponseCode.CODE_OK
                                    || allDeviceResponse.getData() == null
                                    || allDeviceResponse.getData().getList() == null) {
                                ToastUtils.showShort("获取设备失败");
                                return;
                            }
                            LogUtils.d(TAG,"initPlayer========doOnNext===ok");
                            allDevice = allDeviceResponse.getData().getList();
                            DeviceManager.getInstance().putOrUpdate(allDevice);
                            if(allDevice != null && !allDevice.isEmpty()){
                                initPlayer();
                                beginAutoPlay();
                            }
                        });
                    }

                    @Override
                    public void doOnError(Throwable e) {
                        super.doOnError(e);
                        mNoControlAddxVideoView.post(() -> {
                            ToastUtils.showShort("获取设备失败");
                            loadding.setVisibility(View.INVISIBLE);
                        });
                    }
                });
        mSubscription.add(subscribe);
    }

    protected void beginAutoPlay(){
        if(mNoControlAddxVideoView != null){
            mNoControlAddxVideoView.postDelayed(autoPlayRunnable, 400);
        }
    }
    private void initPlayer() {
        LogUtils.d(TAG,"initPlayer========");
        if (mNoControlAddxVideoView != null) {
            LogUtils.d(TAG,"initPlayer========1111");
            mNoControlAddxVideoView.setDeviceBean(allDevice.get(0));
            mNoControlAddxVideoView.init(this);
            mNoControlAddxVideoView.setMVideoCallBack(new SimpleAddxViewCallBack() {
                @Override
                public void onStartPlay() {
                    if(getLifecycle().getCurrentState() == Lifecycle.State.RESUMED){
                    }
                }
            });
            View view = mNoControlAddxVideoView.findViewById(R.id.tv_download_speed);
            if(view != null){
                view.setVisibility(View.GONE);
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (mNoControlAddxVideoView != null && mNoControlAddxVideoView.savePlayStatePlaying()) {
            mNoControlAddxVideoView.startPlay();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNoControlAddxVideoView != null && (mNoControlAddxVideoView.isPlaying() || mNoControlAddxVideoView.getPlayState() == AddxBaseVideoView.CURRENT_STATE_PREPAREING)) {
            mNoControlAddxVideoView.savePlayState();
            mNoControlAddxVideoView.stopPlay();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSubscription.clear();
    }
}
