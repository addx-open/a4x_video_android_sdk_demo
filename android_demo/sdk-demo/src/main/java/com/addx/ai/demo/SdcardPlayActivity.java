package com.addx.ai.demo;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.addx.ai.demo.videoview.kotlinDemoSdcardVideoView;
import com.addx.common.Const;
import com.addx.common.utils.LogUtils;
import com.ai.addx.model.DeviceBean;
import com.ai.addx.model.VideoSliceBean;
import com.ai.addx.model.request.SdcardPlaybackEntry;
import com.ai.addx.model.response.SdcardPlaybackResponse;
import com.ai.addxbase.DeviceClicent;
import com.ai.addxbase.IDeviceClient;
import com.ai.addxbase.mvvm.BaseActivity;
import com.ai.addxbase.util.TimeUtils;
import com.ai.addxbase.util.ToastUtils;
import com.ai.addxnet.HttpSubscriber;
import com.ai.addxvideo.addxvideoplay.SimpleAddxViewCallBack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class SdcardPlayActivity extends BaseActivity {

    private kotlinDemoSdcardVideoView mIAddxSdcardView;//PlaybackLivePlayer
    private DeviceBean deviceBean;

    private TreeMap<Long, VideoSliceBean> dataMap = new TreeMap<>();
    private VideoSliceBean earliestVideoSlice;
    private RecyclerView listview;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_playback;
    }


    @Override
    protected void initView() {
        super.initView();
        LogUtils.d("dd","SdcardPlayActivity---initView");
        deviceBean = (DeviceBean) getIntent().getSerializableExtra("sn");
        initPlayer();
        updateData();
        findViewById(R.id.updateBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateData();
            }
        });
    }

//    void getDeviceInfo(){
//        DeviceClicent.getInstance().queryDeviceInfo(getIntent().getStringExtra("sn"), new IDeviceClient.ResultListener<DeviceBean>() {
//            @Override
//            public void onResult(@NotNull IDeviceClient.ResponseMessage responseMessage, @Nullable DeviceBean result) {
//                deviceBean = result;
//                if(deviceBean != null){
//                    initPlayer();
//                    updateData();
//                }
//            }
//        });
//    }
//    void listDeviceInfo() {
//        DeviceClicent.getInstance().queryDeviceListAsync(new IDeviceClient.ResultListener<List<DeviceBean>>() {
//            @Override
//            public void onResult(@NotNull IDeviceClient.ResponseMessage responseMessage, @Nullable List<DeviceBean> result) {
//                if (responseMessage.getResponseCode()<0) {
//                    ToastUtils.showShort("response error code = " + responseMessage.getResponseCode());
//                    return;
//                }
//                if (result==null||result.isEmpty()){
//                    ToastUtils.showShort("no device");
//                    return;
//                }else {
//                }
//
//                for (DeviceBean bean :result) {
//                    deviceBean = bean;
//                    runOnUiThread(new Runnable(){
//                        @Override
//                        public void run() {
//                            initPlayer();
//                            updateData();
//                        }
//                    });
//                    break;
//                }
//            }
//        });
//    }

    private void initPlayer() {
        mIAddxSdcardView = findViewById(R.id.playback_live_player);

        LogUtils.w("initPlayer", "initPlayer-------deviceBean---" + (deviceBean == null));
        mIAddxSdcardView.init(this, deviceBean, new SimpleAddxViewCallBack() {

            @Override
            public void onStartPlay() {
                //        LogUtils.d("onstartPlay","onstartPlay");
            }

//            @Override
//            public boolean onViewClick(@androidx.annotation.Nullable View v) {
//                if (v == null) {
//                    return false;
//                }
//                if(v.getId() == R.id.start){
////                    if (!mIAddxSdcardView.isPlaying()) {
////                        mIAddxSdcardView.setPlayingStartTime(fragment.getPointTime());
////                        mIAddxSdcardView.setPlayingEndTime(getSelectedFragment().getPlayEndTime());
////                    }
//                }
//                return false;
//            }

            @Override
            public void onStopPlay() {
                //    LogUtils.d("onStopPlay","onStopPlay");
            }

            @Override
            public void onError(int errorCode) {

            }
        });

    }

    public Observable<SdcardPlaybackResponse> retrieveLocalVideo(SdcardPlaybackEntry entry) {
        return mIAddxSdcardView.retrieveLocalVideo(entry);
    }

    public void showList(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LogUtils.d("dd","showList---------dataMap.size:"+dataMap.size());
                if(dataMap.isEmpty()){
                    mIAddxSdcardView.setVisibility(View.INVISIBLE);
                }else{
                    mIAddxSdcardView.setVisibility(View.VISIBLE);
                }
                findViewById(R.id.ll_normal_page).setVisibility(View.VISIBLE);
                findViewById(R.id.sdloadding).setVisibility(View.GONE);
                listview = findViewById(R.id.list);
                sdcardAdapter sdcardAdapter = new sdcardAdapter();
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(SdcardPlayActivity.this);
                listview.setLayoutManager(linearLayoutManager);
                listview.addItemDecoration(new SpacesItemDecoration(12));
                listview.setAdapter(sdcardAdapter);
            }
        });
    }
    public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private int space;

        public SpacesItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view,
                                   RecyclerView parent, RecyclerView.State state) {
            outRect.top = space;
            outRect.bottom = space;

        }
    }
    public class sdcardAdapter extends RecyclerView.Adapter<sdcardAdapter.ViewHolder> {
        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView videoitem;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                videoitem = (TextView)itemView;
            }
        }

        private Object[] keys = dataMap.keySet().toArray();
        public sdcardAdapter() {
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            TextView view = new TextView(SdcardPlayActivity.this);
            return new ViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int i) {
            Long begin= dataMap.get(keys[i]).getStartTime();
            Long end= dataMap.get(keys[i]).getEndTime();
            viewHolder.videoitem.setText(TimeUtils.millis2String(begin));
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mIAddxSdcardView.setPlayingStartTime(begin);
                    mIAddxSdcardView.setPlayingEndTime(end);
                    mIAddxSdcardView.startPlay();
                }
            });
        }
        @Override
        public int getItemCount() {
            return dataMap.size();
        }
    }

    public void updateData() {
        LogUtils.w("initPlayer", "initPlayer-------deviceBean---");
        String dateSpan = ((TextView)findViewById(R.id.datespan)).getText().toString();
        SdcardPlaybackEntry entry = new SdcardPlaybackEntry(deviceBean.getSerialNumber());
        long timeMilli = TimeUtils.string2Millis(dateSpan, new SimpleDateFormat("yyyyMMdd"));
        LogUtils.w("initPlayer", "initPlayer-------dateSpan:%s--timeMilli:%s",dateSpan,timeMilli);
        Calendar instance = Calendar.getInstance();
        instance.setTimeInMillis(timeMilli);
        long startTime = instance.getTimeInMillis();
        entry.setStartTime(startTime);

        long endTime = (instance.getTimeInMillis() + TimeUnit.DAYS.toMillis(1));
        entry.setEndTime(endTime);
        LogUtils.e(TAG, "to---retrieveLocalVideo------");
        retrieveLocalVideo(entry).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new HttpSubscriber<SdcardPlaybackResponse>() {
                    @Override
                    public void doOnNext(SdcardPlaybackResponse sdcardPlaybackResponse) {

                        dataMap.clear();
                        earliestVideoSlice = null;
                        int result = sdcardPlaybackResponse.getResult();
                        LogUtils.d("t","retrieveLocalVideo-----doOnNext---result:"+result);
                        if (result < Const.ResponseCode.CODE_OK) {
                            if (result == -30000) {
                                ToastUtils.showShort(R.string.SDcard_video_viewers_limit);
                                finish();
                                return;
                            }else if(result == -20000){
                                ToastUtils.showShort("nodata");
                            }
                            LogUtils.d("getSdHasVideoDayResponseError", "code=" + result + ",message=" + sdcardPlaybackResponse.getMsg());
                        } else {
                            List<VideoSliceBean> list = sdcardPlaybackResponse.getData().getVideoSlices();
                            //   list.clear();
                            earliestVideoSlice = sdcardPlaybackResponse.getData().getEarliestVideoSlice();
                            if (earliestVideoSlice != null) {
                                earliestVideoSlice.setStartTime(earliestVideoSlice.getStartTime() * 1000);
                                earliestVideoSlice.setEndTime(earliestVideoSlice.getEndTime() * 1000);
                            }

                            if (list != null) {
                                for (VideoSliceBean bean : list) {
                                    bean.setStartTime(bean.getStartTime() * 1000);
                                    bean.setEndTime(bean.getEndTime() * 1000);
                                    dataMap.put(bean.getStartTime(), bean);
                                }
                            }
                            if(dataMap != null && dataMap.isEmpty()){
                                ToastUtils.showShort("nodata");
                            }
                        }
                        LogUtils.d("t","retrieveLocalVideo-----doOnNext---dataMap.size:"+dataMap.size());
                        showList();
                    }

                    @Override
                    public void onStart() {
                        super.onStart();
                    }

                    @Override
                    public void onError(Throwable e) {
                        super.onError(e);
                        dataMap.clear();
                        earliestVideoSlice = null;
                        LogUtils.d("t","retrieveLocalVideo-----onError");

                    }

                    @Override
                    public boolean onTimeOut() {
                        LogUtils.d("t","retrieveLocalVideo-----onTimeOut");

                        return true;
                    }

                    @Override
                    public boolean onNetworkException() {
                        LogUtils.d("t","retrieveLocalVideo-----onNetworkException");

                        return true;
                    }
                });


    }
    @Override
    protected void onStop() {
        super.onStop();
        mIAddxSdcardView.stopPlay();
    }

}
