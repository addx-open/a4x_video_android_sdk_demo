package com.addx.ai.demo;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class DemoUtil {

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
