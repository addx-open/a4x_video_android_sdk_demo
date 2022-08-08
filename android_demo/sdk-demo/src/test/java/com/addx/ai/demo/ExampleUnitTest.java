package com.addx.ai.demo;

import android.content.Context;

import com.ai.addxnet.ApiClient;

import org.junit.Test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void sign() {

        // 1.生成签名URL
        String tenantId = "paastest";
        String accessKey = "viVqluNTSOF50sly757fF7";
        String secretKey = "N38Ywi2YR4qddOqXBjDRhQ";
        String baseUrl="https://api-staging-us.vicohome.io";
        String queryString="/open-api/auth/token";

        String unsignedUrl = baseUrl  + queryString + "?accessKey=" + accessKey + "&timestamp=" + System.currentTimeMillis() / 1000;

        try {
            String signature = DemoUtil.HmacSHA1(unsignedUrl,secretKey);
            String signedUrl = unsignedUrl + "&signature=" + signature;
            System.out.println("signedUrl = " + signedUrl);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        //2.根据签名URL注册账户（注册用户就是生成token）
        // 2.1使用post请求 请求url，https://api-staging-us.vicohome.io/open-api/auth/token?accessKey=viVqluNTSOF50sly757fF7&timestamp=1642672375&signature=lwfddACSqP9JOqWXdI-1eKEPr48=
        //其中signature 是通过上述步骤1获取
        //参数有很多必选项和可选项，我只列出了必选项。详情需要查看文档 https://docs.vicoo.tech/#/cloud/sign_compute?id=authentication
//     Body   {"tenantId": "paastest",
//          "userId": "a4x-test",  //其中userId是区分用户而使用的。在demo中我生成了几个账号，如果测试使用建议使用新的userId
//          "countryNo": "CN",
//          "language": "zh"}

    }
}

}