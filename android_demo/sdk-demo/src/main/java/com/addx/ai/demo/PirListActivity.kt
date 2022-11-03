package com.addx.ai.demo

import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.addx.common.Const
import com.addx.common.ui.FlowLayout
import com.addx.common.utils.LogUtils
import com.addx.common.utils.SizeUtils
import com.ai.addx.model.RecordBean
import com.ai.addx.model.request.DeleteRecordResponse
import com.ai.addx.model.response.LibraryStatusResponse
import com.ai.addxbase.*
import com.ai.addxbase.adapter.base.BaseQuickAdapter
import com.ai.addxbase.adapter.base.BaseViewHolder
import com.ai.addxbase.util.TimeUtils
import com.ai.addxbase.util.ToastUtils
import com.ai.addxbase.view.GridSpacingItemDecoration
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.drawee.view.SimpleDraweeView
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * show pir list of today
 * 显示今日的pir列表
 */
class PirListActivity : BaseActivity() {
    private lateinit var videoList: RecyclerView

    override fun getResid(): Int {
        return R.layout.activity_pir_list
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoList = findViewById<RecyclerView>(R.id.pir_list).apply {
            layoutManager = GridLayoutManager(this@PirListActivity, 2)
            setBackgroundColor(Color.TRANSPARENT)
            addItemDecoration(GridSpacingItemDecoration(2, SizeUtils.dp2px(8f), true))
        }
        loadData()
    }

    var data: List<RecordBean>? = null
    private fun loadData() {
        showLoadingDialog()
        DeviceClicent.getInstance().queryVideoList(VideoConfig.Builder(
            currentDayStartSecond,
            currentDayStartSecond + TimeUnit.DAYS.toSeconds(
                1
            )
        ).withVideoIndex(0, 100)// up to 100
            .build(), object : IDeviceClient.ResultListener<List<RecordBean>> {

            override fun onResult(
                responseMessage: IDeviceClient.ResponseMessage,
                result: List<RecordBean>?
            ) {
                dismissLoadingDialog()
                if (responseMessage.responseCode == Const.ResponseCode.CODE_OK) {
                    if (result.isNullOrEmpty()) {
                        ToastUtils.showShort(R.string.no_data)
                    } else {
                        result?.let {
                            data = it
                            videoList.adapter = RvRecordAdapter(it)
                        }
                    }
                } else {
                    ToastUtils.showShort(R.string.error_unknown)
                }
            }
        })
    }

    private val currentDayStartSecond: Long
        get() {
            val cal = Calendar.getInstance()
            val firstDay = cal[Calendar.DAY_OF_MONTH]
            val year = cal[Calendar.YEAR]
            val month = cal[Calendar.MONTH]
            cal[year, month, firstDay, 0, 0] = 0
            LogUtils.e(TAG, "timeStartThisMonth", (cal.timeInMillis / 1000L).toString() + "")
            return cal.timeInMillis / 1000L
        }

    inner class RvRecordAdapter(data: List<RecordBean>) : BaseQuickAdapter<RecordBean, BaseViewHolder>(R.layout.item_video_record,data) {

        override fun convert(helper: BaseViewHolder, item: RecordBean) {
            val simpleDraweeView = helper.getView<SimpleDraweeView>(R.id.item_thumb)
            val controller: DraweeController = Fresco.newDraweeControllerBuilder().setUri(item.imageUrl)
                .setTapToRetryEnabled(true).setOldController(simpleDraweeView.controller).build()

            simpleDraweeView.controller = controller

            val timeStr = TimeUtils.formatHourMinuteFriendly(item.timestamp * 1000L)

            helper.setText(R.id.item_time, timeStr)

            val duration = item.period.toInt()
            val minute = duration / 60
            val sec = duration % 60
            if (duration == -1) {
                helper.setText(R.id.tv_duration, String.format(Locale.getDefault(), "-:-"))
            } else {
                helper.setText(R.id.tv_duration, String.format(Locale.getDefault(), "%02d:%02d", minute, sec))
            }
            helper.setText(R.id.item_device_name, item.deviceName)
            val ivFlag = helper.getView<ImageView>(R.id.item_flag)
            if (item.marked == 1) {
                ivFlag.setImageResource(R.mipmap.library_marked)
            } else {
                if (item.missing == 0) {
                    ivFlag.setImageDrawable(null)
                } else {
                    ivFlag.setImageResource(R.mipmap.library_missing)
                }
            }


            val tags = item.tags
            val view = helper.getView<FlowLayout>(R.id.tag_root)
            view.removeAllViews()
            if (!TextUtils.isEmpty(tags)) {
                try {
                    val tagItems = tags.split(",").toTypedArray()
                    val hashSet = HashSet<Int>()
                    for (itemTag in tagItems) {
                        if (tagInfos.containsKey(itemTag)) {
                            hashSet.add(tagInfos[itemTag]!!.icon)
                        }
                    }
                    hashSet.forEach {
                        view.addView(ImageView(this@PirListActivity).apply { setImageResource(it) })
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            helper.setChecked(R.id.cb_library_record, item.isSelect)


            // Change the selected of RecordBean when selected is changed
            helper.setOnCheckedChangeListener(R.id.cb_library_record
            ) { buttonView, isChecked -> item.isSelect = isChecked }

            helper.itemView.setOnClickListener(View.OnClickListener {
                showLoadingDialog()
//                IDeviceClient.getInstance().setVideoViewedInfo(item.id,
//                    object : IDeviceClient.ResultListener<Any> {
//                        override fun onResult(
//                            responseMessage: IDeviceClient.ResponseMessage,
//                            result: Any?
//                        ) {
//                            dismissLoadingDialog()
//                            if (responseMessage.responseCode == Const.ResponseCode.CODE_OK) {
//                                item.isViewed = true
//                                notifyItemChanged(mData.indexOf(item))
//                            } else {
//                                ToastUtils.showShort(R.string.network_error)
//                            }
//                        }
//                    })
            })
            helper.itemView.setOnLongClickListener {
                showLoadingDialog()
//                IDeviceClient.getInstance().setVideoMarkInfo(
//                    item.id,
//                    !item.isMarked,
//                    object : IDeviceClient.ResultListener<Any> {
//                        override fun onResult(
//                            responseMessage: IDeviceClient.ResponseMessage,
//                            result: Any?
//                        ) {
//                            dismissLoadingDialog()
//                            if (responseMessage.responseCode == Const.ResponseCode.CODE_OK) {
//                                item.isMarked = !item.isMarked
//                                notifyItemChanged(mData.indexOf(item))
//                            } else {
//                                ToastUtils.showShort(R.string.network_error)
//                            }
//                        }
//                    })
                true
            }
        }
    }

    companion object {
        private const val TAG = "PirListActivity"
    }

    fun clickToDelete(view: View) {
        val selected = ArrayList<Int>()
        data?.forEach {
//            if (it.isSelect) {
//                selected.add(it.id)
//            }
        }
        if (selected.isEmpty()) {
            ToastUtils.showShort(R.string.please_select)
        } else {
//            IDeviceClient.getInstance().deleteVideoRecord(selected,
//                object : IDeviceClient.ResultListener<DeleteRecordResponse.DataBean> {
//                    override fun onResult(
//                        responseMessage: IDeviceClient.ResponseMessage,
//                        result: DeleteRecordResponse.DataBean?
//                    ) {
//                        if (responseMessage.responseCode == Const.ResponseCode.CODE_OK) {
//                            ToastUtils.showShort(R.string.delete_success)
//                            loadData()
//                        } else {
//                            ToastUtils.showShort(R.string.network_error)
//                        }
//                    }
//                })
        }
    }

    fun clickToQueryVideo(view: View) {
        showLoadingDialog()
        DeviceClicent.getInstance().queryVideoStateWithTime(VideoConfig.Builder(
            (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(31))/1000,
            System.currentTimeMillis()/1000
        ).build(),
            object : IDeviceClient.ResultListener<LibraryStatusResponse.DataBean> {
                override fun onResult(
                    responseMessage: IDeviceClient.ResponseMessage,
                    result: LibraryStatusResponse.DataBean?
                ) {
                    dismissLoadingDialog()
                    if (responseMessage.responseCode != 0) {
                        ToastUtils.showShort(R.string.network_error)
                    } else {
                        val msg = if (result?.list.isNullOrEmpty()) {
                            "前31天 无视频"
                        } else {
                            "${result!!.list.size} 天有视频"
                        }
                        ToastUtils.showShort(msg)
                        LogUtils.d(TAG, "queryVideoStateWithTime---------");
                    }
                }
            }
        )
    }
}