package com.addx.ai.demo;

import android.annotation.SuppressLint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.addx.common.Const;
import com.addx.common.utils.SizeUtils;
import com.ai.addxbase.A4xContext;
import com.ai.addxbase.IDeviceClient;
import com.ai.addxbase.LanguageUtils;
import com.ai.addxbase.mvvm.BaseToolBarActivity;
import com.ai.addxbase.util.ToastUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import rx.Subscription;

/**
 * 这个类设置的语言对SDK中UI页面是生效的，对Demo中的语言无效。
 *
 * LanguageUtils.SUPPORT_LANGUAGE_LOCALE 代表的是SDK支持的语言
 */
public class LanguageSettingActivity extends BaseToolBarActivity {

    private Subscription subscribe;
    private IDeviceClient.RequestCancelble cancelble;
    private LinearLayout languagesView;

    @Override
    public String getToolBarTitle() {
        return getString(R.string.language);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_language_setting;
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void initView() {
        super.initView();
        languagesView = findViewById(R.id.languages_view);
        for (Locale locale:LanguageUtils.SUPPORT_LANGUAGE_LOCALE) {
            TextView textView = new TextView(this);
            textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SizeUtils.sp2px(40f)));
            textView.setBackgroundResource(R.color.theme_color);
            textView.setText(locale.getLanguage() + "  " + locale.getDisplayLanguage());
            textView.setGravity(Gravity.CENTER);
            textView.setTag(locale);
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateLanguage((Locale) v.getTag());
                }
            });
            languagesView.addView(textView);
        }
    }


    private void updateLanguage(Locale locale) {
        cancelble = A4xContext.getInstance().setLanguage(this, locale.getLanguage(), new IDeviceClient.ResultListener<Object>() {
            @Override
            public void onResult(@NotNull IDeviceClient.ResponseMessage responseMessage, @Nullable Object o) {
                if (responseMessage.getResponseCode() == Const.ResponseCode.CODE_OK) {
                    ToastUtils.showShort(R.string.toast_change_language);
                    finish();
                    setResult(RESULT_OK);
                } else {
                    ToastUtils.showShort(R.string.network_error);
                }
            }
        });
    }

    @Override
    protected void addListeners() {
        super.addListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cancelble != null) {
            cancelble.cancel();
        }
    }
}
