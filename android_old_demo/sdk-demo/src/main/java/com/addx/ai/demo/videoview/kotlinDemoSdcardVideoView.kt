package com.addx.ai.demo.videoview

import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.addx.common.utils.LogUtils
import com.ai.addxvideo.addxvideoplay.addxplayer.PlayerCallBack
import com.ai.addxvideo.addxvideoplay.addxplayer.PlayerErrorState
import com.ai.addx.model.request.SdcardPlaybackEntry
import com.ai.addx.model.response.GetSdHasVideoDayResponse
import com.ai.addx.model.response.SdcardPlaybackResponse
import com.ai.addxbase.DirManager
import com.ai.addxvideo.addxvideoplay.addxplayer.IVideoPlayer
import com.ai.addxvideo.addxvideoplay.addxplayer.addxijkplayer.AddxVideoIjkPlayer
import com.ai.addxbase.util.TimeUtils
import com.ai.addxvideo.addxvideoplay.IAddxSdcardView
import com.ai.addxvideo.addxvideoplay.addxplayer.webrtcplayer.AddxVideoWebRtcPlayer
import kotlinx.android.synthetic.main.playback_player_full.view.*
import rx.Observable
import java.text.SimpleDateFormat
import java.util.*
import com.addx.ai.demo.R
import com.ai.addx.model.request.ReporLiveCommonEntry
import com.ai.addx.model.request.ReporLiveInterruptEntry
import com.ai.addxvideo.addxvideoplay.AddxBaseVideoView
import com.ai.addxvideo.track.AddxTrack
import com.ai.addxvideo.track.other.TrackManager
import com.ai.addxvideo.track.setting.SettingTrackManager
import rx.Subscriber
import java.util.concurrent.ConcurrentHashMap

open class kotlinDemoSdcardVideoView : DemoBaseVideoView, IAddxSdcardView {
    private var syncPostionTime: Long = 0L
    open var mLivingStartTime: Long = 0
    var mLivingEndTime: Long? = System.currentTimeMillis()
    var mCurrentPosition: Long = 0
    var syncStartTime = false
    override var eventPlayerName = TrackManager.PlayerName.SDCARD_PLAYBACK_PLAYER

    override var thumbSaveKeySuffix: String? = "Sdcard"

    private lateinit var sdcardLayout: ViewGroup
    private lateinit var leftLayout: ViewGroup
    public var mListener: Listener? = null
    public interface Listener{
        fun toEnd()
        fun toError()
        fun toNoHasSdcard()
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

    override fun getPlayingStartTime(): Long {
        return mLivingStartTime!!
    }

    override fun getThumbPath(sn: String): String {
        return DirManager.getInstance().getSdcardCoverPath(sn)
    }

    override fun startPlayInternal() {
        printTime("controlStartPlayTime", mLivingStartTime)

        iAddxPlayer?.setDataSource(dataSourceBean)
        iAddxPlayer?.setDisplay(renderView)
        iAddxPlayer?.setListener(this)
        iAddxPlayer?.startSdcard(mLivingStartTime / 1000)
        AddxTrack.getInstance<Any>().getTrackInstance(SettingTrackManager::class.java)!!.trace("sdcardPlaybackClick")
    }

    /**
     * 返回当前播已经播放时间
     */
    override fun getPlayPosition(): Long {
        val currentPosition = mCurrentPosition + SystemClock.elapsedRealtime() - syncPostionTime
        LogUtils.d(TAG, "sdcardPlayListener-current position=$currentPosition---mLivingStartTime:$mLivingStartTime")
        return currentPosition
    }

    var getSdVideoListCallBackMap: ConcurrentHashMap<Long, PlayerCallBack> = ConcurrentHashMap<Long, PlayerCallBack>()
    var getSdVideoListEmitterMap: ConcurrentHashMap<Long, Subscriber<SdcardPlaybackResponse>?> = ConcurrentHashMap<Long, Subscriber<SdcardPlaybackResponse>?>()

    override fun retrieveLocalVideo(entry: SdcardPlaybackEntry): Observable<SdcardPlaybackResponse> {
        iAddxPlayer?.setListener(this)
        LogUtils.d(TAG, "retrieveLocalVideo---startTime=" + entry.startTime + "  " + TimeUtils.formatLogDay(entry.startTime * 1000) + ",endTime" + +entry.endTime + "  " + TimeUtils.formatLogDay(entry.endTime * 1000))
        val observable = Observable.create(Observable.OnSubscribe<SdcardPlaybackResponse> { emitter ->
            getSdVideoListEmitterMap.put(entry.startTime, emitter as Subscriber<SdcardPlaybackResponse>)
            getSdVideoListCallBackMap.put(entry.startTime, object : PlayerCallBack {
                override fun completed(obj: Any?) {
                    LogUtils.d(TAG, "retrieveLocalVideo---SdcardAddxVideoView--completed")
                    if (obj is SdcardPlaybackResponse) {
                        getSdVideoListEmitterMap.remove(entry.startTime)?.onNext(obj)
                        getSdVideoListCallBackMap.remove(entry.startTime)
                    } else {
                        getSdVideoListEmitterMap.remove(entry.startTime)?.onNext(null)
                        getSdVideoListCallBackMap.remove(entry.startTime)
                    }
                }

                override fun error(errorCode: Int?, errorMsg: String?, throwable: Throwable?) {
                    LogUtils.d(TAG, "retrieveLocalVideo---SdcardAddxVideoView--error")
                    getSdVideoListEmitterMap.remove(entry.startTime)?.onNext(null)
                    getSdVideoListCallBackMap.remove(entry.startTime)
                }
            })
            iAddxPlayer?.getSdVideoList(entry, getSdVideoListCallBackMap.get(entry.startTime))
        })
        return observable
    }


    var getSdHasVideoDaysCallBackMap: ConcurrentHashMap<Long, PlayerCallBack> = ConcurrentHashMap<Long, PlayerCallBack>()
    var getSdHasVideoDaysEmitterMap: ConcurrentHashMap<Long, Subscriber<GetSdHasVideoDayResponse>?> = ConcurrentHashMap<Long, Subscriber<GetSdHasVideoDayResponse>?>()

    override fun getSdHasVideoDays(entry: SdcardPlaybackEntry): Observable<GetSdHasVideoDayResponse> {
        iAddxPlayer?.setListener(this)
        return Observable.create(Observable.OnSubscribe<GetSdHasVideoDayResponse> { emitter ->
            getSdHasVideoDaysEmitterMap.put(entry.startTime, emitter as Subscriber<GetSdHasVideoDayResponse>)
            getSdHasVideoDaysCallBackMap.put(entry.startTime, object : PlayerCallBack {
                override fun completed(obj: Any?) {
                    if (obj is GetSdHasVideoDayResponse) {
                        getSdHasVideoDaysCallBackMap.remove(entry.startTime)
                        getSdHasVideoDaysEmitterMap.remove(entry.startTime)?.onNext(obj)
                    } else {
                        getSdHasVideoDaysCallBackMap.remove(entry.startTime)
                        getSdHasVideoDaysEmitterMap.remove(entry.startTime)?.onNext(null)
                    }
                }

                override fun error(errorCode: Int?, errorMsg: String?, throwable: Throwable?) {
                    getSdHasVideoDaysCallBackMap.remove(entry.startTime)
                    getSdHasVideoDaysEmitterMap.remove(entry.startTime)?.onNext(null)
                }
            })
            iAddxPlayer?.getSdHasVideoDays(entry, getSdHasVideoDaysCallBackMap.get(entry.startTime))
        })
    }

    override fun show(timeout: Long) {
        if (!mShowing) {
            mShowing = true
            startBtn?.visibility = View.VISIBLE
            removeCallbacks(startBtnAction)
            if (mIsFullScreen) {
                leftLayout?.visibility = View.VISIBLE
            } else {
                soundBtn?.visibility = View.VISIBLE
                sdcardLayout.visibility = View.VISIBLE
                //fullScreenBtn?.visibility = View.VISIBLE
            }
        }
        updatePausePlayIcon()
        if (timeout != 0L) {
//            postDelayed(mFadeOut, timeout)
        }
    }

    override fun hide() {
        if (mShowing) {
            mShowing = false
            startBtn?.visibility = View.INVISIBLE
            if (mIsFullScreen) {
                leftLayout?.visibility = View.INVISIBLE
            } else {
                soundBtn?.visibility = View.INVISIBLE
                sdcardLayout.visibility = View.INVISIBLE
                //fullScreenBtn?.visibility = View.VISIBLE
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
            //fullScreenBtn?.visibility = View.VISIBLE
            soundBtn?.visibility = View.INVISIBLE
            sdcardLayout.visibility = View.INVISIBLE
        }
        loadingLayout?.visibility = View.INVISIBLE
        startBtn?.visibility = View.INVISIBLE
        mListener?.toError()
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
            //fullScreenBtn?.visibility = View.VISIBLE
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
            //fullScreenBtn?.visibility = View.INVISIBLE
            soundBtn?.visibility = View.INVISIBLE
            sdcardLayout.visibility = View.INVISIBLE
        }
        loadingLayout?.visibility = View.VISIBLE
        startBtn?.visibility = View.INVISIBLE
    }

    override fun onClickUnderlineErrorButton(tip: TextView?) {
        startBtn?.callOnClick()
    }


    override fun changeUIToPlaying() {
        super.changeUIToPlaying()
        LogUtils.d(TAG, "changeUIToPlaying, mIsFullScreen= $mIsFullScreen")
        if (mIsFullScreen) {
            leftLayout?.visibility = View.INVISIBLE
        } else {
            soundBtn?.visibility = View.INVISIBLE
            //fullScreenBtn?.visibility = View.VISIBLE
            sdcardLayout.visibility = View.INVISIBLE
        }
        loadingLayout?.visibility = View.INVISIBLE
        startBtn?.visibility = View.INVISIBLE
    }

    override fun updateSoundIcon(mute: Boolean) {
        soundBtn?.setImageResource(if (mute) R.mipmap.live_sound_disable else R.mipmap.live_sound_enable)
    }


    private fun updatePausePlayIcon() {
        if (currentState != AddxBaseVideoView.CURRENT_STATE_PLAYING) {
            startBtn?.setImageResource(R.mipmap.live_no_full_play_default)
        } else {
            startBtn?.setImageResource(R.mipmap.live_no_full_pause_default)
        }
    }

    override fun onSeekProcessing(player: IVideoPlayer?, playingTime: Long) {
        post{
            val playingTimeMs = playingTime * 1000L
            if (playingTime == 0L) {
                mListener?.toEnd()
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

    override fun onDevInitiativeSendMsg(player: IVideoPlayer?, type: Int) {
        if(type == 1){
            mListener?.toNoHasSdcard()
        }
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
    override fun onError(player: IVideoPlayer, what: Int, extra: Int) {
        super.onError(player, what, extra)
        LogUtils.e(TAG,"AddxWebRtc--player---callBackOnErrorToViewIfActive---error--sd-----sn:${dataSourceBean!!.serialNumber}---what:$what")
        if(what == PlayerErrorState.WHOLE_P2P_CONNECT_TIMTOUT || what == PlayerErrorState.ERROR_PHONE_NO_INTERNET || what == PlayerErrorState.ERROR_CONNECT_EXCEPTION || what == PlayerErrorState.ERROR_DEVICE_MAX_CONNECT_LIMIT || what == PlayerErrorState.ERROR_DEVICE_NO_ACCESS){
            post {
                mVideoCallBack?.onError(what)
            }
        }
    }
//    fun setListener(){
//        iAddxPlayer?.setListener(this)
//    }

    fun delayReleaseDevice(){
        if(iAddxPlayer is AddxVideoWebRtcPlayer){
            (iAddxPlayer as AddxVideoWebRtcPlayer).delayReleaseDevice(true)
        }
    }
    override fun getNewLiveInterruptWhenLoaddingReportData(): ReporLiveInterruptEntry? {
        return ReporLiveInterruptEntry()
    }

    override fun getNewLiveReportData(
        isSeccess: Boolean,
        entry: ReporLiveCommonEntry?
    ): ReporLiveCommonEntry? {
        return ReporLiveCommonEntry()
    }
}