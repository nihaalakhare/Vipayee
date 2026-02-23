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

    /* ================= PROFILE ================= */
    private static final String KEY_PROFILE_ID = "PROFILE_ID";
    private static final String KEY_CUST_NAME = "CUST_NAME";
    private static final String KEY_PRIMARY_ACC_NO = "PRIMARY_ACC_NO";
    private static final String KEY_PAN = "PAN";
    private static final String KEY_IFSC = "IFSC";
    private static final String KEY_CLIENT_TYPE = "CLIENT_TYPE";
    private static final String KEY_ORG_ELEMENT = "ORG_ELEMENT";

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

    /* =====================================================
       LOGIN DATA
    ===================================================== */

    public void saveLoginData(String mobile, String custId, String loginType) {
        prefs.edit()
                .putString(KEY_MOBILE, mobile)
                .putString(KEY_CUSTID, custId)
                .putString(KEY_LOGIN_TYPE, loginType)
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

    /* =====================================================
       REGISTRATION STATE
    ===================================================== */

    public void setRegistered(boolean value) {
        prefs.edit()
                .putBoolean(KEY_IS_REGISTERED, value)
                .apply();
    }

    public boolean isRegistered() {
        return prefs.getBoolean(KEY_IS_REGISTERED, false)
                && !getMobile().isEmpty()
                && !getCustId().isEmpty();
    }

    /* =====================================================
       AUTH SESSION
    ===================================================== */

    public void saveAuthToken(String token) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply();
    }

    public String getAuthToken() {
        return prefs.getString(KEY_AUTH_TOKEN, null);
    }

    public void setLoggedIn(boolean value) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply();
    }

    public boolean isLoggedIn() {
        String token = getAuthToken();
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
                && token != null
                && !token.trim().isEmpty();
    }

    /* =====================================================
       PROFILE DATA
    ===================================================== */

    public void saveProfileData(
            String profileId,
            String custName,
            String accNo,
            String pan,
            String ifsc,
            String clientType,
            String orgElement) {

        prefs.edit()
                .putString(KEY_PROFILE_ID, profileId)
                .putString(KEY_CUST_NAME, custName)
                .putString(KEY_PRIMARY_ACC_NO, accNo)
                .putString(KEY_PAN, pan)
                .putString(KEY_IFSC, ifsc)
                .putString(KEY_CLIENT_TYPE, clientType)
                .putString(KEY_ORG_ELEMENT, orgElement)
                .apply();
    }

    public String getProfileId() {
        return prefs.getString(KEY_PROFILE_ID, "");
    }

    public String getCustName() {
        return prefs.getString(KEY_CUST_NAME, "");
    }

    public String getPrimaryAccNo() {
        return prefs.getString(KEY_PRIMARY_ACC_NO, "");
    }

    public String getPan() {
        return prefs.getString(KEY_PAN, "");
    }

    public String getIfsc() {
        return prefs.getString(KEY_IFSC, "");
    }

    public String getClientType() {
        return prefs.getString(KEY_CLIENT_TYPE, "");
    }

    public String getOrgElement() {
        return prefs.getString(KEY_ORG_ELEMENT, "");
    }

    public void clearProfile() {
        prefs.edit()
                .remove(KEY_PROFILE_ID)
                .remove(KEY_CUST_NAME)
                .remove(KEY_PRIMARY_ACC_NO)
                .remove(KEY_PAN)
                .remove(KEY_IFSC)
                .remove(KEY_CLIENT_TYPE)
                .remove(KEY_ORG_ELEMENT)
                .apply();
    }

    /* =====================================================
       DEVICE
    ===================================================== */

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

    /* =====================================================
       UPI
    ===================================================== */

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

    public void clearUpiDetails() {
        prefs.edit()
                .remove(KEY_UPI_PA)
                .remove(KEY_UPI_PN)
                .apply();
    }

    /* =====================================================
       TRANSACTION
    ===================================================== */

    public void saveLastTxnAmount(String amount) {
        prefs.edit().putString(KEY_LAST_TXN_AMOUNT, amount).apply();
    }

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

    /* =====================================================
       FULL CLEAR (LOGOUT)
    ===================================================== */

    public void clear() {
        prefs.edit().clear().apply();
    }
}