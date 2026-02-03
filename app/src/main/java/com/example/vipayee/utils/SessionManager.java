package com.example.vipayee.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF = "session_store";

    /* ================= LOGIN DATA ================= */
    private static final String KEY_MOBILE = "mobile";
    private static final String KEY_CUSTID = "custid";
    private static final String KEY_LOGIN_TYPE = "login_type";
    private static final String KEY_IS_REGISTERED = "IS_REGISTERED";

    /* ================= AUTH ================= */
    private static final String KEY_AUTH_TOKEN = "AUTH_TOKEN";
    private static final String KEY_IS_LOGGED_IN = "IS_LOGGED_IN";

    /* ================= DEVICE / SECURITY ================= */
    private static final String KEY_DEVICE_INFO = "DEVICE_INFO";
    private static final String KEY_LAST_LOGIN_TS = "LAST_LOGIN_TS";
    private static final String KEY_LAST_NONCE = "LAST_NONCE";

    /* ================= UPI ================= */
    private static final String KEY_UPI_PA = "UPI_PA";
    private static final String KEY_UPI_PN = "UPI_PN";

    /* ================= TRANSACTION ================= */
    private static final String KEY_LAST_TXN_AMOUNT = "LAST_TXN_AMOUNT";
    private static final String KEY_STAN = "STAN";
    private static final String KEY_CHANNEL_REF_NO = "CHANNEL_REF_NO";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    /* --------------------------------------------------
       LOGIN
    -------------------------------------------------- */

    public void saveLoginData(String mobile, String custId, String loginType) {
        prefs.edit()
                .putString(KEY_MOBILE, mobile)
                .putString(KEY_CUSTID, custId)
                .putString(KEY_LOGIN_TYPE, loginType)
                .putBoolean(KEY_IS_REGISTERED, true)
                .apply();
    }

    public String getMobile() {
        return prefs.getString(KEY_MOBILE, "");
    }

    public String getCustId() {
        return prefs.getString(KEY_CUSTID, "");
    }

    public String getLoginType() {
        return prefs.getString(KEY_LOGIN_TYPE, "0");
    }

    /* --------------------------------------------------
       AUTH
    -------------------------------------------------- */

    public void saveAuthToken(String token) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply();
    }

    public String getAuthToken() {
        return prefs.getString(KEY_AUTH_TOKEN, null);
    }
    public void clearUpiDetails() {
        prefs.edit()
                .remove(KEY_UPI_PA)
                .remove(KEY_UPI_PN)
                .apply();
    }
    public void clearLastTxnAmount() {
        prefs.edit()
                .remove(KEY_LAST_TXN_AMOUNT)
                .apply();
    }
    public boolean isRegistered() {
        return prefs.getBoolean(KEY_IS_REGISTERED, false)
                && !getMobile().isEmpty()
                && !getCustId().isEmpty();
    }

    public void setLoggedIn(boolean value) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply();
    }

    /** ✅ FIXED: prevents empty-token false login */
    public boolean isLoggedIn() {
        String token = getAuthToken();
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
                && token != null
                && !token.trim().isEmpty();
    }

    /* --------------------------------------------------
       DEVICE / SECURITY
    -------------------------------------------------- */

    public void saveDeviceInfo(String deviceInfo) {
        prefs.edit().putString(KEY_DEVICE_INFO, deviceInfo).apply();
    }

    public String getDeviceInfo() {
        return prefs.getString(KEY_DEVICE_INFO, "");
    }

    public void saveLastNonce(String nonce) {
        prefs.edit().putString(KEY_LAST_NONCE, nonce).apply();
    }

    public void saveLastLoginTimestamp(long timestamp) {
        prefs.edit().putLong(KEY_LAST_LOGIN_TS, timestamp).apply();
    }

    /* --------------------------------------------------
       UPI
    -------------------------------------------------- */

    public void saveUpiDetails(String pa, String pn) {
        prefs.edit()
                .putString(KEY_UPI_PA, pa)
                .putString(KEY_UPI_PN, pn)
                .apply();
    }

    public String getUpiPa() {
        return prefs.getString(KEY_UPI_PA, "");
    }

    public String getUpiPn() {
        return prefs.getString(KEY_UPI_PN, "");
    }

    /* --------------------------------------------------
       TRANSACTION
    -------------------------------------------------- */

    public void saveLastTxnAmount(String amount) {
        prefs.edit().putString(KEY_LAST_TXN_AMOUNT, amount).apply();
    }

    /** ✅ FIXED: null means “no active transaction” */
    public String getLastTxnAmount() {
        return prefs.getString(KEY_LAST_TXN_AMOUNT, null);
    }

    public void saveStanAndChannelRef(String stan, String channelRefNo) {
        prefs.edit()
                .putString(KEY_STAN, stan)
                .putString(KEY_CHANNEL_REF_NO, channelRefNo)
                .apply();
    }

    public String getStan() {
        return prefs.getString(KEY_STAN, "");
    }

    public String getChannelRefNo() {
        return prefs.getString(KEY_CHANNEL_REF_NO, "");
    }

    public void clearTransactionData() {
        prefs.edit()
                .remove(KEY_LAST_TXN_AMOUNT)
                .remove(KEY_STAN)
                .remove(KEY_CHANNEL_REF_NO)
                .apply();
    }

    /* --------------------------------------------------
       CLEAR
    -------------------------------------------------- */

    public void clear() {
        prefs.edit().clear().apply();
    }
}
