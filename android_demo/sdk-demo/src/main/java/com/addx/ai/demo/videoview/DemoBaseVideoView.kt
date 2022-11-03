package com.addx.ai.demo.videoview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.os.SystemClock
import android.text.TextUtils
import android.util.AttributeSet
import android.view.*
import android.view.View.OnClickListener
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import com.a4x.player.A4xCommonEntity
import com.a4x.player.A4xVideoViewRender
import com.a4x.player.IA4xLogReportListener
import com.a4x.player.IA4xOnPlayerStateListener
import com.addx.common.Const
import com.addx.common.permission.PermissionHelper
import com.addx.common.rxjava.BaseSubscriber
import com.addx.common.steps.PageStep
import com.addx.common.ui.UiHelper
import com.addx.common.utils.*
import com.ai.addx.model.DeviceBean
import com.ai.addx.model.LoginInfoBean
import com.ai.addx.model.SleepPlanData
import com.ai.addx.model.request.ReporLiveCommonEntry
import com.ai.addx.model.request.ReporLiveInterruptEntry
import com.ai.addx.model.request.ReporLiveP2pConnectEntry
import com.ai.addx.model.request.SerialNoEntry
import com.ai.addx.model.response.BaseResponse
import com.ai.addx.model.response.GetSingleDeviceResponse
import com.ai.addxbase.*
import com.ai.addxbase.GlobalSwap.resConfig
import com.ai.addxbase.addxmonitor.AddxMonitor
import com.ai.addxbase.bluetooth.APDeviceManager
import com.ai.addxbase.bluetooth.LocalWebSocketClient
import com.ai.addxbase.helper.SharePreManager
import com.ai.addxbase.permission.A4xPermissionHelper
import com.ai.addxbase.permission.PermissionPageStep
import com.ai.addxbase.trace.other.TrackManager
import com.ai.addxbase.util.AppUtils
import com.ai.addxbase.util.LocalDrawableUtills
import com.ai.addxbase.util.TimeUtils
import com.ai.addxbase.util.ToastUtils
import com.ai.addxbase.view.dialog.CommonCornerDialog
import com.ai.addxnet.ApiClient
import com.ai.addxnet.HttpSubscriber
import com.ai.addxvideo.R
import com.ai.addxvideo.addxnet.BuildConfig
import com.ai.addxvideo.addxvideoplay.AddxAudioSet
import com.ai.addxvideo.addxvideoplay.IAddxView
import com.ai.addxvideo.addxvideoplay.IAddxViewCallback
import com.ai.addxvideo.addxvideoplay.LiveHelper
import com.ai.addxvideo.addxvideoplay.addxplayer.*
import com.ai.addxvideo.addxvideoplay.addxplayer.addxijkplayer.AddxGLSurfaceView
import com.ai.addxvideo.addxvideoplay.addxplayer.addxijkplayer.AddxVideoIjkPlayer
import com.ai.addxvideo.addxvideoplay.addxplayer.webrtcplayer.*
import com.base.resmodule.view.LoadingDialog
import kotlinx.android.synthetic.main.layout_player_full.view.*
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.File
import java.io.FileNotFoundException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

open abstract class DemoBaseVideoView : FrameLayout, IAddxView, IAddxPlayerStateListener, OnClickListener, View.OnTouchListener  {

    private var mOldOpt: Int = 0
    private var mOldState: Int = 0
    var currentOpt: Int = 0
    private val TAG = "AddxBaseVideoView" + hashCode()
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
    open var mVideoRatio: String = A4xCommonEntity.VideoResolution.VIDEO_SIZE_AUTO.value
    open var mDeviceRatioList: HashMap<Int, String> = HashMap()
    //    open var mNeedUpdateUiWhenStateNoChanged = true
    val mFadeOut = Runnable {
        hideAutoHideUI()
    }
    @Volatile
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
    open var tvErrorButtonLeft: TextView? = null
    var tvUnderLineErrorBtn: TextView? = null
//    private var ivErrorHelp: ImageView? = null
    var errorLayout: ViewGroup? = null
    //    private var oldLayoutParams: ViewGroup.LayoutParams? = null
    private var oldLayoutIndex: Int = 0
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
    var mThumbImgTime: Long = 0

    companion object {
        const val CURRENT_STATE_NORMAL = 0             //正常
        const val CURRENT_STATE_PREPAREING = 1        //准备中
        const val CURRENT_STATE_PLAYING = 2           //播放中
        const val CURRENT_STATE_PAUSE = 3             //暂停
        const val CURRENT_STATE_ERROR = 5             //错误状态

        @JvmStatic
        fun getThumbImagePath(context: Context, sn: String): String?{
            if(sn == null){
                return null
            }
            val localThumbTime = VideoSharePreManager.getInstance().getThumbImgLastLocalFreshTime(sn)/1000
            val serverThumbTime = VideoSharePreManager.getInstance().getThumbImgLastServerFreshTime(sn)
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

//    open fun parseRatio(){}


    open fun init(context: Context?, bean: DeviceBean, iAddxViewCallback: IAddxViewCallback) {
        LogUtils.w(TAG, "setDeviceBean-------" + bean.serialNumber)
        this.dataSourceBean = bean
        if(bean.isDoorbell){
            defaultThumbRid = com.ai.addxvideo.R.mipmap.live_default_cover_4_3
        }
        iAddxPlayer = AddxPlayerManager.getInstance().getPlayer(dataSourceBean)
        iAddxPlayer?.setDataSource(dataSourceBean)
        if(iAddxPlayer is WebrtcPlayerWrap){
            (iAddxPlayer as WebrtcPlayerWrap).setDeviceModel()
        }
        setOnClickListener(this)
        mute = AddxAudioSet.getMuteState(dataSourceBean?.serialNumber)
        mVideoRatio = VideoSharePreManager.getInstance().getLiveRatio(dataSourceBean!!)
        LiveHelper.parseRatio(mDeviceRatioList, dataSourceBean!!)
        if(mDeviceRatioList.isNotEmpty()){
            if(TextUtils.isEmpty(mVideoRatio) || !mDeviceRatioList.containsValue(mVideoRatio)){
                mVideoRatio = mDeviceRatioList[0]!!
            }
        }
        if(TextUtils.isEmpty(mVideoRatio)){
            mVideoRatio = A4xCommonEntity.VideoResolution.VIDEO_SIZE_AUTO.value
        }
        updateThumbImageSource()
        reloadLayout(context)
        resetStateAndUI(false, false)
//        mNeedUpdateUiWhenStateNoChanged = false
        mVideoCallBack = iAddxViewCallback
//        LogUtils.w(TAG, "dataSourceBeanjsonStr:${JSON.toJSONString(dataSourceBean)}")
    }

    override fun onPlayerState(stateCode: Int, errorMessage: A4xCommonEntity.ErrorMessage) {
        LogUtils.w(TAG, "onPlayerState---stateCode:$stateCode---errorMessage:${errorMessage?.value}---renderView:${renderView?.parent == null}")
        when(stateCode){
            IA4xOnPlayerStateListener.MediaPlayState.MEDIA_STATE_OPENING -> onPreparing(iAddxPlayer)
            IA4xOnPlayerStateListener.MediaPlayState.MEDIA_STATE_PLAYING -> onPrepared(iAddxPlayer)
            IA4xOnPlayerStateListener.MediaPlayState.MEDIA_STATE_PAUSE -> onVideoPause(iAddxPlayer)
            IA4xOnPlayerStateListener.MediaPlayState.MEDIA_STATE_FAILED -> {
//                if(mPlayTimeout.get()){
//                    iAddxPlayer?.stopTry()
//                }
                if(errorMessage == A4xCommonEntity.ErrorMessage.ERR_MAX_LIMIT_CONNECTION){
                    onError(iAddxPlayer, PlayerErrorState.ERROR_DEVICE_MAX_CONNECT_LIMIT, 0)
                }else{
                    if(errorMessage == A4xCommonEntity.ErrorMessage.ERR_LOG_IN_EXPIRED || errorMessage == A4xCommonEntity.ErrorMessage.ERR_ACCOUNT_GET_KICKED){
                        ApiClient.getInstance().logoutWhenError(LoginInfoBean.STATE_LOGIN_EXPIRE)
                    }else{
                        onError(iAddxPlayer, 0, 0)
                    }
                }
            }
            IA4xOnPlayerStateListener.MediaPlayState.MEDIA_STATE_CLOSING,
            IA4xOnPlayerStateListener.MediaPlayState.MEDIA_STATE_CLOSED -> {
                LogUtils.w(TAG, "onPlayerState----MEDIA_STATE_CLOSED--sn:${dataSourceBean?.serialNumber}")
                if(isPrepareing()){
                    onVideoPause(iAddxPlayer)
                }
            }
        }
    }

    override fun onDeviceMsgPush(ret: String, obj: Any) {

    }

    override fun onReport(reportTopic: String?, info: IA4xLogReportListener.ReportInfo?) {
        if(info?.isClicked == false){
            mIsUserClick = false
        }
        when(reportTopic){
            IA4xLogReportListener.ReportTopic.DATACHANNEL_SEND -> {
                var reportData = getNewLiveReportData(true, null, info)
                reportData?.cmd = info?.cmd
                TrackManager.getBackEndTrackManager().reportLiveDatachannelStart(reportData)
            }
            IA4xLogReportListener.ReportTopic.DATACHANNEL_SUCCESS -> {
                var reportData = getNewLiveReportData(true, null, info)
                reportData?.cmd = info?.cmd
                TrackManager.getBackEndTrackManager().reportLiveDatachannelSuccess(reportData)
            }
            IA4xLogReportListener.ReportTopic.GET_WEBRTCTICKET -> {
                TrackManager.getBackEndTrackManager().reportLiveGetWebRtcTicket(dataSourceBean?.serialNumber, info?.liveId, true)
            }
            IA4xLogReportListener.ReportTopic.LIVE_FAIL -> {
                TrackManager.getBackEndTrackManager().reportLiveFail(getNewLiveReportData(false, null, info))
            }
            IA4xLogReportListener.ReportTopic.LIVE_INTERRUPT -> {
                val reportData = getNewLiveInterruptWhenLoaddingReportData(info)
                reportData.endWay = mStopType
                TrackManager.getBackEndTrackManager().reportLiveInterrupt(reportData)
            }
            IA4xLogReportListener.ReportTopic.LIVE_P2P_CONNECTED -> {
                val reporLiveP2pConnectEntry = ReporLiveP2pConnectEntry()
                reporLiveP2pConnectEntry.sessionid = info?.sessionId
                getNewLiveReportData(false, reporLiveP2pConnectEntry, info)
                TrackManager.getBackEndTrackManager().reportLivep2pConnected(reporLiveP2pConnectEntry)
            }
            IA4xLogReportListener.ReportTopic.LIVE_SENDOFFER -> {
                val reporLiveP2pConnectEntry = ReporLiveP2pConnectEntry()
                reporLiveP2pConnectEntry.sessionid = info?.sessionId
                getNewLiveReportData(false, reporLiveP2pConnectEntry, info)
                TrackManager.getBackEndTrackManager().reportLiveSendOffer(reporLiveP2pConnectEntry)
            }
            IA4xLogReportListener.ReportTopic.LIVE_START -> {
                TrackManager.getBackEndTrackManager().reportLiveStart(getNewLiveReportData(false, null, info))
            }
            IA4xLogReportListener.ReportTopic.LIVE_STOP -> {
                //没有
            }
            IA4xLogReportListener.ReportTopic.LIVE_SUCCESS -> {
                TrackManager.getBackEndTrackManager().reportLiveSuccess(getNewLiveReportData(true, null, info))
            }
            IA4xLogReportListener.ReportTopic.LIVE_WEBSOCKET_CONNECTED -> {
                TrackManager.getBackEndTrackManager().reportLiveWebsocketConnected(getNewLiveReportData(false, null, info))
            }
            IA4xLogReportListener.ReportTopic.LIVE_WEBSOCKET_START -> {
                TrackManager.getBackEndTrackManager().reportLiveWebsocketStart(getNewLiveReportData(false, null, info))
            }
        }
    }

    //状态处理
    open fun onPrepared(player: IVideoPlayer?) {
        LogUtils.w(TAG, "----------onPrepared-------sn:${dataSourceBean!!.serialNumber}")
//        iAddxPlayer?.screenShot { frame ->
//            if (frame != null) {
//                LogUtils.d(TAG, "onPrepared--screenShot to apply thumbimg----sn:${dataSourceBean!!.serialNumber}")
//                LocalDrawableUtills.instance.putLocalCoverBitmap(dataSourceBean!!.serialNumber, frame)
//                mBitmap = frame
//            }
//        }
        playTimeRecordSpan=SystemClock.elapsedRealtime()-liveStartTime
        mShowStartTimeSpan = System.currentTimeMillis()
        //暂时通过延时1s，解决串屏问题
        post{
            if(iAddxPlayer != null && iAddxPlayer?.isPlaying!!){
                updateStateAndUI(CURRENT_STATE_PLAYING, 0)
                iAddxPlayer?.audioEnable(!mute)
                if (mVideoCallBack != null) {
                    mVideoCallBack!!.onStartPlay()
                }
            }
        }
    }

    open fun onPreparing(player: IVideoPlayer?) {
        LogUtils.w(TAG, "-onPreparing----sn:${dataSourceBean?.serialNumber}")
        post {
            updateStateAndUI(CURRENT_STATE_PREPAREING, currentOpt)
        }
    }

    override fun onVideoSizeChanged(player: IVideoPlayer, ratio: String?) {
        //设置ratio真正成功后的回调
        LogUtils.d(TAG, dataSourceBean?.serialNumber + "----onVideoSizeChanged success")
    }

    open fun onError(player: IVideoPlayer?, what: Int, extra: Int) {
        LogUtils.w(TAG, "----------onError-------sn:${dataSourceBean!!.serialNumber}")
        playTimeRecordSpan = 0
        post {
            stopRecordVideo("error")
            if(what == PlayerErrorState.ERROR_DEVICE_MAX_CONNECT_LIMIT){
                ToastUtils.showShort(com.ai.addxvideo.R.string.live_viewers_limit)
                updateStateAndUI(CURRENT_STATE_NORMAL, what)
            }else{
                checkDeviceExit()
            }
        }
        if (mIsNeedUploadFailLog) {
            AddxThreadPools.executor.execute {
                AddxMonitor.getInstance(A4xContext.getInstance().getmContext()).uploadOnlyLiveLog()
                AddxMonitor.getInstance(A4xContext.getInstance().getmContext()).uploadLastLiveContent()
//                AddxMonitor.getInstance(A4xContext.getInstance().getmContext()).uploadLastDayLog { _, ret -> }
            }
        }
        mIsNeedUploadFailLog = false
    }

    open fun onVideoPause(player: IVideoPlayer?) {
        LogUtils.w(TAG, dataSourceBean?.serialNumber + "----------onVideoPause-------sn:${dataSourceBean!!.serialNumber}")
        post {
            activityContext.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            updateStateAndUI(CURRENT_STATE_PAUSE, currentOpt)
        }
        if (mVideoCallBack != null) {
            mVideoCallBack!!.onStopPlay()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onDownloadSpeedUpdate(bytes: Long) {
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

    //===========以上为状态处理
    override fun isPlaying(): Boolean {
        return currentState == CURRENT_STATE_PLAYING
    }

    override fun isPrepareing(): Boolean {
        return currentState == CURRENT_STATE_PREPAREING
    }

    internal open fun resetResolutionForG0() {
        dataSourceBean?.let {
            if (it.deviceModel.isG0) {
                VideoSharePreManager.getInstance().setLiveRatio(it, A4xCommonEntity.VideoResolution.VIDEO_SIZE_1280x720.value)
            }
        }
    }

    fun initApDeviceSdkBeforeStart(): Boolean{
        LogUtils.d(TAG, "initApDeviceSdkBeforeStart---")
        if(APDeviceManager.INSTANCE.isApDevice(dataSourceBean)){
            LogUtils.d(TAG, "initApDeviceSdkBeforeStart---is ap--usersn:${dataSourceBean?.userSn}")
            val isLogined = LocalWebSocketClient.INSTANCE.isLogined(dataSourceBean?.userSn)
            if(isLogined){
                (iAddxPlayer as WebrtcPlayerWrap).initAPMode(dataSourceBean?.mIp!!, AccountManager.getInstance().userId.toString())
                val token = LocalWebSocketClient.INSTANCE.mCurrentToken
                (iAddxPlayer as WebrtcPlayerWrap).setAPToken(token)
                LogUtils.d(TAG, "initApDeviceSdkBeforeStart---islogined---mIp:${dataSourceBean?.mIp}---" +
                        "userId:${AccountManager.getInstance().userId.toString()}---token:$token")
            }
            return isLogined
        }
        return true
    }

    override fun startPlay() {
        if (!canTryPlay()) {//记录一下，由于离线优先级最高，只要包含离线状态，无论有没有其他状态比如，升级中或者强制升级，都允许进行下一步点击直播
            LogUtils.df(TAG, "start-----fail-----canTryPlay = false")
//            return
        }
        mIsNeedUploadFailLog = true
        val isContinueStart = initApDeviceSdkBeforeStart()
        if(!isContinueStart){
            ToastUtils.showShort("ap live not be logined")
            return
        }
        if(PhoneHardwareUtil.isHarmonyOs()){
            if(PermissionHelper.isPermissionsStorgeGranted(activityContext)){
                startInternal()
            }else{
                ToastUtils.showShort(com.ai.addxvideo.R.string.android_photos_permission)
            }
        }else{
            checkPermissionsDialog { step, result ->
                if (result != PageStep.PageStepResult.Failed) {
                    startInternal()
                } else {
                    ToastUtils.showShort(com.ai.addxvideo.R.string.if_storage_not_access.resConfig().configAppNameDevice())
                    LogUtils.df(
                        TAG,
                        "start--Permissions-----fail-----noPermissions"
                    )
                }
            }
        }
    }

    open fun startInternal() {
        activityContext.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        resetResolutionForG0()
        liveStartTime = SystemClock.elapsedRealtime()
        playTimeRecordSpan = 0
        getCurrentErrorCodeForCountly(currentState, currentOpt)
        playBeginTimeRecord = SystemClock.elapsedRealtime()
        downloadStringBuilder.clear()

        AddxAudioSet.setMisicDisable()
//        if(iAddxPlayer == null){
//            iAddxPlayer = AddxPlayerManager.getInstance().getPlayer(dataSourceBean)
//        }
//        LogUtils.w(TAG, "startPlayInternal------iAddxPlayer is null:${iAddxPlayer == null}-----sn:${dataSourceBean?.serialNumber}")
        iAddxPlayer?.setDataSource(dataSourceBean)
//        iAddxPlayer?.setFullScreen(mIsFullScreen)
        iAddxPlayer?.setRenderView(renderView)
        iAddxPlayer?.setListener(this)
        startReally()
        LogUtils.w(TAG, "startLive--------endtime:${System.currentTimeMillis()}------sn:${dataSourceBean!!.serialNumber}")
    }

    open fun startReally(){
        iAddxPlayer?.startLive(mVideoRatio, A4xCommonEntity.CustomParam().apply { verifyDormancyPlan = true })
    }

    open fun saveScreenShotWhenStopPlay(frame: Bitmap){
        if (frame != null) {
            mLocalThumbTime = TimeUtils.getUTCTime()
            VideoSharePreManager.getInstance().setThumbImgLocalLastFresh(dataSourceBean!!.serialNumber, TimeUtils.getUTCTime())
            setCacheThumbImg(frame)
        }
    }

    open fun getCacheThumbImg(sn: String): Bitmap?{
        return LocalDrawableUtills.instance.getLocalCoverBitmap(sn)
    }

    open fun setCacheThumbImg(frame: Bitmap){
        LocalDrawableUtills.instance.putLocalCoverBitmap(dataSourceBean!!.serialNumber, frame)
    }

    override fun stopPlay() {
        LogUtils.d(TAG, "stopPlay------begintime:${System.currentTimeMillis()}----sn:${dataSourceBean!!.serialNumber}")
        if(isPlaying()){
            iAddxPlayer?.screenShot { frame ->
                if (frame != null) {
                    saveScreenShotWhenStopPlay(frame)
                    mBitmap = frame
                    post{
                        setThumbImageByPlayState()
                    }
                }
            }
        }
        AddxAudioSet.restoreMisic()
        removeCallbacks(mFadeOut)
        mShowing = false
        startBtn?.visibility = View.VISIBLE
        removeCallbacks(startBtnAction)
        stopRecordVideo("stopPlay")
        stopInternal(getStopDelayReleaseTime())
        LogUtils.d(TAG, "stopPlay------endtime:${System.currentTimeMillis()}----sn:${dataSourceBean!!.serialNumber}")
    }

    open fun stopInternal(delayReleaseTime: Int){
        iAddxPlayer?.stopLive()
    }

    open fun needShowSleepState():Boolean{
        return true
    }

    fun resetStateAndUI(isAutoPlay: Boolean, isTimeout: Boolean) {
        LogUtils.d(TAG, "resetStateAndUI init server device state---:${dataSourceBean!!.needForceOta()}---needOta:${dataSourceBean!!.needOta()}-----sn:${dataSourceBean!!.serialNumber}")
        //检查设备
        if(APDeviceManager.INSTANCE.isApDevice(dataSourceBean)){
            if(!LocalWebSocketClient.INSTANCE.isLogined(dataSourceBean?.userSn)){
                LogUtils.d(TAG, "aplist---resetStateAndUI-----not isLogined:${dataSourceBean?.userSn}")
                currentOpt = PlayerErrorState.ERROR_PHONE_NO_AP_CONNECT
                updateStateAndUI(CURRENT_STATE_ERROR, currentOpt)
            }else{
                LogUtils.d(TAG, "aplist---resetStateAndUI-----isLogined:${dataSourceBean?.userSn}")
                if(isTimeout){
                    updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_PLAYER_TIMEOUT)
                }else{
                    currentOpt = CURRENT_STATE_NORMAL
                    updateStateAndUI(CURRENT_STATE_NORMAL, currentOpt)
                }
            }
        }else{
            LogUtils.d(TAG, "aplist---resetStateAndUI--")
            when {
                dataSourceBean!!.isShutDownLowPower -> updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_DEVICE_SHUTDOWN_LOW_POWER)
                dataSourceBean!!.isShutDownPressKey -> updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_DEVICE_SHUTDOWN_PRESS_KEY)
                dataSourceBean!!.isDeviceOffline -> updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_DEVICE_OFFLINE)
                dataSourceBean!!.isDeviceSleep && needShowSleepState() -> updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_DEVICE_SLEEP)
                dataSourceBean!!.isFirmwareUpdateing -> updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_DEVICE_IS_OTA_ING)
                dataSourceBean!!.needOta() && isTimeout -> updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_PLAYER_TIMEOUT)
                (dataSourceBean!!.needOta() || dataSourceBean?.suggestOta() == true) && dataSourceBean?.otaIgnoredStatus() == false && dataSourceBean?.userIgnoreOta() == false  && needShowOtaUpdateUI()-> {//需要OTA
                    LogUtils.d(TAG, "updateStateAndUI------111")
                    updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_DEVICE_NEED_OTA)
                }
                dataSourceBean!!.needForceOta() -> {//需要强制OTA
                    LogUtils.d(TAG, "updateStateAndUI------222")
                    updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_DEVICE_NEED_OTA)
                }
                isTimeout -> updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_PLAYER_TIMEOUT)
//            currentOpt = PlayerErrorState.ERROR_PHONE_NO_AP_CONNECT
                else -> {
                    LogUtils.d(TAG, "updateStateAndUI------333")
                    updateStateAndUI(CURRENT_STATE_NORMAL, currentOpt)
                    if(isAutoPlay){
                        startPlay()
                    }
                }
            }
        }
    }

    fun needShowOtaUpdateUI(): Boolean{
        return mIsClickedNoNeedOtaUpdateMap.get(dataSourceBean?.serialNumber) == null || !mIsClickedNoNeedOtaUpdateMap.get(dataSourceBean?.serialNumber)!!
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
        iAddxPlayer?.audioEnable(!mute)
        reportEnableMicEvent(mute)
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
            .setmNxp()
            .execute(callback)
    }

    var mStartRecordCallback = object: A4xCommonEntity.IPlayerCallback {
        override fun onComplete(obj: Any?) {
            LogUtils.e(TAG, "startRecording-------------- completed---sn:${dataSourceBean!!.serialNumber}")
        }

        override fun onError(errCode: Int, errMsg: String) {
            LogUtils.e(TAG, "startRecording-------------- error---sn:${dataSourceBean!!.serialNumber}")
        }
    }


    fun isNeedApPasswordOrLoginDevice(isNeedLoginWhenNotBeLogined: Boolean): Boolean{
        if (APDeviceManager.INSTANCE.isApDevice(dataSourceBean)) {
            val localBean = APDeviceManager.INSTANCE.getLocalApDeviceInfo(dataSourceBean?.userSn!!)
            if (localBean?.apDeviceSSIDPassword.isNullOrEmpty()) {
                //if(LocalWebSocketClient.INSTANCE.isLogined(dataSourceBean?.userSn!!)){
                //    AddxFunJump.jumpToApSetPasswordActivity(context, dataSourceBean)
                //    return true
                //}else{
                LogUtils.d(TAG, "isNeedSetApPassword------need set password, but is not logined")
                //    if(isNeedLoginWhenNotBeLogined){
                mVideoCallBack?.toConnectApDevice(dataSourceBean?.userSn!!)
                //    }
                //}
            }
        }
        return false
    }

    override fun onClick(v: View?) {
        LogUtils.d(TAG, "onClick-------" + dataSourceBean!!.serialNumber)

        when (v?.id) {
            com.ai.addxvideo.R.id.start -> {
                LogUtils.d(TAG, "onClick------start------sn:${dataSourceBean!!.serialNumber}")
                if (APDeviceManager.INSTANCE.isApDevice(dataSourceBean)) {
                    if(!LocalWebSocketClient.INSTANCE.isLogined(dataSourceBean?.userSn!!)){
                        mVideoCallBack?.toConnectApDevice(dataSourceBean?.userSn!!)
                        return
                    }
                }
                val isToPlay = mVideoCallBack?.onClickStart(v, dataSourceBean!!)
                if (isPlaying()) {
                    mStopType = "endButton"
                    // 由于截图需要时间，我们需要先操作UI暂停，然后再截图完成后再操作player暂停
                    LogUtils.d(TAG, "LocalDrawableUtills----stopPlay--------sn:${dataSourceBean!!.serialNumber}")
                    stopPlay()
                } else {
                    LogUtils.d(TAG, "LocalDrawableUtills----stopPlay---error---click start btn is not at playing")
                    if(isToPlay == true){
                        startPlayWithNetToastandRecord("start_btn_clickid_")
                        reportLiveClickEvent("normal")
                    }
                }
            }
            com.ai.addxvideo.R.id.back -> {
                LogUtils.d(TAG, "onClick------back---sn:" + dataSourceBean!!.serialNumber)
                mVideoCallBack?.onBackPressed()
//                backToNormal()
            }
            com.ai.addxvideo.R.id.fullscreen -> {
//                mVideoCallBack?.onClickFullScreen(v, dataSourceBean!!)
                createRecordClick("fullscreen_btn_clickid_")
                LogUtils.d(TAG, "onClick------fullscreen---sn:" + dataSourceBean!!.serialNumber)
//                if(currentState != CURRENT_STATE_ERROR){
                startFullScreen(false)
//                }
            }
            com.ai.addxvideo.R.id.iv_sound -> {
                LogUtils.d(TAG, "onClick------iv_sound--dataSourceBean?.liveAudioToggleOn:${dataSourceBean?.liveAudioToggleOn}---sn:" + dataSourceBean!!.serialNumber)
                if(dataSourceBean?.liveAudioToggleOn == null || dataSourceBean?.liveAudioToggleOn == true){
                    setMuteState(!mute)
                }else{
                    if(dataSourceBean?.isAdmin == true){
                        adminShowToOpenReceiveVoiceDialog()
                    }else{
                        showToOpenReceiveVoiceDialog()
                    }
                }
            }
            com.ai.addxvideo.R.id.iv_record -> {
                LogUtils.e(TAG, "currentState $currentState isRecording $isRecording---sn:${dataSourceBean!!.serialNumber}")
                if (currentState == CURRENT_STATE_PLAYING) {
                    if (!isRecording) {
                        availableSdcardSize = SDCardUtils.getAvailableSdcardSize(SizeUnit.MB)
                        //startRecord(videoSavePath)
                        videoSavePath = iAddxPlayer!!.startRecord(mStartRecordCallback)
                        startRecordCountTask()
                        setRecordingUi()
                        recordTimeText?.visibility = View.VISIBLE
                        isRecording = true
                        hideAutoHideUI()
                    } else {
                        stopRecordVideo("stop by user")
                        savingRecordLoading?.visibility = View.VISIBLE

                        iAddxPlayer?.screenShot { frame ->
                            if (frame != null) {
                                renderView?.post {
                                    startImgAnimAfterShotScreen(frame, resources.getDimension(com.ai.addxvideo.R.dimen.dp_180).toInt())
                                }
                            }
                        }
                    }

                } else {
                    ToastUtils.showShort(com.ai.addxvideo.R.string.live_not_start)
                }
            }
            com.ai.addxvideo.R.id.iv_screen_shot -> {
                LogUtils.d(TAG, "onClick------iv_screen_shot---sn:" + dataSourceBean!!.serialNumber)
                if (!isRecording && recordIcon?.isEnabled!!) {
                    iAddxPlayer?.screenShot { frame ->
                        if (frame != null) {
                            try {
                                renderView?.post {
                                    startImgAnimAfterShotScreen(frame, resources.getDimension(com.ai.addxvideo.R.dimen.dp_100).toInt())
                                }
                                val sdf = SimpleDateFormat("yyyyMMddhhmmss", Locale.getDefault())
                                val savePath = PathUtils.getExternalDcimPath() + File.separator + A4xContext.getInstance().getmTenantId() + sdf.format(Date()) + ".png"
                                BitmapUtils.saveBitmap(frame, savePath)
                                FileUtils.syncImageToAlbum(context, savePath, Date().time)
                                if(mShowRecordShotToast){
                                    ToastUtils.showShort(com.ai.addxvideo.R.string.image_save_to_ablum)
                                }
                                reportScreenShotEvent(true, null)
                            } catch (e: FileNotFoundException) {
                                e.printStackTrace()
                                ToastUtils.showShort(com.ai.addxvideo.R.string.shot_fail)
                                reportScreenShotEvent(false, "shot frame=null")
                            }
                        } else {
                            ToastUtils.showShort(com.ai.addxvideo.R.string.shot_fail)
                        }
                    }
                } else {
                    ToastUtils.showShort(com.ai.addxvideo.R.string.cannot_take_screenshot)
                    reportScreenShotEvent(false, "recording can not shot")
                }
            }
        }
    }

    open fun setRecordingUi(){
        recordIcon?.setImageResource(com.ai.addxvideo.R.mipmap.video_recording)
    }

    fun createRecordClick(clickType: String){
        if(!(isPlaying() || isPrepareing())){
            mClickId = clickType + System.currentTimeMillis()
        }
        mIsUserClick = true
    }

    fun showNetWorkToast(){
        if (NetworkUtil.isMobileData(A4xContext.getInstance().getmContext()) && !NetworkUtils.isWifiConnected(A4xContext.getInstance().getmContext()) && SharePreManager.getInstance(context).showNetworkDialog()) {
            //showNetWorkDialog()
            ToastUtils.showShort(com.ai.addxvideo.R.string.pay_attention_data)
            SharePreManager.getInstance(context).setShowNetworkDialog(false)
        }
    }

    private fun reportEnableMicEvent(mute: Boolean) {
        val segmentation = TrackManager.get().getSegmentation(TrackManager.EventPair.LIVE_MUTE_SWITCH_CLICK)
        segmentation["live_player_way"] = if (mIsFullScreen) "fullscreen" else "halfscreen"
        segmentation["switch_status"] = if (mute) "close" else "open"
        TrackManager.get().reportEvent(segmentation)
    }

    private fun reportScreenShotEvent(b: Boolean, s: String?) {
        val segmentation = TrackManager.get().getSegmentation(TrackManager.EventPair.LIVE_SCREENSHOT)
        segmentation["live_player_way"] = if (mIsFullScreen) "fullscreen" else "halfscreen "
        segmentation["result"] = b
        if (s != null) {
            segmentation["error_msg"] = s
        }
        TrackManager.get().reportEvent(segmentation)
    }

    override fun getScreenShotPicture(callBack: ScreenSpotCallBack) {
        iAddxPlayer?.screenShot(callBack)
    }

    open fun reloadErrorLayout() {
        errorLayout = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.layout_error)
        errorLayout?.removeAllViews()
        errorLayout?.addView(getErrorView())
        errorLayout?.findViewById<LinearLayout>(com.ai.addxvideo.R.id.ll_living_flag)?.visibility = if(APDeviceManager.INSTANCE.isApDevice(dataSourceBean)) View.VISIBLE else View.GONE
    }

    fun setNormalVideoRatio(){
        var ratio = DeviceUtil.getDeviceScreenRatio(dataSourceBean!!)
//        UiHelper.setConstraintDimensionRatio(
//            this,
//            R.id.rl_video_all_container,
//            ratio
//        )
        UiHelper.setConstraintDimensionRatio(
            renderContainer?.parent as ConstraintLayout,
            com.ai.addxvideo.R.id.surface_container,
            ratio
        )
        UiHelper.setConstraintDimensionRatio(
            thumbImage?.parent as ConstraintLayout,
            com.ai.addxvideo.R.id.thumbImage,
            ratio
        )
        if(!mIsSplit){
            UiHelper.setConstraintDimensionRatio(
                mAddxVideoContentView.findViewById<CardView>(com.ai.addxvideo.R.id.cv_videoview_ratio).parent as ConstraintLayout,
                com.ai.addxvideo.R.id.cv_videoview_ratio,
                ratio
            )
            UiHelper.setConstraintDimensionRatio(
                normalLayout?.parent as ConstraintLayout,
                com.ai.addxvideo.R.id.normal_layout,
                ratio
            )
        }
    }

    open fun setFullScreenRatio() {
        var ratio = DeviceUtil.getDeviceScreenRatio(dataSourceBean!!)
//        UiHelper.setConstraintDimensionRatio(
//            this,
//            R.id.rl_video_all_container,
//            ratio
//        )
        UiHelper.setConstraintDimensionRatio(
            renderContainer?.parent as ConstraintLayout,
            com.ai.addxvideo.R.id.surface_container,
            ratio
        )
        UiHelper.setConstraintDimensionRatio(
            renderContainer?.parent as ConstraintLayout,
            com.ai.addxvideo.R.id.thumbImage,
            ratio
        )
        LogUtils.w(TAG, "setFullscreenRatio ,${renderView?.layoutParams?.width} : ${renderView?.layoutParams?.height} ---sn:${dataSourceBean!!.serialNumber}")
    }

    val fullLayoutViewGroup by lazy { View.inflate(context, fullLayoutId(), null) }
    val normalLayoutViewGroup by lazy { View.inflate(context, normalLayoutId(), null) }

    @SuppressLint("ClickableViewAccessibility")
    private fun reloadLayout(context: Context?) {
        removeAllViews()
        LogUtils.w(TAG, "reloadLayout---------------mIsFullScreen:$mIsFullScreen")
        mAddxVideoContentView = if (mIsFullScreen) fullLayoutViewGroup else normalLayoutViewGroup
        addView(mAddxVideoContentView)
        reloadErrorLayout()
        thumbImage = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.thumbImage)
        normalLayout = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.normal_layout)
        loadingLayout = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.loading)
        fullScreenBtn = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.fullscreen)
        fullScreenBtn?.setOnClickListener(this)
        startBtn = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.start)
        startBtn?.setOnClickListener(this)
        tvDownloadSpeed = findViewById(com.ai.addxvideo.R.id.tv_download_speed)
        renderContainer = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.surface_container)
        animShotView = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.screen_shot_anim)
        recordIcon = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.iv_record)
        recordTimeText = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.tv_record_time)
        if (mIsFullScreen) {
            soundBtn = mAddxVideoContentView.findViewById(com.ai.addxvideo.R.id.iv_sound)
            soundBtn?.setOnClickListener(this)
            renderView?.setZOrderOnTop(true)
            renderView?.setZOrderMediaOverlay(true)
            setFullScreenRatio()
        }else{
            soundBtn = normalSoundBtn
            renderView?.setZOrderOnTop(false)
            renderView?.setZOrderMediaOverlay(false)
            setNormalVideoRatio()
        }
        if (renderView == null) {
            if (iAddxPlayer is AddxVideoIjkPlayer) {
                LogUtils.d(TAG, "renderView--------AddxVideoIjkPlayer")
                renderView = AddxGLSurfaceView(A4xContext.getInstance().getmContext())
            } else {
                LogUtils.d(TAG, "renderView--------webrtc")
                renderView = A4xVideoViewRender(A4xContext.getInstance().getmContext())
                if (iAddxPlayer is IVideoPlayer) {
                    val render:A4xVideoViewRender = renderView as A4xVideoViewRender
                    render.init(object : A4xVideoViewRender.IVideoRenderEvent {
                        override fun onFirstFrameRendered() {
                            LogUtils.d(TAG, "=====first frame rendered")
                        }

                        override fun onFrameResolutionChanged(width: Int, height: Int, rotation: Int) {
                            LogUtils.d(TAG, "=====onFrameResolutionChanged, width=$width, height=$height, rotation=$rotation")
                        }
                    })
                }
                (renderView as A4xVideoViewRender).setDisableCropFrame(true)
            }
            renderView?.id = View.generateViewId()
            LogUtils.w(TAG, "onAddRenderView,${Integer.toHexString(renderView.hashCode())}---sn:${dataSourceBean!!.serialNumber}")
            renderContainer?.addView(renderView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            renderView?.setOnTouchListener(this)
        } else {
            LogUtils.w(TAG, "onAddRenderView,${Integer.toHexString(renderView.hashCode())}---sn:${dataSourceBean!!.serialNumber}")
            renderContainer?.addView(renderView, 0, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
//            setFullScreenRatio(renderView?.layoutParams!!)
        }

        onInitOrReloadUi(context)
        updateStateAndUI(currentState, currentOpt)
        updateSoundIcon(mute)
        setThumbImageByPlayState(true)
        refreshThumbImg()
    }

    internal open fun onInitOrReloadUi(context: Context?) {}


    open fun backToNormal() {
        LogUtils.w(TAG, "backToNormal---sn:${dataSourceBean!!.serialNumber}")
//        mNeedUpdateUiWhenStateNoChanged = true
//        var currentHopePlay = isPlaying() || isPrepareing()
//        if(currentHopePlay){
//            stopPlay()
//        }
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
//            mAddxVideoViewParent.addView(this, oldLayoutIndex, oldLayoutParams)
            mAddxVideoViewParent.addView(this, oldLayoutIndex)
        }

        if (mShowing) {
            removeCallbacks(mFadeOut)
            mShowing = false
            showAutoHideUI(DEFAULT_SHOW_TIME)
        }
        stopRecordVideo("back to normal ")
        onFullScreenStateChange(false)
//        if(currentHopePlay){
//            startPlay()
//        }
    }

    open fun startFullScreen(isReverse: Boolean) {
        LogUtils.w(TAG, "startFullScreen---sn:${dataSourceBean!!.serialNumber}")
//        var currentHopePlay = isPlaying() || isPrepareing()
//        if(currentHopePlay){
//            stopPlay()
//        }
        if(parent == null){
            LogUtils.e(TAG, "startFullScreen------parent == null")
            return
        }
        mIsFullScreen = true
        mSystemUiVisibility = activityContext.window.decorView.systemUiVisibility
//        oldLayoutParams = layoutParams
        mAddxVideoViewParent = parent as ViewGroup
        oldLayoutIndex = mAddxVideoViewParent.indexOfChild(this)
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
//        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val contentView = activityContext.findViewById(android.R.id.content) as ViewGroup
        for (index in 0 until contentView.childCount) {
            contentView.getChildAt(index).visibility = View.INVISIBLE
        }
        // now add player to root
//        contentView.addView(this, params)
        contentView.addView(this)
        if (mShowing) {
            removeCallbacks(mFadeOut)
            mShowing = false
            showAutoHideUI(DEFAULT_SHOW_TIME)
        }
        if (!(isPlaying() || isPrepareing()) && canTryPlay()) {
            startBtn?.callOnClick()
            LogUtils.e(TAG, "auto play ,before state=$currentState---sn:${dataSourceBean!!.serialNumber}")
        }
        if (isSavingRecording) {
            savingRecordLoading?.visibility = View.VISIBLE
        }
        onFullScreenStateChange(true)
        onResetRecordUi()
//        if(currentHopePlay){
//            startPlay()
//        }
    }

    fun canTryPlay(): Boolean{
        return (!dataSourceBean!!.isDeviceSleep || (!needShowSleepState() && dataSourceBean!!.isDeviceSleep)) && !dataSourceBean!!.isFirmwareUpdateing && !dataSourceBean!!.needForceOta() && !dataSourceBean!!.isShutDownLowPower && !dataSourceBean!!.isShutDownPressKey
    }

    protected open fun errorLayoutId(): Int {
        return if (mIsFullScreen) {
            com.ai.addxvideo.R.layout.live_plager_full_error_page
        } else com.ai.addxvideo.R.layout.live_plager_no_full_error_default_page
    }

    fun startPlayWithNetToastandRecord(from: String){
        createRecordClick(from)
        showNetWorkToast()
        startPlay()
    }

    fun onClickErrorRetry(view: View) {
        LogUtils.d(TAG, "errorLayout?.setOnClickListener----------sn:${dataSourceBean!!.serialNumber}")
        if (!AccountManager.getInstance().isLogin) return
        if(view.id == com.ai.addxvideo.R.id.tv_error_btn){
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
                    ToastUtils.showShort(com.ai.addxvideo.R.string.device_no_access.resConfig().configWith(DeviceUtil.getDeviceCategray(dataSourceBean)))
                }
                else -> {
                    startPlayWithNetToastandRecord("retry_btn_clickid_")
                    if (resources.getString(com.ai.addxvideo.R.string.reconnect) == tvUnderLineErrorBtn?.text) {
                        reportLiveReconnectClickEvent()
                    } else {
                        reportLiveClickEvent(PlayerErrorState.getErrorMsg(currentOpt))
                    }
                }
            }
        }else if(view.id == com.ai.addxvideo.R.id.tv_error_btn_left){
            updateStateAndUI(CURRENT_STATE_NORMAL, currentOpt)
            mIsClickedNoNeedOtaUpdateMap.put(dataSourceBean?.serialNumber!!, true)
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
                        copyBean(baseResponse.data)
                        resetStateAndUI(false, false)
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
        tvErrorButtonLeft = inflate.findViewById(com.ai.addxvideo.R.id.tv_error_btn_left)
        tvUnderLineErrorBtn = inflate.findViewById(com.ai.addxvideo.R.id.tv_underline_error_btn)
//        ivErrorHelp = inflate.findViewById(com.ai.addxvideo.R.id.iv_error_help)
        ivErrorThumb = inflate.findViewById(com.ai.addxvideo.R.id.iv_error_thumb)
        ivErrorExit?.setOnClickListener { backToNormal() }
        tvErrorButton?.setOnClickListener { view -> onClickErrorRetry(view) }
        tvErrorButtonLeft?.setOnClickListener { view -> onClickErrorRetry(view) }
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
            .setLeftClickListener {
            }
            .setRightClickListener {
//                AddxMonitor.getInstance(A4xContext.getInstance().getmContext())
//                    .uploadLastDayLog { fileName, ret ->
//                        if (ret == 1) {
//                            ToastUtils.showShort(R.string.uploaded_success)
//                        } else if(ret == -1){
//                            ToastUtils.showShort(R.string.uploaded_fail)
//                        }else{
//                            ToastUtils.showShort("uploading log")
//                        }
//                    }
            }.show()
    }

    open fun updateSoundIcon(mute: Boolean) {
        LogUtils.w(TAG, "updateSoundIcon--dataSourceBean?.liveAudioToggleOn：${dataSourceBean?.liveAudioToggleOn}--" + (soundBtn == null).toString() + "--" + mute.toString()+"---sn:${dataSourceBean!!.serialNumber}")
        if(mIsFullScreen) {
            soundBtn?.setImageResource(if (mute) com.ai.addxvideo.R.mipmap.live_last_sound_disable else com.ai.addxvideo.R.mipmap.live_last_sound_enable)
            normalSoundBtn?.setImageResource(if (mute) com.ai.addxvideo.R.mipmap.live_last_sound_disable else com.ai.addxvideo.R.mipmap.live_last_sound_enable)
        }else{
            soundBtn?.setImageResource(if (mute) com.ai.addxvideo.R.mipmap.voice_black_notalk else com.ai.addxvideo.R.mipmap.voice_black_talk)
            normalSoundBtn?.setImageResource(if (mute) com.ai.addxvideo.R.mipmap.voice_black_notalk else com.ai.addxvideo.R.mipmap.voice_black_talk)
        }
        if(dataSourceBean?.liveAudioToggleOn == false){
            soundBtn?.alpha = 0.3f
            normalSoundBtn?.alpha = 0.3f
        }else{
            soundBtn?.alpha = 1.0f
            normalSoundBtn?.alpha = 1.0f
        }
    }

    fun updateLiveAudioToggleOn(enable: Boolean){
        dataSourceBean?.liveAudioToggleOn = enable
        updateSoundIcon(mute)
    }

    open fun updateStateAndUI(state: Int, opt: Int) {
//        if (!checkPassState(state, opt)) {
//            LogUtils.d(TAG, "checkPassState false , state = $state  opt = $opt---sn:${dataSourceBean!!.serialNumber}")
//            return
//        }
        LogUtils.d(TAG, "updateStateAndUI")
        mOldState = currentState
        mOldOpt = currentOpt
        currentOpt = opt
        currentState = state
//        if (currentState == mOldState && !mNeedUpdateUiWhenStateNoChanged) return
        LogUtils.d(TAG, "updateStateAndUI--oldState=$mOldState  currentState=$state oldOpt===$mOldOpt currentOpt===$opt---sn:${dataSourceBean!!.serialNumber}")
        post {
//            callBackViewState(state, mOldState)
            onPlayStateChanged(currentState, mOldState)
            when (currentState) {
                CURRENT_STATE_PREPAREING -> {
                }
                CURRENT_STATE_PLAYING -> {
                }
                CURRENT_STATE_ERROR, CURRENT_STATE_PAUSE, CURRENT_STATE_NORMAL -> {
                    mVideoCallBack?.onStopPlay()
                }
            }
            hideNavKey()
            updateUi(mOldState, state, mOldOpt, opt)
            if (currentState != CURRENT_STATE_PLAYING && currentState != CURRENT_STATE_PREPAREING && mOldState != currentState) {
                setThumbImageByPlayState()
            }
        }
    }

//    private fun checkPassState(state: Int, opt: Int): Boolean {
//        if (state == CURRENT_STATE_ERROR && opt != PlayerErrorState.ERROR_PHONE_NO_INTERNET && !NetworkUtils.isConnected(A4xContext.getInstance().getmContext())) {//check
//            updateStateAndUI(state, PlayerErrorState.ERROR_PHONE_NO_INTERNET)
//            return false
//        }
//        return true
//    }

    private fun updateUi(oldUiState: Int, newUiState: Int, oldOption: Int, newOption: Int) {
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
            CURRENT_STATE_PAUSE -> {
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
                PlayerErrorState.ERROR_PHONE_NO_AP_CONNECT,
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

    open fun changeUIToConnecting() {
        LogUtils.w(TAG, "-----changeUIToConnecting, mIsFullScreen= $mIsFullScreen---sn:${dataSourceBean?.serialNumber}")
        startBtn?.visibility = View.INVISIBLE
        errorLayout?.visibility = View.INVISIBLE
        loadingLayout?.visibility = View.VISIBLE
        thumbImage?.visibility = View.VISIBLE
    }

    open fun changeUIToIdle() {
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
            com.ai.addxvideo.R.string.sleep_end_time_atsteam.resConfig().appendDevice().append(pairTime.first).append(pairTime.second).build()
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
                        tvErrorTips?.text = it.plus("\n\n" + com.ai.addxvideo.R.string.admin_wakeup_camera.resConfig().configDevice())
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
                if (dataSourceBean?.adminId == AccountManager.getInstance().userId) {//管理员
                    if (!needForceOta!!) {
                        setErrorInfo(
                            com.ai.addxvideo.R.string.fireware_need_update_tips, com.ai.addxvideo.R.mipmap.ic_video_device_upgrade,
                            errorBtnText = com.ai.addxvideo.R.string.update, errorBtnVisible = true,
                            underlineErrorBtnText = com.ai.addxvideo.R.string.firmware_update_skip, underlineErrorBtnVisible = true, underLineErrorBtnColor = com.ai.addxvideo.R.color.theme_color, errorBtnLeftVisible = true)
                    } else {
                        setErrorInfo(com.ai.addxvideo.R.string.fireware_need_update_tips, com.ai.addxvideo.R.mipmap.ic_video_device_upgrade, errorBtnText = com.ai.addxvideo.R.string.update, errorBtnVisible = true, underlineErrorBtnVisible = false)
                    }
                } else {
                    if (!needForceOta!!) {
                        setErrorInfo(
                            com.ai.addxvideo.R.string.forck_update_share, com.ai.addxvideo.R.mipmap.ic_video_device_upgrade,
                            errorBtnText = com.ai.addxvideo.R.string.update, errorBtnVisible = false,
                            underlineErrorBtnText = com.ai.addxvideo.R.string.firmware_update_skip, underlineErrorBtnVisible = false, underLineErrorBtnColor = com.ai.addxvideo.R.color.theme_color, errorBtnLeftVisible = true)
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
                LogUtils.w(TAG, "AddxWebRtc------setErrorInfo-----------ERROR_DEVICE_MAX_CONNECT_LIMIT-------sn:${dataSourceBean!!.serialNumber}")
                setErrorInfo(com.ai.addxvideo.R.string.server_error, com.ai.addxvideo.R.mipmap.live_exception)
//                changeUIToIdle()
//                ToastUtils.showShort(R.string.live_viewers_limit)
            }
            PlayerErrorState.ERROR_PHONE_NO_AP_CONNECT -> {
                LogUtils.w(TAG, "AddxWebRtc------setErrorInfo-----------ERROR_PHONE_NO_AP_CONNECT-------sn:${dataSourceBean!!.serialNumber}")
                setErrorInfo(com.ai.addxvideo.R.string.home_notconnect_hotspot, com.ai.addxvideo.R.mipmap.live_exception, flagVisible=false, underlineErrorBtnText = com.ai.addxvideo.R.string.home_viewdevice)
            }
            else -> {
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
                     underLineErrorBtnColor: Int? = com.ai.addxvideo.R.color.theme_color,
                     errorBtnLeftText: Int? = com.ai.addxvideo.R.string.later,
                     errorBtnLeftVisible: Boolean? = false) {
        flagRes?.let { ivErrorFlag?.setImageResource(it) }
        flagVisible?.let { ivErrorFlag?.visibility = if (it) View.VISIBLE else View.GONE }
        if(errorMsg == com.ai.addxvideo.R.string.fireware_need_update_tips){
            tvErrorTips?.text = activityContext.getString(errorMsg, dataSourceBean?.newestFirmwareId)
        }else if(errorMsg == com.ai.addxvideo.R.string.forck_update_share){
            if(dataSourceBean?.needForceOta()!!){
                tvErrorTips?.text = activityContext.getString(errorMsg, dataSourceBean?.newestFirmwareId).plus(
                    com.ai.addxvideo.R.string.unavailable_before_upgrade.resConfig().configWith(DeviceUtil.getDeviceCategray(dataSourceBean)))
            }else{
                tvErrorTips?.text = activityContext.getString(errorMsg, dataSourceBean?.newestFirmwareId)
            }
        }else if(errorMsg == com.ai.addxvideo.R.string.low_power || errorMsg == com.ai.addxvideo.R.string.turned_off){
            LogUtils.d(TAG, "dataSourceBean?.getOfflineTime()-----errorMsg == R.string.low_power:${errorMsg == com.ai.addxvideo.R.string.low_power}----getOfflineTime：${dataSourceBean?.getOfflineTime()}")
            tvErrorTips?.text = errorMsg.resConfig().configWith(DeviceUtil.getDeviceCategray(dataSourceBean)) + "\n" +resources.getString(
                com.ai.addxvideo.R.string.off_time, if (dataSourceBean?.getOfflineTime() == null) "" else TimeUtils.formatYearSecondFriendly(dataSourceBean?.getOfflineTime()!!.toLong() * 1000))
        }else if(errorMsg == com.ai.addxvideo.R.string.camera_sleep || errorMsg == com.ai.addxvideo.R.string.camera_poor_network_short){
            tvErrorTips?.text = errorMsg.resConfig().configWith(DeviceUtil.getDeviceCategray(dataSourceBean))
        }else if(errorMsg == com.ai.addxvideo.R.string.camera_poor_network){
            tvErrorTips?.text = errorMsg.resConfig().append(DeviceUtil.getDeviceCategray(dataSourceBean)).configWith(DeviceUtil.getDeviceCategray(dataSourceBean))
        }else{
            tvErrorTips?.setText(errorMsg)
        }

        errorBtnVisible?.let { tvErrorButton?.visibility = if (errorBtnVisible && (mIsClickedNoNeedOtaUpdateMap.get(dataSourceBean?.serialNumber) == null || !mIsClickedNoNeedOtaUpdateMap.get(dataSourceBean?.serialNumber)!!)) View.VISIBLE else View.GONE }
        errorBtnText?.let { tvErrorButton?.setText(it) }

        tvErrorButtonLeft?.visibility = if(errorBtnLeftVisible == true && (mIsClickedNoNeedOtaUpdateMap.get(dataSourceBean?.serialNumber) == null || !mIsClickedNoNeedOtaUpdateMap.get(dataSourceBean?.serialNumber)!!))  View.VISIBLE else View.GONE

        underlineErrorBtnText?.let { tvUnderLineErrorBtn?.setText(it) }
        if(underlineErrorBtnText == com.ai.addxvideo.R.string.firmware_update_skip){
            tvUnderLineErrorBtn?.visibility = if(underlineErrorBtnVisible == false || mIsClickedNoNeedOtaUpdateMap.containsKey(dataSourceBean?.serialNumber) ||  dataSourceBean?.userIgnoreOta() == true || dataSourceBean?.adminId != AccountManager.getInstance().userId) View.GONE else View.VISIBLE
        }else{
            underlineErrorBtnVisible?.let { tvUnderLineErrorBtn?.visibility = if (it) View.VISIBLE else View.GONE }
        }
        underLineErrorBtnColor?.let { tvUnderLineErrorBtn?.setTextColor(activityContext.resources.getColor(underLineErrorBtnColor)) }
        fullScreenBtn?.visibility = View.INVISIBLE
    }

    fun hideNavKey() {
        if (mIsFullScreen) {
            CommonUtil.hideNavKey(activityContext)
        }
    }

    open fun getThumbPath(sn: String): String {
        return DirManager.getInstance().getCoverPath(sn)
    }

    open fun onMicPremissionSuccess(){

    }

    fun requestMicPermission() {
        A4xPermissionHelper.requestMicPermission(activityContext) { step, result ->
            LogUtils.df(TAG, "permission state %s", result.name)
            hideNavKey()
            if (result == PageStep.PageStepResult.Success) {
                mVideoCallBack?.onGetMicPremissionSuccess()
                onMicPremissionSuccess()
            }
        }
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
                            hideAutoHideUI()
                        } else {
                            mShowStartTimeSpan = System.currentTimeMillis()
                            showAutoHideUI(DEFAULT_SHOW_TIME) // start timeout
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
    protected open fun showAutoHideUI(timeout: Long) {
        hideNavKey()
    }

    protected open fun hideAutoHideUI() {
        hideNavKey()
    }

    override fun onMicFrame(data: ByteArray?) {

    }

    open fun onClickUnderlineErrorButton(tip: TextView?) {
        LogUtils.d(TAG, "onClickUnderlineErrorButton----------sn:${dataSourceBean!!.serialNumber}")
        when (currentOpt) {
            PlayerErrorState.ERROR_DEVICE_AUTH_LIMITATION,
            PlayerErrorState.ERROR_DEVICE_NO_ACCESS -> {}//checkDeviceExit(1)
            PlayerErrorState.ERROR_DEVICE_SLEEP -> {return
            }//sleep click do noting
            PlayerErrorState.ERROR_DEVICE_NEED_OTA ->{
                updateStateAndUI(CURRENT_STATE_NORMAL, currentOpt)
                if(dataSourceBean?.suggestOta() == true){
                    var snEntry = SerialNoEntry(dataSourceBean!!.serialNumber, false)
                    ApiClient.getInstance().ignoreOta(snEntry)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : BaseSubscriber<BaseResponse>() {
                            override fun doOnNext(t: BaseResponse) {
                                if (t.result < Const.ResponseCode.CODE_OK) {
                                    LogUtils.d(TAG, "ignoreOta--fail")
                                } else {
                                    mIsClickedNoNeedOtaUpdateMap.put(dataSourceBean?.serialNumber!!, true)
                                    LogUtils.d(TAG, "ignoreOta--success")
                                }
                            }

                            override fun doOnError(e: Throwable) {
                                LogUtils.d(TAG, "ignoreOta--fail net error")
                            }
                        })
                }
                return
            }
            else -> {
                if(APDeviceManager.INSTANCE.isApDevice(dataSourceBean)){
                    if(LocalWebSocketClient.INSTANCE.isLogined(dataSourceBean?.userSn)){
                        LogUtils.e(TAG, "aplist---isLogined---")
                        val isToPlay = mVideoCallBack?.onClickUnderline(tip, dataSourceBean!!)
                        if(isToPlay == true){
                            startPlayWithNetToastandRecord("underline_retry_btn_clickid_")
                        }
                    }else{
                        LogUtils.e(TAG, "aplist---isLogined------not isLogined")
                        mVideoCallBack?.toConnectApDevice(dataSourceBean?.userSn!!)
                    }
                }else{
                    val isToPlay = mVideoCallBack?.onClickUnderline(tip, dataSourceBean!!)
                    if(isToPlay == true){
                        startPlayWithNetToastandRecord("underline_retry_btn_clickid_")
                    }
                }
            }
        }
        when (tvUnderLineErrorBtn?.text) {
            resources.getString(com.ai.addxvideo.R.string.reconnect) -> {
                reportLiveReconnectClickEvent()
            }
            resources.getString(com.ai.addxvideo.R.string.refresh) -> {
                mVideoCallBack!!.onClickRefresh(tip)
            }
            else -> {
                reportLiveClickEvent(PlayerErrorState.getErrorMsg(currentOpt))
            }
        }
    }

    open fun onClickErrorTips(tip: TextView?) {
        LogUtils.d(TAG, "onClickErrorTips base----------sn:${dataSourceBean!!.serialNumber}")
        createRecordClick("clicktip_btn_clickid_")
    }

    fun checkDeviceExit() {
        if(APDeviceManager.INSTANCE.isApDevice(dataSourceBean)){
//            currentOpt = PlayerErrorState.ERROR_PHONE_NO_AP_CONNECT
            resetStateAndUI(false, true)
        }else{
            val observable = ApiClient.getInstance().getSingleDevice(SerialNoEntry(dataSourceBean?.serialNumber))
            observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : HttpSubscriber<GetSingleDeviceResponse>() {
                    override fun doOnNext(t: GetSingleDeviceResponse) {
                        if (t.result == Const.ResponseCode.DEVICE_NO_ACCESS || t.result == Const.ResponseCode.DEVICE_1_NO_ACCESS || t.result == Const.ResponseCode.DEVICE_2_NO_ACCESS) {
                            currentOpt = PlayerErrorState.ERROR_DEVICE_NO_ACCESS
                            updateStateAndUI(CURRENT_STATE_ERROR, currentOpt)
                        } else if (t.result >= Const.ResponseCode.CODE_OK) {
                            copyBean(t.data)
                            LogUtils.d(TAG, "resetStateAndUI====doOnNext---sn:${dataSourceBean!!.serialNumber}")
                            resetStateAndUI(false, true)
                        }
                    }

                    override fun onError(e: Throwable?) {
                        super.onError(e)
                        LogUtils.d(TAG, "resetStateAndUI====onError=---sn:${dataSourceBean!!.serialNumber}")
                        LogUtils.d(TAG, "getSingleDevice====onError=---sn:${dataSourceBean!!.serialNumber}")
                        updateStateAndUI(CURRENT_STATE_ERROR, currentOpt)
                    }
                })
        }
    }

    private fun copyBean(deviceBean: DeviceBean){
        val protocal = dataSourceBean?.deviceModel?.streamProtocol
        dataSourceBean?.copy(deviceBean)
        dataSourceBean?.deviceModel?.streamProtocol = protocal
    }

    // 具体操作记录 stopway ，等上报点以后清空stopway
    var countlyLiveStopWay = ""

    open fun reportLiveClickEvent(type: String){
        LogUtils.d(TAG, "reportLiveClickEvent======---sn:${dataSourceBean!!.serialNumber}")
        val datas = HashMap<String, Any?>()
//        val playerStat: PlayerStatsInfo? = iAddxPlayer?.playerStatInfo
        var mLiveId: String? = ""
        //todo yafei 弃用
//        if(iAddxPlayer is WebrtcPlayerWrap){
//            mLiveId = (iAddxPlayer as WebrtcPlayerWrap).getConnection()?.getIotServiceInfo()?.data?.traceId
//        }
        datas["live_id"] = mLiveId
        datas["connect_device"] = dataSourceBean?.serialNumber
        datas["button_type"] = type
        TrackManager.get().apply {
            reportEvent(getSegmentation(TrackManager.EventPair.LIVE_CLICK).apply {
                putAll(datas)
            })
        }
    }
    open fun reportLiveReconnectClickEvent(){
        LogUtils.d(TAG, "reportLiveReconnectClickEvent======---sn:${dataSourceBean!!.serialNumber}")
        val datas = HashMap<String, Any?>()
//        val playerStat: PlayerStatsInfo? = iAddxPlayer?.playerStatInfo
        var mLiveId: String? = ""
        //todo yafei 弃用
//        if(iAddxPlayer is WebrtcPlayerWrap){
//            mLiveId = (iAddxPlayer as WebrtcPlayerWrap).getConnection()?.getIotServiceInfo()?.data?.traceId
//        }
        datas["live_id"] = mLiveId
        datas["connect_device"] = dataSourceBean?.serialNumber
        datas["error_code"] = currentOpt
        datas["error_msg"] = PlayerErrorState.getErrorMsg(currentOpt)
        TrackManager.get().apply {
            reportEvent(getSegmentation(TrackManager.EventPair.LIVE_RECONNECT_CLICK).apply {
                putAll(datas)
            })
        }
    }

    fun getNewLiveReportData(isSeccess: Boolean, en: ReporLiveCommonEntry?, info: IA4xLogReportListener.ReportInfo?): ReporLiveCommonEntry? {
        var entry = en
        if(en == null){
            entry = ReporLiveCommonEntry()
        }

        entry?.serialNumber = dataSourceBean?.serialNumber
        entry?.liveId = info?.liveId
        var systemVersion: String? = dataSourceBean?.firmwareId
        if (!TextUtils.isEmpty(dataSourceBean?.displayGitSha)) {
            systemVersion = String.format(Locale.CHINA, "%s(%s)", systemVersion, dataSourceBean?.displayGitSha)
        }
        entry?.connect_device_version = systemVersion
        entry?.connect_device_model = if (TextUtils.isEmpty(dataSourceBean?.displayModelNo)) dataSourceBean?.modelNo else dataSourceBean?.displayModelNo
        entry?.connect_device = dataSourceBean?.serialNumber

        entry?.user_id = AccountManager.getInstance().userId.toString()
        entry?.country = A4xContext.getInstance().getCountryNo()
        entry?.version_name = "${PackageUtils.getAppVersionName(A4xContext.getInstance().getmContext())}(${BuildConfig.commitId})"
        entry?.version_code = PackageUtils.getAppVersionCode(A4xContext.getInstance().getmContext()).toString()
        entry?.network = if (NetworkUtils.isWifiConnected(A4xContext.getInstance().getmContext())) "wifi" else "4g"
        entry?.device_name = PhoneHardwareUtil.getSystemModel()
        entry?.error_msg = info?.error_msg
        entry?.stream_protocol = dataSourceBean?.deviceModel?.streamProtocol
        entry?.p2p_connection_type = info?.p2p_connection_type
        entry?.live_result = if (isSeccess) 1 else 0

        entry?.isClick = mIsUserClick
        entry?.clickid = mClickId
        entry?.live_player_type = if(mIsFullScreen) "full" else{ if(mIsSplit) "quad" else "half"}
        entry?.wait_time = info?.wait_time
        entry?.download_speeds = downloadStringBuilder.toString()
        entry?.current_is_fullscreen = mIsFullScreen.toString()
        entry?.connectLog = info?.connectLog
        entry?.connect_device_state = info?.devState
        entry?.cmd = info?.cmd
        return entry
    }

    fun getNewLiveInterruptWhenLoaddingReportData(info: IA4xLogReportListener.ReportInfo?): ReporLiveInterruptEntry {
        val data = ReporLiveInterruptEntry()
        data.endWay = mStopType
        getNewLiveReportData(false, data, info)
        return data
    }

    fun setThumbImageByPlayState(shouldSkipVisible: Boolean = false) {
        LogUtils.d(TAG, "setThumbImageByPlayState--------currentState:${currentState}---sn:${dataSourceBean!!.serialNumber}")
        val needBlur = when (currentState) {
            CURRENT_STATE_ERROR-> true
            else -> false
        }
        LogUtils.d(TAG, "setThumbImageByPlayState----------thumbimage visable:${thumbImage?.visibility == View.VISIBLE}---needBlur:${needBlur}---shouldSkipVisible:${shouldSkipVisible}----currentState:${currentState}-")
        thumbImage?.let {
            if (it.visibility == View.VISIBLE || shouldSkipVisible) {
                LogUtils.e(TAG, "setThumbImageByPlayState-----setThumbImageInternal----sn:${dataSourceBean!!.serialNumber}")
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

    open fun setThumbImageInternal(view: ImageView,needBlur: Boolean) {
        LogUtils.d(TAG, "setThumbImageInternal---mBitmap:${mBitmap == null}--sn:${dataSourceBean?.serialNumber}")
        dataSourceBean?.let {
            if (it.isDeviceSleep && needShowSleepState()){
                view.setImageResource(com.ai.addxvideo.R.drawable.live_sleep_bg)
                return
            }
        }
        if (needBlur) {
            view.setImageBitmap(BitmapUtils.rsCropAndBlur(context, mBitmap, 15, 3, if(dataSourceBean!!.isDoorbell) 4.0f/3 else 16.0f/9))
        } else {
            view.setImageBitmap(mBitmap)
        }
    }

    fun startThumbImgChangeAnimtor(path: String){
        var localBitmap = BitmapUtils.getBitmap(path)
        if(localBitmap != null){
            post{
                LogUtils.e(TAG, "------setThumbPath----sn:${dataSourceBean?.serialNumber}")
                var layers: Array<Drawable?> = arrayOfNulls(2)
                layers[0] = BitmapDrawable(mBitmap)
                layers[1] = BitmapDrawable(localBitmap)
                var transitionDrawable = TransitionDrawable (layers)
                thumbImage?.setImageDrawable(transitionDrawable)
                transitionDrawable.startTransition(500)
                postDelayed({mBitmap = localBitmap}, 500)
            }
        }
    }

    open fun updateThumbImageSource(){
//        LogUtils.d(TAG, "updateThumbImageSource-------mServerThumbTime:${mServerThumbTime}---mLocalThumbTime:${mLocalThumbTime}")
        mLocalThumbTime = VideoSharePreManager.getInstance().getThumbImgLastLocalFreshTime(dataSourceBean!!.serialNumber) / 1000
        mServerThumbTime = VideoSharePreManager.getInstance().getThumbImgLastServerFreshTime(dataSourceBean!!.serialNumber)
        if(mServerThumbTime > mLocalThumbTime){
//            LogUtils.d(TAG, "updateThumbImageSource------toRequestAndRefreshThumbImg-------sn:${dataSourceBean!!.serialNumber}")
            val imgPath = DownloadUtil.getThumbImgDir(activityContext) + MD5Util.md5(dataSourceBean?.serialNumber)+".jpg"
            val bitmap = BitmapUtils.cropRectBitmap(imgPath, if(dataSourceBean!!.isDoorbell) 4.0f/3 else 16.0f/9)
            if(bitmap != null){
                mBitmap = bitmap
                return
            }
        }
        if (mBitmap == null) {
            mBitmap = getCacheThumbImg(dataSourceBean!!.serialNumber)
            if (mBitmap == null) {
                mBitmap = BitmapFactory.decodeResource(resources, defaultThumbRid!!)
            }
        }
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

    var mStopWay: String? = null
    var mStopRecordCallback = object : A4xCommonEntity.IPlayerCallback {
        override fun onError(errCode: Int, errMsg: String) {
            LogUtils.d("stopRecordVideo------------error")
            ToastUtils.showShort(com.ai.addxvideo.R.string.record_failed)
            reportRecordEvent(false, "error", mStopWay)
        }

        override fun onComplete(response: Any?) {
            LogUtils.d("stopRecordVideo------------completed")
            try {
                isRecording = false
                isSavingRecording = false
                ToastUtils.showShort(com.ai.addxvideo.R.string.video_saved_to_ablum)
                FileUtils.syncVideoToAlbum(A4xContext.getInstance().getmContext(), videoSavePath, Date().time)
                reportRecordEvent(true, null, mStopWay)
            } catch (e: Exception) {
                e.printStackTrace()
                reportRecordEvent(false, e.message, mStopWay)
            }
            post {
                recordIcon?.isEnabled = true
                savingRecordLoading?.visibility = View.INVISIBLE
                onResetRecordUi()
            }
        }
    }
    protected fun stopRecordVideo(stopWay: String) {
        if (!isRecording || (recordIcon != null && !recordIcon?.isEnabled!!) || isSavingRecording) {
            LogUtils.d("stopRecordVideo------------没有正在录制或者保存还没有完成")
            return
        }
        recordIcon?.isEnabled = false
        isSavingRecording = true
        savingRecordLoading?.visibility = View.INVISIBLE
        mStopWay = stopWay
        iAddxPlayer?.stopRecord(mStopRecordCallback)
        recordTimeText?.visibility = View.INVISIBLE
    }

    private fun reportRecordEvent(success: Boolean, errorMsg: String?, stopWay: String?) {
        val segmentation = TrackManager.get().getSegmentation(TrackManager.EventPair.LIVE_RECORD_VIDEO)
        segmentation["live_player_way"] = if (mIsFullScreen) "fullscreen" else "halfscreen "
        segmentation["storage_space"] = "${availableSdcardSize}MB"
        segmentation["result"] = success
        if (errorMsg != null) {
            segmentation["error_msg"] = errorMsg
        }
        segmentation["stop_way"] = stopWay
        TrackManager.get().reportEvent(segmentation)
    }

    @SuppressLint("SetTextI18n")
    open fun onResetRecordUi() {
        recordIcon?.setImageResource(com.ai.addxvideo.R.mipmap.live_last_record)
        recordIcon?.setBackgroundResource(0)
        recordTimeText?.visibility = View.INVISIBLE
        if (recordCounterTask != null) {
            recordCounterTask!!.unsubscribe()
        }
        recordTimeText?.text = "00:00"
    }


    internal open fun startImgAnimAfterShotScreen(bitmap: Bitmap, toBottom: Int) {

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

    open fun refreshThumbImg(){
    }

    open fun updateStateAndUIWhenNetChange(isConnected: Boolean) {
//        if (isConnected) {
//            updateStateAndUI(CURRENT_STATE_NORMAL, currentOpt)
//        } else {
        //显示错误后过一会底层会回调loading导致显示错误后回继续loading
//            updateStateAndUI(CURRENT_STATE_ERROR, PlayerErrorState.ERROR_PHONE_NO_INTERNET)
//        }
    }
    open fun preApplyConnectWhenB(errorCode: Int){
        if(iAddxPlayer is WebrtcPlayerWrap && dataSourceBean?.isSupportBattery == false){
            if(dataSourceBean!!.isDeviceOffline || dataSourceBean!!.isDeviceSleep || dataSourceBean!!.isFirmwareUpdateing ||dataSourceBean!!.needForceOta()){
                return
            }
            iAddxPlayer?.setListener(this)
            (iAddxPlayer as WebrtcPlayerWrap).setKeepAlive()
        }
    }

    //原來viewcallback方法抽离
    internal open fun onPlayStateChanged(currentState: Int, oldState: Int){

    }
    internal open fun isSupportGuide() {
        mVideoCallBack?.isSupportGuide()
    }
    internal open fun onFullScreenStateChange(fullScreen: Boolean) {
        mVideoCallBack?.onFullScreenStateChange(fullScreen, dataSourceBean!!)
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
        LogUtils.d(TAG, "refreshVideoView-------")
        val protocal = dataSourceBean?.deviceModel?.streamProtocol
        dataSourceBean?.copyDeviceBeanAllData(bean)
        dataSourceBean?.deviceModel?.streamProtocol = protocal
        iAddxPlayer = AddxPlayerManager.getInstance().getPlayer(dataSourceBean)
        resetStateAndUI(false, false)
        thumbImage?.requestLayout()
        onInitOrReloadUi(context)
    }

    fun adminShowToOpenReceiveVoiceDialog(){
        CommonCornerDialog(context)
            .setTitleText(com.ai.addxvideo.R.string.unmute_failed_title)
            .setMessage(com.ai.addxvideo.R.string.unmute_failed_content)
            .setLeftText(com.ai.addxvideo.R.string.cancel)
            .setRightText(com.ai.addxvideo.R.string.go_open)
            .setRightClickListener{
//                if(mIsFullScreen){
//                    backToNormal()
//                }
                AddxFunJump.deviceVoiceSettingActivity(context, dataSourceBean, true)
            }
            .show()
    }

    fun showToOpenReceiveVoiceDialog(){
        CommonCornerDialog(context)
            .setMessage(com.ai.addxvideo.R.string.unmute_failed_content_guest)
            .setLeftTextInVisable()
            .setRightText(com.ai.addxvideo.R.string.ok)
            .show()
    }

    open fun getStopDelayReleaseTime(): Int{
        return 20//s
    }
}

