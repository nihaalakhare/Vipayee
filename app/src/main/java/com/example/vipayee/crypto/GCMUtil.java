package com.example.vipayee.crypto;

import static com.example.vipayee.AppConstants.AES_NO_PADDING;
import static com.example.vipayee.AppConstants.AES_PKCS7_PADDING;
import static com.example.vipayee.AppConstants.AES_SECRET_KEY_SPEC;
import static com.example.vipayee.AppConstants.CHAR_ENCODE_STANDARD;
import static com.example.vipayee.AppConstants.GCM_NO_PADDING;
import static com.example.vipayee.AppConstants.GCM_SECRET_KEY_SPEC;


import android.util.Base64;

import com.example.vipayee.AppConstants;

import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class GCMUtil {

    private static final int TAG_SIZE = 128;
    private static final byte[] FIXED_IV =
            AppConstants.IV.getBytes(StandardCharsets.UTF_8);

    public static String encrypt(String text, byte[] key ) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(key,AES_SECRET_KEY_SPEC);
        Cipher cipher = Cipher.getInstance(AES_PKCS7_PADDING);
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(text.getBytes());
        return Base64.encodeToString(encrypted,Base64.DEFAULT);
    }

    public static String decrypt(String base64DecodedBytes, byte[] key) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(key,AES_SECRET_KEY_SPEC);
        Cipher cipher = Cipher.getInstance(AES_NO_PADDING);
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decoded = Base64.decode(base64DecodedBytes, Base64.NO_WRAP);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, CHAR_ENCODE_STANDARD);
    }




//    public static String encrypt(String plain, byte[] key) throws Exception {
//
//        Cipher cipher = Cipher.getInstance(GCM_NO_PADDING);
//
//        GCMParameterSpec spec =
//                new GCMParameterSpec(TAG_SIZE, FIXED_IV);
//
//        cipher.init(
//                Cipher.ENCRYPT_MODE,
//                new SecretKeySpec(key, GCM_SECRET_KEY_SPEC),
//                spec
//        );
//
//        byte[] encrypted = cipher.doFinal(
//                plain.getBytes(StandardCharsets.UTF_8)
//        );
//
//        return Base64.encodeToString(encrypted, Base64.NO_WRAP);
//    }
//
//    public static String decrypt(String base64Cipher, byte[] key) throws Exception {
//
//        Cipher cipher = Cipher.getInstance(GCM_NO_PADDING);
//
//        GCMParameterSpec spec =
//                new GCMParameterSpec(TAG_SIZE, FIXED_IV);
//
//        cipher.init(
//                Cipher.DECRYPT_MODE,
//                new SecretKeySpec(key, GCM_SECRET_KEY_SPEC),
//                spec
//        );
//
//        byte[] decoded =
//                Base64.decode(base64Cipher, Base64.NO_WRAP);
//
//        return new String(
//                cipher.doFinal(decoded),
//                StandardCharsets.UTF_8
//        );
//    }
}
