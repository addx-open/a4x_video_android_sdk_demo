package com.addx.ai.demo

import android.os.Bundle
import android.util.Pair
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.addx.common.utils.ViewModelHelper
import com.ai.addxavlinkage.ADDXAvlinkage
import com.ai.addxavlinkage.viewmodel.AccountViewModel
import com.ai.addxbase.util.ToastUtils
import com.alibaba.fastjson.JSON

class AlexaActivity : BaseActivity() {


    private var code: String? = null
    private var clientId: String? = null
    private var responseType: String? = null
    private var state: String? = null
    private var scope: String? = null
    private var redirectUri: String? = null
    private var mAccountViewModel: AccountViewModel? = null

    override fun getResid(): Int {
        return R.layout.activity_alexa
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appLinkData = intent.data
        state = appLinkData?.getQueryParameter("state")
        scope = appLinkData?.getQueryParameter("scope")
        code = appLinkData?.getQueryParameter("code")
        clientId = appLinkData?.getQueryParameter("client_id")
        responseType = appLinkData?.getQueryParameter("response_type")
        redirectUri = appLinkData?.getQueryParameter("redirect_uri")
        mAccountViewModel = ViewModelHelper[AccountViewModel::class.java, this]
    }
    fun clicktoAuthorization(v: View?){
        ADDXAvlinkage.get().startAuthorization(this)
    }
    fun clicktoAlexa(v: View?) {
        ADDXAvlinkage.get().startAlexa(this)
    }
    fun clickAuthorization(v: View?) {
        showLoadingDialog()
        if(code==null){
            ToastUtils.showShort("需要先去 alexa 跳转过来")
        }
        val data= ADDXAvlinkage.get().authorization(code,clientId,scope,redirectUri,state,null,mAccountViewModel)
        data?.observe(this,{
            ToastUtils.showShort(it.second)
            dismissLoadingDialog()
        })
    }
    fun clickBind(v: View?) {
        showLoadingDialog()
        val data: MutableLiveData<Pair<AccountViewModel.State, String>>? = ADDXAvlinkage.get().bindPlatform("vicoo","amazon",null,mAccountViewModel!!)
        data?.observe(this, {
            dismissLoadingDialog()
            when (it.first) {
                AccountViewModel.State.ERROR -> {
                    //TODO 需要处理
                }
                AccountViewModel.State.SUCCESS -> {
                    val jsonObject = JSON.parseObject(it.second)
                    val jsonArray = jsonObject["loginParams"]
                    val jsonObject1 = JSON.parseObject(jsonArray.toString())
                    val jsonArray1 = jsonObject1["lwaLoginUrl"]
                    val jsonArray2 = jsonObject1["alexaAppSkillLinkUrl"]
                    //根据链接进行相应的跳转
                    ADDXAvlinkage.get().openAlexaAppToAppUrl(this,jsonArray2.toString(),jsonArray1.toString())
                }
            }
        })
    }
    fun clickBindResult(v: View?) {
        val data: MutableLiveData<Pair<AccountViewModel.State, String>> =
            ADDXAvlinkage.get().bindResult("amazon",code,state,null,mAccountViewModel!!)!!
        data.observe(this,{
            when (it.first) {
                AccountViewModel.State.ERROR -> {
                    Toast.makeText(this,"ERROR",Toast.LENGTH_LONG).show()
                }
                AccountViewModel.State.SUCCESS -> {
                    Toast.makeText(this,"SUCCESS",Toast.LENGTH_LONG).show()
                }
            }

        })
    }

}