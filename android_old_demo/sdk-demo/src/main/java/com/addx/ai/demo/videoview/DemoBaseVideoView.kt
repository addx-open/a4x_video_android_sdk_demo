package com.addx.ai.demo.videoview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Looper
import android.os.SystemClock
import android.text.TextUtils
import android.util.AttributeSet
import android.view.*
import android.view.View.OnClickListener
import android.widget.*
import com.addx.common.Const
import com.addx.common.utils.*
import com.ai.addx.model.DeviceBean
import com.ai.addx.model.Ratio
import com.ai.addx.model.SleepPlanData
import com.ai.addx.model.request.ReporLiveCommonEntry
import com.ai.addx.model.request.ReporLiveInterruptEntry
import com.ai.addx.model.request.SerialNoEntry
import com.ai.addx.model.response.GetSingleDeviceResponse
import com.ai.addxbase.*
import com.ai.addxbase.addxmonitor.AddxMonitor
import com.ai.addxbase.addxmonitor.FileLogUpload
import com.ai.addxbase.helper.SharePreManager
import com.ai.addxbase.util.LocalDrawableUtills
import com.ai.addxbase.util.TimeUtils
import com.ai.addxbase.util.ToastUtils
import com.ai.addxbase.view.dialog.CommonCornerDialog
import com.ai.addxnet.ApiClient
import com.ai.addxnet.HttpSubscriber
import com.ai.addxvideo.addxvideoplay.*
import com.ai.addxvideo.addxvideoplay.addxplayer.*
import com.ai.addxvideo.addxvideoplay.addxplayer.addxijkplayer.AddxGLSurfaceView
import com.ai.addxvideo.addxvideoplay.addxplayer.addxijkplayer.AddxVideoIjkPlayer
import com.ai.addxvideo.addxvideoplay.addxplayer.webrtcplayer.*
import com.base.resmodule.view.LoadingDialog
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.File
import java.io.FileNotFoundException
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import com.addx.ai.demo.R
import com.addx.common.steps.PageStep
import com.ai.addxbase.permission.PermissionPageStep
import com.ai.addxbase.util.AppUtils
import com.ai.addxvideo.track.other.TrackManager

open abstract class DemoBaseVideoView : FrameLayout, IAddxView, IAddxPlayerStateListener, OnClickListener, View.OnTouchListener  {

    private var mOldOpt: Int = 0
    private var mOldState: Int = 0
    public var currentOpt: Int = 0
    val TAG = "DemoBaseVideoView" + hashCode()
    open var iAddxPlayer: IVideoPlayer? = null
    var dataSourceBean: DeviceBean? = null
    var mNetWorkDialog: CommonCornerDialog? = null
    var currentState = CURRENT_STATE_NORMAL
    var mIsFullScreen = false
    val activityContext: Activity by lazy { CommonUtil.getActivityContext(context) }
    var mute: Boolean = true
    var mSystemUiVisibility = 0
    open var disableCropFrame = true
    open var mShowing: Boolean = false
    var mVideoCallBack: IAddxViewCallback? = null
    open val DEFAULT_SHOW_TIME = 3500L
    open var mVideoRatio: Ratio = Ratio.P720
    open var mDeviceRatioList: ArrayList<String> = ArrayList()
    val mFadeOut = Runnable {
        hide()
    }
    var liveStartTime = 0L
    open var eventPlayerName = TrackManager.PlayerName.HOME_PLAYER
    protected var videoSavePath: String? = null

    @Volatile
    protected var isRecording = false
    protected var recordCounterTask: Subscription? = null
    var mSavePlayState: Int = CURRENT_STATE_NORMAL

    @Volatile
    var isChanggingRatio: Boolean = false

    //most common views
    var defaultThumbRid: Int? = com.ai.addxvideo.R.mipmap.live_default_cover
    var soundBtn: ImageView? = null
    var fullScreenBtn: View? = null
    open var startBtn: ImageView? = null
    var thumbImage: ImageView? = null
    private var renderContainer: ViewGroup? = null
    var renderView: SurfaceView? = null
    open lateinit var mAddxVideoContentView: View
    var ivErrorFlag: ImageView? = null
    private var ivErrorExit: ImageView? = null
    private var ivErrorSetting: ImageView? = null
    protected var tvErrorTips: TextView? = null
    open var tvErrorButton: TextView? = null
    var tvUnderLineErrorBtn: TextView? = null
    private var ivErrorHelp: ImageView? = null
    var errorLayout: ViewGroup? = null
    private var oldLayoutParams: ViewGroup.LayoutParams? = null
    private lateinit var mAddxVideoViewParent: ViewGroup
    var normalLayout: ViewGroup? = null
    var loadingLayout: LinearLayout? = null
    var tvDownloadSpeed: TextView? = null
    var ivErrorThumb: ImageView? = null
    var animShotView: LinearLayout? = null
    var recordIcon: ImageView? = null
    var recordTimeText: TextView? = null
    var normalSoundBtn: ImageView? = null

    @Volatile
    var isSavingRecording: Boolean = false
    var playBeginTimeRecord: Long = 0
    var playTimeRecordSpan: Long = 0
    var savingRecordLoading: LinearLayout? = null
    var availableSdcardSize: Float? = null
    open var thumbSaveKeySuffix: String? = null
    var mShowStartTimeSpan: Long = 0
    var startBtnAction = {
        if(isPlaying() && System.currentTimeMillis() - mShowStartTimeSpan >= START_SHOW_SPAN){
            startBtn?.visibility = View.INVISIBLE
        }
    }
    var downloadStringBuilder:StringBuilder=StringBuilder()
    var START_SHOW_SPAN: Long = 3000
    var mBitmap: Bitmap? = null
    var mStopType: String = ""
    var mShowRecordShotToast: Boolean = false
    //    var mUploadlog: ImageView? = null
    var mIsNeedUploadFailLog: Boolean = false
    var mIsUserClick = false
    var mClickId: String? = null
    var mIsSplit: Boolean = false
    var mLocalThumbTime: Long = 0
    var mServerThumbTime: Long = 0
    companion object {
        const val CURRENT_STATE_NORMAL = 0             //正常
        const val CURRENT_STATE_PREPAREING = 1        //准备中
        const val CURRENT_STATE_PLAYING = 2           //播放中
        const val CURRENT_STATE_PAUSE = 3             //暂停
        const val CURRENT_STATE_AUTO_COMPLETE = 4     //自动播放结束
        const val CURRENT_STATE_ERROR = 5             //错误状态

        @JvmStatic
        fun getThumbImagePath(context: Context, sn: String): String?{
            if(sn == null){
                return null
            }
            var localThumbTime = VideoSharePreManager.getInstance(context).getThumbImgLastLocalFreshTime(sn)/1000
            var serverThumbTime = VideoSharePreManager.getInstance(context).getThumbImgLastServerFreshTime(sn)
            var imgPath:String? = null
            if(serverThumbTime > localThumbTime){
                imgPath = DownloadUtil.getThumbImgDir(context) + MD5Util.md5(sn)+".jpg"
            }
            if (FileUtils.isFileExists(imgPath)) {
                return imgPath
            }else{
                imgPath = LocalDrawableUtills.instance.getDiskCacheFilePath(sn.plus(null))
                if (FileUtils.isFileExists(imgPath)) {
                    return imgPath
                }
            }
            return null
        }
        @JvmStatic
        var mIsClickedNoNeedOtaUpdateMap: HashMap<String, Boolean> = HashMap()
    }

    constructor(context: Context) : super(context) {
        getDeclaredAttrs(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        getDeclaredAttrs(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        getDeclaredAttrs(context, attrs)
    }

    protected open fun getDeclaredAttrs(context: Context, attrs: AttributeSet?) {
        // do some init things
    }

    open fun parseRatio(){}


    open fun init(context: Context?, bean: DeviceBean, iAddxViewCallback: IAddxViewCallback) {
        if (bean == null) {
            throw NullPointerException("$TAG---setDeviceBean----sn is null")
        }
//        LogUtils.w(TAG, "setDeviceBean-------" + bean.serialNumber)
        this.dataSourceBean = bean
        iAddxPlayer = AddxPlayerManager.getInstance().getPlayer(dataSourceBean)
        iAddxPlayer?.setDataSource(dataSourceBean)
//        setBackgroundColor(Color.BLACK)
        setOnClickListener(this)
        mute = AddxAudioSet.getMuteState(dataSourceBean?.serialNumber)
        parseRatio()
        updateThumbImageSource()
        reloadLayout(context)
        setDeviceState(false, false)
        mVideoCallBack = iAddxViewCallback
    }

    //状态处理
    override fun onPrepared(player: IVideoPlayer) {
        LogUtils.w(TAG, "---------DemoBaseVideoView-------onPrepared-------sn:${dataSourceBean!!.serialNumber}")
        playTimeRecordSpan=SystemClock.elapsedRealtime()-liveStartTime
        mShowStartTimeSpan = System.currentTimeMillis()
        //暂时通过延时1s，解决串屏问题
        postDelayed({
            if(iAddxPlayer != null && iAddxPlayer?.isPlaying()!!){
                updateStateAndUI(CURRENT_STATE_PLAYING, 0)
                iAddxPlayer?.muteVoice(mute, true)
                if (mVideoCallBack != null) {
                    mVideoCallBack!!.onStartPlay()
                }
            }
        }, 1000)
    }

    override fun onPreparing(player: IVideoPlayer) {
        LogUtils.w(TAG, "DemoBaseVideoView---------------DemoBaseVideoView-------onPreparing----sn:${dataSourceBean?.serialNumber}")
        post {
            updateStateAndUI(CURRENT_STATE_PREPAREING, currentOpt)
        }
    }

    override fun onVideoSizeChanged(player: IVideoPlayer, ratio: Ratio?) {
        //设置ratio真正成功后的回调
        LogUtils.d(TAG, dataSourceBean?.serialNumber + "----onVideoSizeChanged success")
    }

    override fun onError(player: IVideoPlayer, what: Int, extra: Int) {
        LogUtils.e(TAG,"AddxWebRtc--player---callBackOnErrorToViewIfActive---error--baseview----")
        LogUtils.w(TAG, "DemoBaseVideoView--------------DemoBaseVideoView--------onError-------sn:${dataSourceBean!!.serialNumber}")
        playTimeRecordSpan = 0
        post {
            stopRecordVideo("error")
            if(what == PlayerErrorState.ERROR_DEVICE_MAX_CONNECT_LIMIT){
                updateStateAndUI(CURRENT_STATE_ERROR, what)
            }else{
                checkDeviceExit()
            }
        }
    }

    override fun onVideoPause(player: IVideoPlayer) {
        LogUtils.w(TAG, dataSourceBean?.serialNumber + "---------DemoBaseVideoView-------onVideoPause-------sn:${dataSourceBean!!.serialNumber}")
        post {
            activityContext.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            updateStateAndUI(CURRENT_STATE_PAUSE, currentOpt)
        }
        if (mVideoCallBack != null) {
            mVideoCallBack!!.onStopPlay()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onDownloadSpeedUpdate(player: IVideoPlayer, bytes: Long) {
        if (SystemClock.elapsedRealtime()-liveStartTime<=60000){
            if (downloadStringBuilder.isNotEmpty()) {
                downloadStringBuilder.append(",")
            }
            downloadStringBuilder.append(bytes/1024)
            downloadStringBuilder.append("K/s")
        }

        post {
            val df = DecimalFormat("0.0")
            val M = 1024 * 1024.toLong()
            if (bytes >= M) {
                val v = bytes * 1f / M
                tvDownloadSpeed?.text = df.format(v.toDouble()) + "M/s"
            } else {
                tvDownloadSpeed?.text = df.format(bytes / 1024f.toDouble()) + "K/s"
            }
        }
    }

    override fun onSeekComplete(player: IVideoPlayer) {

    }

    override fun onRetryConnect() {
        post {
//            iAddxPlayer?.playerStatInfo?.misretryconnect = true
            mIsUserClick = false
            stopRecordVideo("onRetryConnect")
            startPlay()
        }
    }

    //===========以上为状态处理
    override fun isPlaying(): Boolean {
        return currentState == CURRENT_STATE_PLAYING
    }

    fun isPrepareing(): Boolean {
        return currentState == CURRENT_STATE_PREPAREING
    }

    override fun startPlay() {
        LogUtils.w(TAG, "DemoBaseVideoView---------------startPlay--------------sn:${dataSourceBean!!.serialNumber}")
        if (dataSourceBean?.needForceOta()!!) {
            onError(iAddxPlayer!!, PlayerErrorState.ERROR_DEVICE_NEED_OTA, 0)
            LogUtils.df(TAG, "DemoBaseVideoView---------------startPlay-----fail-----needForceOta")
            return
        }
        mIsNeedUploadFailLog = true

        checkPermissionsDialog(object: PageStep.StepResultCallback{
            override fun onStepResult(step: PageStep, result: PageStep.PageStepResult) {
                if (result != PageStep.PageStepResult.Failed) {
                    startPlayInternal()
                } else{
                    LogUtils.df(TAG, "DemoBaseVideoView---------------startPlay-----fail-----noPermissions")
                }
            }
        })
    }

    internal open fun resetRatioInfoForG0() {
        dataSourceBean?.let {
            if (it.deviceModel.isG0) {
                VideoSharePreManager.getInstance(context).setLiveRatio(it, Ratio.P720)
            }
        }
    }

    open fun startPlayInternal() {
        activityContext.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        resetRatioInfoForG0()
        liveStartTime = SystemClock.elapsedRealtime()
        playTimeRecordSpan = 0
        getCurrentErrorCodeForCountly(currentState, currentOpt)
        playBeginTimeRecord = SystemClock.elapsedRealtime()
        downloadStringBuilder.clear()

        AddxAudioSet.setMisicDisable()
        if(iAddxPlayer == null){
            iAddxPlayer = AddxPlayerManager.getInstance().getPlayer(dataSourceBean)
        }
        LogUtils.w(TAG, "DemoBaseVideoView---------------startPlayInternal------iAddxPlayer is null:${iAddxPlayer == null}-----sn:${dataSourceBean?.serialNumber}")
        iAddxPlayer?.setDataSource(dataSourceBean)
        iAddxPlayer?.setDisplay(renderView)
        iAddxPlayer?.setFullScreen(mIsFullScreen)
        iAddxPlayer?.setListener(this)
        iAddxPlayer?.startLive()
    }

    override fun stopPlay() {
        LogUtils.d(TAG, "DemoBaseVideoView------stopPlay------sn:${dataSourceBean!!.serialNumber}")
        AddxAudioSet.restoreMisic()
//        iAddxPlayer?.playerStatInfo?.misretryconnect = false
        removeCallbacks(mFadeOut)
        mShowing = false
        iAddxPlayer?.stop()
        startBtn?.visibility = View.VISIBLE
        removeCallbacks(startBtnAction)
        stopRecordVideo("stopPlay")
    }

//    override fun setDeviceBean(deviceBean: DeviceBean?) {
//        if (deviceBean == null) {
//            throw NullPointerException("$TAG---setDeviceBean----sn is null")
//        }
//        LogUtils.w(TAG, "DemoBaseVideoView------setDeviceBean----------sn:${deviceBean!!.serialNumber}")
//        this.dataSourceBean = deviceBean
//        iAddxPlayer = AddxPlayerManager.getInstance().getPlayer(deviceBean)
//        iAddxPlayer?.setDataSource(deviceBean)
//    }

    fun setDeviceState(isAutoPlay: Boolean, isTimeout: Boolean) {
        LogUtils.d(TAG, "DemoBaseVideoView------setDeviceState init server device state---:${dataSourceBean!!.needForceOta()}---needOta:${dataSourceBean!!.needOta()}-----sn:${dataSourceBean!!.serialNumber}")
        //检查设备
        when {
            dataSourceBean!!.isShutDownLowPower -> updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_DEVICE_SHUTDOWN_LOW_POWER)
            dataSourceBean!!.isShutDownPressKey -> updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_DEVICE_SHUTDOWN_PRESS_KEY)
            dataSourceBean!!.isDeviceOffline -> updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_DEVICE_OFFLINE)
            dataSourceBean!!.isDeviceSleep -> updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_DEVICE_SLEEP)
            dataSourceBean!!.isFirmwareUpdateing -> updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_DEVICE_IS_OTA_ING)
            dataSourceBean!!.needOta() && isTimeout -> updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_PLAYER_TIMEOUT)
            dataSourceBean!!.needOta() && (mIsClickedNoNeedOtaUpdateMap.get(dataSourceBean?.serialNumber) == null || !mIsClickedNoNeedOtaUpdateMap.get(dataSourceBean?.serialNumber)!!)-> {//需要OTA
                updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_DEVICE_NEED_OTA)
            }
            dataSourceBean!!.needForceOta() -> {//需要强制OTA
                updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_DEVICE_NEED_OTA)
            }
            isTimeout -> updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_PLAYER_TIMEOUT)
            else -> {
                updateStateAndUI(CURRENT_STATE_NORMAL, currentOpt)
                if(isAutoPlay){
                    startPlay()
                }
            }
        }
    }

//    override fun copyStatus(player: IAddxView) {
////        player.sn = sn
////        //   newplayer.isMicOpen = isMicOpen
////        if (currentState == CURRENT_STATE_PLAYING) {
////            player.startPlay()
////        }
//        //其它状态恢复
//    }
//
//    override fun setUiSplit(split: Boolean) {
//
//    }

    override fun setPlayPosition(position: Long) {
        iAddxPlayer?.SeekTo(position)
    }

    override fun onWebRTCCommandSend(command: DataChannelCommand?) {

    }

    fun saveMuteState(mute: Boolean) {
        val localDevice = DeviceManager.getInstance().get(dataSourceBean!!.serialNumber)
        if (localDevice != null) {
            localDevice.isNeedMute = mute
            DeviceManager.getInstance().update(localDevice)
        }
    }

    fun setMuteState(mute: Boolean) {
        this.mute = mute
        iAddxPlayer?.muteVoice(mute, true)
        saveMuteState(mute)
        updateSoundIcon(mute)
    }

    //
    protected abstract fun fullLayoutId(): Int

    protected abstract fun normalLayoutId(): Int

    private fun checkPermissionsDialog(callback: PageStep.StepResultCallback)
    {
        PermissionPageStep(activityContext)
            .setRequestedPermissions(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
            .setRationaleMessage(com.ai.addxvideo.R.string.if_storage_not_access, AppUtils.getAppName())
            .setSettingsMessage(com.ai.addxvideo.R.string.if_storage_not_access, AppUtils.getAppName())
            .setTitleMessage("")
            .setGuideDescVisible(false)
            .setShowGuideImageBg(false)
            .setSharePrefsUtils(SharePrefsUtils(activityContext, PermissionPageStep.SHAREPRE_NAME))
            .setmNxp()
            .execute(callback)
    }

    override fun onClick(v: View?) {
        LogUtils.d(TAG, "onClick-------" + dataSourceBean!!.serialNumber)

        when (v?.id) {
            com.ai.addxvideo.R.id.start -> {
                LogUtils.d(TAG, "DemoBaseVideoView------onClick------start------sn:${dataSourceBean!!.serialNumber}")
                var isToPlay = mVideoCallBack?.onClickStart(v, dataSourceBean!!)
                if (isPlaying()) {
                    mStopType = "endButton"
                    // 由于截图需要时间，我们需要先操作UI暂停，然后再截图完成后再操作player暂停
                    if(currentState == CURRENT_STATE_PLAYING){
                        LogUtils.d(TAG, "DemoBaseVideoView------LocalDrawableUtills----stopPlay------saveShot----sn:${dataSourceBean!!.serialNumber}")
                        iAddxPlayer?.saveShot { frame ->
                            if (frame != null) {
                                mLocalThumbTime = TimeUtils.getUTCTime()
                                VideoSharePreManager.getInstance(context).setThumbImgLocalLastFresh(dataSourceBean!!.serialNumber, TimeUtils.getUTCTime())
                                LocalDrawableUtills.instance.putLocalCoverBitmap(dataSourceBean!!.serialNumber.plus(thumbSaveKeySuffix), frame)
                                mBitmap = frame
                                post{
                                    setThumbImageByPlayState()
                                }
                            }
                        }
                    }
                    stopPlay()
                } else {
                    if(isToPlay == true){
//                      LogUtils.df(TAG, "permission state result != PageStep.PageStepResult.Failed)----skip:"+(result == PageStep.PageStepResult.Skipped))
                        createRecordClick("start_btn_clickid_")
                        showNetWorkToast()
                        startPlay()
                    }
                }
            }
            com.ai.addxvideo.R.id.back -> {
                LogUtils.d(TAG, "DemoBaseVideoView------onClick------back---sn:" + dataSourceBean!!.serialNumber)
                mVideoCallBack?.onBackPressed()
//                backToNormal()
            }
            com.ai.addxvideo.R.id.fullscreen -> {
                mVideoCallBack?.onClickFullScreen(v, dataSourceBean!!)
                createRecordClick("fullscreen_btn_clickid_")
                LogUtils.d(TAG, "DemoBaseVideoView------onClick------fullscreen---sn:" + dataSourceBean!!.serialNumber)
                if(currentState != CURRENT_STATE_ERROR){
                    startFullScreen(false)
                }
            }
            com.ai.addxvideo.R.id.iv_sound -> {
                LogUtils.d(TAG, "DemoBaseVideoView------onClick------iv_sound---sn:" + dataSourceBean!!.serialNumber)
                setMuteState(!mute)
            }
            com.ai.addxvideo.R.id.iv_record -> {
                LogUtils.e(TAG, "DemoBaseVideoView------currentState $currentState isRecording $isRecording---sn:${dataSourceBean!!.serialNumber}")
                if (currentState == CURRENT_STATE_PLAYING) {
                    if (!isRecording) {
                        availableSdcardSize = SDCardUtils.getAvailableSdcardSize(SizeUnit.MB)
                        val sdfVideo = SimpleDateFormat("yyyyMMddhhmmss", Locale.getDefault())
                        videoSavePath = DirManager.getInstance().getRecordVideoPath() + sdfVideo.format(Date()) + ".mp4"
                        //startRecord(videoSavePath)
                        iAddxPlayer!!.startRecording(videoSavePath!!, object : MP4VideoFileRenderer.VideoRecordCallback {
                            override fun completed() {
                                LogUtils.e(TAG, "DemoBaseVideoView------startRecording-------------- completed---sn:${dataSourceBean!!.serialNumber}")
                            }

                            override fun error() {
                                LogUtils.e(TAG, "DemoBaseVideoView------startRecording-------------- error---sn:${dataSourceBean!!.serialNumber}")
                            }
                        })
                        startRecordCountTask()
                        recordIcon?.setImageResource(com.ai.addxvideo.R.mipmap.video_recording)
                        recordTimeText?.visibility = View.VISIBLE
                        isRecording = true
                        hide()
                    } else {
                        stopRecordVideo("stop by user")
                        savingRecordLoading?.visibility = View.VISIBLE

                        iAddxPlayer?.saveShot { frame ->
                            if (frame != null) {
                                renderView?.post {
                                    startBitmapAnim(frame, resources.getDimension(com.ai.addxvideo.R.dimen.dp_180).toInt())
                                }
                            }
                        }
                    }

                } else {
                    ToastUtils.showShort(com.ai.addxvideo.R.string.live_not_start)
                }
            }
            com.ai.addxvideo.R.id.iv_screen_shot -> {
                LogUtils.d(TAG, "DemoBaseVideoView------onClick------iv_screen_shot---sn:" + dataSourceBean!!.serialNumber)
                if (!isRecording && recordIcon?.isEnabled!!) {
                    iAddxPlayer?.saveShot { frame ->
                        if (frame != null) {
                            try {
                                renderView?.post {
                                    startBitmapAnim(frame, resources.getDimension(com.ai.addxvideo.R.dimen.dp_100).toInt())
                                }
                                val sdf = SimpleDateFormat("yyyyMMddhhmmss", Locale.getDefault())
                                val savePath = PathUtils.getExternalDcimPath() + File.separator + A4xContext.getInstance().getmTenantId() + sdf.format(Date()) + ".png"
                                BitmapUtils.saveBitmap(frame, savePath)
                                FileUtils.syncImageToAlbum(context, savePath, Date().time)
                                if(mShowRecordShotToast){
                                    ToastUtils.showShort(com.ai.addxvideo.R.string.image_save_to_ablum)
                                }
                            } catch (e: FileNotFoundException) {
                                e.printStackTrace();
                                ToastUtils.showShort(com.ai.addxvideo.R.string.shot_fail)
                            }
                        } else {
                            ToastUtils.showShort(com.ai.addxvideo.R.string.shot_fail)
                        }
                    }
                } else {
                    ToastUtils.showShort(com.ai.addxvideo.R.string.cannot_take_screenshot)
                }
            }
        }
    }

    fun createRecordClick(clickType: String){
        mClickId = clickType + System.currentTimeMillis()
        mIsUserClick = true
    }

    fun showNetWorkToast(){
        if (NetworkUtil.isMobileData(A4xContext.getInstance().getmContext()) && !NetworkUtils.isWifiConnected(A4xContext.getInstance().getmContext()) && SharePreManager.getInstance(context).showNetworkDialog()) {
            //showNetWorkDialog()
            ToastUtils.showShort(com.ai.addxvideo.R.string.pay_attention_data)
            SharePreManager.getInstance(context).setShowNetworkDialog(false)
        }
    }

    override fun getScreenShotPicture(callBack: IVideoPlayer.ScreenSpotCallBack) {
        iAddxPlayer?.saveShot(callBack)
    }

    open fun reloadErrorLayout() {
        errorLayout = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.layout_error)
        errorLayout?.removeAllViews()
        errorLayout?.addView(getErrorView())
//        errorLayout?.setOnClickListener {
//            LogUtils.d(TAG, "DemoBaseVideoView------errorLayout?.setOnClickListener-------" + dataSourceBean!!.serialNumber)
//            if(PlayerErrorState.ERROR_DEVICE_IS_OTA_ING != currentOpt){
//                onClickUnderlineErrorButton(null)
//            }
//        }
    }

    open fun setFullScreenRatio(layoutParams: ViewGroup.LayoutParams) {
        if (!mIsFullScreen) {
            return
        }
        val screenHeight = CommonUtil.getScreenHeight(context) + CommonUtil.getStatusBarHeight(context)
        val screenWidth = CommonUtil.getScreenWidth(context)
        val height = screenWidth.coerceAtMost(screenHeight)
        val width = screenWidth.coerceAtLeast(screenHeight)
        if (renderView != null) {
            if (disableCropFrame) {
                renderView?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
                renderView?.layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
            } else {
                layoutParams.width = width * Const.Screen.RATIO.toInt()
                layoutParams.height = height
            }
        }
        LogUtils.w(TAG, "DemoBaseVideoView------setFullscreenRatio ,${renderView?.layoutParams?.width} : ${renderView?.layoutParams?.height} ---sn:${dataSourceBean!!.serialNumber}")
    }

    val fullLayoutViewGroup by lazy { View.inflate(context, fullLayoutId(), null) }
    val normalLayoutViewGroup by lazy { View.inflate(context, normalLayoutId(), null) }

    @SuppressLint("ClickableViewAccessibility")
    private fun reloadLayout(context: Context?) {
        removeAllViews()
        LogUtils.w("reloadLayout", mIsFullScreen)
        mAddxVideoContentView = if (mIsFullScreen) fullLayoutViewGroup else normalLayoutViewGroup
        addView(mAddxVideoContentView)
        reloadErrorLayout()
        thumbImage = findViewById(com.ai.addxvideo.R.id.thumbImage)
        normalLayout = findViewById(com.ai.addxvideo.R.id.normal_layout)
        loadingLayout = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.loading)
        fullScreenBtn = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.fullscreen)
        fullScreenBtn?.setOnClickListener(this)
        if(mIsFullScreen){
            soundBtn = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.iv_sound)
            soundBtn?.setOnClickListener(this)
        }else{
            soundBtn = normalSoundBtn
        }
        startBtn = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.start)
        startBtn?.setOnClickListener(this)
        tvDownloadSpeed = findViewById(com.ai.addxvideo.R.id.tv_download_speed)
        renderContainer = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.surface_container)
        animShotView = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.screen_shot_anim)
        recordIcon = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.iv_record)
        recordTimeText = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.tv_record_time)
//        savingRecordLoading = contentView.findViewById(R.id.is_saving_record)
        if (mIsFullScreen) {
            renderView?.setZOrderOnTop(true)
            renderView?.setZOrderMediaOverlay(true)
        }else{
            renderView?.setZOrderOnTop(false)
            renderView?.setZOrderMediaOverlay(false)
        }
        if (renderView == null) {
            renderView = if (iAddxPlayer is AddxVideoIjkPlayer) {
                AddxGLSurfaceView(A4xContext.getInstance().getmContext())
            } else {
                CustomSurfaceViewRenderer(A4xContext.getInstance().getmContext())
            }
            renderView?.id = View.generateViewId()
            LogUtils.w(TAG, "DemoBaseVideoView------onAddRenderView,${Integer.toHexString(renderView.hashCode())}---sn:${dataSourceBean!!.serialNumber}")
            renderContainer?.addView(renderView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            renderView?.setOnTouchListener(this)
        } else {
            LogUtils.w(TAG, "DemoBaseVideoView------onAddRenderView,${Integer.toHexString(renderView.hashCode())}---sn:${dataSourceBean!!.serialNumber}")
            renderContainer?.addView(renderView, 0, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            setFullScreenRatio(renderView?.layoutParams!!)
        }

        onInitOrReloadUi(context)
        updateStateAndUI(currentState, currentOpt)
        updateSoundIcon(mute)
        setThumbImageByPlayState(true)
        refreshThumbImg()
    }

    internal open fun onInitOrReloadUi(context: Context?) {}


    open fun backToNormal() {
        LogUtils.w(TAG, "DemoBaseVideoView------backToNormal---sn:${dataSourceBean!!.serialNumber}")
        isChanggingRatio = false
        CommonUtil.showNavKey(activityContext, mSystemUiVisibility)
        CommonUtil.showSupportActionBar(activityContext, true, true)
        CommonUtil.scanForActivity(activityContext).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        //remove self (player) from root
        val contentView = activityContext.findViewById(android.R.id.content) as ViewGroup
        contentView.removeView(this)
        //remove renderView from full layout container
        renderContainer?.removeView(renderView)

        for (index in 0 until contentView.childCount) {
            contentView.getChildAt(index).visibility = View.VISIBLE
        }
        mIsFullScreen = false
        //init normal layout  and add renderView to container
        reloadLayout(context)

        //add self to old parent
        if(parent != mAddxVideoViewParent){
            mAddxVideoViewParent.addView(this, oldLayoutParams)
        }

        if (mShowing) {
            removeCallbacks(mFadeOut)
            mShowing = false
            show(DEFAULT_SHOW_TIME)
        }
        stopRecordVideo("back to normal ")
        iAddxPlayer?.setFullScreen(mIsFullScreen)
        onFullScreenStateChange(false)
    }

    open fun startFullScreen(isReverse: Boolean) {
        LogUtils.w(TAG, "DemoBaseVideoView------startFullScreen---sn:${dataSourceBean!!.serialNumber}")
        mIsFullScreen = true
        mSystemUiVisibility = activityContext.window.decorView.systemUiVisibility
        oldLayoutParams = layoutParams
        mAddxVideoViewParent = parent as ViewGroup
        CommonUtil.hideSupportActionBar(activityContext, true, true)
        hideNavKey()
        if(isReverse){
            CommonUtil.scanForActivity(activityContext).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        }else{
            CommonUtil.scanForActivity(activityContext).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        //remove self(Player) from parent
        mAddxVideoViewParent.removeView(this)
        // remove renderView from it's current parent  and  prepare add it to full screen layout 's surface container
        renderContainer?.removeView(renderView)
        // init full layout and add it to surfaceContainer
        reloadLayout(context)
        //设置reload 后的UI状态
        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val contentView = activityContext.findViewById(android.R.id.content) as ViewGroup
        for (index in 0 until contentView.childCount) {
            contentView.getChildAt(index).visibility = View.INVISIBLE
        }
        // now add player to root
        contentView.addView(this, params)
        if (mShowing) {
            removeCallbacks(mFadeOut)
            mShowing = false
            show(DEFAULT_SHOW_TIME)
        }
        autoPlayIfNeed()
        if (isSavingRecording) {
            savingRecordLoading?.visibility = View.VISIBLE
        }
        onFullScreenStateChange(true)
        iAddxPlayer?.setFullScreen(mIsFullScreen)
        onResetRecordUi()
    }

    private fun autoPlayIfNeed() {
        if (!(isPlaying() || isPrepareing())) {
            startBtn?.callOnClick()
            LogUtils.e(TAG, "DemoBaseVideoView------auto play ,before state=$currentState---sn:${dataSourceBean!!.serialNumber}")
        }
    }

    protected open fun errorLayoutId(): Int {
        return if (mIsFullScreen) {
            com.ai.addxvideo.R.layout.live_plager_full_error_page
        } else com.ai.addxvideo.R.layout.live_plager_no_full_error_default_page
    }

    fun onClickErrorRetry() {
        LogUtils.d(TAG, "DemoBaseVideoView------errorLayout?.setOnClickListener----------sn:${dataSourceBean!!.serialNumber}")
        if (!AccountManager.getInstance().isLogin) return
        when (currentOpt) {
            PlayerErrorState.ERROR_DEVICE_NEED_OTA -> {
                AddxFunJump.toUpdate(activityContext, dataSourceBean!!)
            }
            PlayerErrorState.ERROR_DEVICE_SLEEP -> {
                if (dataSourceBean != null && dataSourceBean!!.isAdmin) {
//                    RxBus.getDefault().post(dataSourceBean?.serialNumber, Const.Rxbus.EVENT_WAKE_UP_SLEEP_DEVICE)
                    wakeDevice(dataSourceBean!!.serialNumber)
                }
            }

            PlayerErrorState.ERROR_DEVICE_NO_ACCESS -> {
                ToastUtils.showShort(com.ai.addxvideo.R.string.device_no_access)
            }
            else -> {
                createRecordClick("retry_btn_clickid_")
                showNetWorkToast()
                startPlay()
            }
        }
    }

    fun wakeDevice(sn: String) {
        val loadingDialog = LoadingDialog(context)
        loadingDialog.show()
        ApiClient.getInstance().sleepSwitchSetting(SleepPlanData(sn, 0)).flatMap {
            if (it.result == Const.ResponseCode.CODE_OK) {
                return@flatMap ApiClient.getInstance().getSingleDevice(SerialNoEntry(sn))
            } else {
                return@flatMap rx.Observable.error<GetSingleDeviceResponse>(Throwable("sleepSwitchSetting wakeup camera failed code=" + it.result))
            }
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : HttpSubscriber<GetSingleDeviceResponse>() {
                override fun doOnNext(baseResponse: GetSingleDeviceResponse) {
                    loadingDialog.dismiss()
                    if (baseResponse.result < Const.ResponseCode.CODE_OK) {
                        ToastUtils.showShort(com.ai.addxvideo.R.string.open_fail_retry)
                    } else {
                        ToastUtils.showShort(com.ai.addxvideo.R.string.setup_success)
                        dataSourceBean?.copy(baseResponse.data)
                        setDeviceState(false, false)
                    }
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                    ToastUtils.showShort(com.ai.addxvideo.R.string.open_fail_retry)
                    loadingDialog.dismiss()
                }
            })
    }

    open fun getErrorView(): View {
        val inflate = View.inflate(context, errorLayoutId(), null)
//        mUploadlog = inflate.findViewById(R.id.uploadlog)
//        mUploadlog?.visibility = if (mIsNeedUploadFailLog) View.VISIBLE else View.GONE
        tvErrorTips = inflate.findViewById(com.ai.addxvideo.R.id.tv_error_tips)
        ivErrorExit = inflate.findViewById(com.ai.addxvideo.R.id.iv_error_exit)
        ivErrorSetting = inflate.findViewById(com.ai.addxvideo.R.id.iv_error_setting)
        ivErrorFlag = inflate.findViewById(com.ai.addxvideo.R.id.iv_error_flag)
        tvErrorButton = inflate.findViewById(com.ai.addxvideo.R.id.tv_error_btn)
        tvUnderLineErrorBtn = inflate.findViewById(com.ai.addxvideo.R.id.tv_underline_error_btn)
        ivErrorHelp = inflate.findViewById(com.ai.addxvideo.R.id.iv_error_help)
        ivErrorThumb = inflate.findViewById(com.ai.addxvideo.R.id.iv_error_thumb)
        ivErrorExit?.setOnClickListener { backToNormal() }
        tvErrorButton?.setOnClickListener { onClickErrorRetry() }
        tvErrorTips?.setOnClickListener { onClickErrorTips(tvErrorTips) }
        tvUnderLineErrorBtn?.setOnClickListener {  onClickUnderlineErrorButton(tvUnderLineErrorBtn) }
//        mUploadlog?.setOnClickListener({uploadErrorLog()})
        return inflate
    }

    fun uploadErrorLog() {
        CommonCornerDialog(activityContext)
            .setTitleText(com.ai.addxvideo.R.string.send_log)
            .setMessage(com.ai.addxvideo.R.string.send_log_tips)
            .setLeftText(com.ai.addxvideo.R.string.cancel)
            .setRightText(com.ai.addxvideo.R.string.ok)
            .setLeftClickListener(OnClickListener {
            })
            .setRightClickListener(OnClickListener {
                AddxMonitor.getInstance(A4xContext.getInstance().getmContext()).uploadLastDayLog(object: FileLogUpload.Callback{
                    override fun invoke(fileName: String?, ret: Boolean) {
                        if (ret) {
                            ToastUtils.showShort(com.ai.addxvideo.R.string.uploaded_success)
                        } else {
                            ToastUtils.showShort(com.ai.addxvideo.R.string.uploaded_fail)
                        }
                    }
                })
            }).show()
    }

    open fun updateSoundIcon(mute: Boolean) {
        LogUtils.w(TAG, "DemoBaseVideoView------updateSoundIcon----" + (soundBtn == null).toString() + "--" + mute.toString()+"---sn:${dataSourceBean!!.serialNumber}")
        if(mIsFullScreen) {
            soundBtn?.setImageResource(if (mute) com.ai.addxvideo.R.mipmap.live_last_sound_disable else com.ai.addxvideo.R.mipmap.live_last_sound_enable)
        }else{
            soundBtn?.setImageResource(if (mute) com.ai.addxvideo.R.mipmap.voice_black_notalk else com.ai.addxvideo.R.mipmap.voice_black_talk)
        }
    }

    open fun updateStateAndUI(state: Int, opt: Int) {
        if (!checkPassState(state, opt)) {
            LogUtils.d(TAG, "DemoBaseVideoView------checkPassState false , state = $state  opt = $opt---sn:${dataSourceBean!!.serialNumber}")
            return
        }
        mOldState = currentState
        mOldOpt = currentOpt
        currentOpt = opt
        currentState = state
        LogUtils.d(TAG, "DemoBaseVideoView------oldState=$mOldState  currentState=$state oldOpt===$mOldOpt currentOpt===$opt---sn:${dataSourceBean!!.serialNumber}")
        if (Looper.myLooper() != Looper.getMainLooper()) {
            post {
                updateStateAndUI(mOldState, mOldOpt, state, opt)
            }
        } else {
            updateStateAndUI(mOldState, mOldOpt, state, opt)
        }
    }

    private fun checkPassState(state: Int, opt: Int): Boolean {
        if (state == CURRENT_STATE_ERROR && opt != PlayerErrorState.ERROR_PHONE_NO_INTERNET && !NetworkUtils.isConnected(A4xContext.getInstance().getmContext())) {//check
            updateStateAndUI(state, PlayerErrorState.ERROR_PHONE_NO_INTERNET)
            return false
        }
        return true
    }

    private fun updateStateAndUI(tempState: Int, tempOpt: Int,state: Int, opt: Int){
        callBackViewState(state, tempState)
        updatePropertyBeforeUiStateChanged(tempState, state, tempOpt, opt)
        hideNavKey()
        updateUiState(tempState, state, tempOpt, opt)

        LogUtils.d(TAG, "DemoBaseVideoView------onStateAndUiUpdated oldState $tempState  newState : $state---sn:${dataSourceBean!!.serialNumber}")

        if (currentState != CURRENT_STATE_PLAYING && currentState != CURRENT_STATE_PREPAREING && tempState != currentState) {
            setThumbImageByPlayState()
        }
    }

    private fun updateUiState(oldUiState: Int, newUiState: Int, oldOption: Int, newOption: Int) {
        when (newUiState) {
            CURRENT_STATE_NORMAL -> {
                changeUIToIdle()
            }
            CURRENT_STATE_PREPAREING -> {
                changeUIToConnecting()
            }
            CURRENT_STATE_PLAYING -> {
                changeUIToPlaying()
            }
            CURRENT_STATE_PAUSE, CURRENT_STATE_AUTO_COMPLETE -> {
                if (currentOpt == 0) {
                    changeUIToIdle()
                } else {
                    changeUIToError(currentOpt)
                }
            }
            CURRENT_STATE_ERROR -> {
                changeUIToError(newOption)
            }
        }
    }

    private fun updatePropertyBeforeUiStateChanged(oldUiState: Int, newUiState: Int, oldOption: Int, newOption: Int) {
        //do noting
    }

    var errorCodeWhenUserClickForCountly = 0 // 仅仅离线和被动的错误算是errorCode
    private var retryErrorCodeOnClickStartByUserForCountly = 0 //从errorCode中筛选出来，属于errorRetry的code
    private fun getCurrentErrorCodeForCountly(state: Int, opt: Int) {
        if (state == CURRENT_STATE_ERROR) {
            errorCodeWhenUserClickForCountly = opt
            when (opt) {
                PlayerErrorState.ERROR_DEVICE_NO_ACCESS,
                PlayerErrorState.ERROR_DEVICE_SHUTDOWN_LOW_POWER,
                PlayerErrorState.ERROR_DEVICE_SHUTDOWN_PRESS_KEY,
                PlayerErrorState.ERROR_DEVICE_OFFLINE,
                PlayerErrorState.ERROR_DEVICE_UNACTIVATED,
                PlayerErrorState.ERROR_DEVICE_AUTH_LIMITATION,
                PlayerErrorState.ERROR_DEVICE_MAX_CONNECT_LIMIT,
                PlayerErrorState.ERROR_PHONE_NO_INTERNET,
                PlayerErrorState.ERROR_UDP_AWAKE_FAILED,
                PlayerErrorState.ERROR_PLAYER_TIMEOUT,
                PlayerErrorState.ERROR_CONNECT_TIMEOUT,
                PlayerErrorState.ERROR_CONNECT_EXCEPTION -> {
                    retryErrorCodeOnClickStartByUserForCountly = opt
                }
                else -> {
                    retryErrorCodeOnClickStartByUserForCountly = 0
                }
            }
        } else {
            errorCodeWhenUserClickForCountly = 0
            retryErrorCodeOnClickStartByUserForCountly = 0
        }
    }

    private fun callBackViewState(currentState: Int, oldState: Int) {
        onPlayStateChanged(currentState, oldState)
        when (currentState) {
            CURRENT_STATE_PREPAREING -> {
            }
            CURRENT_STATE_PLAYING -> {
            }
            CURRENT_STATE_ERROR, CURRENT_STATE_AUTO_COMPLETE, CURRENT_STATE_PAUSE, CURRENT_STATE_NORMAL -> {
                mVideoCallBack?.onStopPlay()
            }
        }
    }

    protected open fun changeUIToPlaying() {
        LogUtils.w(TAG, "changeUIToPlaying, mIsFullScreen= $mIsFullScreen-----sn:${dataSourceBean?.serialNumber}")
        thumbImage?.visibility = View.INVISIBLE
        loadingLayout?.visibility = View.INVISIBLE
        errorLayout?.visibility = View.INVISIBLE
        if((mIsFullScreen && currentState == CURRENT_STATE_NORMAL) || !mIsFullScreen) {
            startBtn?.visibility = View.VISIBLE
        }else{
            startBtn?.visibility = View.INVISIBLE
        }
        removeCallbacks(startBtnAction)
        postDelayed(startBtnAction,START_SHOW_SPAN)
    }

    protected open fun changeUIToConnecting() {
        LogUtils.w(TAG, "-----changeUIToConnecting, mIsFullScreen= $mIsFullScreen---sn:${dataSourceBean?.serialNumber}")
        startBtn?.visibility = View.INVISIBLE
        errorLayout?.visibility = View.INVISIBLE
        loadingLayout?.visibility = View.VISIBLE
        thumbImage?.visibility = View.VISIBLE
    }

    protected open fun changeUIToIdle() {
        LogUtils.w(TAG, "-----changeUIToIdle, mIsFullScreen= $mIsFullScreen--sn:${dataSourceBean?.serialNumber}")
        thumbImage?.visibility = View.VISIBLE
        errorLayout?.visibility = View.INVISIBLE
        loadingLayout?.visibility = View.INVISIBLE
        if((mIsFullScreen && currentState == CURRENT_STATE_NORMAL) || !mIsFullScreen) {
            startBtn?.visibility = View.VISIBLE
            removeCallbacks(startBtnAction)
        }
    }

    fun getSleepMsg(): String {
        return if (dataSourceBean?.deviceDormancyWakeTime != null && dataSourceBean!!.deviceDormancyWakeTime > 1000) {
            val pairTime =
                TimeUtils.formatWeekDayAndTime(dataSourceBean?.deviceDormancyWakeTime!! * 1000)
            context.getString(com.ai.addxvideo.R.string.sleep_end_time_atsteam, pairTime.first, pairTime.second)
        } else {
            context.getString(com.ai.addxvideo.R.string.sleeping)
        }
    }

    protected open fun changeUIToError(opt: Int?) {
        when (opt) {
            PlayerErrorState.ERROR_DEVICE_UNACTIVATED -> setErrorInfo(com.ai.addxvideo.R.string.camera_not_activated, com.ai.addxvideo.R.mipmap.live_error_unactivated)
            PlayerErrorState.ERROR_DEVICE_SLEEP -> {
                setErrorInfo(
                    com.ai.addxvideo.R.string.camera_sleep, com.ai.addxvideo.R.mipmap.ic_sleep_main_live, true, com.ai.addxvideo.R.string.camera_wake_up, dataSourceBean?.isAdmin
                    ?: false, null, false)

                getSleepMsg().let {
                    if (it.isNotEmpty() && dataSourceBean?.isAdmin!!) {
                        tvErrorTips?.text = it
                    } else {
                        tvErrorTips?.text = it.plus("\n\n" + context.resources.getString(com.ai.addxvideo.R.string.admin_wakeup_camera))
                    }
                }
            }
            PlayerErrorState.ERROR_DEVICE_NO_ACCESS -> setErrorInfo(com.ai.addxvideo.R.string.error_2002, com.ai.addxvideo.R.mipmap.live_error__no_access, underlineErrorBtnText = com.ai.addxvideo.R.string.refresh)
            PlayerErrorState.ERROR_DEVICE_SHUTDOWN_LOW_POWER -> {
                setErrorInfo(com.ai.addxvideo.R.string.low_power, com.ai.addxvideo.R.mipmap.lowpowershutdown)
            }
            PlayerErrorState.ERROR_DEVICE_SHUTDOWN_PRESS_KEY -> {
                setErrorInfo(com.ai.addxvideo.R.string.turned_off, com.ai.addxvideo.R.mipmap.shutdown)
            }
            PlayerErrorState.ERROR_DEVICE_OFFLINE -> {
                setErrorInfo(com.ai.addxvideo.R.string.camera_poor_network, com.ai.addxvideo.R.mipmap.live_offline)
            }
            PlayerErrorState.ERROR_DEVICE_NEED_OTA -> {
                if (!AccountManager.getInstance().isLogin) {
                    return
                }
                val needForceOta = dataSourceBean?.needForceOta()
                if (dataSourceBean?.adminId == AccountManager.getInstance().userId) {
                    if (!needForceOta!!) {
                        if(mIsClickedNoNeedOtaUpdateMap.get(dataSourceBean?.serialNumber) == null || !mIsClickedNoNeedOtaUpdateMap.get(dataSourceBean?.serialNumber)!!){
                            setErrorInfo(
                                com.ai.addxvideo.R.string.fireware_need_update_tips, com.ai.addxvideo.R.mipmap.ic_video_device_upgrade,
                                errorBtnText = com.ai.addxvideo.R.string.update, errorBtnVisible = true,
                                underlineErrorBtnText = com.ai.addxvideo.R.string.do_not_update, underLineErrorBtnColor = com.ai.addxvideo.R.color.theme_color)
                        }
                    } else {
                        setErrorInfo(com.ai.addxvideo.R.string.fireware_need_update_tips, com.ai.addxvideo.R.mipmap.ic_video_device_upgrade, errorBtnText = com.ai.addxvideo.R.string.update, errorBtnVisible = true, underlineErrorBtnVisible = false)
                    }
                } else {
                    if (!needForceOta!!) {
                        if(mIsClickedNoNeedOtaUpdateMap.get(dataSourceBean?.serialNumber) == null || !mIsClickedNoNeedOtaUpdateMap.get(dataSourceBean?.serialNumber)!!){
                            setErrorInfo(
                                com.ai.addxvideo.R.string.forck_update_share, com.ai.addxvideo.R.mipmap.ic_video_device_upgrade,
                                errorBtnText = com.ai.addxvideo.R.string.update, errorBtnVisible = false,
                                underlineErrorBtnText = com.ai.addxvideo.R.string.do_not_update, underLineErrorBtnColor = com.ai.addxvideo.R.color.theme_color)
                        }
                    } else {
                        setErrorInfo(com.ai.addxvideo.R.string.forck_update_share, com.ai.addxvideo.R.mipmap.ic_video_device_upgrade, underlineErrorBtnVisible = false)
                    }

                }
            }
            PlayerErrorState.ERROR_DEVICE_IS_OTA_ING -> setErrorInfo(com.ai.addxvideo.R.string.device_is_updating, flagRes = com.ai.addxvideo.R.mipmap.ic_video_device_upgrade, underlineErrorBtnVisible = false)


            PlayerErrorState.ERROR_PHONE_NO_INTERNET -> {
                setErrorInfo(com.ai.addxvideo.R.string.failed_to_get_information_and_try)
            }

            PlayerErrorState.ERROR_PLAYER_TIMEOUT -> {
                setErrorInfo(com.ai.addxvideo.R.string.live_stream_timeout, com.ai.addxvideo.R.mipmap.live_timeout)
            }
            PlayerErrorState.ERROR_CONNECT_TIMEOUT -> {
                setErrorInfo(com.ai.addxvideo.R.string.network_error_our_server, com.ai.addxvideo.R.mipmap.live_timeout)
            }
            PlayerErrorState.ERROR_UNKNOWN,
            PlayerErrorState.ERROR_CONNECT_EXCEPTION -> {
                setErrorInfo(com.ai.addxvideo.R.string.server_error, com.ai.addxvideo.R.mipmap.live_exception)
            }

            PlayerErrorState.ERROR_DEVICE_MAX_CONNECT_LIMIT -> {
                LogUtils.w(TAG, "DemoBaseVideoView------AddxWebRtc------setErrorInfo-----------ERROR_DEVICE_MAX_CONNECT_LIMIT-------sn:${dataSourceBean!!.serialNumber}")
                setErrorInfo(com.ai.addxvideo.R.string.server_error, com.ai.addxvideo.R.mipmap.live_exception)
//                changeUIToIdle()
                ToastUtils.showShort(com.ai.addxvideo.R.string.live_viewers_limit)
            }

            else -> {
                RuntimeException("changeUIToError----未知错误---").printStackTrace()
                setDefaultErrorInfo()
            }
        }
        errorLayout?.visibility = View.VISIBLE
    }

    open fun setDefaultErrorInfo(){
        setErrorInfo(com.ai.addxvideo.R.string.live_stream_error, com.ai.addxvideo.R.mipmap.live_exception)
    }

    fun setErrorInfo(errorMsg: Int,
                     flagRes: Int? = com.ai.addxvideo.R.mipmap.ic_video_network_device_disconnect,
                     flagVisible: Boolean? = true,
                     errorBtnText: Int? = com.ai.addxvideo.R.string.reconnect,
                     errorBtnVisible: Boolean? = false,
                     underlineErrorBtnText: Int? = com.ai.addxvideo.R.string.reconnect,
                     underlineErrorBtnVisible: Boolean? = true,
                     underLineErrorBtnColor: Int? = com.ai.addxvideo.R.color.theme_color) {
        flagRes?.let { ivErrorFlag?.setImageResource(it) }
        flagVisible?.let { ivErrorFlag?.visibility = if (it) View.VISIBLE else View.GONE }
        if(errorMsg == com.ai.addxvideo.R.string.fireware_need_update_tips){
            tvErrorTips?.text = activityContext.getString(errorMsg, dataSourceBean?.newestFirmwareId)
        }else if(errorMsg == com.ai.addxvideo.R.string.forck_update_share){
            if(dataSourceBean?.needForceOta()!!){
                tvErrorTips?.text = activityContext.getString(errorMsg, dataSourceBean?.newestFirmwareId).plus(activityContext.getString(
                    com.ai.addxvideo.R.string.unavailable_before_upgrade))
            }else{
                tvErrorTips?.text = activityContext.getString(errorMsg, dataSourceBean?.newestFirmwareId)
            }
        }else if(errorMsg == com.ai.addxvideo.R.string.low_power || errorMsg == com.ai.addxvideo.R.string.turned_off){
            LogUtils.d(TAG, "dataSourceBean?.getOfflineTime()-----errorMsg == R.string.low_power:${errorMsg == com.ai.addxvideo.R.string.low_power}----getOfflineTime：${dataSourceBean?.getOfflineTime()}")
            tvErrorTips?.setText(resources.getString(errorMsg) + "\n" +resources.getString(com.ai.addxvideo.R.string.off_time, if (dataSourceBean?.getOfflineTime() == null) "" else TimeUtils.formatYearSecondFriendly(dataSourceBean?.getOfflineTime()!!.toLong() * 1000)))
        }else{
            tvErrorTips?.setText(errorMsg)
        }
        errorBtnVisible?.let { tvErrorButton?.visibility = if (errorBtnVisible) View.VISIBLE else View.GONE }
        errorBtnText?.let { tvErrorButton?.setText(it) }

        underlineErrorBtnText?.let { tvUnderLineErrorBtn?.setText(it) }
        underlineErrorBtnVisible?.let { tvUnderLineErrorBtn?.visibility = if (it) View.VISIBLE else View.GONE }
        underLineErrorBtnColor?.let { tvUnderLineErrorBtn?.setTextColor(activityContext.resources.getColor(underLineErrorBtnColor)) }
    }

    fun hideNavKey() {
        if (mIsFullScreen) {
            CommonUtil.hideNavKey(activityContext)
        }
    }

    open fun getThumbPath(sn: String): String {
        return DirManager.getInstance().getCoverPath(sn)
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        hideNavKey()
        when (v?.id) {
            renderView?.id -> {
                if (currentState != CURRENT_STATE_PLAYING) {
                    return false
                }
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (mShowing) {
                            hide()
                        } else {
                            mShowStartTimeSpan = System.currentTimeMillis()
                            show(DEFAULT_SHOW_TIME) // start timeout
                        }
                        return true
                    }
                }
            }
        }
        return false
    }
    protected open fun micTouch(v: View?, event: MotionEvent?): Int{
        return 0
    }
    protected open fun show(timeout: Long) {
        hideNavKey()
    }

    protected open fun hide() {
        hideNavKey()
    }

    override fun onMicFrame(data: ByteArray?) {

    }

    open fun onClickUnderlineErrorButton(tip: TextView?) {
        LogUtils.d(TAG, "DemoBaseVideoView------onClickUnderlineErrorButton----------sn:${dataSourceBean!!.serialNumber}")
        when (currentOpt) {
            PlayerErrorState.ERROR_DEVICE_AUTH_LIMITATION,
            PlayerErrorState.ERROR_DEVICE_NO_ACCESS -> {}//checkDeviceExit(1)
            PlayerErrorState.ERROR_DEVICE_SLEEP -> {return
            }//sleep click do noting
            PlayerErrorState.ERROR_DEVICE_NEED_OTA ->{
                updateStateAndUI(CURRENT_STATE_NORMAL, currentOpt)
                mIsClickedNoNeedOtaUpdateMap.put(dataSourceBean?.serialNumber!!, true)
                return
            }
            else -> {
                var isToPlay = mVideoCallBack?.onClickUnderline(tip, dataSourceBean!!)
                if(isToPlay == true){
                    createRecordClick("underline_retry_btn_clickid_")
                    showNetWorkToast()
                    startPlay()
                }
            }
        }
        if(resources.getString(com.ai.addxvideo.R.string.refresh) == tvUnderLineErrorBtn?.text){
            mVideoCallBack!!.onClickRefresh(tip)
        }
    }

    open fun onClickErrorTips(tip: TextView?) {
        LogUtils.d(TAG, "DemoBaseVideoView------onClickErrorTips base----------sn:${dataSourceBean!!.serialNumber}")
        createRecordClick("clicktip_btn_clickid_")
    }

    fun checkDeviceExit() {
        val observable = ApiClient.getInstance().getSingleDevice(SerialNoEntry(dataSourceBean?.serialNumber))
        observable.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : HttpSubscriber<GetSingleDeviceResponse>() {
                override fun doOnNext(t: GetSingleDeviceResponse) {
                    if (t.result == Const.ResponseCode.DEVICE_NO_ACCESS || t.result == Const.ResponseCode.DEVICE_1_NO_ACCESS || t.result == Const.ResponseCode.DEVICE_2_NO_ACCESS) {
                        currentOpt = PlayerErrorState.ERROR_DEVICE_NO_ACCESS
                        updateStateAndUI(CURRENT_STATE_ERROR, currentOpt)
                    } else if (t.result >= Const.ResponseCode.CODE_OK) {
                        dataSourceBean?.copy(t.data)
                        LogUtils.d(TAG, "DemoBaseVideoView------setDeviceState====doOnNext---sn:${dataSourceBean!!.serialNumber}")
                        setDeviceState(false, true)
                    }
                }

                override fun onError(e: Throwable?) {
                    super.onError(e)
                    LogUtils.d(TAG, "DemoBaseVideoView------setDeviceState====onError=---sn:${dataSourceBean!!.serialNumber}")
                    LogUtils.d(TAG, "DemoBaseVideoView------getSingleDevice====onError=---sn:${dataSourceBean!!.serialNumber}")
                    updateStateAndUI(CURRENT_STATE_ERROR, currentOpt)
                }
            })
    }

    // 具体操作记录 stopway ，等上报点以后清空stopway
    var countlyLiveStopWay = ""

    fun setThumbImageByPlayState(shouldSkipVisible: Boolean = false) {
        LogUtils.d(TAG, "DemoBaseVideoView------setThumbImageByPlayState ---sn:${dataSourceBean!!.serialNumber}")
        if (dataSourceBean == null) {
            LogUtils.e(TAG, "DemoBaseVideoView------sn is null , do not set bg---sn:${dataSourceBean!!.serialNumber}")
            return
        }

        val needBlur = when (currentState) {
            CURRENT_STATE_NORMAL,
            CURRENT_STATE_PAUSE,
            CURRENT_STATE_AUTO_COMPLETE -> false
            else -> true
        }
        thumbImage?.let {
            if (it.visibility == View.VISIBLE || shouldSkipVisible) {
                LogUtils.e(TAG, "DemoBaseVideoView------thumbImage visible = ${it.visibility == View.VISIBLE}---sn:${dataSourceBean!!.serialNumber}")
                setThumbImageInternal(it, needBlur)
            }
        }

        ivErrorThumb?.let {
            if ((it.visibility == View.VISIBLE || shouldSkipVisible)) {
                if (currentState == CURRENT_STATE_ERROR) setThumbImageInternal(it, needBlur)
                else if (currentOpt < 0) setThumbImageInternal(it, true)
            }
        }
    }

    private fun setThumbImageInternal(view: ImageView,needBlur: Boolean) {
        dataSourceBean?.let {
            if (it.isDeviceSleep){
                view.setImageResource(com.ai.addxvideo.R.drawable.live_sleep_bg)
                return
            }
        }
        LogUtils.d(TAG, "DemoBaseVideoView------setThumbImageInternal---mBitmap:${mBitmap == null}--sn:${dataSourceBean!!.serialNumber}")
        if (needBlur) {
            view.setImageBitmap(BitmapUtils.rsBlur(context, mBitmap, 15, 3))
        } else {
            view.setImageBitmap(mBitmap)
        }
    }

    fun updateThumbImageSource(){
//        LogUtils.d(TAG, "DemoBaseVideoView------updateThumbImageSource-------mServerThumbTime:${mServerThumbTime}---mLocalThumbTime:${mLocalThumbTime}")
        mLocalThumbTime = VideoSharePreManager.getInstance(context).getThumbImgLastLocalFreshTime(dataSourceBean!!.serialNumber) / 1000
        mServerThumbTime = VideoSharePreManager.getInstance(context).getThumbImgLastServerFreshTime(dataSourceBean!!.serialNumber)
        if(mServerThumbTime > mLocalThumbTime){
//            LogUtils.d(TAG, "DemoBaseVideoView------updateThumbImageSource------toRequestAndRefreshThumbImg-------sn:${dataSourceBean!!.serialNumber}")
            val imgPath = DownloadUtil.getThumbImgDir(activityContext) + MD5Util.md5(dataSourceBean?.serialNumber)+".jpg"
            mBitmap = BitmapUtils.getBitmap(imgPath)
            if(mBitmap != null){
                return
            }
        }
        mBitmap = LocalDrawableUtills.instance.getLocalCoverBitmap(dataSourceBean!!.serialNumber.plus(thumbSaveKeySuffix))
        if (mBitmap == null) {
            mBitmap = BitmapFactory.decodeResource(resources, defaultThumbRid!!)
        }
    }

    override fun onWhiteLightResult(isOpen: Boolean) {}

    override fun onTriggerAlarm(isOpen: Boolean) {}

    override fun getPlayPosition(): Long {
        return iAddxPlayer?.currentPosition!!
    }

    override fun onSeekProcessing(player: IVideoPlayer?, playingTime: Long) {

    }

    override fun onDevInitiativeSendMsg(player: IVideoPlayer?, type: Int) {

    }

    override fun onRotateAction(player: IVideoPlayer, limit: Int){
        ToastUtils.showShort(com.ai.addxvideo.R.string.limit_reached)
    }

    /**
     * 开启录像任务
     */
    fun startRecordCountTask() {
        if (recordCounterTask != null) {
            recordCounterTask!!.unsubscribe()
        }
        recordCounterTask = rx.Observable.interval(0, 1, TimeUnit.SECONDS)
            .onBackpressureBuffer()
            .take(Int.MAX_VALUE)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : HttpSubscriber<Long?>() {
                override fun doOnNext(t: Long?) {
                    if (t != null) {
                        val minute = t / 60
                        val sec = t % 60
                        recordTimeText?.text = String.format("%02d:%02d", minute, sec)
                    }
                }
            })
    }


    protected fun stopRecordVideo(stopWay: String) {
        if (!isRecording || (recordIcon != null && !recordIcon?.isEnabled!!) || isSavingRecording) {
            LogUtils.d("stopRecordVideo------------没有正在录制或者保存还没有完成")
            return
        }
        recordIcon?.isEnabled = false
        isSavingRecording = true
        savingRecordLoading?.visibility = View.INVISIBLE
        iAddxPlayer?.stopRecording(object : MP4VideoFileRenderer.VideoRecordCallback {
            override fun error() {
                LogUtils.d("stopRecordVideo------------error")
                ToastUtils.showShort(com.ai.addxvideo.R.string.record_failed)
            }

            override fun completed() {
                LogUtils.d("stopRecordVideo------------completed")
                try {
                    isRecording = false
                    isSavingRecording = false
                    if(mShowRecordShotToast){
                    }
                    ToastUtils.showShort(com.ai.addxvideo.R.string.video_saved_to_ablum)
                    FileUtils.syncVideoToAlbum(A4xContext.getInstance().getmContext(), videoSavePath, Date().time)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                post {
                    recordIcon?.isEnabled = true
                    savingRecordLoading?.visibility = View.INVISIBLE
                    onResetRecordUi()
                }
            }
        })
        recordTimeText?.visibility = View.INVISIBLE
    }

    @SuppressLint("SetTextI18n")
    protected fun onResetRecordUi() {
        recordIcon?.setImageResource(com.ai.addxvideo.R.mipmap.live_last_record)
        recordTimeText?.visibility = View.INVISIBLE
        if (recordCounterTask != null) {
            recordCounterTask!!.unsubscribe()
        }
        recordTimeText?.text = "00:00"
    }


    internal open fun startBitmapAnim(bitmap: Bitmap, toBottom: Int) {

    }

    fun savePlayState() {
        mSavePlayState = currentState
    }

    fun savePlayStatePlaying(): Boolean {
        return mSavePlayState == CURRENT_STATE_PLAYING
    }

    override fun getPlayState(): Int {
        return currentState
    }

    override fun onReveivedVideoFrame(frame: Bitmap?) {
//        post { setThumbImageByPlayState() }
        if (frame != null) {
            LogUtils.d(TAG, "DemoBaseVideoView------LocalDrawableUtills----onReveivedVideoFrame------saveShot----sn:${dataSourceBean!!.serialNumber}")
            LocalDrawableUtills.instance.putLocalCoverBitmap(dataSourceBean!!.serialNumber.plus(thumbSaveKeySuffix), frame)
            mBitmap = frame
        }
    }

    //    fun releasePlayer() {
//        if (renderView is CustomSurfaceViewRenderer) {
//            LogUtils.e(TAG, "DemoBaseVideoView------releasePlayer----------iAddxPlayer is null: true---sn:${dataSourceBean!!.serialNumber}")
//            AddxPlayerManager.getInstance().releasePlayer(dataSourceBean)
//        }
//    }
    internal open fun refreshThumbImg(){
    }

//    open fun showAlarmDialog(ringListener: AddxLiveOptListener.RingListener?) {
//
//    }
//    open fun setOptListener(addxLiveOptListener: AddxLiveOptListener?) {
//    }

//    override fun voice() {
//    }
//
//    override fun ring(ringListener: AddxLiveOptListener.RingListener) {
//    }
//
//    override fun sportAuto(isInit: Boolean, isSelected: Boolean, sportAutoTrackListener: AddxLiveOptListener.SportAutoTrackListener) {
//    }
//
//    override fun light(listener: AddxLiveOptListener.Listener) {
//    }
//
//    override fun setting() {
//    }

//    override fun toDeviceInfo() {
//
//    }

    open fun updateStateAndUIWhenNetChange(isConnected: Boolean) {
        if (isConnected) {
            updateStateAndUI(CURRENT_STATE_NORMAL, currentOpt)
        } else {
            updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_PHONE_NO_INTERNET)
        }
    }
    open fun preApplyConnectWhenB(errorCode: Int){
        if(iAddxPlayer is AddxVideoWebRtcPlayer){
            if(dataSourceBean!!.isDeviceOffline || dataSourceBean!!.isDeviceSleep || dataSourceBean!!.isFirmwareUpdateing ||dataSourceBean!!.needForceOta()){
                return
            }
            iAddxPlayer?.setListener(this)
            (iAddxPlayer as AddxVideoWebRtcPlayer)?.preApplyConnectWhenB(errorCode)
        }
    }

    //原來viewcallback方法抽离
    internal open fun onPlayStateChanged(currentState: Int, oldState: Int){

    }
    internal open fun isSupportGuide() {
        mVideoCallBack?.isSupportGuide()
    }
    internal open fun onFullScreenStateChange(fullScreen: Boolean) {
        mVideoCallBack?.onFullScreenStateChange(fullScreen)
    }

    override fun isFullScreen(): Boolean{
        return mIsFullScreen
    }

    override fun getAddxPlayer(): IVideoPlayer?{
        return iAddxPlayer
    }

    override fun isRinging(): Boolean{
        return false
    }
    override fun getWhiteLightOn():Boolean{
        return false
    }

    override  fun hideNav(){
    }

    fun refreshVideoView(bean: DeviceBean){
        dataSourceBean?.copy(bean)
        iAddxPlayer = AddxPlayerManager.getInstance().getPlayer(dataSourceBean)
        setDeviceState(false, false)
        thumbImage?.requestLayout()
        onInitOrReloadUi(context)
    }
}

