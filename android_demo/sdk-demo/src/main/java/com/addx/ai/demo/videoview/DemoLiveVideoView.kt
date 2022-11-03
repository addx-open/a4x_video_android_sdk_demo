package com.addx.ai.demo.videoview

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.SystemClock
import android.text.TextUtils
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.view.children
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a4x.player.A4xCommonEntity
import com.addx.common.Const
import com.addx.common.utils.*
import com.ai.addx.model.*
import com.ai.addx.model.request.SerialNoEntry
import com.ai.addx.model.response.BaseResponse
import com.ai.addx.model.response.PreLocationResponse.DataBean.PreLocationBean
import com.ai.addx.model.response.UserConfigResponse
import com.ai.addxbase.*
import com.ai.addxbase.GlobalSwap.resConfig
import com.ai.addxbase.adapter.base.BaseQuickAdapter
import com.ai.addxbase.bluetooth.APDeviceManager
import com.ai.addxbase.bluetooth.LocalWebSocketClient
import com.ai.addxbase.helper.SharePreManager
import com.ai.addxbase.theme.IVLiveVideoView
import com.ai.addxbase.util.ToastUtils
import com.ai.addxbase.view.GridSpacingItemDecoration
import com.ai.addxbase.view.dialog.CommonCornerDialog
import com.ai.addxnet.ApiClient
import com.ai.addxnet.HttpSubscriber
import com.ai.addxvideo.PreLocationConst
import com.ai.addxvideo.addxvideoplay.*
import com.ai.addxvideo.addxvideoplay.AddxLiveOptListener.SportAutoTrackListener
import com.ai.addxvideo.addxvideoplay.addxplayer.*
import com.ai.addxvideo.addxvideoplay.view.RockerView
import com.ai.addxvideo.addxvideoplay.view.RockerView.OnRockerPositionChangeListener
import com.ai.addxvideo.addxvideoplay.view.VisualizedView
import com.ai.addxbase.trace.other.TrackManager
import com.airbnb.lottie.LottieAnimationView
import com.alibaba.fastjson.JSON
import com.bumptech.glide.Glide
import com.facebook.common.util.UriUtil
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.drawee.view.SimpleDraweeView
import kotlinx.android.synthetic.main.layout_player_full.view.*
import kotlinx.android.synthetic.main.layout_player_full.view.iv_mic
import kotlinx.android.synthetic.main.layout_player_normal.view.*
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import com.addx.ai.demo.R
import com.ai.addx.model.response.PreLocationResponse

private const val TAG = "DemoLiveVideoView"
open class  DemoLiveVideoView: DemoBaseVideoView, OnRockerPositionChangeListener,
    AddxLiveOptListener {
    private val  mIVLiveVideoView by lazy{
        IVLiveVideoView.create()
    }
    open var showPirToast: Boolean = false//push进来
    internal open var whiteLightOn: Boolean = false
    private var blackBg: View? = null
    private var visualizedView: VisualizedView? = null
    private var tvRatio: TextView? = null
    private var liveFlagLayout: View? = null
    private var fullScreenLayout: View? = null
    internal open  var ivLight: ImageView? = null
    private var ivLightLoading: View? = null
    private var mIvFullScreenMore: ImageView? = null
    private var mLivingIcon: LottieAnimationView? = null
    private var mLivingText: TextView? = null
    private val MIC_TIP_FADEOUT_SPAN: Long = 3000
    private val MIC_SHOW_SHAPE_SPAN: Long = 300
    private var mVoiceTip: LinearLayout? = null
    private var frameVisualizedView: FrameLayout? = null
    private val RING_SPAN: Long = 5000
    var liveFullScreenMenuWindow: LiveFullScreenMenuPopupWindow? = null
    private var liveFullScreenRatioPopupWindow: LiveFullScreenRatioPopupWindow? = null
    private var mMicFramelayout: FrameLayout? = null
    private var mMicText: TextView? = null
    //    private var mRefreshThumbImg: ConcurrentHashMap<String, Long> = ConcurrentHashMap()
    internal var mRinging = false
    private var mIsShotScreenAnim: Boolean = false
    private var mMicTouchListener: OnTouchListener? = null
    private var mOnRockerPositionChangeListener: OnRockerPositionChangeListener? = null

    private var isSportTrackLoading = false
    private var isSportMoveMode = false
    private var isSportTrackOpen = false
    private val mSportSubscription = CompositeSubscription()
    private var isSportLoaded = false
    private var isSportTrackEnabled = false
    private var mIvSportTrackIcon: ImageView? = null
    private var mTvSportTrackText: TextView? = null
    private var deletePrePositionMode = false
    private var mRingRunnable: Runnable? = null
    private var mIvRing:ImageView? = null

    private var mVoiceDownTime: Long = 0
    private var mTouchUp: Boolean = false
    private var mMicTipFadeOutRunnable = {
        mVoiceTip?.visibility = View.INVISIBLE
    }
    private var mMicVisualizedViewShowRunnable = {
        if(!mTouchUp){
            frameVisualizedView?.visibility = View.VISIBLE
        }
    }
    private val mRockerDisableRunnable = Runnable {
        isSportTrackEnabled = true
        resetSportTrackForView()
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun getDeclaredAttrs(context: Context, attrs: AttributeSet?) {
        super.getDeclaredAttrs(context, attrs)
        if (attrs != null) {
            val tya: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.LiveWebRTCPlayer)
            mIsSplit = tya.getBoolean(R.styleable.LiveWebRTCPlayer_isSplit, false)

            eventPlayerName = if (mIsSplit) TrackManager.PlayerName.HOME_SPLIT_PLAYER else TrackManager.PlayerName.HOME_PLAYER
            LogUtils.d(TAG, "mIsSplit $mIsSplit")
            tya.recycle()
        } else {
            LogUtils.d(TAG, "getDeclaredAttrs attrs is null")
        }
    }

    override fun init(context: Context?, bean: DeviceBean, iAddxViewCallback: IAddxViewCallback) {
        mMicTouchListener = OnTouchListener { v: View?, event: MotionEvent ->
            micTouch(v, event)
            if (event.action == MotionEvent.ACTION_DOWN) {
                findViewById<TextView>(R.id.tv_voice_tip).setText(R.string.release_stop)
            } else {
                findViewById<TextView>(R.id.tv_voice_tip).setText(R.string.hold_speak)
            }
            true
        }
        mOnRockerPositionChangeListener = object:
            OnRockerPositionChangeListener {
            override fun onRockerStartTouch(canRotate: Boolean) {
                if (!canRotate) ToastUtils.showShort(R.string.motion_sport_auto_is_open)
                RockerControlManager.getInstance().onRockerStartTouch(this@DemoLiveVideoView)
            }

            override fun onRockerEndTouch() {
                RockerControlManager.getInstance().release()
            }

            override fun onRockerPositionChange(x: Float, y: Float) {
                RockerControlManager.getInstance().onPositionChange(
                    x,
                    y,
                    bean.serialNumber,
                    this@DemoLiveVideoView
                )
            }
        }
        super.init(context, bean, iAddxViewCallback)
//        setVideoBottomAnimotorAndResetSize(false, false)
//        setOptListener()
        initVideoBottomExpend()
        preApplyConnectWhenB(0)
    }

    override fun fullLayoutId(): Int = R.layout.layout_player_full

    override fun normalLayoutId(): Int = R.layout.layout_player_normal

    override fun errorLayoutId(): Int {
        return when {
            mIsFullScreen -> R.layout.live_plager_full_error_page
            mIsSplit -> R.layout.live_plager_no_full_error_multi_page1
            else -> R.layout.live_plager_no_full_error_default_page
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        val ret = micTouch(v, event)
        if(ret != 0){
            return ret == 1
        }
        return super.onTouch(v, event)
    }

    override fun micTouch(v: View?, event: MotionEvent?): Int{
        when (v?.id) {
            R.id.iv_mic -> {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        LogUtils.d(TAG, "mVoiceTip=11===="+(SystemClock.elapsedRealtime() - mVoiceDownTime))
                        mTouchUp = false
                        mVoiceDownTime = System.currentTimeMillis()
                        return if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            requestMicPermission()
                            -1
                        } else {
                            SystemUtil.Vibrate(activityContext, 100)
                            removeCallbacks(mFadeOut)
                            iAddxPlayer?.audioEnable(true)
                            iAddxPlayer?.speakEnable(true)
//                            iAddxPlayer?.setVolume(VideoSharePreManager.getInstance().getCommunicationVoiceSize(dataSourceBean!!.serialNumber))
                            LogUtils.d(TAG, "hide  mShowing  startBtn?.visibility = View.INVISIBLE")
                            startBtn?.visibility = View.INVISIBLE
                            updateSoundIcon(false)
                            reportVoiceTalkEvent()
                            mVoiceTip?.visibility = View.INVISIBLE
                            postDelayed(mMicVisualizedViewShowRunnable, MIC_SHOW_SHAPE_SPAN)
                            mMicFramelayout?.setBackgroundResource(R.drawable.bg_circle_fill_gray_mic_focus)
                            mMicText?.setText(R.string.release_stop)
                            1
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        LogUtils.d(TAG, "mVoiceTip=====${mVoiceDownTime}=="+(System.currentTimeMillis() - mVoiceDownTime))
                        mTouchUp = true
                        if(System.currentTimeMillis() - mVoiceDownTime < MIC_SHOW_SHAPE_SPAN){
                            mVoiceDownTime = System.currentTimeMillis()
                            removeCallbacks(mMicTipFadeOutRunnable)
                            postDelayed(mMicTipFadeOutRunnable, MIC_TIP_FADEOUT_SPAN)
                            mVoiceTip?.visibility = View.VISIBLE
                        }
                        removeCallbacks(mMicVisualizedViewShowRunnable)
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            iAddxPlayer?.speakEnable(false)
//                            iAddxPlayer?.setVolume(3.0f)
                            mute = false
                            iAddxPlayer?.audioEnable(!mute)
                            updateSoundIcon(mute)
                            frameVisualizedView?.visibility = View.INVISIBLE
                            mMicFramelayout?.setBackgroundResource(R.drawable.bg_circle_fill_gray)
                            mMicText?.setText(R.string.hold_speak)
                        }
                    }
                }


            }
        }
        return 0
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        when (v?.id) {
            R.id.tv_ratio -> {
                if (!isRecording && mAddxVideoContentView.iv_record.isEnabled) {
                    showRationChoosePopupWindow()
                } else {
                    ToastUtils.showShort(R.string.cannot_switch)
                }
            }

            R.id.fullscreen_more -> {
                showMoreWindow()
            }
            R.id.back -> {
//                setVideoBottomAnimotorAndResetSize(false, isPlaying())
            }
        }
    }

    fun showMoreWindow() {
        if(liveFullScreenMenuWindow != null && liveFullScreenMenuWindow?.isShowing == true){
            liveFullScreenMenuWindow?.dismiss()
        }
//        liveFullScreenMenuWindow = LiveFullScreenMenuPopupWindow(this, dataSourceBean!!, context, this)
        liveFullScreenMenuWindow?.showAtLocation(activityContext.window.decorView, Gravity.END, 0, 0)
        hideAutoHideUI()
    }

    var setWhiteLightCallback = object : A4xCommonEntity.IPlayerCallback {
        override fun onError(errorCode: Int, errorMsg: String) {
            LogUtils.d(TAG, "===setWhiteLight=error==")
            post {
                mSetWhiteLightListener?.callback(false, whiteLightOn)
                refreshLightUI(true, whiteLightOn)
            }
        }

        override fun onComplete(data: Any?) {
            LogUtils.d(TAG, "====setWhiteLight=completed=data:${data.toString()}")
            whiteLightOn = !whiteLightOn
            post {
                ivLight?.visibility = View.VISIBLE
                ivLightLoading?.visibility = View.GONE
                mSetWhiteLightListener?.callback(true, whiteLightOn)
                refreshLightUI(true, whiteLightOn)
            }
        }
    }

    var mSetWhiteLightListener: AddxLiveOptListener.Listener? = null
    private fun setWhiteLight(listener: AddxLiveOptListener.Listener) {
        mSetWhiteLightListener = listener
        iAddxPlayer?.setWhiteLight(!whiteLightOn, setWhiteLightCallback)
    }

    var mRingListenerCallback: AddxLiveOptListener.RingListener? = null
    var mSetAlarmCallack = object : A4xCommonEntity.IPlayerCallback {
        override fun onComplete(data: Any?) {
            reportAlarmEvent(mIsFullScreen, true, null)
            mRingListenerCallback?.callback(true, true)
//            refreshRingIconUI(true, true)
        }

        override fun onError(errorCode: Int, errorMsg: String) {
            reportAlarmEvent(mIsFullScreen, false, errorMsg)
            mRingListenerCallback?.callback(false, false)
            LogUtils.d(TAG,"mRinging  $mRinging")
        }
    }

    fun setShowPir(){
        showPirToast = true
    }

    fun showAlarmDialog(ringListener: AddxLiveOptListener.RingListener?) {
        LogUtils.d(TAG, "showAlarmDialog---${R.string.do_alarm_tips.resConfig().configWith(DeviceUtil.getDeviceCategray(dataSourceBean))}")
        if (activityContext != null) {
            val alarmDialog = CommonCornerDialog(activityContext)
            alarmDialog.dismissAfterRightClick=false
            alarmDialog.setRightClickListener{
                mRinging = true
                mRingListenerCallback = ringListener
                if(APDeviceManager.INSTANCE.isApDevice(dataSourceBean)){
                    iAddxPlayer?.setAlarm(mSetAlarmCallack)
                }else{
                    //b0系列固件不支持datachannel触发,经过云腾和永龙，曾妮确认，push和g0设备经过mqtt，直播经过datachannel
                    ApiClient.getInstance().doAlarm(
                        dataSourceBean!!.serialNumber,
                        object : HttpSubscriber<BaseResponse>() {
                            override fun doOnNext(t: BaseResponse?) {
                                mSetAlarmCallack.onComplete(null)
                            }

                            override fun doOnError(e: Throwable?) {
                                mSetAlarmCallack.onError(-1, "")
                            }
                        })
                }
                alarmDialog.dismiss()
                hideNavKey()
            }
            alarmDialog.setTitle(R.string.do_alarm_tips.resConfig().configWith(DeviceUtil.getDeviceCategray(dataSourceBean)))
            alarmDialog.setTitleNoBolder()
            alarmDialog.setLeftText(R.string.cancel)
            alarmDialog.setRightText(R.string.alarm_on)
            alarmDialog.setTitleLeftIcon(R.mipmap.ring_focus_min)
            alarmDialog.setRightTextColor(Color.parseColor("#FF6A6A"))
            alarmDialog.show()
        }
    }

    override fun changeUIToError(opt: Int?) {
        super.changeUIToError(opt)
        val shortTips = mIsSplit && !mIsFullScreen
        when (opt) {
            PlayerErrorState.ERROR_DEVICE_UNACTIVATED -> setErrorInfo(if (!shortTips) R.string.camera_not_activated else R.string.camera_not_activated_short, R.mipmap.live_error_unactivated)
            PlayerErrorState.ERROR_DEVICE_SLEEP -> {
                setErrorInfo(
                    R.string.camera_sleep, R.mipmap.ic_sleep_main_live, true, R.string.camera_wake_up, dataSourceBean?.isAdmin
                    ?: false, null, false)
                getSleepMsg().let {
                    if (it.isNotEmpty() && dataSourceBean?.isAdmin!!) {
                        tvErrorTips?.text = it
                    } else {
                        if(mIsSplit){
                            tvErrorTips?.text = it
                        }else{
                            tvErrorTips?.text = it.plus("\n\n" + R.string.admin_wakeup_camera.resConfig().configDevice())
                        }
                    }
                }
            }
            PlayerErrorState.ERROR_DEVICE_AUTH_LIMITATION,
            PlayerErrorState.ERROR_DEVICE_NO_ACCESS -> {
                setErrorInfo(if (!shortTips) R.string.error_2002 else R.string.error_2002_short, R.mipmap.live_error__no_access, underlineErrorBtnText = R.string.refresh)
            }
            PlayerErrorState.ERROR_DEVICE_SHUTDOWN_LOW_POWER -> {
                setErrorInfo(R.string.low_power, R.mipmap.lowpowershutdown)
            }
            PlayerErrorState.ERROR_DEVICE_SHUTDOWN_PRESS_KEY -> {
                if(mIsSplit){
                    setErrorInfo(R.string.turned_off, R.mipmap.shutdown)
                }else{
                    setErrorInfo(R.string.turned_off, R.mipmap.shutdown)
                }
            }
            PlayerErrorState.ERROR_DEVICE_OFFLINE -> {
                if(mIsSplit){
                    setErrorInfo(R.string.camera_poor_network_short, R.mipmap.live_offline)
                }else{
                    setErrorInfo(R.string.camera_poor_network, R.mipmap.live_offline)
                }
            }
            PlayerErrorState.ERROR_PHONE_NO_INTERNET -> {
                setErrorInfo(if (shortTips) R.string.phone_weak_network_short else R.string.failed_to_get_information_and_try)
            }
        }

        errorLayout?.visibility = View.VISIBLE
        loadingLayout?.visibility = View.INVISIBLE
        startBtn?.visibility = View.INVISIBLE
        liveFlagLayout?.visibility = if(APDeviceManager.INSTANCE.isApDevice(dataSourceBean)) View.VISIBLE else View.INVISIBLE
        setLiveErrorTip()
    }

    // current tate pause /inited / uninited / for living
    override fun changeUIToIdle() {
        super.changeUIToIdle()
        if (mIsFullScreen) {
            blackBg?.visibility = View.INVISIBLE
            fullScreenLayout?.visibility = View.INVISIBLE
        } else {
            fullScreenBtn?.visibility = View.VISIBLE
        }
        setLiveTip()
        updatePausePlayIcon()
    }

    override fun changeUIToConnecting() {
        super.changeUIToConnecting()
        if (mIsFullScreen) {
            fullScreenLayout?.visibility = View.INVISIBLE
            blackBg?.visibility = View.INVISIBLE
        } else {
//            fullScreenBtn?.visibility = View.INVISIBLE
        }
        loadingLayout?.visibility = View.VISIBLE
        LogUtils.d(TAG, "hide  mShowing  startBtn?.visibility = View.INVISIBLE")
        startBtn?.visibility = View.INVISIBLE
        liveFlagLayout?.visibility = if(APDeviceManager.INSTANCE.isApDevice(dataSourceBean)) View.VISIBLE else View.INVISIBLE
    }

    override fun changeUIToPlaying() {
        super.changeUIToPlaying()
        mLivingIcon?.setAnimation(R.raw.live_ani)
        mLivingIcon?.resumeAnimation()
        mLivingText?.text = "LIVE"
        if (mIsFullScreen) {
            fullScreenLayout?.visibility = View.INVISIBLE
            blackBg?.visibility = View.INVISIBLE
        } else {
            fullScreenBtn?.visibility = View.VISIBLE
        }
        liveFlagLayout?.visibility = View.VISIBLE
        updatePausePlayIcon()
    }

    private fun updatePausePlayIcon() {
        if (currentState != DemoBaseVideoView.CURRENT_STATE_PLAYING) {
            startBtn?.setImageResource(R.mipmap.live_no_full_play_default)
        } else {
            startBtn?.setImageResource(R.mipmap.live_no_full_pause_default)
        }
    }

    override fun showAutoHideUI(timeout: Long) {
        super.showAutoHideUI(timeout)
        if (!mShowing) {
            mShowing = true
            if((mIsFullScreen && currentState == DemoBaseVideoView.CURRENT_STATE_NORMAL) || !mIsFullScreen){
                startBtn?.visibility = View.VISIBLE
                removeCallbacks(startBtnAction)
                postDelayed(startBtnAction, START_SHOW_SPAN)
            }
            if (mIsFullScreen) {
                fullScreenLayout?.visibility = View.VISIBLE
                blackBg?.visibility = View.VISIBLE
            } else {
            }
        }
        updatePausePlayIcon()
        if (timeout != 0L) {
            removeCallbacks(mFadeOut)
//            postDelayed(mFadeOut, timeout)
        }
    }

    override fun hideAutoHideUI() {
        if (mShowing) {
            mShowing = false
            startBtn?.visibility = View.INVISIBLE
            if (mIsFullScreen) {
                fullScreenLayout?.visibility = View.INVISIBLE
                blackBg?.visibility = View.INVISIBLE
            }
        }
        removeCallbacks(mFadeOut)
    }

    override fun reloadErrorLayout() {
        super.reloadErrorLayout()
        if (mIsSplit && !mIsFullScreen) {
            tvErrorButton?.visibility = View.INVISIBLE
            ivErrorFlag?.visibility = View.INVISIBLE
        } else {
            tvErrorButton?.visibility = View.VISIBLE
            ivErrorFlag?.visibility = View.VISIBLE
        }
    }

    private fun setListener(){
        item_replay?.setOnClickListener {
            if (APDeviceManager.INSTANCE.isApDevice(dataSourceBean)) {
                if(LocalWebSocketClient.INSTANCE.isLogined(dataSourceBean?.userSn!!)){
                    if(isPlaying() || isPrepareing()){
                        stopPlay()
                    }
                    AddxFunJump.jumpToPlayback(context, dataSourceBean)
                }else{
                    mVideoCallBack?.toConnectApDevice(dataSourceBean?.userSn!!)
                }
            }else{
                mVideoCallBack?.toLibrary(dataSourceBean!!)
            }
        }
        item_share?.setOnClickListener {
            AddxFunJump.jumpToSharePage(activityContext, dataSourceBean!!)
            mVideoCallBack?.toShare(dataSourceBean!!)
        }
        item_setting?.setOnClickListener {
            setting()
        }
        LogUtils.d(TAG, "micTouch-----iv_mic:${iv_mic == null}")
        iv_mic?.setOnTouchListener(mMicTouchListener)
    }

    private fun setVideoTopUi() {
        item_device_name.text = dataSourceBean?.deviceName
        val isAdmin = dataSourceBean?.adminId == AccountManager.getInstance().userId
        if (isAdmin) {
            item_share.setImageResource(R.mipmap.home_item_admin)
        } else {
            item_share.setImageResource(R.mipmap.home_item_shared)
        }
        Glide.with(activityContext)
            .load(dataSourceBean?.smallIcon)
            .placeholder(R.mipmap.ic_camera_place_holder_small)
            .into(camera_type_icon)
        if (dataSourceBean!!.getDeviceModel()?.canStandby) {
            if (APDeviceManager.INSTANCE.isApDevice(dataSourceBean)) {
                if (LocalWebSocketClient.INSTANCE.isLogined(dataSourceBean?.userSn)) {
                    item_battery?.alpha =  1.0f
                }else{
                    item_battery?.alpha =  0.4f
                }
            }else{
                item_battery?.alpha = if (dataSourceBean?.online == 1) 1.0f else 0.4f
            }
            LogUtils.d(TAG, JSON.toJSONString(dataSourceBean))
            item_battery?.setCharging(
                dataSourceBean!!.quantityCharge,
                dataSourceBean!!.getIsCharging(),
                dataSourceBean!!.batteryLevel
            )
            item_battery?.visibility = VISIBLE
        } else {
            item_battery?.visibility = GONE
        }
        val ignore = dataSourceBean!!.firmwareStatus shr 1 and 1
        val newVersion = dataSourceBean!!.firmwareStatus and 1
        setVisible(R.id.iv_update_point, /*ignore == 0 && */newVersion == 1 && isAdmin)
    }

    override fun onInitOrReloadUi(context: Context?) {
        if (mIsFullScreen) {
            mAddxVideoContentView.back.setOnClickListener(this)
            mAddxVideoContentView.iv_record.setOnClickListener(this)
            mAddxVideoContentView.iv_screen_shot.setOnClickListener(this)
            mAddxVideoContentView.setOnClickListener(this)
//            contentView.iv_ring.setOnClickListener(this)
            mAddxVideoContentView.zoom_view.setZoomEnable(true)
//            contentView.iv_setting.setOnClickListener(this)
            mAddxVideoContentView.iv_mic.setOnTouchListener(this)
            fullScreenLayout = mAddxVideoContentView.findViewById(R.id.full_screen_icons)
            tvRatio = mAddxVideoContentView.findViewById(R.id.tv_ratio)
            tvRatio?.setOnClickListener(this)
            blackBg = findViewById(R.id.bg_black)
            mAddxVideoContentView.rocker?.visibility = if (dataSourceBean?.isSupportRocker!!) View.VISIBLE else View.GONE
            mAddxVideoContentView.rocker.setOnPositionChangeListener(this)
            mIvFullScreenMore = mAddxVideoContentView.findViewById(R.id.fullscreen_more)
            mIvFullScreenMore?.setOnClickListener(this)
            soundBtn = mAddxVideoContentView.findViewById(R.id.iv_sound)
            soundBtn?.setOnClickListener(this)
        } else {
            soundBtn = normalSoundBtn
            setListener()
            if (mIsSplit) {
                item_device_name?.text = dataSourceBean?.deviceName
            } else {
                setVideoTopUi()
            }
            if(A4xContext.getInstance().getmIsThrid()){
                item_replay?.visibility = GONE
            }
        }

        mLivingIcon = mAddxVideoContentView.findViewById(R.id.living_icon)
        mLivingText = mAddxVideoContentView.findViewById(R.id.tv_live_ani_text)
        setLiveTip()
        if (APDeviceManager.INSTANCE.isApDevice(dataSourceBean)) {
            item_replay.setImageResource(R.mipmap.ap_alive_sdcard_icon)
            if (LocalWebSocketClient.INSTANCE.isLogined(dataSourceBean?.userSn)) {
                LogUtils.d(TAG, "onInitOrReloadUi---set item_replay isLogined dataSourceBean?.isSdCardNormal:${dataSourceBean?.isSdCardNormal}")
                if (dataSourceBean?.isSdCardNormal == true) {
                    item_replay?.visibility = VISIBLE
                }else{
                    item_replay?.visibility = GONE
                }
            } else {
                LogUtils.d(TAG, "onInitOrReloadUi---set item_replay not isLogined dataSourceBean?.isSdCardNormal:${dataSourceBean?.isSdCardNormal}")
                item_replay?.visibility = GONE
            }
            item_share?.visibility = GONE
        }
        frameVisualizedView = findViewById(R.id.frame_visualized_voice)
        visualizedView = findViewById(R.id.visualized_voice)
        mVoiceTip = findViewById(R.id.voice_tip)
        liveFlagLayout = mAddxVideoContentView.findViewById(R.id.ll_living_flag)
        mAddxVideoContentView.findViewById<View>(R.id.rocker)?.visibility = if (dataSourceBean!!.isSupportRocker) View.VISIBLE else View.INVISIBLE

        val wifiLevel = WifiManager.calculateSignalLevel(dataSourceBean!!.signalStrength, 4)
        mAddxVideoContentView.findViewById<ImageView>(R.id.iv_wifi)?.setImageLevel(wifiLevel)
//        contentView.findViewById<BatteryView>(R.id.iv_power)?.setCharging(dataSourceBean!!.getIsCharging(), dataSourceBean!!.batteryLevel)
//        updateRatioTextView(mVideoRatio)
        tvRatio?.text = Ratio.getShowTextByResolutionStr(activityContext, mVideoRatio)
        if((mIVLiveVideoView.isNeedFRocker() || mIVLiveVideoView.isNeedFSpeaker()) && !mIsSplit){
//            layout_pre_location.layoutParams.height = 0
            layout_pre_location?.visibility = GONE
            rl_rocker_and_mic_container?.visibility = GONE
        }
    }

    private fun setLiveErrorTip(){
        if(APDeviceManager.INSTANCE.isApDevice(dataSourceBean)){
            var errorLiveIcon = errorLayout?.findViewById<LottieAnimationView>(R.id.living_icon)
            errorLiveIcon?.pauseAnimation()
            var errorLiveText = errorLayout?.findViewById<TextView>(R.id.tv_live_ani_text)
            if (LocalWebSocketClient.INSTANCE.isLogined(dataSourceBean?.userSn)) {
                errorLiveIcon?.setImageResource(R.drawable.bg_circle_fill_green)
                errorLiveText?.setText(R.string.set_info_status2)
            } else {
                errorLiveIcon?.setImageResource(R.drawable.bg_circle_fill_red)
                errorLiveText?.setText(R.string.set_info_status1)
            }
        }
    }

    private fun setLiveTip(){
        if (APDeviceManager.INSTANCE.isApDevice(dataSourceBean)) {
            mLivingIcon?.pauseAnimation()
            if (LocalWebSocketClient.INSTANCE.isLogined(dataSourceBean?.userSn)) {
                mLivingIcon?.setImageResource(R.drawable.bg_circle_fill_green)
                mLivingText?.setText(R.string.set_info_status2)
            } else {
                mLivingIcon?.setImageResource(R.drawable.bg_circle_fill_red)
                mLivingText?.setText(R.string.set_info_status1)
            }
            liveFlagLayout?.visibility = View.VISIBLE
        }else{
            if(isPlaying()){
                liveFlagLayout?.visibility = View.VISIBLE
            }else{
                liveFlagLayout?.visibility = View.INVISIBLE
            }
        }
    }

    //=====================RockerView.OnPositionChangeListener============
    override fun onRockerStartTouch(mCanRotate: Boolean) {
        if (!mCanRotate) {
            ToastUtils.showShort(R.string.motion_sport_auto_is_open)
            RockerControlManager.getInstance().onRockerStartTouch(this)
            return
        }
        LogUtils.d(TAG, "hide  mShowing  startBtn?.visibility = View.INVISIBLE")
        startBtn?.visibility = View.INVISIBLE
        removeCallbacks(mFadeOut)
    }

    override fun onRockerEndTouch() {
        if((mIsFullScreen && currentState == DemoBaseVideoView.CURRENT_STATE_NORMAL) || !mIsFullScreen) {
            startBtn?.visibility = View.VISIBLE
            removeCallbacks(startBtnAction)
        }
        showAutoHideUI(DEFAULT_SHOW_TIME)
    }

    override fun onRockerPositionChange(x: Float, y: Float) {
        RockerControlManager.getInstance().onPositionChange(x, y, dataSourceBean!!.serialNumber, this)
    }
    //=====================RockerView.OnPositionChangeListener==end==========

    fun showRationChoosePopupWindow() {
        liveFullScreenRatioPopupWindow = LiveFullScreenRatioPopupWindow(mDeviceRatioList, context, object: RadioGroup.OnCheckedChangeListener{
            override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
                liveFullScreenRatioPopupWindow?.dismiss()
                hideNavKey()
                mDeviceRatioList[checkedId]?.let {
                    changeResolution(it)
                    reportChangeRationEvent(mVideoRatio, it)
                }
            }


        })
        liveFullScreenRatioPopupWindow?.showAtLocation(this, Gravity.END, 0, 0)
        mDeviceRatioList.forEach {
            if(mVideoRatio == it.value){
                liveFullScreenRatioPopupWindow?.setCheck(it.key)
            }
        }
        hideAutoHideUI()
    }

    override fun onPrepared(player: IVideoPlayer?) {
        super.onPrepared(player)
        if (isChanggingRatio) {
            if (iAddxPlayer is WebrtcPlayerWrap) {
                iAddxPlayer?.setResolution(mVideoRatio, mSetResolutionCallback)
            }

            isChanggingRatio = false
        }
        post{
            mLivingIcon?.loop(true)
            mLivingIcon?.playAnimation()
        }
    }

    override fun onVideoPause(player: IVideoPlayer?) {
        super.onVideoPause(player)
        post{
            mLivingIcon?.pauseAnimation()
        }
    }

    override fun onError(player: IVideoPlayer?, what: Int, extra: Int) {
        super.onError(player, what, extra)
//        post{
//            mUploadlog?.visibility = if (mIsNeedUploadFailLog) View.VISIBLE else View.GONE
//        if (mIsNeedUploadFailLog){
//            AddxMonitor.getInstance(A4xContext.getInstance().getmContext()).uploadLastDayLog(object: FileLogUpload.Callback{
//                override fun invoke(fileName: String?, ret: Boolean) {
//                }
//            })
//        }
//        mIsNeedUploadFailLog = false
//        }
    }

    override fun onMicFrame(data: ByteArray?) {
        super.onMicFrame(data)
        if (data != null) {
            visualizedView?.updateVolumeByVolumeData(data)
        }
        mVideoCallBack?.onMicFrame(data)
    }

    var mSetResolutionCallback = object : A4xCommonEntity.IPlayerCallback{
        override fun onError(errCode: Int, errMsg: String?) {

        }
        override fun onComplete(response: Any?) {

        }
    }

    @Synchronized
    private fun changeResolution(resolution: String) {
        mVideoRatio = resolution
        isChanggingRatio = true
        VideoSharePreManager.getInstance().setLiveRatio(dataSourceBean!!, mVideoRatio)
        var ratio = Ratio.convertResolutionStrToRatioStr(mVideoRatio)
        VideoSharePreManager.getInstance().setLiveRatioHd(dataSourceBean, ratio == Ratio.R_HD)
//        updateRatioTextView(resolution)
        tvRatio?.text = Ratio.getShowTextByResolutionStr(context, resolution)
        iAddxPlayer?.setResolution(mVideoRatio, mSetResolutionCallback)
    }

    override fun resetResolutionForG0() {
        dataSourceBean?.let {
            if (it.deviceModel.isG0) {
                mVideoRatio = A4xCommonEntity.VideoResolution.VIDEO_SIZE_1280x720.value
                VideoSharePreManager.getInstance().setLiveRatio(it, A4xCommonEntity.VideoResolution.VIDEO_SIZE_1280x720.value)
                var ratio = Ratio.convertResolutionStrToRatioStr(mVideoRatio)
                VideoSharePreManager.getInstance().setLiveRatioHd(it, ratio == Ratio.R_HD)
//                updateRatioTextView(mVideoRatio)
                tvRatio?.text = Ratio.getShowTextByResolutionStr(context, mVideoRatio)
            }
        }
    }

    fun setUiSplit(isSplit: Boolean) {
        this.mIsSplit = isSplit
    }

    override fun stopPlay() {
//        togglePlayerBottomControlVisable(false)
        if (whiteLightOn) {
            iAddxPlayer?.setWhiteLight(false, object: A4xCommonEntity.IPlayerCallback{
                override fun onError(errCode: Int, errMsg: String?) {
                }

                override fun onComplete(response: Any?) {
                }

            })
        }
        super.stopPlay()
        liveFullScreenMenuWindow?.dismiss()
    }

//    fun togglePlayerBottomControlVisable(isOpen: Boolean){
//        if(mIsFullScreen){
//            return
//        }
//        if(isOpen){
//            rl_expand_under_player_container?.visibility = View.VISIBLE
//        }else{
//            rl_expand_under_player_container?.visibility = View.GONE
//        }
//    }

    override fun onClickErrorTips(tip: TextView?) {
        LogUtils.d(TAG, "onClickErrorTips-------" + dataSourceBean!!.serialNumber)
        super.onClickErrorTips(tip)
        if (mIsSplit && !mIsFullScreen && canTryPlay()) {
            val isToPlay = mVideoCallBack?.onClickErrorTip(tip, dataSourceBean!!)
            if(isToPlay == true){
                startPlayWithNetToastandRecord("split_errortip_clickid_")
            }
        }
    }

    private fun reportAlarmEvent(isFullScreen: Boolean, success: Boolean, errorMsg: String?) {
        val segmentation = TrackManager.get().getSegmentation(TrackManager.EventPair.LIVE_ALARM_BELL)
        segmentation["live_player_way"] = if (isFullScreen) "fullscreen" else "halfscreen "
        segmentation["result"] = success
        if (errorMsg != null) {
            segmentation["error_msg"] = success
        }
        TrackManager.get().reportEvent(segmentation)
    }

    private fun reportVoiceTalkEvent() {
        val segmentation = TrackManager.get().getSegmentation(TrackManager.EventPair.LIVE_VOICE_CALLS)
        segmentation["live_player_way"] = if (mIsFullScreen) "fullscreen " else "halfscreen"
        segmentation["result"] = true
        TrackManager.get().reportEvent(segmentation)
    }

    private fun reportToSettingEvent() {
        val segmentation = TrackManager.get().getSegmentation(TrackManager.EventPair.LIVE_CAMERA_SETTING_CLICK)
        segmentation["live_player_way"] = if (mIsFullScreen) "fullscreen" else "halfscreen"
        TrackManager.get().reportEvent(segmentation)
    }

    private fun reportChangeRationEvent(from: String, to: String) {
        val segmentation = TrackManager.get().getSegmentation(TrackManager.EventPair.LIVE_RESOLUTION_CHANGE)
        segmentation["live_player_way"] = if (mIsFullScreen) "fullscreen" else "halfscreen"
        segmentation["from_resolution"] = from
        segmentation["to_resolution"] = to
        TrackManager.get().reportEvent(segmentation)
    }

    //===============================IAddxView========================

    override fun backToNormal() {
        super.backToNormal()
        mLivingIcon?.loop(true)
        mLivingIcon?.playAnimation()
    }

    override fun refreshThumbImg() {
        if (TextUtils.isEmpty(dataSourceBean?.getThumbImgUrl())) {
            return
        }
        if (dataSourceBean?.thumbImgTime != null && dataSourceBean?.thumbImgTime!! > mServerThumbTime && dataSourceBean?.thumbImgTime!! > mLocalThumbTime) {
            if (mThumbImgTime == dataSourceBean?.thumbImgTime!!) {
                return
            }
            mThumbImgTime = dataSourceBean?.thumbImgTime!!

            var imgPath = DownloadUtil.getThumbImgDir(context) + MD5Util.md5(dataSourceBean?.serialNumber)+".jpg"
            FileUtils.createOrExistsDir(DownloadUtil.getThumbImgDir(context))
            DownloadUtil.downloadImg(dataSourceBean?.getThumbImgUrl(), imgPath, object : DownloadUtil.DownloadListener {
                override fun success(url: String?, path: String?) {
                    LogUtils.d(TAG, "toRequestAndRefreshThumbImg===success==code:${this@DemoLiveVideoView.hashCode()}==sn:${dataSourceBean?.serialNumber}==path:$path==url:${dataSourceBean?.getThumbImgUrl()}")
                    VideoSharePreManager.getInstance().setThumbImgServerLastFresh(dataSourceBean?.serialNumber, dataSourceBean?.thumbImgTime, dataSourceBean?.thumbImgUrl)
                    mServerThumbTime = dataSourceBean?.thumbImgTime!!
                    startThumbImgChangeAnimtor(imgPath)
                }

                override fun fail(url: String?) {
                    LogUtils.d(TAG, "toRequestAndRefreshThumbImg=====fail==code:${this@DemoLiveVideoView.hashCode()}==:sn:${dataSourceBean?.serialNumber}===url:$url")
                }
            })
        }
    }

    override fun brokeVoice(p0: AddxLiveOptListener.BrokeVoiceListener?, p1: Int, p2: Int) {
        
    }

    override fun ring(ringListener: AddxLiveOptListener.RingListener) {
        showAlarmDialog(ringListener)
    }

    override fun sport(p0: SportAutoTrackListener?) {
        
    }

    override fun light(listener: AddxLiveOptListener.Listener) {
        setWhiteLight(listener)
    }

//    override fun voice() {
//        if(!mIsFullScreen){
//            setMuteState(!mute)
//        }
//        normalSoundBtn?.setImageResource(if (mute) R.mipmap.voice_black_notalk else R.mipmap.voice_black_talk)
//    }

    override fun setting() {
        if(mIsFullScreen){
            reportToSettingEvent()
        }
        LogUtils.d(TAG, "dataSourceBean-----value:${JSON.toJSONString(dataSourceBean)}")
        AddxFunJump.deviceSettings(activityContext, dataSourceBean!!)
        mVideoCallBack?.onToSetting(dataSourceBean!!)
    }

    override fun startFullScreen(isReverse: Boolean) {
        super.startFullScreen(isReverse)
        if (canTryPlay()) {
            mLivingIcon?.loop(true)
            mLivingIcon?.playAnimation()
            if(!dataSourceBean?.isSupportRocker!! && !mIVLiveVideoView.isNeedFRocker()){
                var constraintLayout = mAddxVideoContentView.iv_mic.parent as ConstraintLayout
                var constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                constraintSet.connect(mAddxVideoContentView.iv_mic.getId(),ConstraintSet.TOP,constraintLayout.getId(),ConstraintSet.TOP,0)
                constraintSet.applyTo(constraintLayout);
            }
        }else{
            fullScreenLayout?.visibility = View.GONE
        }
        if (showPirToast) {
            showPirToast = false
            fullLayoutViewGroup?.findViewById<View>(R.id.pir_toast)?.visibility = View.VISIBLE
            postDelayed({
                fullLayoutViewGroup?.findViewById<View>(R.id.pir_toast)?.visibility = View.INVISIBLE
            }, 2000)
        }
    }

    override fun startImgAnimAfterShotScreen(bitmap: Bitmap, toBottom: Int) {
        animShotView?.let { it ->
            if (mIsShotScreenAnim) return
            mIsShotScreenAnim = true
            it.setOnClickListener(null)
            it.visibility = View.VISIBLE
            findViewById<ImageView>(R.id.screen_shot_anim_img).setImageBitmap(bitmap)

            val screen_shot_anim_text = findViewById<TextView>(R.id.screen_shot_anim_text)
            val textParams = screen_shot_anim_text.layoutParams as LinearLayout.LayoutParams
            textParams.weight = -1.0f
            screen_shot_anim_text.layoutParams = textParams
            screen_shot_anim_text?.visibility = View.INVISIBLE

            val layoutParams1 = it.layoutParams as RelativeLayout.LayoutParams
            layoutParams1.width = RelativeLayout.LayoutParams.MATCH_PARENT
            layoutParams1.height = RelativeLayout.LayoutParams.MATCH_PARENT
            layoutParams1.bottomMargin = resources.getDimension(R.dimen.dp_20).toInt()
            layoutParams1.marginStart = resources.getDimension(R.dimen.dp_20).toInt()
            it.setLayoutParams(layoutParams1)

            it.postDelayed({
                val changeBounds = ChangeBounds()
                changeBounds.duration = 500
                TransitionManager.beginDelayedTransition(it.parent as ViewGroup?, changeBounds)

                layoutParams1.width = resources.getDimension(R.dimen.dp_160).toInt()
                layoutParams1.height = resources.getDimension(R.dimen.dp_120).toInt()
                layoutParams1.bottomMargin = toBottom
                layoutParams1.marginStart = resources.getDimension(R.dimen.dp_80).toInt()
                it.setLayoutParams(layoutParams1)

                it.postDelayed({
                    textParams.weight = 3.0f
                    screen_shot_anim_text.layoutParams = textParams
                    screen_shot_anim_text.visibility = View.VISIBLE
                    it.setOnClickListener { it.visibility = View.GONE }
                    it.postDelayed({
                        mIsShotScreenAnim = false
                        TransitionManager.beginDelayedTransition(it.parent as ViewGroup?, changeBounds)
                        layoutParams1.marginStart = -resources.getDimension(R.dimen.dp_160).toInt()
                        it.setLayoutParams(layoutParams1)
                    }, 2000)
                }, 500)
            }, 500)
        }
    }

    private fun loadSportTrack(bean: DeviceBean, sportAutoTrackListener: AddxLiveOptListener.SportAutoTrackListener?) {
        if (bean.isSupportRocker) {
            if(isSportLoaded){
                return
            }
            isSportTrackLoading = true
            resetSportTrackForView()
            val subscribe: Subscription = ApiClient.getInstance()
                .getUserConfig(SerialNoEntry(bean.serialNumber))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : HttpSubscriber<UserConfigResponse>() {
                    override fun doOnNext(userConfigResponse: UserConfigResponse) {
                        isSportLoaded = true
                        isSportTrackLoading = false
                        if (userConfigResponse.data != null && userConfigResponse.data.motionTrackMode != null) {
                            DeviceConfigHelp.cacheConfig(userConfigResponse.data)
                            isSportMoveMode = userConfigResponse.data.motionTrackMode == 0
                            isSportTrackOpen = DeviceConfigHelp.isSportTrackOpen(
                                userConfigResponse.data
                            )
                            if (!isSportMoveMode || isSportTrackOpen) {
                                if (SharePreManager.getInstance(activityContext)
                                        .shouldShowSportTrackGuide()
                                ) {
                                    SharePreManager.getInstance(activityContext)
                                        .setShouldShowSportTrackGuide(
                                            false
                                        )
                                }
                            }
                            sportAutoTrackListener?.callback(
                                true,
                                isSportTrackOpen
                            )
                        } else {
                            sportAutoTrackListener?.callback(
                                false,
                                isSportTrackOpen
                            )
                        }
                        resetSportTrackForView()
                    }

                    override fun doOnError(e: Throwable) {
                        super.doOnError(e)
                        sportAutoTrackListener?.callback(
                            false,
                            isSportTrackOpen
                        )
                        isSportTrackLoading = false
                        resetSportTrackForView()
                    }
                })
            mSportSubscription.add(subscribe)
        }
    }

    private fun changeSportTrack(
        device: DeviceBean,
        isSelected: Boolean,
        sportAutoTrackListener: AddxLiveOptListener.SportAutoTrackListener?
    ) {
        isSportTrackLoading = true
        resetSportTrackForView()
        val bean = UserConfigBean()
        bean.serialNumber = device.serialNumber
        bean.motionTrack = if (isSelected) 1 else 0
        val subscribe: Subscription = ApiClient.getInstance()
            .updateUserConfig(bean)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : HttpSubscriber<BaseResponse>() {
                override fun doOnNext(userConfigResponse: BaseResponse) {
                    if (userConfigResponse.result == Const.ResponseCode.CODE_OK) {
                        val cacheConfig: UserConfigBean =
                            DeviceConfigHelp.getCacheConfig(device.serialNumber)!!
                        cacheConfig.motionTrack = if (isSelected) 1 else 0
                        isSportTrackOpen = isSelected
                        DeviceConfigHelp.cacheConfig(cacheConfig)
                        isSportTrackLoading = false
                        if (!isSportTrackLoading) {
                            LiveHelper.checkShouldShowGuide(activityContext, device, isSportMoveMode, isSelected)
                        }
                    } else {
                        sportAutoTrackListener?.callback(
                            false,
                            isSportTrackOpen
                        )
                        ToastUtils.showShort(R.string.open_fail_retry)
                        isSportTrackLoading = false
                    }
                    resetSportTrackForView()
                }

                override fun doOnError(e: Throwable) {
                    super.doOnError(e)
                    sportAutoTrackListener?.callback(
                        false,
                        isSportTrackOpen
                    )
                    ToastUtils.showShort(R.string.open_fail_retry)
                    isSportTrackLoading = false
                    resetSportTrackForView()
                }
            })
        mSportSubscription.add(subscribe)
    }

    private fun resetSportTrackForView() {
        if (!mIsSplit) {
            post(Runnable {
                LogUtils.d(
                    TAG,
                    "resetSportTrackForView-------mIvRocker is null:${mIvSportTrackIcon == null}---isSportTrackLoading:${isSportTrackLoading}--isSportTrackOpen:${isSportTrackOpen}"
                )
                setSportTrackViewState(mIvSportTrackIcon)
                if (mTvSportTrackText != null) {
                    if (SUPPORT_SPORT_TRACK) {
                        mTvSportTrackText?.setText(if (isSportMoveMode) R.string.action_tracking else R.string.human_tracking)
                    } else {
                        mTvSportTrackText?.setText(R.string.motion_tracking)
                    }
                }
            })
        }
    }

    private fun setSportTrackViewState(view: ImageView?) {
        if (view == null) return
        view.isSelected = isSportTrackOpen
        if (isSportTrackLoading) {
            view.imageTintList = ColorStateList.valueOf(view.context.resources.getColor(R.color.theme_color))
            view.setImageResource(R.drawable.ic_svg_anim_loading)
            if (view.drawable is Animatable) {
                (view.drawable as Animatable).start()
            }
        } else {
            if (SUPPORT_SPORT_TRACK) {
                view.setImageResource(if (isSportMoveMode) R.mipmap.sport_track_move else R.mipmap.sport_track_person)
            } else {
                view.setImageResource(R.mipmap.sport_track_move)
            }
            if (isSportTrackOpen) {
                view.imageTintList = ColorStateList.valueOf(view.context.resources.getColor(if (isSportTrackEnabled) R.color.theme_color else R.color.theme_color_a30))
            } else {
                view.imageTintList = ColorStateList.valueOf(if (mIvSportTrackIcon === view) -0xb4b1aa else Color.WHITE)
            }
            view.isEnabled = isSportTrackEnabled
        }
    }

    private fun addExtendOptBtnsByDeviceType(bean: DeviceBean) {
        var itemCount = 1
        addOptFuntionBtn(bean, LiveOptMenuConstants.VOICE_TYPE, false)
        LogUtils.d(TAG, "bean.isSupportAlarm()====")
        if (bean.isSupportAlarm) {
            itemCount++
            addOptFuntionBtn(bean, LiveOptMenuConstants.RING_TYPE, false)
        }
        if (bean.isSupportSportAuto && bean.isAdmin) {
            itemCount++
            addOptFuntionBtn(bean, LiveOptMenuConstants.SPORT_AUTO_TYPE, false)
        }
        if (bean.isSupportLight) {
            itemCount++
            if (isLayoutMore(bean, itemCount)) {
                //addOptFuntionBtn(bean, LiveOptMenuConstants.MORE_TYPE, false)
                addOptFuntionBtn(bean, LiveOptMenuConstants.LIGHT_TYPE, true)
            } else {
                addOptFuntionBtn(bean, LiveOptMenuConstants.LIGHT_TYPE, false)
            }
        }
        if (bean.isSupportRocker) {
            itemCount++
            if (isLayoutMore(bean, itemCount)) {
                //addOptFuntionBtn(bean, LiveOptMenuConstants.MORE_TYPE, false)
                addOptFuntionBtn(bean, LiveOptMenuConstants.PRE_LOCATION_TYPE, true)
            } else {
                addOptFuntionBtn(bean, LiveOptMenuConstants.PRE_LOCATION_TYPE, false)
            }
            if(mIVLiveVideoView.isNeedFRocker()){
                itemCount++
                addOptFuntionBtn(bean, LiveOptMenuConstants.F_ROCKER_TYPE, true)
            }
        }
        if(mIVLiveVideoView.isNeedFSpeaker()){
            itemCount++
            if (isLayoutMore(bean, itemCount)) {
                addOptFuntionBtn(bean, LiveOptMenuConstants.F_SPEAKER_TYPE, true)
            } else {
                addOptFuntionBtn(bean, LiveOptMenuConstants.F_SPEAKER_TYPE, false)
            }
        }
        LogUtils.d(TAG, "addExtendOptBtnsByDeviceType----itemCount:$itemCount")
//        LogUtils.d(TAG, "addExtendOptBtnsByDeviceType----adminCount:$adminCount----adminRealCount:$adminRealCount----shareCount:$shareCount----adminCount:$shareRealCount")
        if(itemCount < 4){
            //小于4时，行需要均等分，大于4行的时候自动均等分，兼容三星手机不能显示问题
            more?.children?.forEach {
                (it.layoutParams as GridLayout.LayoutParams).columnSpec =
                    GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
        }
    }

    private fun isLayoutMore(
        bean: DeviceBean,
        itemCount: Int
    ): Boolean {
        return itemCount >= 4
    }

    private fun addOptFuntionBtn(bean: DeviceBean, type: Int, isInMore: Boolean) {
        val view = inflate(activityContext, R.layout.item_live_fun_one_btn, null)
        more?.addView(view)
        val icon: SimpleDraweeView = view.findViewById(R.id.iv_icon)
        val textView = view.findViewById<TextView>(R.id.tv_text)
        LogUtils.d(TAG, "addOptFuntionBtn====type==$type")
        when (type) {
            LiveOptMenuConstants.VOICE_TYPE -> {
                soundBtn = icon
                normalSoundBtn = icon
                icon.setOnClickListener { v: View? ->
                    if(dataSourceBean?.liveAudioToggleOn == null || dataSourceBean?.liveAudioToggleOn == true){
                        if (!mIsFullScreen) {
                            setMuteState(!mute)
                        }
                    }else{
                        if(dataSourceBean?.isAdmin == true){
                            adminShowToOpenReceiveVoiceDialog()
                        }else{
                            showToOpenReceiveVoiceDialog()
                        }
                    }
                }
                updateSoundIcon(mute)
                textView.setText(R.string.sound)
            }
            LiveOptMenuConstants.RING_TYPE -> {
                mIvRing = icon
                LogUtils.d(TAG, "mIvRing---hashcode:${mIvRing.hashCode()}")
                icon.setOnClickListener { v: View? ->
//                    view.setEnabled(false);
                    if (mRinging) {
                        ToastUtils.showShort(R.string.alarm_playing)
                        return@setOnClickListener
                    }
                    ring(object: AddxLiveOptListener.RingListener {
                        override fun callback(ret: Boolean, isOpen: Boolean) {
                            delayRecoverRing()
                            refreshRingUI(ret, isOpen)
                        }

                        override fun ringEnd() {
                            this@DemoLiveVideoView.refreshRingUI(false, false)
                        }
                    })
                }
                icon.setImageResource(R.mipmap.ring_black)
                textView.setText(R.string.alert_buttom)
            }
            LiveOptMenuConstants.SPORT_AUTO_TYPE -> {
                mIvSportTrackIcon = icon
                mTvSportTrackText = textView
                icon.setOnClickListener { v: View? ->
                    view.isEnabled = false
                }
                icon.setImageResource(R.mipmap.sport_track_move)
                textView.setText(R.string.motion_tracking)
            }
            LiveOptMenuConstants.LIGHT_TYPE -> {
                ivLight = icon
                icon.setOnClickListener { v: View? ->
                    light { ret: Boolean, isOpen: Boolean ->
                        refreshLightUI(ret, isOpen)
                    }
                }
                icon.setImageResource(R.mipmap.light_black)
                textView.setText(R.string.white_light)
            }
            LiveOptMenuConstants.PRE_LOCATION_TYPE -> {
                icon.setOnClickListener { v: View? ->
                    LogUtils.d(TAG, "PRE_LOCATION_TYPE-------")
//                    rl_expand_under_player_container.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    LiveHelper.checkShowPreLocationGuideDialog(activityContext)
                    RockerControlManager.getInstance().reportOnPreLocationShow(this@DemoLiveVideoView)
                    deletePrePositionMode = false
                    rl_rocker_and_mic_container?.visibility = GONE
                    if (bean.isAdmin) {
                        setVisible(R.id.complete_delete_root, true)
                    } else {
                        setVisible(R.id.complete_delete_root, false)
                    }
                    tv_complete_delete.text = activityContext.getString(R.string.delete)
                    iv_complete_delete.setImageResource(R.mipmap.compelete_delete)
                    setVisible(R.id.pre_location_opt, true)
                    setVisible(R.id.close_root, true)
                    setVisible(R.id.rv_pre_position, true)
                    setVisible(R.id.layout_pre_location, true)
                }
                icon.setImageResource(R.mipmap.prelocation_black)
                textView.setText(R.string.preset_location)
            }
            LiveOptMenuConstants.MORE_TYPE -> {
                icon.setOnClickListener { v: View? -> }
                icon.setImageResource(R.mipmap.more)
                textView.setText(R.string.more)
            }
            LiveOptMenuConstants.F_SPEAKER_TYPE -> {
                mIVLiveVideoView.setSpeakerOptBtn(activityContext, mAddxVideoContentView, icon, textView, mMicTouchListener)
            }
            LiveOptMenuConstants.F_ROCKER_TYPE -> {
                mIVLiveVideoView.setRockerOptBtn(activityContext, mAddxVideoContentView, icon, textView, object: IVLiveVideoView.OnPositionChangeListener{
                    override fun onStartTouch(mCanRotate: Boolean) {
                        mOnRockerPositionChangeListener?.onRockerStartTouch(mCanRotate)
                    }

                    override fun onEndTouch() {
                        mOnRockerPositionChangeListener?.onRockerEndTouch()
                    }

                    override fun onPositionChange(x: Float, y: Float) {
                        mOnRockerPositionChangeListener?.onRockerPositionChange(x, y)
                    }
                })
            }
            else -> LogUtils.d(TAG, "addOptFuntionBtn====type error")
        }
    }

    fun refreshLightUI(ret: Boolean, isOpen: Boolean){
        ivLight?.setImageResource(R.mipmap.light_black)
        if(isOpen){
            ivLight?.imageTintList = ColorStateList.valueOf(activityContext.resources.getColor(R.color.theme_color))
        }else{
            ivLight?.imageTintList = ColorStateList.valueOf(activityContext.resources.getColor(R.color.black_333))
        }
    }

    fun delayRecoverRing(){
        LogUtils.d(TAG, "delayRecoverRing--- mIvRing is null:${mIvRing == null}---hashcode:${mIvRing.hashCode()}")
        if (mRingRunnable == null) {
            mRingRunnable = Runnable {
                LogUtils.d(TAG, "mRingRunnable--- mIvRing is null:${mIvRing == null}----hashcode:${mIvRing.hashCode()}")
                mRinging = false
                mIvRing?.setBackgroundResource(R.drawable.bg_circle_fill_gray)
                mIvRing?.setImageResource(R.mipmap.ring_black)
                findViewById<FrameLayout>(R.id.layout_tip_ring)?.visibility = View.INVISIBLE
                liveFullScreenMenuWindow?.refreshUI()
            }
        }
        postDelayed(
            mRingRunnable,
            RING_SPAN
        )
    }

    fun refreshRingUI(ret: Boolean, isOpen: Boolean){
        LogUtils.d(TAG, "refreshRingUI---ret:$ret----isOpen:$isOpen--mIvRing is null:${mIvRing == null}")
        post {
            if (ret && isOpen) {
                mIvRing?.setBackgroundResource(R.drawable.bg_corners_60_red)
                val uri = Uri.Builder()
                    .scheme(UriUtil.LOCAL_RESOURCE_SCHEME)
                    .path(java.lang.String.valueOf(R.mipmap.ring_focus_ani))
                    .build()
                val controller: DraweeController =
                    Fresco.newDraweeControllerBuilder()
                        .setUri(uri)
                        .setAutoPlayAnimations(true)
                        .build()
                (mIvRing as SimpleDraweeView)?.controller = controller
            }else{
                mRinging = false
                mIvRing?.setImageResource(R.mipmap.ring_black)
                mIvRing?.setBackgroundResource(R.drawable.bg_circle_fill_gray)
            }

            if (mIsFullScreen) {
                liveFullScreenMenuWindow?.dismiss()
                if (ret && isOpen) {
                    findViewById<FrameLayout>(R.id.layout_tip_ring).visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setVideoBottomAnimotorAndResetSize(needAnim: Boolean, isOpen: Boolean) {
        LogUtils.d(TAG, "setVideoBottomAnimotorAndResetSize=======")
        if (mIsSplit) return
        var llVideoBottomContainer: ViewGroup = normalLayoutViewGroup.findViewById(R.id.rl_expand_under_player_container)
        if (isPlaying()) {
            LogUtils.d(TAG, "setVideoBottomAnimotorAndResetSize=======VISIBLE")
            refreshRingUI(mRinging, mRinging)
            llVideoBottomContainer.visibility = VISIBLE
        } else {
            llVideoBottomContainer.visibility = GONE
        }
        if (needAnim) {
            llVideoBottomContainer.postOnAnimationDelayed({
                if (isPlaying()) {
                    LogUtils.d(TAG, "isPlaying======= = ")
                    val position = IntArray(2)
                    getLocationOnScreen(position)
                }
            }, 600)

            //View 测量完成后，再执行才会有动画
            llVideoBottomContainer.post {
                androidx.transition.TransitionManager.beginDelayedTransition(
                    llVideoBottomContainer,
                    androidx.transition.ChangeBounds()
                )
                setVideoBottomSize(llVideoBottomContainer, !isOpen)
            }
        } else {
            setVideoBottomSize(llVideoBottomContainer, true)
        }
    }

    private fun setVideoBottomSize(root: ViewGroup, toClose: Boolean) {
        val rootParams = root.layoutParams
        if (toClose) {
            rootParams.height = 0
        } else {
            if (root.measuredHeight <= 0) {
                root.measure(0, 0)
            }
            rootParams.height = root.measuredHeight
        }
        root.layoutParams = rootParams
    }

    private fun initPreLocationData(bean: DeviceBean) {
        val rvPrePosition: RecyclerView = findViewById(R.id.rv_pre_position)
        val layoutManager = GridLayoutManager(
            activityContext,
            PreLocationConst.PRE_LOCATION_SPAN_COUNT
        )
        rvPrePosition.layoutManager = layoutManager
        val rockerView: RockerView = findViewById(R.id.view_rocker)
        rockerView.setOnPositionChangeListener(mOnRockerPositionChangeListener)
        val preLocationBeanList = PreLocationAdapter.adapterListerForAddItem(
            bean, RockerControlManager.getInstance().getProLocationData(
                bean.serialNumber
            )
        )
        val preLocationAdapter = PreLocationAdapter(preLocationBeanList) { list: List<PreLocationResponse.DataBean.PreLocationBean?> ->
            if (list.isEmpty()) {
                if (!bean.isAdmin) {
                    rv_pre_position?.visibility = INVISIBLE
                    complete_delete_root?.visibility = INVISIBLE
                    no_pre_position_tip?.visibility = VISIBLE
                } else {
                    rv_pre_position?.visibility = VISIBLE
                    complete_delete_root?.visibility = VISIBLE
                    no_pre_position_tip?.visibility = INVISIBLE
                }
            } else {
                no_pre_position_tip?.visibility = INVISIBLE
                rv_pre_position?.visibility = VISIBLE
                if (!bean.isAdmin) {
                    complete_delete_root?.visibility = INVISIBLE
                }
            }
        }
        preLocationAdapter.setPlayer(this@DemoLiveVideoView)
        preLocationAdapter.setBean(bean)
        RockerControlManager.getInstance().onLoadPreLocationData(bean.serialNumber)
        RockerControlManager.getInstance().addListener(preLocationAdapter)
        if (bean.isAdmin) {
            preLocationAdapter.onItemLongClickListener = BaseQuickAdapter.OnItemLongClickListener { adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
                if (!deletePrePositionMode) {
                    deletePrePositionMode = true
                    tv_complete_delete.text = activityContext.getString(R.string.done)
                    iv_complete_delete.setImageResource(R.mipmap.complete_ok)
                    preLocationAdapter.preDeletePositionMode = deletePrePositionMode
                    preLocationAdapter.notifyDataSetChanged()
                }
                true
            }
        }
        preLocationAdapter.setBgMode(PreLocationAdapter.BgMode.BG_TEXT)
        val space = SizeUtils.dp2px(PreLocationConst.PRE_LOCATION_SPACE_DP.toFloat())
        val itemDecorationCount = rvPrePosition.itemDecorationCount
        if (itemDecorationCount == 0) {
            val itemDecoration = GridSpacingItemDecoration(
                PreLocationConst.PRE_LOCATION_SPAN_COUNT,
                space,
                false
            )
            rvPrePosition.addItemDecoration(itemDecoration)
        } else {
            for (decorationCount in itemDecorationCount downTo 2) {
                rvPrePosition.removeItemDecorationAt(decorationCount - 1)
            }
        }
        rvPrePosition.adapter = preLocationAdapter

        close_root.setOnClickListener { v: View? ->
            layout_pre_location?.visibility = View.GONE
            rl_rocker_and_mic_container?.visibility = VISIBLE
            closePreLocationAndShowRockerMic(bean)
            if (deletePrePositionMode) {
                deletePrePositionMode = false
                tv_complete_delete.text = activityContext.getString(R.string.delete)
                iv_complete_delete.setImageResource(R.mipmap.compelete_delete)
                preLocationAdapter.preDeletePositionMode = deletePrePositionMode
                preLocationAdapter.notifyDataSetChanged()
            }
        }
        complete_delete_root.setOnClickListener { v: View? ->
            deletePrePositionMode = !deletePrePositionMode
            tv_complete_delete.text =
                activityContext.getString(if (deletePrePositionMode) R.string.done else R.string.delete)
            iv_complete_delete.setImageResource(if (deletePrePositionMode) R.mipmap.complete_ok else R.mipmap.compelete_delete)
            preLocationAdapter.preDeletePositionMode = deletePrePositionMode
            preLocationAdapter.notifyDataSetChanged()
        }
    }

    private fun closePreLocationAndShowRockerMic(bean: DeviceBean) {
        setVisible(R.id.rl_rocker_and_mic_container, true)
//        setVisible(R.id.voice_icon, true)
//        if (bean.isSupportRocker) {
//            setVisible(R.id.view_rocker, true)
//        } else {
//            setVisible(R.id.view_rocker, false)
//        }

        setVisible(R.id.layout_pre_location, false)
//        setVisible(R.id.pre_location_opt, false)
//        setVisible(R.id.complete_delete_root, false)
//        setVisible(R.id.close_root, false)
//        setVisible(R.id.rv_pre_position, false)
//        setVisible(R.id.no_pre_position_tip, false)
    }

    private fun setVisible(viewId: Int, visible: Boolean){
        val view: View? = findViewById(viewId)
        view?.visibility = if (visible) VISIBLE else INVISIBLE
    }

    private fun initVideoBottomExpend(){
        if (!mIsSplit) {
            val layoutParams = voice_icon.layoutParams as RelativeLayout.LayoutParams
            if (dataSourceBean?.isSupportRocker != true) {
                view_rocker?.visibility = GONE
                layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_END)
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
                layoutParams.bottomMargin = context.resources.getDimension(R.dimen.dp_30).toInt()
            } else {
                view_rocker?.visibility = VISIBLE
                layoutParams.removeRule(RelativeLayout.CENTER_IN_PARENT)
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END)
                layoutParams.marginEnd = context.resources.getDimension(R.dimen.dp_10).toInt()
                initPreLocationData(dataSourceBean!!)
            }
            mVoiceTip = findViewById(R.id.voice_tip)
            mMicFramelayout = findViewById(R.id.fr_mic)
            mMicText = findViewById(R.id.tv_mic_text)

            more?.removeAllViews()
            addExtendOptBtnsByDeviceType(dataSourceBean!!)
        }
    }

    override fun onPlayStateChanged(currentState: Int, oldState: Int) {
        LogUtils.d(TAG, "onPlayStateChanged-------currentState:$currentState---oldState:$oldState")
        if(!mIsFullScreen){
            mVideoCallBack?.onPlayStateChanged(currentState, oldState)
            if (currentState == CURRENT_STATE_PLAYING) {
                if (dataSourceBean!!.isSupportRocker) {
                    mSportSubscription.clear()
                    val cacheConfig: UserConfigBean = DeviceConfigHelp.getCacheConfig(dataSourceBean!!.serialNumber)
                    isSportMoveMode = DeviceConfigHelp.isSportTrackMove(cacheConfig)
                    isSportTrackOpen = DeviceConfigHelp.isSportTrackOpen(cacheConfig)
                    isSportTrackEnabled = true
                    isSportTrackLoading = true
                    if (mIvSportTrackIcon != null) mIvSportTrackIcon?.removeCallbacks(mRockerDisableRunnable)
                }
                rl_expand_under_player_container?.visibility = View.VISIBLE
                setVideoBottomAnimotorAndResetSize(true, true)
                loadSportTrack(dataSourceBean!!, null)
                if (!mIsSplit) {
                    closePreLocationAndShowRockerMic(dataSourceBean!!)
                }
            } else {
                if (!mIsSplit) {
                    rl_expand_under_player_container?.visibility = View.GONE
                    setVideoBottomAnimotorAndResetSize(true, false)
                }
            }
        }
    }

    override fun isSupportGuide() {
        mVideoCallBack?.isSupportGuide()
    }
    override fun onFullScreenStateChange(fullScreen: Boolean) {
        super.onFullScreenStateChange(fullScreen)
        if (fullScreen && dataSourceBean!!.isSupportRocker()) {
            //第一次点击全屏的时候，全屏的状态未刷新，要刷新一下。
            resetSportTrackForView()
        }
    }

    override fun isRinging(): Boolean{
        return mRinging
    }
    override fun getWhiteLightOn():Boolean{
        return whiteLightOn
    }

    override  fun hideNav(){
        CommonUtil.hideNavKey(activityContext)
        CommonUtil.hideNavKey(liveFullScreenMenuWindow!!.contentView)
    }

    override fun getBrokeVoiceType(): Int {
        return 1
    }

    override fun getSportTrackOpen(): Boolean {
        return false
    }

    override fun onDebug(p0: MutableMap<String, String>?) {
        
    }
}