package com.example.vipayee.crypto;
import static com.example.vipayee.AppConstants.AES_NO_PADDING;
import static com.example.vipayee.AppConstants.AES_PKCS7_PADDING;
import static com.example.vipayee.AppConstants.AES_SECRET_KEY_SPEC;
import static com.example.vipayee.AppConstants.CHAR_ENCODE_STANDARD;


import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AESEnDecryption {

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
}

