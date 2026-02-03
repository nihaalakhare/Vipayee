package com.example.vipayee.utils;

import android.content.Context;
import android.provider.Settings;

import com.example.vipayee.AppConstants;

import org.json.JSONObject;
public class DeviceInfoUtil {
    public static String getDeviceInfo(Context context) {
        try {
            JSONObject json = new JSONObject();

            String versionName = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;

            json.put("imei", "");
            json.put("icc_no", "");
            json.put("android_id", AppConstants.android_id);
            json.put("application_version", versionName);
            json.put("android_version", android.os.Build.VERSION.RELEASE);

            return json.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

}
