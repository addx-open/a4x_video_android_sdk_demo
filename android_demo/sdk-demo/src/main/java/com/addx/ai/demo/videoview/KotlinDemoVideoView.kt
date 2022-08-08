package com.addx.ai.demo.videoview

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.SystemClock
import android.text.TextUtils
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.addx.common.Const
import com.addx.common.steps.PageStep
import com.addx.common.utils.*
import com.ai.addx.model.*
import com.ai.addx.model.request.SerialNoEntry
import com.ai.addx.model.response.BaseResponse
import com.ai.addx.model.response.PreLocationResponse
import com.ai.addx.model.response.UserConfigResponse
import com.ai.addxbase.*
import com.ai.addxbase.addxmonitor.AddxMonitor
import com.ai.addxbase.addxmonitor.FileLogUpload
import com.ai.addxbase.helper.SharePreManager
import com.ai.addxbase.permission.PermissionPageStep
import com.ai.addxbase.theme.IVLiveVideoView
import com.ai.addxbase.util.ToastUtils
import com.ai.addxbase.view.BatteryView
import com.ai.addxbase.view.GridSpacingItemDecoration
import com.ai.addxbase.view.dialog.CommonCornerDialog
import com.ai.addxnet.ApiClient
import com.ai.addxnet.HttpSubscriber
import com.ai.addxvideo.PreLocationConst
import com.ai.addxvideo.addxvideoplay.*
import com.ai.addxvideo.addxvideoplay.addxplayer.*
import com.ai.addxvideo.addxvideoplay.addxplayer.webrtcplayer.*
import com.ai.addxvideo.addxvideoplay.view.LiveRatioDialog
import com.ai.addxvideo.addxvideoplay.view.RockerView
import com.ai.addxvideo.addxvideoplay.view.VisualizedView
import com.airbnb.lottie.LottieAnimationView
import com.alibaba.fastjson.JSON
import com.blankj.rxbus.RxBus
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.facebook.common.util.UriUtil
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.drawee.view.SimpleDraweeView
//import kotlinx.android.synthetic.main.item_device_default.view.*
import kotlinx.android.synthetic.main.layout_player_full.view.*
import kotlinx.android.synthetic.main.layout_player_full.view.back
import kotlinx.android.synthetic.main.layout_player_full.view.iv_mic
import kotlinx.android.synthetic.main.layout_player_full.view.iv_record
import kotlinx.android.synthetic.main.layout_player_full.view.iv_screen_shot
import kotlinx.android.synthetic.main.layout_player_full.view.rocker
import kotlinx.android.synthetic.main.layout_player_full.view.zoom_view
import kotlinx.android.synthetic.main.layout_player_normal.view.*
import kotlinx.android.synthetic.main.layout_player_normal.view.camera_type_icon
import kotlinx.android.synthetic.main.layout_player_normal.view.close_root
import kotlinx.android.synthetic.main.layout_player_normal.view.complete_delete_root
import kotlinx.android.synthetic.main.layout_player_normal.view.item_battery
import kotlinx.android.synthetic.main.layout_player_normal.view.item_device_name
import kotlinx.android.synthetic.main.layout_player_normal.view.item_replay
import kotlinx.android.synthetic.main.layout_player_normal.view.item_setting
import kotlinx.android.synthetic.main.layout_player_normal.view.item_share
import kotlinx.android.synthetic.main.layout_player_normal.view.iv_complete_delete
import kotlinx.android.synthetic.main.layout_player_normal.view.layout_pre_location
import kotlinx.android.synthetic.main.layout_player_normal.view.more
import kotlinx.android.synthetic.main.layout_player_normal.view.no_pre_position_tip
import kotlinx.android.synthetic.main.layout_player_normal.view.rl_rocker_and_mic_container
import kotlinx.android.synthetic.main.layout_player_normal.view.rv_pre_position
import kotlinx.android.synthetic.main.layout_player_normal.view.tv_complete_delete
import kotlinx.android.synthetic.main.layout_player_normal.view.view_rocker
import kotlinx.android.synthetic.main.layout_player_normal.view.voice_icon
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import com.addx.ai.demo.R
import com.ai.addx.model.request.ReporLiveCommonEntry
import com.ai.addx.model.request.ReporLiveInterruptEntry
import com.ai.addxvideo.track.other.TrackManager

open class  KotlinDemoVideoView: DemoBaseVideoView, RockerView.OnPositionChangeListener, AddxLiveOptListener{

    private val  mIVLiveVideoView by lazy{
        IVLiveVideoView.create()
    }
    //    private var ivRockerAuto: ImageView? = null
    open var showPirToast: Boolean = false
    private var whiteLightSetting = false
    internal open var whiteLightOn: Boolean = false
    private val liveRationDialog by lazy {
        LiveRatioDialog(context)
    }
    private var blackBg: View? = null
    private var visualizedView: VisualizedView? = null
    private var tvRatio: TextView? = null
    private var liveFlagLayout: View? = null
    private var fullScreenLayout: View? = null
    internal open  var ivLight: ImageView? = null
    private var ivLightLoading: View? = null
    private var tvPirToast: TextView? = null
    private var ivPosition: ImageView? = null
    private var mIvFullScreenMore: ImageView? = null
    private var mLivingIcon: LottieAnimationView? = null
    private val MIC_TIP_FADEOUT_SPAN: Long = 3000
    private val MIC_SHOW_SHAPE_SPAN: Long = 300
    private var mVoiceTip: LinearLayout? = null
    private var frameVisualizedView: FrameLayout? = null
    private val RING_SPAN: Long = 5000
//    var liveFullScreenMenuWindow: LiveFullScreenMenuPopupWindow? = null
    private var liveFullScreenRatioPopupWindow: LiveFullScreenRatioPopupWindow? = null
    private var mMicFramelayout: FrameLayout? = null
    private var mMicText: TextView? = null
    private var mRefreshThumbImg: ConcurrentHashMap<String, Long> = ConcurrentHashMap()
    internal var mRinging = false
    private var mIsShotScreenAnim: Boolean = false
    private var mMicTouchListener: OnTouchListener? = null
    private var mRockerListener: RockerView.OnPositionChangeListener? = null

    private var isSportTrackLoading = false
    private var isSportMoveMode = false
    private var isSportTrackOpen = false
    private val mSubscription = CompositeSubscription()
    private var isSportTrackEnabled = false
    private var mIvRocker: ImageView? = null
    private var mTvRocker: TextView? = null
    private var deletePrePositionMode = false
    private var mRingRunnable: Runnable? = null
    private var mIvRing:ImageView? = null
    private var m2KRatio: Ratio = Ratio.P_2K_2560
    private var mHDRatio: Ratio = Ratio.P1080
    private var mSDRatio: Ratio = Ratio.P720

    private var mVoiceDownTime: Long = 0
    private var mTouchUp: Boolean = false
    private var mMicTipFadeOutRunnable = {
        if(System.currentTimeMillis() - mVoiceDownTime >= MIC_TIP_FADEOUT_SPAN){
            mVoiceTip?.visibility = View.INVISIBLE
        }
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
    override fun parseRatio(){
        mDeviceRatioList.clear()
        mDeviceRatioList.add(Ratio.R_AUTO)
        if (dataSourceBean?.deviceSupport?.deviceSupportResolutionV1 != null && dataSourceBean?.deviceSupport?.deviceSupportResolutionV1!!.size > 0) {
            dataSourceBean?.deviceSupport?.deviceSupportResolutionV1?.forEach {item ->
                if(!TextUtils.isEmpty(item.resolutionLevel) && !TextUtils.isEmpty(item.resolution)){
                    when(item.resolutionLevel.toLowerCase()){
                        "2k" -> {
                            m2KRatio = parseRatio(item.resolution, Ratio.P_2K_2560)
                            mDeviceRatioList.add(Ratio.R_2K)
                        }
                        "hd" -> {
                            mHDRatio = parseRatio(item.resolution, Ratio.P1080)
                            mDeviceRatioList.add(Ratio.R_HD)
                        }
                        "sd" -> {
                            mSDRatio = parseRatio(item.resolution, Ratio.P720)
                            mDeviceRatioList.add(Ratio.R_SD)
                        }
                    }
                }
            }
        }else{
            mHDRatio = if (dataSourceBean!!.deviceModel.canRotate) {
                if (mShouldUseDeviceUploadRatio) {
                    val deviceSupportResolution = dataSourceBean!!.deviceSupport.deviceSupportResolution
                    val ratio1 = parseRatio(deviceSupportResolution[0], Ratio.P360)
                    val ratio2 = parseRatio(deviceSupportResolution[deviceSupportResolution.size - 1], Ratio.P1080)
                    if (ratio1.wight > ratio2.wight) ratio1 else ratio2
                } else {
                    Ratio.P1080
                }
            } else {
                Ratio.P1080
            }
            mDeviceRatioList.add(Ratio.R_HD)

            mSDRatio = if (dataSourceBean!!.deviceModel.canRotate) {
                if (mShouldUseDeviceUploadRatio) {
                    val deviceSupportResolution = dataSourceBean!!.deviceSupport.deviceSupportResolution
                    val ratio1 = parseRatio(deviceSupportResolution[0], Ratio.P360)
                    val ratio2 = parseRatio(deviceSupportResolution[deviceSupportResolution.size - 1], Ratio.P1080)
                    if (ratio1.wight < ratio2.wight) ratio1 else ratio2
                } else {
                    Ratio.P360
                }
            } else {
                Ratio.P720
            }
            mDeviceRatioList.add(Ratio.R_SD)
        }
    }

    private val mShouldUseDeviceUploadRatio by lazy {
        dataSourceBean!!.deviceSupport != null && dataSourceBean!!.deviceSupport.deviceSupportResolution != null && dataSourceBean!!.deviceSupport.deviceSupportResolution.size >= 2
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun getDeclaredAttrs(context: Context, attrs: AttributeSet?) {
        super.getDeclaredAttrs(context, attrs)
        if (attrs != null) {
            val tya: TypedArray = context.obtainStyledAttributes(attrs, com.ai.addxvideo.R.styleable.LiveWebRTCPlayer)
            mIsSplit = tya.getBoolean(com.ai.addxvideo.R.styleable.LiveWebRTCPlayer_isSplit, false)

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
//            if (event.action == MotionEvent.ACTION_DOWN) {
//                tv_voice_tip.setText(com.ai.addxvideo.R.string.release_stop)
//            } else {
//                tv_voice_tip.setText(com.ai.addxvideo.R.string.hold_speak)
//            }
            true
        }
        mRockerListener = object: RockerView.OnPositionChangeListener {
            override fun onStartTouch(canRotate: Boolean) {
                if (!canRotate) ToastUtils.showShort(com.ai.addxvideo.R.string.motion_sport_auto_is_open)
                RockerControlManager.getInstance().onRockerStartTouch(this@KotlinDemoVideoView)
            }

            override fun onEndTouch() {
                RockerControlManager.getInstance().release()
            }

            override fun onPositionChange(x: Float, y: Float) {
                RockerControlManager.getInstance().onPositionChange(
                    x,
                    y,
                    bean.serialNumber,
                    this@KotlinDemoVideoView
                )
            }
        }
        super.init(context, bean, iAddxViewCallback)
        setRockerState(dataSourceBean, false, false)
//        setOptListener()
        initVideoBottomExpend()
        preApplyConnectWhenB(0)
    }

    override fun fullLayoutId(): Int = com.ai.addxvideo.R.layout.layout_webrtc_player_full

    override fun normalLayoutId(): Int = if (!mIsSplit) com.ai.addxvideo.R.layout.layout_webrtc_player_normal else com.ai.addxvideo.R.layout.layout_webrtc_player_split

    override fun errorLayoutId(): Int {
        return when {
            mIsFullScreen -> com.ai.addxvideo.R.layout.live_plager_full_error_page
            mIsSplit -> com.ai.addxvideo.R.layout.live_plager_no_full_error_multi_page1
            else -> com.ai.addxvideo.R.layout.live_plager_no_full_error_default_page
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        var ret = micTouch(v, event)
        if(ret != 0){
            return ret == 1
        }
        return super.onTouch(v, event)
    }
    override fun micTouch(v: View?, event: MotionEvent?): Int{
        when (v?.id) {
            com.ai.addxvideo.R.id.iv_mic -> {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        LogUtils.d(TAG, "mVoiceTip=11===="+(SystemClock.elapsedRealtime() - mVoiceDownTime))
                        mTouchUp = false
                        mVoiceDownTime = System.currentTimeMillis()
                        return if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            requestPermission()
                            -1
                        } else {
                            SystemUtil.Vibrate(activityContext, 100)
                            removeCallbacks(mFadeOut)
                            iAddxPlayer?.muteVoice(false, true)
                            iAddxPlayer?.setMicEnable(true)
//                            if (iAddxPlayer is AddxVideoWebRtcPlayer) {
//                                if(dataSourceBean?.deviceModel?.isB0!!){
//                                    iAddxPlayer?.setVolume(4f)
//                                    LogUtils.d(TAG,"micTouch setvolume=4")
//                                }else{
//                                    iAddxPlayer?.setVolume(1.0f)
//                                    LogUtils.d(TAG,"micTouch setvolume=0.5")
//                                }
//                            }
                            LogUtils.d(TAG, "hide  mShowing  startBtn?.visibility = View.INVISIBLE")
                            startBtn?.visibility = View.INVISIBLE
                            updateSoundIcon(false)
                            reportVoiceTalkEvent()
                            mVoiceTip?.visibility = View.INVISIBLE
                            postDelayed(mMicVisualizedViewShowRunnable, MIC_SHOW_SHAPE_SPAN)
                            mMicFramelayout?.setBackgroundResource(com.ai.addxvideo.R.drawable.bg_circle_fill_gray_mic_focus)
                            mMicText?.setText(com.ai.addxvideo.R.string.release_stop)
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
//                            post(mFadeOut)
//                            if((mIsFullScreen && currentState == CURRENT_STATE_NORMAL) || !mIsFullScreen){
//                                startBtn?.visibility = View.VISIBLE
//                            }
                            iAddxPlayer?.setMicEnable(false)
//                            if (iAddxPlayer is AddxVideoWebRtcPlayer) {
//                                if(dataSourceBean?.deviceModel?.isB0!!){
//                                    LogUtils.d(TAG,"micTouch setvolume=8")
//                                    iAddxPlayer?.setVolume(4f)
//                                }else{
//                                    LogUtils.d(TAG,"micTouch setvolume=1.0")
//                                    iAddxPlayer?.setVolume(3f)
//                                }
//                            }
                            mute = false
                            iAddxPlayer?.muteVoice(mute, true)
                            updateSoundIcon(mute)
                            frameVisualizedView?.visibility = View.INVISIBLE
                            mMicFramelayout?.setBackgroundResource(com.ai.addxvideo.R.drawable.bg_circle_fill_gray)
                            mMicText?.setText(com.ai.addxvideo.R.string.hold_speak)
                        }
                    }
                }


            }
        }
        return 0
    }
    private fun requestPermission() {
        PermissionPageStep(activityContext)
            .setRequestedPermissions(arrayOf(Manifest.permission.RECORD_AUDIO))
            .setTitleMessage(com.ai.addxvideo.R.string.microphone_permission)
            .setSettingsMessage(com.ai.addxvideo.R.string.microphone_permission_tips)
            .setRationaleMessage(com.ai.addxvideo.R.string.microphone_permission_tips)
            .setGuideDesc(com.ai.addxvideo.R.string.microphone)
            .setGuideIcon(com.ai.addxvideo.R.mipmap.permission_mic)
            .execute { _: PageStep?, result: PageStep.PageStepResult ->
                LogUtils.df(TAG, "permission state %s", result.name)
                hideNavKey()
            }
    }


    override fun onClick(v: View?) {
        super.onClick(v)
        when (v?.id) {
//            R.id.iv_ring -> {
//                showAlarmDialog()
//            }
//            R.id.iv_light -> {
//                setWhiteLight()
//            }
//            R.id.iv_setting -> {
//                JumpUtils.toDeviceSettingForResult(activityContext, dataSourceBean)
//                reportToSettingEvent()
//            }
            com.ai.addxvideo.R.id.tv_ratio -> {
                if (!isRecording && mAddxVideoContentView.iv_record.isEnabled) {
                    showRationChoosePopupWindow()
                } else {
                    ToastUtils.showShort(com.ai.addxvideo.R.string.cannot_switch)
                }
            }
//            R.id.iv_position -> {
//                showPositionWindow()
//            }

//            com.ai.addxvideo.R.id.fullscreen_more -> {
//                showMoreWindow()
//            }
            com.ai.addxvideo.R.id.back -> {
                if(!mIsSplit){
                    var llRockerController: ViewGroup = normalLayoutViewGroup.findViewById(com.ai.addxvideo.R.id.rl_rocker_controller)
                    if(isPlaying()){
                        LogUtils.d(
                            TAG,
                            "llRockerController?.visibility = VISIBLE--llRockerController is null:${llRockerController == null}"
                        )
                        llRockerController?.visibility = FrameLayout.VISIBLE
                    }else{
                        llRockerController?.visibility = FrameLayout.GONE
                    }
                    toggleRootViewForDeviceB(llRockerController, false)
                }
            }
        }
    }

//    fun showMoreWindow() {
//        if(liveFullScreenMenuWindow != null && liveFullScreenMenuWindow?.isShowing == true){
//            liveFullScreenMenuWindow?.dismiss()
//        }
//        liveFullScreenMenuWindow = LiveFullScreenMenuPopupWindow(this, dataSourceBean!!, context, this)
//        liveFullScreenMenuWindow?.showAtLocation(activityContext.getWindow().getDecorView(), Gravity.END, 0, 0)
//        hide()
//    }

//    fun showPositionWindow() {
//        RockerPositionPopupWindow(this, dataSourceBean!!, context).showAtLocation(this, Gravity.RIGHT, 0, 0)
//        RockerControlManager.getInstance().onPreLocationShow(this)
//        hide()
//    }

    private fun setWhiteLight(listener: AddxLiveOptListener.Listener) {
        if (whiteLightSetting) {
            return
        }
//        ivLight?.visibility = View.INVISIBLE
//        ivLightLoading?.visibility = View.VISIBLE
        whiteLightSetting = true
        iAddxPlayer?.sendWightLight(!whiteLightOn, object : PlayerCallBack {
            override fun error(errorCode: Int?, errorMsg: String?, throwable: Throwable?) {
                LogUtils.d(TAG, "===setWhiteLight=error==")
                post {
                    listener.callback(false, false)
                    ivLight?.setImageResource(com.ai.addxvideo.R.mipmap.light_black)
                    ivLight?.setImageTintList(
                        ColorStateList.valueOf(
                            activityContext.resources.getColor(
                                com.ai.addxvideo.R.color.black_333
                            )
                        )
                    )
                }
            }

            override fun completed(data: Any?) {
                LogUtils.d(TAG, "====setWhiteLight=completed=data:${data.toString()}")
                post {
                    ivLight?.setImageResource(com.ai.addxvideo.R.mipmap.light_black)
                    if (data.toString().equals("0")) {
                        listener.callback(true, true)
                        ivLight?.setImageTintList(
                            ColorStateList.valueOf(
                                activityContext.resources.getColor(
                                    if (whiteLightOn) com.ai.addxvideo.R.color.theme_color else com.ai.addxvideo.R.color.black_333
                                )
                            )
                        )
                    } else {
                        listener.callback(false, false)
                        ivLight?.setImageTintList(
                            ColorStateList.valueOf(
                                activityContext.resources.getColor(
                                    com.ai.addxvideo.R.color.black_333
                                )
                            )
                        )
                    }
                }
            }
        })
    }

    fun showAlarmDialog(ringListener: AddxLiveOptListener.RingListener?) {
        if (activityContext != null) {
            val alarmDialog = CommonCornerDialog(activityContext)
            alarmDialog.dismissAfterRightClick=false
            alarmDialog.setRightClickListener(OnClickListener {
                mRinging = true
                postDelayed(Runnable {
                    mRinging = false
                }, 5000)
                iAddxPlayer?.triggerAlarm(object : PlayerCallBack {
                    override fun completed(data: Any?) {
                        reportAlarmEvent(mIsFullScreen, true, null)
                        ringListener?.callback(true, true)
                        refreshRingIconUI(true, true)
                    }

                    override fun error(errorCode: Int?, errorMsg: String?, throwable: Throwable?) {
                        reportAlarmEvent(mIsFullScreen, false, errorMsg)
                        ringListener?.callback(false, false)
                        LogUtils.d(TAG,"mRinging  $mRinging")
//                        ToastUtils.showShort(R.string.network_error)
                    }


                })
                alarmDialog.dismiss()
                hideNavKey()
            })
            alarmDialog.setTitle(com.ai.addxvideo.R.string.do_alarm_tips)
            alarmDialog.setTitleNoBolder()
            alarmDialog.setLeftText(com.ai.addxvideo.R.string.cancel)
            alarmDialog.setRightText(com.ai.addxvideo.R.string.alarm_on)
            alarmDialog.setTitleLeftIcon(com.ai.addxvideo.R.mipmap.ring_focus_min)
            alarmDialog.setRightTextColor(Color.parseColor("#FF6A6A"))
            alarmDialog.show()
        }
    }

    override fun changeUIToError(opt: Int?) {
        super.changeUIToError(opt)
        val shortTips = mIsSplit && !mIsFullScreen
        when (opt) {
            PlayerErrorState.ERROR_DEVICE_UNACTIVATED -> setErrorInfo(if (!shortTips) com.ai.addxvideo.R.string.camera_not_activated else com.ai.addxvideo.R.string.camera_not_activated_short, com.ai.addxvideo.R.mipmap.live_error_unactivated)
            PlayerErrorState.ERROR_DEVICE_SLEEP -> {
                setErrorInfo(
                    com.ai.addxvideo.R.string.camera_sleep, com.ai.addxvideo.R.mipmap.ic_sleep_main_live, true, com.ai.addxvideo.R.string.camera_wake_up, dataSourceBean?.isAdmin
                    ?: false, null, false)
                getSleepMsg().let {
                    if (it.isNotEmpty() && dataSourceBean?.isAdmin!!) {
                        tvErrorTips?.text = it
                    } else {
                        if(mIsSplit){
                            tvErrorTips?.text = it
                        }else{
                            tvErrorTips?.text = it.plus("\n\n" + context.resources.getString(com.ai.addxvideo.R.string.admin_wakeup_camera))
                        }
                    }
                }
            }
            PlayerErrorState.ERROR_DEVICE_AUTH_LIMITATION,
            PlayerErrorState.ERROR_DEVICE_NO_ACCESS -> {
                setErrorInfo(if (!shortTips) com.ai.addxvideo.R.string.error_2002 else com.ai.addxvideo.R.string.error_2002_short, com.ai.addxvideo.R.mipmap.live_error__no_access, underlineErrorBtnText = com.ai.addxvideo.R.string.refresh)
            }
            PlayerErrorState.ERROR_DEVICE_SHUTDOWN_LOW_POWER -> {
                if(mIsSplit){
                    setErrorInfo(com.ai.addxvideo.R.string.low_power, com.ai.addxvideo.R.mipmap.lowpowershutdown)
                }else{
                    setErrorInfo(com.ai.addxvideo.R.string.low_power, com.ai.addxvideo.R.mipmap.lowpowershutdown)
                }
            }
            PlayerErrorState.ERROR_DEVICE_SHUTDOWN_PRESS_KEY -> {
                if(mIsSplit){
                    setErrorInfo(com.ai.addxvideo.R.string.turned_off, com.ai.addxvideo.R.mipmap.shutdown)
                }else{
                    setErrorInfo(com.ai.addxvideo.R.string.turned_off, com.ai.addxvideo.R.mipmap.shutdown)
                }
            }
            PlayerErrorState.ERROR_DEVICE_OFFLINE -> {
                if(mIsSplit){
                    setErrorInfo(com.ai.addxvideo.R.string.camera_poor_network_short, com.ai.addxvideo.R.mipmap.live_offline)
                }else{
                    setErrorInfo(com.ai.addxvideo.R.string.camera_poor_network, com.ai.addxvideo.R.mipmap.live_offline)
                }
            }
            PlayerErrorState.ERROR_PHONE_NO_INTERNET -> {
                setErrorInfo(if (shortTips) com.ai.addxvideo.R.string.phone_weak_network_short else com.ai.addxvideo.R.string.failed_to_get_information_and_try)
            }
        }

        if (mIsFullScreen) {
        } else {
            fullScreenBtn?.visibility = View.VISIBLE
//            soundBtn?.visibility = View.INVISIBLE
        }
        errorLayout?.visibility = View.VISIBLE
        loadingLayout?.visibility = View.INVISIBLE
        startBtn?.visibility = View.INVISIBLE
        liveFlagLayout?.visibility = View.INVISIBLE
    }

    // current tate pause /inited / uninited / for living
    override fun changeUIToIdle() {
        super.changeUIToIdle()
        if (mIsFullScreen) {
            blackBg?.visibility = View.INVISIBLE
            fullScreenLayout?.visibility = View.INVISIBLE
        } else {
            fullScreenBtn?.visibility = View.VISIBLE
//            soundBtn?.visibility = View.INVISIBLE
        }
        liveFlagLayout?.visibility = View.INVISIBLE
        updatePausePlayIcon()
    }

    override fun changeUIToConnecting() {
        super.changeUIToConnecting()
        if (mIsFullScreen) {
            fullScreenLayout?.visibility = View.INVISIBLE
            blackBg?.visibility = View.INVISIBLE
        } else {
//            fullScreenBtn?.visibility = View.INVISIBLE
//            soundBtn?.visibility = View.INVISIBLE
        }
        loadingLayout?.visibility = View.VISIBLE
        LogUtils.d(TAG, "hide  mShowing  startBtn?.visibility = View.INVISIBLE")
        startBtn?.visibility = View.INVISIBLE
        liveFlagLayout?.visibility = View.INVISIBLE
    }

    override fun changeUIToPlaying() {
        super.changeUIToPlaying()
        if (mIsFullScreen) {
            fullScreenLayout?.visibility = View.INVISIBLE
            blackBg?.visibility = View.INVISIBLE
            if (showPirToast) {
                showPirToast = false
                tvPirToast?.visibility = View.VISIBLE
                postDelayed({
                    fullLayoutViewGroup?.findViewById<View>(com.ai.addxvideo.R.id.pir_toast)?.visibility = View.INVISIBLE
                }, 2000)
            }
        } else {
//            soundBtn?.visibility = View.INVISIBLE
            fullScreenBtn?.visibility = View.VISIBLE
        }
        liveFlagLayout?.visibility = View.VISIBLE
        updatePausePlayIcon()
    }

    private fun updatePausePlayIcon() {
        if (currentState != AddxBaseVideoView.CURRENT_STATE_PLAYING) {
            startBtn?.setImageResource(com.ai.addxvideo.R.mipmap.live_no_full_play_default)
        } else {
            startBtn?.setImageResource(com.ai.addxvideo.R.mipmap.live_no_full_pause_default)
        }
    }

    override fun show(timeout: Long) {
        super.show(timeout)
        LogUtils.d(TAG, "show  mShowing  $mShowing")
        if (!mShowing) {
            mShowing = true
            if((mIsFullScreen && currentState == AddxBaseVideoView.CURRENT_STATE_NORMAL) || !mIsFullScreen){
                startBtn?.visibility = View.VISIBLE
                removeCallbacks(startBtnAction)
                postDelayed(startBtnAction, START_SHOW_SPAN)
            }
            if (mIsFullScreen) {
                fullScreenLayout?.visibility = View.VISIBLE
                blackBg?.visibility = View.VISIBLE
            } else {
//                soundBtn?.visibility = View.VISIBLE
//                fullScreenBtn?.visibility = View.VISIBLE
            }
        }
        updatePausePlayIcon()
        if (timeout != 0L) {
            removeCallbacks(mFadeOut)
//            postDelayed(mFadeOut, timeout)
        }
    }

    override fun hide() {
        LogUtils.d(TAG, "hide  mShowing  $mShowing")
        if (mShowing) {
            mShowing = false
            LogUtils.d(TAG, "hide  mShowing  startBtn?.visibility = View.INVISIBLE")
            startBtn?.visibility = View.INVISIBLE
            if (mIsFullScreen) {
                fullScreenLayout?.visibility = View.INVISIBLE
                blackBg?.visibility = View.INVISIBLE
            } else {
//                soundBtn?.visibility = View.INVISIBLE
//                fullScreenBtn?.visibility = View.INVISIBLE
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
            mVideoCallBack?.toLibrary(dataSourceBean!!)
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
            item_share.setImageResource(com.ai.addxvideo.R.mipmap.home_item_admin)
        } else {
            item_share.setImageResource(com.ai.addxvideo.R.mipmap.home_item_shared)
        }
        Glide.with(activityContext)
            .load(dataSourceBean?.smallIcon)
            .placeholder(com.ai.addxvideo.R.mipmap.ic_camera_place_holder_small)
            .into(camera_type_icon)
        val batteryView: BatteryView = item_battery
        if (dataSourceBean!!.getDeviceModel()?.canStandby) {
            batteryView.alpha = if (dataSourceBean?.online == 1) 1.0f else 0.4f
            LogUtils.d(TAG, JSON.toJSONString(dataSourceBean))
            batteryView.setCharging(
                dataSourceBean!!.quantityCharge,
                dataSourceBean!!.getIsCharging(),
                dataSourceBean!!.batteryLevel
            )
            batteryView.visibility = VISIBLE
        } else {
            batteryView.visibility = GONE
        }
        val ignore = dataSourceBean!!.firmwareStatus shr 1 and 1
        val newVersion = dataSourceBean!!.firmwareStatus and 1
        setVisible(com.ai.addxvideo.R.id.iv_update_point, /*ignore == 0 && */newVersion == 1 && isAdmin)
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
            fullScreenLayout = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.full_screen_icons)
            tvRatio = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.tv_ratio)
            tvRatio?.setOnClickListener(this)
            blackBg = findViewById(com.ai.addxvideo.R.id.bg_black)
            mAddxVideoContentView.rocker.visibility = if (dataSourceBean?.isSupportRocker!!) View.VISIBLE else View.GONE
            mAddxVideoContentView.rocker.setOnPositionChangeListener(this)
            mIvFullScreenMore = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.fullscreen_more)
            mIvFullScreenMore?.setOnClickListener(this)
            soundBtn = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.iv_sound)
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
        frameVisualizedView = findViewById(com.ai.addxvideo.R.id.frame_visualized_voice)
        visualizedView = findViewById(com.ai.addxvideo.R.id.visualized_voice)
        mVoiceTip = findViewById(com.ai.addxvideo.R.id.voice_tip)
        liveFlagLayout = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.ll_living_flag)
        tvPirToast = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.pir_toast)
        mLivingIcon = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.living_icon)
//        contentView.iv_power?.visibility = if (dataSourceBean!!.isSupportBattery) View.VISIBLE else View.INVISIBLE
//        contentView.iv_power?.setColorWhite();
        mAddxVideoContentView.findViewById<View>(com.ai.addxvideo.R.id.rocker)?.visibility = if (dataSourceBean!!.isSupportRocker) View.VISIBLE else View.INVISIBLE

//        contentView.findViewById<View>(R.id.iv_light)?.visibility = if (dataSourceBean!!.isSupportLight) View.VISIBLE else View.INVISIBLE
        val wifiLevel = WifiManager.calculateSignalLevel(dataSourceBean!!.signalStrength, 4)
        mAddxVideoContentView.findViewById<ImageView>(com.ai.addxvideo.R.id.iv_wifi)?.setImageLevel(wifiLevel)
//        contentView.findViewById<BatteryView>(R.id.iv_power)?.setCharging(dataSourceBean!!.getIsCharging(), dataSourceBean!!.batteryLevel)
        mVideoRatio = VideoSharePreManager.getInstance(context).getLiveRatio(dataSourceBean!!)
        updateRatioTextView(mVideoRatio)
        if((mIVLiveVideoView.isNeedFRocker() || mIVLiveVideoView.isNeedFSpeaker()) && !mIsSplit){
//            layout_pre_location.layoutParams.height = 0
            layout_pre_location.visibility = GONE
            rl_rocker_and_mic_container.visibility = GONE
        }
    }

    private fun updateRatioTextView(ratio: Ratio) {
        tvRatio?.setText(when (ratio) {
            mSDRatio -> com.ai.addxvideo.R.string.ratio_720p
            mHDRatio -> com.ai.addxvideo.R.string.video_hb
            m2KRatio -> com.ai.addxvideo.R.string.twok
            else -> com.ai.addxvideo.R.string.auto
        })
    }

    //=====================RockerView.OnPositionChangeListener============
    override fun onStartTouch(mCanRotate: Boolean) {
        if (!mCanRotate) {
            ToastUtils.showShort(com.ai.addxvideo.R.string.motion_sport_auto_is_open)
            RockerControlManager.getInstance().onRockerStartTouch(this)
            return
        }
        LogUtils.d(TAG, "hide  mShowing  startBtn?.visibility = View.INVISIBLE")
        startBtn?.visibility = View.INVISIBLE
        removeCallbacks(mFadeOut)
    }

//    override fun onWebRTCCommandSend(command: DataChannelCommand?) {
//        if (Looper.myLooper() != Looper.getMainLooper()) {
//            post {
//                mVideoCallBack?.onWebRTCCommandSend(command)
//            }
//        } else {
//            mVideoCallBack?.onWebRTCCommandSend(command)
//        }
//    }

    override fun onEndTouch() {
        if((mIsFullScreen && currentState == AddxBaseVideoView.CURRENT_STATE_NORMAL) || !mIsFullScreen) {
            startBtn?.visibility = View.VISIBLE
            removeCallbacks(startBtnAction)
        }
        show(DEFAULT_SHOW_TIME)
    }

    override fun onPositionChange(x: Float, y: Float) {
        RockerControlManager.getInstance().onPositionChange(x, y, dataSourceBean!!.serialNumber, this)
    }
    //=====================RockerView.OnPositionChangeListener==end==========

    fun showRationChoosePopupWindow() {
        liveFullScreenRatioPopupWindow = LiveFullScreenRatioPopupWindow(mVideoRatio, mDeviceRatioList, context, object: RadioGroup.OnCheckedChangeListener{
            override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
                when (checkedId) {
                    com.ai.addxvideo.R.id.rb_ratio_auto -> changeRatio(Ratio.AUTO)
                    com.ai.addxvideo.R.id.rb_ratio_720 -> changeRatio(mSDRatio)
                    com.ai.addxvideo.R.id.rb_ratio_1080 -> changeRatio(mHDRatio)
                    com.ai.addxvideo.R.id.rb_ratio_2560 -> changeRatio(m2KRatio)
                }

                var toRatio = when (checkedId) {
                    com.ai.addxvideo.R.id.rb_ratio_auto -> Ratio.AUTO
                    com.ai.addxvideo.R.id.rb_ratio_720 -> Ratio.P720
                    com.ai.addxvideo.R.id.rb_ratio_1080 -> Ratio.P1080
                    com.ai.addxvideo.R.id.rb_ratio_2560 -> Ratio.P_2K_2560
                    else -> Ratio.AUTO
                }
                liveFullScreenRatioPopupWindow?.dismiss()
                liveRationDialog.dismiss()
                hideNavKey()
                reportChangeRationEvent(mVideoRatio, toRatio)
            }


        })
        liveFullScreenRatioPopupWindow?.showAtLocation(this, Gravity.END, 0, 0)
        when (mVideoRatio) {
            Ratio.AUTO -> liveFullScreenRatioPopupWindow?.setCheck(0)
            mSDRatio -> liveFullScreenRatioPopupWindow?.setCheck(1)
            mHDRatio -> liveFullScreenRatioPopupWindow?.setCheck(2)
            m2KRatio -> liveFullScreenRatioPopupWindow?.setCheck(3)
        }
        hide()
    }

    override fun onPrepared(player: IVideoPlayer) {
        super.onPrepared(player)
        if (isChanggingRatio) {
            if (iAddxPlayer is AddxVideoWebRtcPlayer) {
                iAddxPlayer?.changeVideoRatio(mVideoRatio)
            }

            isChanggingRatio = false
        }
//        mLivingIcon?.setAnimation("live_ani.json")
        mLivingIcon?.loop(true)
        mLivingIcon?.playAnimation()
    }

    override fun onError(player: IVideoPlayer, what: Int, extra: Int) {
        super.onError(player, what, extra)
//        post{
//            mUploadlog?.visibility = if (mIsNeedUploadFailLog) View.VISIBLE else View.GONE
        if (mIsNeedUploadFailLog){
            AddxMonitor.getInstance(A4xContext.getInstance().getmContext()).uploadLastDayLog(object: FileLogUpload.Callback{
                override fun invoke(fileName: String?, ret: Boolean) {
                }
            })
        }
        mIsNeedUploadFailLog = false
//        }
    }

    override fun onMicFrame(data: ByteArray?) {
        super.onMicFrame(data)
        if (data != null) {
//            LogUtils.d(TAG, "visualizedView->${visualizedView.hashCode()},visibility:${visualizedView?.visibility}")
            visualizedView?.updateVolumeByVolumeData(data)
        }
        mVideoCallBack?.onMicFrame(data)
    }

    override fun onWhiteLightResult(success: Boolean) {
        LogUtils.d(TAG, "showAlarmDialog===onWhiteLightResult===whiteLightOn:$whiteLightOn")
        if (success) {
            whiteLightOn = !whiteLightOn
        }
        post {
            whiteLightSetting = false
//            ivLight.setImageResource(if (whiteLightOn) R.mipmap.ic_light_open else R.mipmap.light_black)
            ivLight?.setImageResource(com.ai.addxvideo.R.mipmap.light_black)
            ivLight?.setImageTintList(ColorStateList.valueOf(if (whiteLightOn) getResources().getColor(
                com.ai.addxvideo.R.color.theme_color) else getResources().getColor(com.ai.addxvideo.R.color.black_333)))
            ivLight?.visibility = View.VISIBLE
            ivLightLoading?.visibility = View.GONE
        }
    }

    override fun onWebRTCCommandSend(commandType: DataChannelCommand?) {
        post{
            if (commandType != null && mIvRocker != null && (commandType.action == CommandType.MOVETOCOORDINATE.type || commandType.action == CommandType.ROCKERROTATE.type)
                && mIvRocker!!.isSelected && !isSportTrackLoading) {
                mIvRocker!!.removeCallbacks(mRockerDisableRunnable)
                mIvRocker!!.postDelayed(mRockerDisableRunnable, 5000)
                if (isSportTrackOpen && isSportTrackEnabled) {
                    isSportTrackEnabled = false
                    resetSportTrackForView()
                }
            }
        }
        mVideoCallBack?.onWebRTCCommandSend(commandType)
    }

    override fun onTriggerAlarm(isOpen: Boolean) {

    }

    @Synchronized
    private fun changeRatio(ratio: Ratio) {
        mVideoRatio = ratio
        isChanggingRatio = true
        VideoSharePreManager.getInstance(context).setLiveRatio(dataSourceBean!!, mVideoRatio)
        VideoSharePreManager.getInstance(context).setLiveRatioHd(dataSourceBean, mVideoRatio == mHDRatio)
        updateRatioTextView(ratio)
        iAddxPlayer?.changeVideoRatio(mVideoRatio)
    }

    override fun resetRatioInfoForG0() {
        dataSourceBean?.let {
            if (it.deviceModel.isG0) {
                mVideoRatio = Ratio.P720
                VideoSharePreManager.getInstance(context).setLiveRatio(it, Ratio.P720)
                VideoSharePreManager.getInstance(context).setLiveRatioHd(it, mVideoRatio == mHDRatio)
                updateRatioTextView(Ratio.P720)
            }
        }
    }

    fun setUiSplit(isSplit: Boolean) {
        this.mIsSplit = isSplit
    }

    override fun stopPlay() {
        if (whiteLightOn) {
            iAddxPlayer?.sendWightLight(false, object: PlayerCallBack{
                override fun completed(data: Any?) {
                }

                override fun error(errorCode: Int?, errorMsg: String?, throwable: Throwable?) {
                }
            })
        }
        super.stopPlay()
//        liveFullScreenMenuWindow?.dismiss()
        resetExtendOptBtn()
    }

    fun resetExtendOptBtn(){
        if(mIsFullScreen){
            return
        }
        setVisible(com.ai.addxvideo.R.id.voice_icon, true)
        setVisible(com.ai.addxvideo.R.id.view_rocker, true)
        rl_rocker_and_mic_container.visibility = VISIBLE
        layout_pre_location.visibility = INVISIBLE
    }

    override fun onClickErrorTips(tip: TextView?) {
        LogUtils.d(TAG, "onClickErrorTips-------" + dataSourceBean!!.serialNumber)
        super.onClickErrorTips(tip)
        if (mIsSplit && !mIsFullScreen) {
            onClickUnderlineErrorButton(tip)
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

    private fun reportChangeRationEvent(from: Ratio, to: Ratio) {
        val segmentation = TrackManager.get().getSegmentation(TrackManager.EventPair.LIVE_RESOLUTION_CHANGE)
        segmentation["live_player_way"] = if (mIsFullScreen) "fullscreen" else "halfscreen"
        segmentation["from_resolution"] = from.value
        segmentation["to_resolution"] = to.value
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

//        LogUtils.d(TAG, "AddxBaseVideoView------updateThumbImageSource--refreshThumbImg---mServerThumbTime:${mServerThumbTime}-----mLocalThumbTime:${mLocalThumbTime}")
//        LogUtils.d(TAG,"toRequestAndRefreshThumbImg=====localThumbTime:${localThumbTime}===serverThumbTime:${serverThumbTime}")
        if (dataSourceBean?.thumbImgTime != null && dataSourceBean?.thumbImgTime!! > mServerThumbTime && dataSourceBean?.thumbImgTime!! > mLocalThumbTime) {
            if (mRefreshThumbImg.get(dataSourceBean?.serialNumber) == dataSourceBean?.thumbImgTime!!) {
                return
            }
            mRefreshThumbImg.put(dataSourceBean?.serialNumber!!, dataSourceBean?.thumbImgTime!!)

            var imgPath = DownloadUtil.getThumbImgDir(context) + MD5Util.md5(dataSourceBean?.serialNumber)+".jpg"
            FileUtils.createOrExistsDir(DownloadUtil.getThumbImgDir(context))
            DownloadUtil.downloadImg(dataSourceBean?.getThumbImgUrl(), imgPath, object : DownloadUtil.DownloadListener {
                override fun success(url: String?, path: String?) {
                    LogUtils.d(TAG, "toRequestAndRefreshThumbImg===success==code:${this@KotlinDemoVideoView.hashCode()}==sn:${dataSourceBean?.serialNumber}==path:$path==url:${dataSourceBean?.getThumbImgUrl()}")
                    VideoSharePreManager.getInstance(context).setThumbImgServerLastFresh(dataSourceBean?.serialNumber, dataSourceBean?.thumbImgTime, dataSourceBean?.thumbImgUrl)
                    mServerThumbTime = dataSourceBean?.thumbImgTime!!
                    setThumbPath(imgPath)
                }

                override fun fail(url: String?) {
                    LogUtils.d(TAG, "toRequestAndRefreshThumbImg=====fail==code:${this@KotlinDemoVideoView.hashCode()}==:sn:${dataSourceBean?.serialNumber}===url:$url")
                }
            })
        }
    }

    private fun setThumbPath(path: String){
        var localBitmap = BitmapUtils.getBitmap(path)
        if(localBitmap != null){
            post{
                LogUtils.e(TAG, "AddxBaseVideoView------setThumbPath----sn:${dataSourceBean?.serialNumber}")
                var layers: Array<Drawable?> = arrayOfNulls(2)
                layers.set(0, BitmapDrawable(mBitmap))
                layers.set(1, BitmapDrawable(localBitmap))
                var transitionDrawable = TransitionDrawable (layers)
                thumbImage?.setImageDrawable(transitionDrawable)
                transitionDrawable.startTransition(500)
            }
        }
    }

    override fun ring(ringListener: AddxLiveOptListener.RingListener) {
        showAlarmDialog(ringListener)
    }

    override fun light(listener: AddxLiveOptListener.Listener) {
        setWhiteLight(listener)
    }

    override fun sportAuto(isInit: Boolean, isSelected: Boolean, sportAutoTrackListener: AddxLiveOptListener.SportAutoTrackListener) {
        if(mIsFullScreen){
//            mAddxLiveOptListener?.sportAuto(isInit, isSelected, sportAutoTrackListener)
            //全屏
            if (!mIsSplit) {
                mIvRocker!!.setImageResource(com.ai.addxvideo.R.mipmap.rocker_black)
                mTvRocker!!.setText(com.ai.addxvideo.R.string.motion_tracking)
            }
            if (isInit) {
                loadSportTrack(dataSourceBean!!, sportAutoTrackListener)
            } else {
                if (!mIsSplit) {
                    changeSportTrack(
                        dataSourceBean!!,
                        !mIvRocker!!.isSelected,
                        sportAutoTrackListener
                    )
                } else {
                    changeSportTrack(dataSourceBean!!, isSelected, sportAutoTrackListener)
                }
            }
        }
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
        AddxFunJump.deviceSettings(activityContext, dataSourceBean!!)
        mVideoCallBack?.onToSetting(dataSourceBean!!)
    }

    override fun startFullScreen(isReverse: Boolean) {
        super.startFullScreen(isReverse)
        mLivingIcon?.loop(true)
        mLivingIcon?.playAnimation()
        if(!dataSourceBean?.isSupportRocker!! && !mIVLiveVideoView.isNeedFRocker()){
            var constraintLayout = mAddxVideoContentView.iv_mic.parent as ConstraintLayout
            var constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)
            constraintSet.connect(mAddxVideoContentView.iv_mic.getId(),ConstraintSet.TOP,constraintLayout.getId(),ConstraintSet.TOP,0)
            constraintSet.applyTo(constraintLayout);
        }
    }

    override fun startBitmapAnim(bitmap: Bitmap, toBottom: Int) {
        animShotView?.let { it ->
            if (mIsShotScreenAnim) return
            mIsShotScreenAnim = true
            it.setOnClickListener(null)
            it.visibility = View.VISIBLE
            findViewById<ImageView>(com.ai.addxvideo.R.id.screen_shot_anim_img).setImageBitmap(bitmap)

            var screen_shot_anim_text = findViewById<TextView>(com.ai.addxvideo.R.id.screen_shot_anim_text)
            var textParams = screen_shot_anim_text.layoutParams as LinearLayout.LayoutParams
            textParams.weight = -1.0f
            screen_shot_anim_text.layoutParams = textParams
            screen_shot_anim_text.visibility = View.INVISIBLE

            var layoutParams1 = it.layoutParams as RelativeLayout.LayoutParams
            layoutParams1.width = RelativeLayout.LayoutParams.MATCH_PARENT
            layoutParams1.height = RelativeLayout.LayoutParams.MATCH_PARENT
            layoutParams1.bottomMargin = resources.getDimension(com.ai.addxvideo.R.dimen.dp_20).toInt()
            layoutParams1.marginStart = resources.getDimension(com.ai.addxvideo.R.dimen.dp_20).toInt()
            it.setLayoutParams(layoutParams1)

            it.postDelayed({
                var changeBounds = ChangeBounds()
                changeBounds.duration = 500
                TransitionManager.beginDelayedTransition(it.parent as ViewGroup?, changeBounds)

                layoutParams1.width = resources.getDimension(com.ai.addxvideo.R.dimen.dp_160).toInt()
                layoutParams1.height = resources.getDimension(com.ai.addxvideo.R.dimen.dp_120).toInt()
                layoutParams1.bottomMargin = toBottom
                layoutParams1.marginStart = resources.getDimension(com.ai.addxvideo.R.dimen.dp_80).toInt()
                it.setLayoutParams(layoutParams1)

                it.postDelayed({
                    textParams.weight = 3.0f
                    screen_shot_anim_text.layoutParams = textParams
                    screen_shot_anim_text.visibility = View.VISIBLE
                    it.setOnClickListener { it.visibility = View.GONE }
                    it.postDelayed({
                        mIsShotScreenAnim = false
//                    it.visibility = View.INVISIBLE
                        TransitionManager.beginDelayedTransition(it.parent as ViewGroup?, changeBounds)
                        layoutParams1.marginStart = -resources.getDimension(com.ai.addxvideo.R.dimen.dp_160).toInt()
                        it.setLayoutParams(layoutParams1)
                    }, 2000)
                }, 500)
            }, 500)
        }
    }

    private fun loadSportTrack(bean: DeviceBean, sportAutoTrackListener: AddxLiveOptListener.SportAutoTrackListener?) {
        if (bean.isSupportRocker) {
            isSportTrackLoading = true
            resetSportTrackForView()
            val subscribe: Subscription = ApiClient.getInstance()
                .getUserConfig(SerialNoEntry(bean.serialNumber))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : HttpSubscriber<UserConfigResponse>() {
                    override fun doOnNext(userConfigResponse: UserConfigResponse) {
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
                                isSportTrackOpen,
                                isSportMoveMode,
                                false
                            )
                        } else {
                            sportAutoTrackListener?.callback(
                                false,
                                isSportTrackOpen,
                                isSportMoveMode,
                                false
                            )
                        }
                        resetSportTrackForView()
                    }

                    override fun doOnError(e: Throwable) {
                        super.doOnError(e)
                        sportAutoTrackListener?.callback(
                            false,
                            isSportTrackOpen,
                            isSportMoveMode,
                            false
                        )
                        isSportTrackLoading = false
                        resetSportTrackForView()
                    }
                })
            mSubscription.add(subscribe)
        }
    }

    private fun resetSportMoveMode(bean: DeviceBean) {
        if (bean.isSupportRocker) {
            mSubscription.clear()
            val cacheConfig: UserConfigBean = DeviceConfigHelp.getCacheConfig(bean.serialNumber)
            isSportMoveMode = DeviceConfigHelp.isSportTrackMove(cacheConfig)
            isSportTrackOpen = DeviceConfigHelp.isSportTrackOpen(cacheConfig)
            isSportTrackEnabled = true
            isSportTrackLoading = true
            if (mIvRocker != null) mIvRocker?.removeCallbacks(mRockerDisableRunnable)
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
                        sportAutoTrackListener?.callback(
                            true, isSportTrackOpen, DeviceConfigHelp.isSportTrackMove(
                                cacheConfig
                            ), true
                        )
                        RockerControlManager.getInstance().setSportTrackState(
                            this@KotlinDemoVideoView,
                            isSportTrackOpen,
                            true,
                            "",
                            isSportMoveMode
                        )
                        isSportTrackLoading = false
                        checkShouldShowGuide(activityContext, device, isSelected)
                    } else {
                        sportAutoTrackListener?.callback(
                            false,
                            isSportTrackOpen,
                            isSportMoveMode,
                            false
                        )
                        ToastUtils.showShort(com.ai.addxvideo.R.string.open_fail_retry)
                        RockerControlManager.getInstance().setSportTrackState(
                            this@KotlinDemoVideoView,
                            isSportTrackOpen,
                            false,
                            "",
                            isSportMoveMode
                        )
                        isSportTrackLoading = false
                    }
                    resetSportTrackForView()
                }

                override fun doOnError(e: Throwable) {
                    super.doOnError(e)
                    sportAutoTrackListener?.callback(
                        false,
                        isSportTrackOpen,
                        isSportMoveMode,
                        false
                    )
                    ToastUtils.showShort(com.ai.addxvideo.R.string.open_fail_retry)
                    isSportTrackLoading = false
                    resetSportTrackForView()
                    RockerControlManager.getInstance().setSportTrackState(
                        this@KotlinDemoVideoView,
                        isSportTrackOpen,
                        false,
                        "",
                        isSportMoveMode
                    )
                }
            })
        mSubscription.add(subscribe)
    }

    private fun resetSportTrackForView() {
        if (!mIsSplit) {
            post(Runnable {
                LogUtils.d(
                    TAG,
                    "resetSportTrackForView-------mIvRocker is null:${mIvRocker == null}---isSportTrackLoading:${isSportTrackLoading}--isSportTrackOpen:${isSportTrackOpen}"
                )
                setRockerViewState(mIvRocker)
                if (mTvRocker != null) {
                    if (SUPPORT_SPORT_TRACK) {
                        mTvRocker?.setText(if (isSportMoveMode) com.ai.addxvideo.R.string.action_tracking else com.ai.addxvideo.R.string.human_tracking)
                    } else {
                        mTvRocker?.setText(com.ai.addxvideo.R.string.motion_tracking)
                    }
                }
            })
        }
    }

    private fun setRockerViewState(view: ImageView?) {
        if (view == null) return
        view.isSelected = isSportTrackOpen
        if (isSportTrackLoading) {
            view.imageTintList = ColorStateList.valueOf(view.context.resources.getColor(com.ai.addxvideo.R.color.theme_color))
            view.setImageResource(com.ai.addxvideo.R.drawable.ic_svg_anim_loading)
            if (view.drawable is Animatable) {
                (view.drawable as Animatable).start()
            }
        } else {
            if (SUPPORT_SPORT_TRACK) {
                view.setImageResource(if (isSportMoveMode) com.ai.addxvideo.R.mipmap.sport_track_move else com.ai.addxvideo.R.mipmap.sport_track_person)
            } else {
                view.setImageResource(com.ai.addxvideo.R.mipmap.sport_track_move)
            }
            if (isSportTrackOpen) {
                view.imageTintList = ColorStateList.valueOf(view.context.resources.getColor(if (isSportTrackEnabled) com.ai.addxvideo.R.color.theme_color else com.ai.addxvideo.R.color.theme_color_a30))
            } else {
                view.imageTintList = ColorStateList.valueOf(if (mIvRocker === view) -0xb4b1aa else Color.WHITE)
            }
            view.isEnabled = isSportTrackEnabled
        }
    }

    private fun setRockerState(bean: DeviceBean?, isPlaying: Boolean, needAnim: Boolean) {
        if (mIsSplit) return
        var llRockerController: ViewGroup = normalLayoutViewGroup.findViewById(com.ai.addxvideo.R.id.rl_rocker_controller)
        LogUtils.d(
            TAG,
            "llRockerController?.visibility = VISIBLE--setRockerState--needAnim：${needAnim}---isPlaying：$isPlaying---llRockerController is null:${llRockerController == null}"
        )
        if (isPlaying) {
            llRockerController.visibility = VISIBLE
        } else {
            llRockerController.visibility = GONE
        }
        if (needAnim) {
            llRockerController.postOnAnimationDelayed({
                if (isPlaying) {
                    LogUtils.d(TAG, "isPlaying======= = ")
                    val position = IntArray(2)
                    getLocationOnScreen(position)
                }
            }, 600)
            startAnimation(llRockerController, !isPlaying)
        } else {
            toggleRootViewForDeviceB(llRockerController, true)
        }
    }

    private fun startAnimation(root: ViewGroup, toClose: Boolean) {
        //View 测量完成后，再执行才会有动画
        root.post {
            LogUtils.d(TAG, "toClose = $toClose")
            androidx.transition.TransitionManager.beginDelayedTransition(
                root,
                androidx.transition.ChangeBounds()
            )
            toggleRootViewForDeviceB(root, toClose)
        }
    }

    private fun toggleRootViewForDeviceB(root: ViewGroup, toClose: Boolean) {
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

    private fun addExtendOptBtnsByDeviceType(bean: DeviceBean) {
        var adminCount = 1
        var adminRealCount = 1
        var shareCount = 1
        var shareRealCount = 1
        if (bean.isSupportAlarm) {
            adminCount++
            shareCount++
        }
        if (bean.isSupportSportAuto) {
            adminCount++
        }
        if (bean.isSupportLight) {
            adminCount++
            shareCount++
        }
        if (bean.isSupportRocker) {
            adminCount++
            shareCount++
        }
        addOptFuntionBtn(bean, LiveOptMenuConstants.VOICE_TYPE, false)
        LogUtils.d(TAG, "bean.isSupportAlarm()====")
        if (bean.isSupportAlarm) {
            LogUtils.d(TAG, "bean.isSupportAlarm()====" + bean.isSupportAlarm)
            adminRealCount++
            shareRealCount++
            addOptFuntionBtn(bean, LiveOptMenuConstants.RING_TYPE, false)
        }
        if (bean.isSupportSportAuto && bean.isAdmin) {
            adminRealCount++
            addOptFuntionBtn(bean, LiveOptMenuConstants.SPORT_AUTO_TYPE, false)
        }
        if (bean.isSupportLight) {
            adminRealCount++
            shareRealCount++
            if (isLayoutMore(bean, adminCount, adminRealCount, shareCount, shareRealCount)) {
                //addOptFuntionBtn(bean, LiveOptMenuConstants.MORE_TYPE, false)
                addOptFuntionBtn(bean, LiveOptMenuConstants.LIGHT_TYPE, true)
            } else {
                addOptFuntionBtn(bean, LiveOptMenuConstants.LIGHT_TYPE, false)
            }
        }
        if (bean.isSupportRocker) {
            adminRealCount++
            shareRealCount++
            if (isLayoutMore(bean, adminCount, adminRealCount, shareCount, shareRealCount)) {
                //addOptFuntionBtn(bean, LiveOptMenuConstants.MORE_TYPE, false)
                addOptFuntionBtn(bean, LiveOptMenuConstants.PRE_LOCATION_TYPE, true)
            } else {
                addOptFuntionBtn(bean, LiveOptMenuConstants.PRE_LOCATION_TYPE, false)
            }
            if(mIVLiveVideoView.isNeedFRocker()){
                addOptFuntionBtn(bean, LiveOptMenuConstants.F_ROCKER_TYPE, true)
            }
        }
        if(mIVLiveVideoView.isNeedFSpeaker()){
            addOptFuntionBtn(bean, LiveOptMenuConstants.F_SPEAKER_TYPE, true)
        }
    }

    private fun isLayoutMore(
        bean: DeviceBean,
        adminCount: Int,
        adminRealCount: Int,
        shareCount: Int,
        shareRealCount: Int
    ): Boolean {
        return adminCount > 4 && bean.isAdmin && adminRealCount == 4 || shareCount > 4 && !bean.isAdmin && shareRealCount == 4
    }

    private fun addOptFuntionBtn(bean: DeviceBean, type: Int, isInMore: Boolean) {
        val view = inflate(activityContext, com.ai.addxvideo.R.layout.item_live_fun_one_btn, null)
//        val layoutParams = LinearLayout.LayoutParams(
//            LinearLayout.LayoutParams.WRAP_CONTENT,
//            LinearLayout.LayoutParams.WRAP_CONTENT
//        )
//        layoutParams.weight = 1f
        if (isInMore) {
            more?.addView(view)
        } else {
            more?.addView(view)
        }
        (view.layoutParams as GridLayout.LayoutParams).columnSpec =
            GridLayout.spec(GridLayout.UNDEFINED, 1f)
        val icon: SimpleDraweeView = view.findViewById(com.ai.addxvideo.R.id.iv_icon)
        val textView = view.findViewById<TextView>(com.ai.addxvideo.R.id.tv_text)
        LogUtils.d(TAG, "addOptFuntionBtn====type==$type")
        when (type) {
            LiveOptMenuConstants.VOICE_TYPE -> {
                soundBtn = icon
                normalSoundBtn = icon
                icon.setOnClickListener { v: View? ->
                    if (!mIsFullScreen) {
                        setMuteState(!mute)
                    }
//                    normalSoundBtn?.setImageResource(if (mute) R.mipmap.voice_black_notalk else R.mipmap.voice_black_talk)
                    if (mute) {
                        icon.setImageResource(com.ai.addxvideo.R.mipmap.voice_black_notalk)
                    } else {
                        icon.setImageResource(com.ai.addxvideo.R.mipmap.voice_black_talk)
                    }
                }
                if (mute) {
                    icon.setImageResource(com.ai.addxvideo.R.mipmap.voice_black_notalk)
                } else {
                    icon.setImageResource(com.ai.addxvideo.R.mipmap.voice_black_talk)
                }
                textView.setText(com.ai.addxvideo.R.string.sound)
            }
            LiveOptMenuConstants.RING_TYPE -> {
                mIvRing = icon
                icon.setOnClickListener { v: View? ->
//                    view.setEnabled(false);
                    if (mRinging) {
                        ToastUtils.showShort(com.ai.addxvideo.R.string.alarm_playing)
                        return@setOnClickListener
                    }
                    ring(object: AddxLiveOptListener.RingListener {
                        override fun callback(ret: Boolean, isOpen: Boolean) {
                            refreshRingIconUI(ret, isOpen)
                        }

                        override fun ringEnd() {
                            this@KotlinDemoVideoView.ringEndRefreshUI()
                        }
                    })
                }
                icon.setImageResource(com.ai.addxvideo.R.mipmap.ring_black)
                textView.setText(com.ai.addxvideo.R.string.alert_buttom)
            }
            LiveOptMenuConstants.SPORT_AUTO_TYPE -> {
                mIvRocker = icon
                mTvRocker = textView
                icon.setOnClickListener { v: View? ->
                    view.isEnabled = false
                    changeSportTrack(
                        bean,
                        !(mIvRocker?.isSelected()!!),
                        AddxLiveOptListener.SportAutoTrackListener { ret: Boolean?, isOpen: Boolean?, isSportMoveMode: Boolean?, shouldCheck: Boolean? ->
                            view.isEnabled = true
                        })
                }
                icon.setImageResource(com.ai.addxvideo.R.mipmap.sport_track_move)
                textView.setText(com.ai.addxvideo.R.string.motion_tracking)
            }
            LiveOptMenuConstants.LIGHT_TYPE -> {
                ivLight = icon
                icon.setOnClickListener { v: View? ->
//                    view.setEnabled(false);
//                    UiUtil.loadingOptAni(icon);
                    light({ ret: Boolean, isOpen: Boolean ->
                        refreshLightUI(ret, isOpen)
                    })
                }
                icon.setImageResource(com.ai.addxvideo.R.mipmap.light_black)
                textView.setText(com.ai.addxvideo.R.string.white_light)
            }
            LiveOptMenuConstants.PRE_LOCATION_TYPE -> {
                icon.setOnClickListener { v: View? ->
                    LogUtils.d(TAG, "PRE_LOCATION_TYPE-------")
                    mAddxVideoContentView.findViewById<RelativeLayout>(com.ai.addxvideo.R.id.rl_rocker_controller).layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    LiveHelper.checkShowPreLocationGuideDialog(activityContext)
                    RockerControlManager.getInstance().onPreLocationShow(this@KotlinDemoVideoView)
                    deletePrePositionMode = false
                    setVisible(com.ai.addxvideo.R.id.voice_icon, false)
//                    setVisible(R.id.item_top, false)
                    setVisible(com.ai.addxvideo.R.id.view_rocker, false)
                    if (bean.isAdmin) {
                        setVisible(com.ai.addxvideo.R.id.complete_delete_root, true)
                    } else {
                        setVisible(com.ai.addxvideo.R.id.complete_delete_root, false)
                    }
                    tv_complete_delete.text = activityContext.getString(com.ai.addxvideo.R.string.delete)
//                    helper.setText(R.id.tv_complete_delete, R.string.delete)
                    iv_complete_delete.setImageResource(com.ai.addxvideo.R.mipmap.compelete_delete)
                    setVisible(com.ai.addxvideo.R.id.close_root, true)
                    setVisible(com.ai.addxvideo.R.id.rv_pre_position, true)
                    setVisible(com.ai.addxvideo.R.id.layout_pre_location, true)
                }
                icon.setImageResource(com.ai.addxvideo.R.mipmap.prelocation_black)
                textView.setText(com.ai.addxvideo.R.string.preset_location)
            }
            LiveOptMenuConstants.MORE_TYPE -> {
                icon.setOnClickListener { v: View? -> }
                icon.setImageResource(com.ai.addxvideo.R.mipmap.more)
                textView.setText(com.ai.addxvideo.R.string.more)
            }
            LiveOptMenuConstants.F_SPEAKER_TYPE -> {
                mIVLiveVideoView.setSpeakerOptBtn(activityContext, mAddxVideoContentView, icon, textView, mMicTouchListener)
            }
            LiveOptMenuConstants.F_ROCKER_TYPE -> {
                mIVLiveVideoView.setRockerOptBtn(activityContext, mAddxVideoContentView, icon, textView, object: IVLiveVideoView.OnPositionChangeListener{
                    override fun onStartTouch(mCanRotate: Boolean) {
                        mRockerListener?.onStartTouch(mCanRotate)
                    }

                    override fun onEndTouch() {
                        mRockerListener?.onEndTouch()
                    }

                    override fun onPositionChange(x: Float, y: Float) {
                        mRockerListener?.onPositionChange(x, y)
                    }
                })
            }
            else -> LogUtils.d(TAG, "addOptFuntionBtn====type error")
        }
    }

    fun refreshLightUI(ret: Boolean, isOpen: Boolean){
//        ivLight?.setImageResource(R.mipmap.light_black)
//        ivLight?.imageTintList = ColorStateList.valueOf(if (isOpen) activityContext.getResources().getColor(R.color.theme_color) else activityContext.getResources().getColor(R.color.black_333))
        ivLight?.setImageResource(com.ai.addxvideo.R.mipmap.light_black)
        ivLight?.setImageTintList(ColorStateList.valueOf(if (isOpen) activityContext.getResources().getColor(
            com.ai.addxvideo.R.color.theme_color) else activityContext.getResources().getColor(com.ai.addxvideo.R.color.black_333)))
    }

    fun ringEndRefreshUI(){
        mRinging = false
        mIvRing?.setImageResource(com.ai.addxvideo.R.mipmap.ring_black)
        mIvRing?.setBackgroundResource(com.ai.addxvideo.R.drawable.bg_circle_fill_gray)
    }

    fun refreshRingIconUI(ret: Boolean, isOpen: Boolean){
        mIvRing?.post {
            if (ret && isOpen) {
                if (mRingRunnable == null) {
                    mRingRunnable = Runnable {
                        mIvRing?.setBackgroundResource(com.ai.addxvideo.R.drawable.bg_circle_fill_gray)
                        mIvRing?.setImageResource(com.ai.addxvideo.R.mipmap.ring_black)
                        findViewById<FrameLayout>(com.ai.addxvideo.R.id.layout_tip_ring)?.visibility = View.INVISIBLE
//                        liveFullScreenMenuWindow?.refreshUI()
                    }
                }
                //                                    icon.setImageBitmap(null);
                this@KotlinDemoVideoView.postDelayed(
                    mRingRunnable,
                    RING_SPAN
                )
                mIvRing?.setBackgroundResource(com.ai.addxvideo.R.drawable.bg_corners_60_red)
                val uri = Uri.Builder()
                    .scheme(UriUtil.LOCAL_RESOURCE_SCHEME)
                    .path(java.lang.String.valueOf(com.ai.addxvideo.R.mipmap.ring_focus_ani))
                    .build()
                val controller: DraweeController =
                    Fresco.newDraweeControllerBuilder()
                        .setUri(uri)
                        .setAutoPlayAnimations(true)
                        .build()
                (mIvRing as SimpleDraweeView)?.controller = controller
            }else{
                mRinging = false
                mIvRing?.setImageResource(com.ai.addxvideo.R.mipmap.ring_black)
                mIvRing?.setBackgroundResource(com.ai.addxvideo.R.drawable.bg_circle_fill_gray)
            }

            if (mIsFullScreen) {
//                liveFullScreenMenuWindow?.dismiss()
                findViewById<FrameLayout>(com.ai.addxvideo.R.id.layout_tip_ring).visibility = View.VISIBLE
            }
        }
    }

    private fun initPreLocationData(adapter: PreLocationAdapter, serialNumber: String) {
        RockerControlManager.getInstance().onLoadPreLocationData(serialNumber)
        RockerControlManager.getInstance().addListener(adapter)
    }

    private fun initRockerDataIfNeed(bean: DeviceBean) {
        val rvPrePosition: RecyclerView = findViewById(com.ai.addxvideo.R.id.rv_pre_position)
        val layoutManager = GridLayoutManager(
            activityContext,
            PreLocationConst.PRE_LOCATION_SPAN_COUNT
        )
        rvPrePosition.layoutManager = layoutManager
        val rockerView: RockerView = findViewById(com.ai.addxvideo.R.id.view_rocker)
        rockerView.setOnPositionChangeListener(mRockerListener)
        val preLocationBeanList = PreLocationAdapter.adapterListerForAddItem(
            bean, RockerControlManager.getInstance().getProLocationData(
                bean.serialNumber
            )
        )
        val preLocationAdapter = PreLocationAdapter(preLocationBeanList) { list: List<PreLocationResponse.DataBean.PreLocationBean?> ->
            LogUtils.d(
                "dd",
                "initPreLocationData========getData:" + bean.serialNumber + "===" + list.size
            )
            if (list.size == 0) {
                if (!bean.isAdmin) {
                    rv_pre_position.setVisibility(INVISIBLE)
                    complete_delete_root.setVisibility(INVISIBLE)
                    no_pre_position_tip.setVisibility(VISIBLE)
                } else {
                    rv_pre_position.setVisibility(VISIBLE)
                    complete_delete_root.setVisibility(VISIBLE)
                    no_pre_position_tip.setVisibility(INVISIBLE)
                }
            } else {
                no_pre_position_tip.setVisibility(INVISIBLE)
                rv_pre_position.setVisibility(VISIBLE)
                if (!bean.isAdmin) {
                    complete_delete_root.setVisibility(INVISIBLE)
                }
            }
        }
        preLocationAdapter.setPlayer(this@KotlinDemoVideoView)
        preLocationAdapter.setBean(bean)
        initPreLocationData(preLocationAdapter, bean.serialNumber)
        if (bean.isAdmin) {
            preLocationAdapter.onItemLongClickListener = BaseQuickAdapter.OnItemLongClickListener { adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
                if (!deletePrePositionMode) {
                    deletePrePositionMode = true
                    tv_complete_delete.text = activityContext.getString(com.ai.addxvideo.R.string.done)
                    iv_complete_delete.setImageResource(com.ai.addxvideo.R.mipmap.complete_ok)
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

//        helper.setOnClickListener(R.id.pre_location_root, v -> {
//            RockerControlManager.getInstance().onPreLocationShow(helper.getPlayer());
//            deletePrePositionMode = false;
//            setVisible(R.id.voice_icon, false);
//            setVisible(R.id.pre_location_root, false);
//            setVisible(R.id.view_rocker, false);
//            if (bean.isAdmin()) {
//                setVisible(R.id.rocker_root, false);
//                setVisible(R.id.complete_delete_root, true);
//            }
//            helper.setText(R.id.tv_complete_delete, R.string.delete);
//            helper.setImageResource(R.id.iv_complete_delete, R.mipmap.compelete_delete);
//            setVisible(R.id.close_root, true);
//            setVisible(R.id.rv_pre_position, true);
//        });
        close_root.setOnClickListener(OnClickListener { v: View? ->
//            if (bean.isAdmin()) {
//                setVisible(R.id.rocker_root, true);
//            }
//            layout_pre_location.layoutParams.height = 0
            layout_pre_location.visibility = View.GONE
            onCloseChangeUi(bean)
            if (deletePrePositionMode) {
                deletePrePositionMode = false
                tv_complete_delete.text = activityContext.getString(com.ai.addxvideo.R.string.delete)
                iv_complete_delete.setImageResource(com.ai.addxvideo.R.mipmap.compelete_delete)
                preLocationAdapter.preDeletePositionMode = deletePrePositionMode
                preLocationAdapter.notifyDataSetChanged()
            }
        })
        complete_delete_root.setOnClickListener(OnClickListener { v: View? ->
            deletePrePositionMode = !deletePrePositionMode
            tv_complete_delete.text =
                activityContext.getString(if (deletePrePositionMode) com.ai.addxvideo.R.string.done else com.ai.addxvideo.R.string.delete)
            iv_complete_delete.setImageResource(if (deletePrePositionMode) com.ai.addxvideo.R.mipmap.complete_ok else com.ai.addxvideo.R.mipmap.compelete_delete)
            preLocationAdapter.preDeletePositionMode = deletePrePositionMode
            preLocationAdapter.notifyDataSetChanged()
        })
    }

    private fun onCloseChangeUi(bean: DeviceBean) {
        setVisible(com.ai.addxvideo.R.id.voice_icon, true)
//        setVisible(R.id.item_top, true)
        if (bean.isSupportRocker) {
            setVisible(com.ai.addxvideo.R.id.view_rocker, true)
        } else {
            setVisible(com.ai.addxvideo.R.id.view_rocker, false)
        }
        setVisible(com.ai.addxvideo.R.id.complete_delete_root, false)
        setVisible(com.ai.addxvideo.R.id.close_root, false)
        setVisible(com.ai.addxvideo.R.id.rv_pre_position, false)
//        setVisible(R.id.layout_pre_location, false)
    }
    private fun setVisible(viewId: Int, visible: Boolean){
        val view: View = findViewById(viewId)
        view.visibility = if (visible) VISIBLE else INVISIBLE
    }

    private fun initVideoBottomExpend(){
        if (!mIsSplit) {
            val layoutParams = voice_icon.layoutParams as RelativeLayout.LayoutParams
            if (dataSourceBean?.isSupportRocker != true) {
                view_rocker.visibility = GONE
                layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_END)
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
                layoutParams.bottomMargin = context.resources.getDimension(com.ai.addxvideo.R.dimen.dp_30).toInt()
            } else {
                view_rocker.visibility = VISIBLE
                layoutParams.removeRule(RelativeLayout.CENTER_IN_PARENT)
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END)
                layoutParams.marginEnd = context.resources.getDimension(com.ai.addxvideo.R.dimen.dp_10).toInt()
                initRockerDataIfNeed(dataSourceBean!!)
            }
            mVoiceTip = findViewById(com.ai.addxvideo.R.id.voice_tip)
            mMicFramelayout = findViewById(com.ai.addxvideo.R.id.fr_mic)
            mMicText = findViewById(com.ai.addxvideo.R.id.tv_mic_text)

            more?.removeAllViews()
//            item_top?.removeAllViews()
            addExtendOptBtnsByDeviceType(dataSourceBean!!)
        }
    }

    private fun checkShouldShowGuide(context: Context, bean: DeviceBean, selected: Boolean) {
        if (!isSportTrackLoading) {
            LiveHelper.Companion.checkShouldShowGuide(context, bean, isSportMoveMode, selected)
        }
    }

    override fun onPlayStateChanged(currentState: Int, oldState: Int) {
        if(!mIsFullScreen){
            if (currentState == oldState) return
            mVideoCallBack?.onPlayStateChanged(currentState, oldState)
            if (currentState == AddxBaseVideoView.CURRENT_STATE_PLAYING) {
                resetSportMoveMode(dataSourceBean!!)
                setRockerState(dataSourceBean!!, true, true)
                loadSportTrack(dataSourceBean!!, null)
            } else {
                setRockerState(dataSourceBean!!, false, true)
                if (!mIsSplit) {
                    onCloseChangeUi(dataSourceBean!!)
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
//        CommonUtil.hideNavKey(liveFullScreenMenuWindow!!.contentView)
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