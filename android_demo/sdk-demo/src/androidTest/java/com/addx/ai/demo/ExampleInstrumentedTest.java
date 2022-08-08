package com.addx.ai.demo;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("com.addx.ai.demo", appContext.getPackageName());
    }

    @Test
    public void sign() {
        String tenantId = "paastest";
        String accessKey = "viVqluNTSOF50sly757fF7";
        String secretKey = "N38Ywi2YR4qddOqXBjDRhQ";
        String baseUrl="https://api-staging-us.vicohome.io";
        String queryString="/open-api/auth/token";

        String unsignedUrl = baseUrl + "?" + queryString + "&accessKey=" + accessKey + "&timestamp=" + System.currentTimeMillis() / 1000;

        try {
            String signature = HmacSHA1(unsignedUrl,secretKey);
            String signedUrl = unsignedUrl + "&signature=" + signature;
            System.out.println("signedUrl = " + signedUrl);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.addx.ai.demo", appContext.getPackageName());
    }

    public static String HmacSHA1(String str, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        return sign(str, createMacForHmacSHA1(secret));
    }

    public static Mac createMacForHmacSHA1(String secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] key = Base64.getDecoder().decode(secretKey);
        // Get an HMAC-SHA1 signing key from the raw key bytes
        SecretKeySpec sha1Key = new SecretKeySpec(key, "HmacSHA1");
        // Get an HMAC-SHA1 Mac instance and initialize it with the HMAC-SHA1 key
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(sha1Key);
        return mac;
    }

    public static String sign(String str, Mac mac) {
        // compute the binary signature for the request
        byte[] sigBytes = mac.doFinal(str.getBytes());
        // base 64 encode the binary signature
        // Base64 is JDK 1.8 only - older versions may need to use Apache Commons or similar.
        String signature = Base64.getEncoder().encodeToString(sigBytes);
        // convert the signature to 'web safe' base 64
        signature = signature.replace('+', '-');
        signature = signature.replace('/', '_');
        return signature;
    }

}
