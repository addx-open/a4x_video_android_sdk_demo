package com.addx.ai.demo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.addx.common.permission.PermissionHelper;
import com.addx.common.utils.LogUtils;
import com.ai.addx.model.DeviceBean;
import com.ai.addxbase.A4xContext;
import com.ai.addxbase.ADDXBind;
import com.ai.addxbase.AddxNode;
import com.ai.addxbase.AddxVideoContextInitCallBack;
import com.ai.addxbase.permission.A4xPermissionHelper;
import com.ai.addxbase.util.ToastUtils;
import com.ai.addxsettings.ADDXSettings;
import com.ai.addxsettings.dialog.CommonBottomDialog;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class MainActivity extends BaseActivity {
    public static final String TAG = "MainActivity";
    private View mDemoListView;

    private static final int REQUEST_CODE_CHANGE_LANGUAGE = 10001;

    public static class AccountInfo {
        public boolean isProd;
        public String token;

        public AccountInfo(boolean isProd, String token) {
            this.isProd = isProd;
            this.token = token;
        }
    }
    public static HashMap<String, AccountInfo> mTokenTests = new HashMap<>();

    static {
        mTokenTests.put("a4x-test1", new AccountInfo(false, "Bearer eyJhbGciOiJIUzUxMiJ9.eyJ0aGlyZFVzZXJJZCI6ImE0eC10ZXN0MSIsImFjY291bnRJZCI6InBhYXN0ZXN0Iiwic2VlZCI6IjEyZDVhODNiMmIzNjRlYmM5ZjFmZGExZTI1ZGRhODYzIiwiZXhwIjoyNjQyNjcyNTc4LCJ1c2VySWQiOjEwMDA3NzF9.S_XjOkNrU7hlTFIsYfz3Idm4o9E08Vi_Isz1TDnneBa6IElvJ5iBM5Laj3W3GHNFX_lSk9J1SVWo54aWGMCy9w"));
        mTokenTests.put("a4x-test2", new AccountInfo(false, "Bearer eyJhbGciOiJIUzUxMiJ9.eyJ0aGlyZFVzZXJJZCI6ImE0eC10ZXN0MiIsImFjY291bnRJZCI6InBhYXN0ZXN0Iiwic2VlZCI6IjlmMDJmOTBiN2JjMjQyNTA5YWZiOWQ3ODA4N2NiNzZjIiwiZXhwIjoyNjQyNjcyNjExLCJ1c2VySWQiOjEwMDA3NzJ9.SXcb0PKvuTSjcl2snQ0BJgRjZ2yO8lNKvmiyiIYVcPCiZp7DJ0cUCLsgl6QLnNU60KCYL7i170hb2hL_4xFUTA"));
        mTokenTests.put("a4x-test3", new AccountInfo(false, "Bearer eyJhbGciOiJIUzUxMiJ9.eyJ0aGlyZFVzZXJJZCI6ImE0eC10ZXN0MyIsImFjY291bnRJZCI6InBhYXN0ZXN0Iiwic2VlZCI6ImIxYmVkNzlhNWNjNzQxMGFiNjUwZjYyN2QwNDQyNTJhIiwiZXhwIjoyNjQyNjcyNjI5LCJ1c2VySWQiOjEwMDA3NzN9.AEY7zjZQ4NlznhxrSafXTOH1ziI8v3ZjpVZtUctwfkXIycVlRxFCIS-BNI21AyGZOOGX3aCyWQLXDrwKM8KfcQ"));
        //上面的是staging环境，下面的是prod环境
        mTokenTests.put("a4x-prod1", new AccountInfo(true, "Bearer eyJhbGciOiJIUzUxMiJ9.eyJ0aGlyZFVzZXJJZCI6ImE0eC1wcm9kMSIsImFjY291bnRJZCI6InNka2RlbW8iLCJzZWVkIjoiNjNmYjNkMmU1ZGVjNDRjNTlhNjk2ZmZmYTU0NDE2YjIiLCJleHAiOjI2NDQ5MTIwNTAsInVzZXJJZCI6MTI3NTk5fQ.A98xaFTU-IdJ4jgzFINki-WqKGE35kzTD-ncHRz72IKfyOq0vWn5jHgoEezIDh8cfOmIMzUZUDalKHNEhwpttA"));
        mTokenTests.put("a4x-prod2", new AccountInfo(true, "Bearer eyJhbGciOiJIUzUxMiJ9.eyJ0aGlyZFVzZXJJZCI6ImE0eC1wcm9kMiIsImFjY291bnRJZCI6InNka2RlbW8iLCJzZWVkIjoiNDVjNDkzOTI3ODRiNDM0MWI1Y2UyMTM2MjMxMmQyZTQiLCJleHAiOjI2NDQ5MTIwODYsInVzZXJJZCI6MTI3NjAwfQ.wu8VJkdv8uU07HiXfVlvF7N2tL0AA1BE0nHCQrXYWKKB4SJFudWT_r3Xwm7LeV8ZszZXZAhEAgfIZfdZjYIn2Q"));
        mTokenTests.put("a4x-prod3", new AccountInfo(true, "Bearer eyJhbGciOiJIUzUxMiJ9.eyJ0aGlyZFVzZXJJZCI6ImE0eC1wcm9kMyIsImFjY291bnRJZCI6InNka2RlbW8iLCJzZWVkIjoiY2Q3ZjVkMWE1ZGM4NGU4MDljZjNmY2U4MTkyNWZiZTMiLCJleHAiOjI2NDQ5MTIxMDMsInVzZXJJZCI6MTI3NjAxfQ.XSMkpVAQ_Rk6j6uioHHN2SJwfA4ewlMnnnIXd9mx0278s7CWtLsxkWExdZWTPWwYX-1nPRgUCgMNFOolCWmGqg"));
    }

    /**
     * 此设置仅对SDK中的UI有效
     */
    public void clickToChangeLanguage(View view) {
        startActivityForResult(new Intent(this, LanguageSettingActivity.class), REQUEST_CODE_CHANGE_LANGUAGE);
    }

    public void clickToCheckShareInfo(View view) {
        ADDXSettings.getAndShowAllRequestQrDialog(this, new ADDXSettings.QueryShareRequestCallback() {
            @Override
            public void onStart() {
                ToastUtils.showShort("start check");
            }

            @Override
            public void onSuccess() {
                ToastUtils.showShort("success");
            }

            @Override
            public void onFailed() {
                ToastUtils.showShort("onfailed");
            }
        });
    }

    public static class Event {
        int type;

        public Event(int t) {
            type = t;
        }
    }

    public static final String LOGIN_OK = "LOGIN_OK";
    public static final String PLAY_OK = "PLAY_OK";

    @Override
    protected int getResid() {
        return R.layout.activity_demo_main;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDemoListView = findViewById(R.id.demolist);
        initSettingParams();
        ADDXBind.setLongClickQrListener(new ADDXBind.QrListener() {
            @Override
            public void getQrData(@org.jetbrains.annotations.Nullable String bitmapBase64Array) {
                LogUtils.d(TAG, "getQrData------bitmapBase64Array:%s", bitmapBase64Array);
            }
        });
    }


    public void clickSdcardPlayActivity(View v) {
        Intent devicelistintent = new Intent(this, SdcardPlayActivity.class);
        startActivity(devicelistintent);
    }

    /**
     * 初始化Setting SDK的配置,需要在进入Setting页面之前配置完成
     */
    private void initSettingParams() {
        ADDXSettings.Builder builder = new ADDXSettings.Builder().setActivityZoneClickCallback((v, deviceBean) -> {
            ToastUtils.showShort("click ActivityZone");
            return false;
        }).setSDCardClickCallback((v, deviceBean) -> {
            ToastUtils.showShort("click SDCard");
            return false;
        });
        if (Settings.userMoreSettings) {
            builder.ennableMoreSettings(new ADDXSettings.SettingsMoreClickListener() {
                @Override
                public void onSettingsMoreClick(@NotNull View v, @NotNull DeviceBean deviceBean) {
                    ToastUtils.showShort("click MoreSetting");
                }
            }, null);
        }
        if (Settings.disableMotionPageSettings) {
            builder.disableDurationAndResolution();
        }
        ADDXSettings.setFunctionBuilder(builder);
    }


    /**
     * Query the list of requests to bind my device by scanning the shared QR code of my device
     * 查询通过扫描我的设备二维码，绑定我设备的请求列表
     * <p>
     * 可以通过推送的方式监听，或者在其他合适的位置查询
     */
    public void clickQuerySharedList(View view) {
        ADDXSettings.getAndShowAllRequestQrDialog(this, null);//查询并处理请求
//        DeviceClicent.getInstance().queryAllSharedInfo(xxx);  仅查询请求列表
    }


    public void clickToLogout(View view) {
        if (DemoGlobal.isSDKInited) {
            DemoGlobal.isSDKInited = false;
            ToastUtils.showShort(R.string.logout_success);
            mDemoListView.setVisibility(View.INVISIBLE);
            A4xContext.getInstance().releaseA4xSdk();
        }
    }

    /**
     * 我们通过文档（https://docs.vicoo.tech/#/cloud/sign_compute?id=authentication）的描述，
     * 生成了可登录的账户(Global.INSTANCE.getMTokenTests())
     * 按照文档的描述，我们把生成的步骤写在了 ExampleUnitTest 中，
     * <p>
     * 建议自测的时候不要使用demo中的的token，通过文档中的描述 或者 ExampleUnitTest 自己生成新的token，确保账户(userId)的唯一性。
     * token是初始化SDK必须的参数。也是区分账户的参数。
     */
    public void onClickLogin(View v) {
        if (!DemoGlobal.isSDKInited) {
            new CommonBottomDialog.Builder(this).title("Choose User Id to login")
                    .displayedValues(mTokenTests.keySet().toArray(new String[3]))
                    .itemClick(new CommonBottomDialog.OnItemClickListener() {
                        @Override
                        public void onItemClick(int position, @NotNull String value) {
                            Log.d(TAG, "value = " + value);
                            initSDK(mTokenTests.get(value));
                            A4xPermissionHelper.requestStoragePermissions(MainActivity.this, null);
                        }
                    }).show();
        } else {
            ToastUtils.showShort(R.string.already_login);
        }
    }

    private void initSDK(AccountInfo accountInfo) {
        showLoadingDialog();
        A4xContext.getInstance().initA4xSdk(getApplicationContext(), accountInfo.isProd ? "sdkdemo" : "paastest", "zh", "CN", accountInfo.isProd ? AddxNode.PROD_NODE_US : AddxNode.STRAGE_NODE_US, accountInfo.token, new AddxVideoContextInitCallBack() {
            @Override
            public void success() {
                DemoGlobal.isSDKInited = true;
                showSuccessPage();
                dismissLoadingDialog();
            }

            @Override
            public void fail(int code, String message) {
                LogUtils.d(TAG, "sdk init failed : %s", message);
                showFailedPage();
                dismissLoadingDialog();
            }
        });
    }


    /**
     * show failed page when sdk init failed
     */
    private void showFailedPage() {
        ToastUtils.showShort(R.string.login_failed);
    }

    /**
     * show success page when sdk init failed
     * <p>
     * 成功的页面包含如下两条逻辑
     * <p>
     * 1.账号已绑定了设备
     * 直接使用Live SDK中的UI展示设备列表，并且仍然显示添加设备按钮
     * 2.账号未绑定设备，此时设备列表无设备
     * 展示添加设备的按钮，
     */
    private void showSuccessPage() {
        ToastUtils.showShort(R.string.login_sccessful_sdk);
        mDemoListView.setVisibility(View.VISIBLE);
    }

    /**
     * click event
     * Add friend`s device
     * 1.Enter The Scan Qr Code Page
     * 2.The QRCode is one of device in the device list page
     * 3.In order to add a friend`s device , you need to go to this page to scan the QR code  of the friend`s device
     * on the sharing page
     */
    private static final int REQUST_CODE_SCAN_QR = 11;

    public void clickAddFriendDevice(View v) {
        ADDXSettings.launchScanFriendQRPageForResult(this, REQUST_CODE_SCAN_QR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUST_CODE_SCAN_QR) {
            if (resultCode == RESULT_OK) {
                ToastUtils.showShort(R.string.shared_request_success);
            } else {
                ToastUtils.showShort(R.string.shared_request_failed);
            }
        } else if (requestCode == REQUEST_CODE_CHANGE_LANGUAGE) {
            if (resultCode == RESULT_OK) {
                recreate();//让语言设置生效
            }
        }
    }

    /**
     * click event
     * Enter device list page
     * 1.Show All devices you binded
     * 2.Include settings , Device Shares functions
     */
    public void clickShowDeviceList(View v) {
        Intent devicelistintent = new Intent(this, DeviceList.class);
        startActivity(devicelistintent);
    }

    /**
     * click event
     * Enter custom player device list page
     */
    public void clickCustomerPlayerDeviceList(View v) {
        Intent devicelistintent = new Intent(this, PlayerDeviceList.class);
        startActivity(devicelistintent);
    }

    /**
     * click event
     * Enter the device bind page
     * It provides a way to add devices to the device list
     */
    public void clickToAddDevice(View v) {
        ADDXBind.lanchBind(this, new ADDXBind.Builder()
                .withBindCallback(new ADDXBind.BindInterface() {
                    @Override
                    public void onBindCancel() {
                    }

                    @Override
                    public void onBindSccess(String sn) {
                        ToastUtils.showShort("bind success sn :" + sn);
                    }

                    @Override
                    public void onBindStart(String callBackUrl) {
                        ToastUtils.showShort("bind start callBackUrl :" + callBackUrl);
                    }
                }));
    }

    /**
     * click event
     * Enter the device video list page
     * It provides a way to add devices to the device list
     */
    public void clickShowVideoList(View v) {
        Intent devicelistintent = new Intent(this, PirListActivity.class);
        startActivity(devicelistintent);
    }

    public void clickBindAlexa(View v) {
        Intent devicelistintent = new Intent(this, AlexaActivity.class);
        startActivity(devicelistintent);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }
}
