package com.addx.ai.demo.videoview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.setPadding
import com.a4x.player.A4xCommonEntity
import com.a4x.player.IA4xLogReportListener
import com.addx.common.utils.LogUtils
import com.ai.addx.model.request.SdcardPlaybackEntry
import com.ai.addx.model.response.GetSdHasVideoDayResponse
import com.ai.addx.model.response.SdcardPlaybackResponse
import com.ai.addxbase.DirManager
import com.ai.addxbase.util.TimeUtils
import kotlinx.android.synthetic.main.playback_player_full.view.*
import rx.Observable
import java.text.SimpleDateFormat
import java.util.*
import com.addx.ai.demo.R
import com.addx.common.Const
import com.addx.common.utils.BitmapUtils
import com.addx.common.utils.NetworkUtils
import com.addx.common.utils.SizeUtils
import com.ai.addx.model.VideoSliceBean
import com.ai.addxvideo.addxvideoplay.IAddxSdcardView
import com.ai.addxvideo.addxvideoplay.addxplayer.IVideoPlayer
import com.ai.addxvideo.addxvideoplay.addxplayer.PlayerErrorState
import com.ai.addxvideo.addxvideoplay.addxplayer.addxijkplayer.AddxVideoIjkPlayer
import rx.Subscriber
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "DemoSdcardVideoView"
open class DemoSdcardVideoView : DemoBaseVideoView, IAddxSdcardView {
    private var syncPostionTime: Long = 0L
    open var mLivingStartTime: Long = 0
    var mLivingEndTime: Long? = System.currentTimeMillis()
    var mCurrentPosition: Long = 0
    var syncStartTime = false

    private lateinit var sdcardLayout: ViewGroup
    private lateinit var leftLayout: ViewGroup
    public var mListener: Listener? = null
    var mSdcardCacheThumbBitmap: Bitmap? = null

    public interface Listener{
        fun onError(code: Int)
        fun onPlayToEnd()
        fun onPlayError()
        fun onReceivePushError(state: A4xCommonEntity.DeviceState)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun normalLayoutId(): Int {
        return R.layout.playback_player_normal
    }

    override fun fullLayoutId(): Int {
        return R.layout.playback_player_full
    }

    override fun onInitOrReloadUi(context: Context?) {
        if (mIsFullScreen) {
            mAddxVideoContentView.back.setOnClickListener(this)

            mAddxVideoContentView.setOnClickListener(this)
            mAddxVideoContentView.zoom_view.setZoomEnable(true)

            leftLayout = findViewById(R.id.layout_left)
        } else {
            sdcardLayout = mAddxVideoContentView.findViewById(R.id.ll_sdcard)
        }

        soundBtn = mAddxVideoContentView.findViewById(R.id.iv_sound)
        soundBtn?.setOnClickListener(this)
        mAddxVideoContentView.iv_record.setOnClickListener(this)
        mAddxVideoContentView.iv_screen_shot.setOnClickListener(this)
        mShowRecordShotToast = true
    }

    override fun setPlayingStartTime(startTime: Long) {
        mLivingStartTime = startTime
        syncStartTime = false
        syncPostionTime = SystemClock.elapsedRealtime()
        printTime("setPlayingStartTime", startTime)
        if(iAddxPlayer is AddxVideoIjkPlayer){
            (iAddxPlayer as AddxVideoIjkPlayer).setPlayingStartTime(startTime)
        }
    }

    override fun setPlayingEndTime(endTime: Long) {
        mLivingEndTime = endTime
        if(iAddxPlayer is AddxVideoIjkPlayer){
            (iAddxPlayer as AddxVideoIjkPlayer).setPlayingEndTime(endTime)
        }
    }

    override fun getBrokeVoiceType(): Int {
        return 1
    }

    override fun getPlayingStartTime(): Long {
        return mLivingStartTime!!
    }

    override fun getThumbPath(sn: String): String {
        return DirManager.getInstance().getSdcardCoverPath(sn)
    }

    /**
     * 返回当前播已经播放时间
     */
    fun getPlayPosition(): Long {
        val currentPosition = mCurrentPosition + SystemClock.elapsedRealtime() - syncPostionTime
        LogUtils.d(TAG, "sdcardPlayListener-current position=$currentPosition---mLivingStartTime:$mLivingStartTime")
        return currentPosition
    }

    var getSdVideoListCallBackMap:ConcurrentHashMap<Long, A4xCommonEntity.IVideoRecordCallback> = ConcurrentHashMap<Long, A4xCommonEntity.IVideoRecordCallback>()
    var getSdVideoListEmitterMap:ConcurrentHashMap<Long, Subscriber<SdcardPlaybackResponse>?> = ConcurrentHashMap<Long, Subscriber<SdcardPlaybackResponse>?>()

    override fun getVideoList(entry: SdcardPlaybackEntry): Observable<SdcardPlaybackResponse> {
        iAddxPlayer?.setListener(this)
        LogUtils.d(TAG, "getVideoList---startTime=" + entry.startTime + "  " + TimeUtils.formatLogDay(entry.startTime * 1000) + ",endTime" + +entry.endTime + "  " + TimeUtils.formatLogDay(entry.endTime * 1000))
        val observable = Observable.create(Observable.OnSubscribe<SdcardPlaybackResponse> { emitter ->
            getSdVideoListEmitterMap.put(entry.startTime, emitter as Subscriber<SdcardPlaybackResponse>)
            getSdVideoListCallBackMap.put(entry.startTime, object : A4xCommonEntity.IVideoRecordCallback {
                override fun onComplete(data: A4xCommonEntity.RecordFileSlice?) {
                    LogUtils.d(TAG, "=====getVideoList---SdcardAddxVideoView--completed, slice.size=${data?.dayRecordSlice?.size}")
                    var ret = SdcardPlaybackResponse()
                    ret.result = Const.ResponseCode.CODE_OK
                    ret.data = SdcardPlaybackResponse.DataBean()
                    ret.data.videoSlices = ArrayList<VideoSliceBean>()
                    if(data != null && data.dayRecordSlice != null && data.dayRecordSlice.isNotEmpty()){
                        for (item in data.dayRecordSlice) {
                            var record = VideoSliceBean()
                            record.endTime = item.stopTime
                            record.startTime = item.startTime
                            ret.data.videoSlices.add(record)
//                            LogUtils.d(TAG, "=====recordSlice, starttime=${record.startTime}")
                        }
                    }

                    if(data != null && data.earliestRecordSlice != null){
                        ret.data.earliestVideoSlice = VideoSliceBean()
                        ret.data.earliestVideoSlice.startTime = data.earliestRecordSlice.startTime
                        ret.data.earliestVideoSlice.endTime = data.earliestRecordSlice.stopTime
                    }

                    getSdVideoListEmitterMap.remove(entry.startTime)?.onNext(ret)
                    getSdVideoListCallBackMap.remove(entry.startTime)
                }

                override fun onError(errCode: Int, errMsg: String) {
                    LogUtils.d(TAG, "====getVideoList---SdcardAddxVideoView--error")
                    getSdVideoListEmitterMap.remove(entry.startTime)?.onNext(null)
                    getSdVideoListCallBackMap.remove(entry.startTime)
                }
            })
            iAddxPlayer?.getSDVideoList(entry, getSdVideoListCallBackMap.get(entry.startTime))
        })
        return observable
    }


    var getSdHasVideoDaysCallBackMap:ConcurrentHashMap<Long, A4xCommonEntity.IHaveRecordDayCallback> = ConcurrentHashMap<Long, A4xCommonEntity.IHaveRecordDayCallback>()
    var getSdHasVideoDaysEmitterMap:ConcurrentHashMap<Long, Subscriber<GetSdHasVideoDayResponse>?> = ConcurrentHashMap<Long, Subscriber<GetSdHasVideoDayResponse>?>()

    override fun getSdHasVideoDays(entry: SdcardPlaybackEntry): Observable<GetSdHasVideoDayResponse> {
        iAddxPlayer?.setListener(this)
        LogUtils.d(TAG, "====getSdHasVideoDays---begin")
        return Observable.create(Observable.OnSubscribe<GetSdHasVideoDayResponse> { emitter ->
            getSdHasVideoDaysEmitterMap.put(entry.startTime, emitter as Subscriber<GetSdHasVideoDayResponse>)
            getSdHasVideoDaysCallBackMap.put(entry.startTime, object : A4xCommonEntity.IHaveRecordDayCallback {

                override fun onError(errCode: Int, errMsg: String?) {
                    LogUtils.d(TAG, "====getSdHasVideoDays onError")
                    getSdHasVideoDaysCallBackMap.remove(entry.startTime)
                    getSdHasVideoDaysEmitterMap.remove(entry.startTime)?.onNext(null)
                }

                override fun onComplete(response: A4xCommonEntity.HaveRecordDayResponse?) {
                    LogUtils.d(TAG, "====getSdHasVideoDays SUCC")
                    var obj = GetSdHasVideoDayResponse()
                    obj.result = Const.ResponseCode.CODE_OK
                    obj.data = GetSdHasVideoDayResponse.DataBean()
                    obj.data.videoInfo = ArrayList<GetSdHasVideoDayResponse.DataBean.VideoInfoEntity>()
                    if(response != null && !response.recordInfo.isEmpty()){
                        for(item in response.recordInfo){
                            var entity = GetSdHasVideoDayResponse.DataBean.VideoInfoEntity()
                            entity.isHasVideo = item.hasVideo
                            entity.startTime  = item.startTime.toInt()
                            obj.data.videoInfo.add(entity)
                        }
                    }
                    getSdHasVideoDaysCallBackMap.remove(entry.startTime)
                    getSdHasVideoDaysEmitterMap.remove(entry.startTime)?.onNext(obj)
                }
            })
            iAddxPlayer?.dayHasVideo(entry, getSdHasVideoDaysCallBackMap.get(entry.startTime))
        })
    }

    override fun getSportTrackOpen(): Boolean {
        return false
    }

    override fun onDebug(p0: MutableMap<String, String>?) {
        
    }

    override fun showAutoHideUI(timeout: Long) {
        if (!mShowing) {
            mShowing = true
            startBtn?.visibility = View.VISIBLE
            removeCallbacks(startBtnAction)
            if (mIsFullScreen) {
                leftLayout?.visibility = View.VISIBLE
            } else {
                soundBtn?.visibility = View.VISIBLE
                sdcardLayout.visibility = View.VISIBLE
                fullScreenBtn?.visibility = View.VISIBLE
            }
        }
        updatePausePlayIcon()
        if (timeout != 0L) {
//            postDelayed(mFadeOut, timeout)
        }
    }

    override fun hideAutoHideUI() {
        if (mShowing) {
            mShowing = false
            startBtn?.visibility = View.INVISIBLE
            if (mIsFullScreen) {
                leftLayout?.visibility = View.INVISIBLE
            } else {
                soundBtn?.visibility = View.INVISIBLE
                sdcardLayout.visibility = View.INVISIBLE
                fullScreenBtn?.visibility = View.VISIBLE
            }
        }
        removeCallbacks(mFadeOut)
    }

    override fun changeUIToError(opt: Int?) {
        super.changeUIToError(opt)
        when (opt) {
            PlayerErrorState.ERROR_PLAYER_TIMEOUT -> {
                setErrorInfo(R.string.sdvideo_timeout, R.mipmap.live_timeout)
            }
            PlayerErrorState.ERROR_CONNECT_TIMEOUT -> {
                setErrorInfo(R.string.sdvideo_timeout, R.mipmap.live_timeout)
            }
        }
        if (mIsFullScreen) {

        } else {
            fullScreenBtn?.visibility = View.VISIBLE
            soundBtn?.visibility = View.INVISIBLE
            sdcardLayout.visibility = View.INVISIBLE
        }
        loadingLayout?.visibility = View.INVISIBLE
        startBtn?.visibility = View.INVISIBLE
        mListener?.onPlayError()
    }

    override fun setDefaultErrorInfo(){
        setErrorInfo(R.string.sdvideo_error, R.mipmap.live_exception)
    }

    // current tate pause /inited / uninited / for living
    override fun changeUIToIdle() {
        super.changeUIToIdle()
        if (mIsFullScreen) {
            leftLayout?.visibility = View.INVISIBLE
        } else {
            fullScreenBtn?.visibility = View.VISIBLE
            soundBtn?.visibility = View.INVISIBLE
            sdcardLayout.visibility = View.INVISIBLE
        }
        loadingLayout?.visibility = View.INVISIBLE
        startBtn?.visibility = View.VISIBLE
        removeCallbacks(startBtnAction)
        updatePausePlayIcon()
    }


    override fun changeUIToConnecting() {
        super.changeUIToConnecting()
        LogUtils.d(TAG, "changeUIToConnecting, mIsFullScreen= $mIsFullScreen")
        if (mIsFullScreen) {
            leftLayout?.visibility = View.INVISIBLE
        } else {
//            fullScreenBtn?.visibility = View.INVISIBLE
            soundBtn?.visibility = View.INVISIBLE
            sdcardLayout.visibility = View.INVISIBLE
        }
        loadingLayout?.visibility = View.VISIBLE
        startBtn?.visibility = View.INVISIBLE
    }

    override fun onClickUnderlineErrorButton(tip: TextView?) {
        if(resources.getString(R.string.reconnect).equals(tvUnderLineErrorBtn?.text)){
            reportLiveReconnectClickEvent()
        }else{
            reportLiveClickEvent(PlayerErrorState.getErrorMsg(currentOpt))
        }
        startBtn?.callOnClick()
    }


    override fun changeUIToPlaying() {
        super.changeUIToPlaying()
        LogUtils.d(TAG, "changeUIToPlaying, mIsFullScreen= $mIsFullScreen")
        if (mIsFullScreen) {
            leftLayout?.visibility = View.INVISIBLE
        } else {
            soundBtn?.visibility = View.INVISIBLE
            fullScreenBtn?.visibility = View.VISIBLE
            sdcardLayout.visibility = View.INVISIBLE
        }
        loadingLayout?.visibility = View.INVISIBLE
        startBtn?.visibility = View.INVISIBLE
    }

    override fun updateSoundIcon(mute: Boolean) {
        soundBtn?.setImageResource(if (mute) R.mipmap.live_last_sound_disable else R.mipmap.live_last_sound_enable)
        normalSoundBtn?.setImageResource(if (mute) R.mipmap.live_last_sound_disable else R.mipmap.live_last_sound_enable)
    }


    private fun updatePausePlayIcon() {
        if (currentState != CURRENT_STATE_PLAYING) {
            startBtn?.setImageResource(R.mipmap.live_no_full_play_default)
        } else {
            startBtn?.setImageResource(R.mipmap.live_no_full_pause_default)
        }
    }

    fun onSeekProcessing(player: IVideoPlayer?, playingTime: Long) {
        post{
            val playingTimeMs = playingTime * 1000L
            if (playingTime == 0L) {
                mListener?.onPlayToEnd()
                stopPlay()
                return@post
            }
            syncStartTime = true
            mLivingStartTime = playingTimeMs
            mCurrentPosition = 0
            syncPostionTime = SystemClock.elapsedRealtime()
            printTime("syncPlayingTime", playingTimeMs)
        }
    }

//    override fun reportP2pLiveEvent(success: Boolean) {
//        val datas = getReportData(success)
//        AddxTrack.getInstance<Any>().getTrackInstance(SettingTrackManager::class.java)!!.trace("sdcardPlaybackLive", datas)
//    }

    override fun startInternal() {
        LogUtils.w(TAG, "AddxBaseVideoView---------------startsdcard----begintime:${System.currentTimeMillis()}-----------sn:${dataSourceBean!!.serialNumber}")
        printTime("controlStartPlayTime", mLivingStartTime)

        iAddxPlayer?.setDataSource(dataSourceBean)
        iAddxPlayer?.setRenderView(renderView)
        iAddxPlayer?.setListener(this)
        iAddxPlayer?.startSdcard(mLivingStartTime / 1000)
        LogUtils.w(TAG, "AddxBaseVideoView---------------startsdcard------endtime:${System.currentTimeMillis()}---------sn:${dataSourceBean!!.serialNumber}")
    }

    override fun stopInternal(delayReleaseTime: Int){
        LogUtils.w(TAG, "AddxBaseVideoView---------------stopInternal------begintime:${System.currentTimeMillis()}---------sn:${dataSourceBean!!.serialNumber}")
        iAddxPlayer?.stopSdcard(delayReleaseTime)
        LogUtils.w(TAG, "AddxBaseVideoView---------------stopInternal------endtime:${System.currentTimeMillis()}---------sn:${dataSourceBean!!.serialNumber}")
    }

    private fun printTime(tag: String, playingTimeMs: Long) {
        val simpleDateFormat = SimpleDateFormat("yyyy:MM:dd,HH:mm:ss", Locale.getDefault())
        val time = simpleDateFormat.format(playingTimeMs)
        LogUtils.d(tag, time)
    }

    override fun startFullScreen(isReverse: Boolean) {
        stopRecordVideo("fullscreen")
        super.startFullScreen(isReverse)
    }
    override fun backToNormal() {
        super.backToNormal()
        onResetRecordUi()
        recordIcon?.isEnabled = true
    }

    override fun onError(player: IVideoPlayer?, what: Int, extra: Int) {
        super.onError(player, what, extra)
        LogUtils.e(TAG,"onError---error--sd-----sn:${dataSourceBean!!.serialNumber}---what:$what")
        post {
            var isNetConnected = NetworkUtils.isConnected(context)
            if(what == PlayerErrorState.ERROR_DEVICE_MAX_CONNECT_LIMIT || what == PlayerErrorState.ERROR_DEVICE_NO_ACCESS) {
                mListener?.onError(what)
            }else if(!isNetConnected){
                mListener?.onError(PlayerErrorState.ERROR_PHONE_NO_INTERNET)
            }else{
                mListener?.onError(PlayerErrorState.WHOLE_P2P_CONNECT_TIMTOUT)
            }
        }
    }

    override fun onDeviceMsgPush(ret: String, obj: Any) {
        LogUtils.e(TAG,"onDeviceMsgPush-----sd-----sn:${dataSourceBean!!.serialNumber}")
        when(ret){
            A4xCommonEntity.DeviceDataPushAction.DEVICE_RECORDPLAY_SEEK_TIME -> onSeekProcessing(iAddxPlayer, (obj as A4xCommonEntity.RecordPlaySeekPos).seekTime as Long)
            A4xCommonEntity.DeviceDataPushAction.DEVICE_EVENTREPORT -> {
                var event = (obj as A4xCommonEntity.DeviceEventReport).eventType
                LogUtils.e(TAG,"onDeviceMsgPush--event:$event")
                mListener?.onReceivePushError(event)
            }

        }
    }

    override fun onReport(reportTopic: String?, info: IA4xLogReportListener.ReportInfo?) {
        super.onReport(reportTopic, info)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_sound -> {
                LogUtils.d(TAG, "SdcardAddxVideoView------onClick------iv_sound----sn:" + dataSourceBean!!.serialNumber)
                setMuteState(!mute)
                return
            }
        }
        super.onClick(v)
    }

    var mStopDelayReleaseTime = Int.MAX_VALUE
    override fun getStopDelayReleaseTime(): Int{
        return mStopDelayReleaseTime//s
    }

    fun setStopDelayReleaseTime(stopDelayReleaseTime: Int){
        mStopDelayReleaseTime = stopDelayReleaseTime
    }

    override fun setThumbImageInternal(view: ImageView, needBlur: Boolean) {
        dataSourceBean?.let {
            if (it.isDeviceSleep){
                view.setImageResource(R.drawable.live_sleep_bg)
                return
            }
        }
        LogUtils.d(TAG, "AddxBaseVideoView------setThumbImageInternal---mBitmap:${mBitmap == null}--sn:${dataSourceBean!!.serialNumber}")
        if (needBlur) {
            view.setImageBitmap(BitmapUtils.rsCropAndBlur(context, mBitmap, 15, 3, if(dataSourceBean!!.isDoorbell) 4.0f/3 else 16.0f/9))
        } else {
            view.setImageBitmap(mBitmap)
        }
    }

    override fun updateThumbImageSource(){
        if (mBitmap == null) {
            mBitmap = BitmapFactory.decodeResource(resources, defaultThumbRid!!)
        }
    }

    override fun setRecordingUi(){
        super.setRecordingUi()
        recordIcon?.setPadding(SizeUtils.dp2px(3.0f))
        recordIcon?.setBackgroundResource(R.drawable.bg_circle_stoke_white)
    }

    override fun onResetRecordUi() {
        super.onResetRecordUi()
        recordIcon?.setPadding(0)
    }

    override fun saveScreenShotWhenStopPlay(frame: Bitmap){

    }

    override fun setCacheThumbImg(frame: Bitmap){
        mSdcardCacheThumbBitmap = frame
    }

    override fun getCacheThumbImg(sn: String): Bitmap?{
        return mSdcardCacheThumbBitmap
    }
}