package com.example.vipayee.manager;
import android.content.Context;
import android.util.Log;

import com.example.vipayee.AppConstants;
import com.example.vipayee.api.ApiClient;
import com.example.vipayee.crypto.GCMUtil;
import com.example.vipayee.utils.DeviceInfoUtil;
import com.example.vipayee.utils.HeaderUtil;

import org.json.JSONObject;

import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistrationManager {

    private static final String TAG = "REG_MANAGER";
    private final Context context;

    public interface RegistrationCallback {
        void onSuccess();
        void onError(String message);
    }

    public RegistrationManager(Context context) {
        this.context = context;
    }

    public void verifyMobile(String mobile,
                             String custId,
                             RegistrationCallback callback) {

        try {
            JSONObject payload = new JSONObject();
            payload.put("mobile_number", mobile);
            payload.put("custid", custId);

            Log.d(TAG, "📦 RAW PAYLOAD: " + payload);

            String encrypted = GCMUtil.encrypt(
                    payload.toString(),
                    AppConstants.getSecretKeyBytes()
            );

            JSONObject wrapper = new JSONObject();
            wrapper.put("d", encrypted);

            RequestBody body = RequestBody.create(
                    MediaType.parse("text/plain"),
                    wrapper.toString()
            );

            Map<String, String> headers =
                    HeaderUtil.baseHeaders(
                            DeviceInfoUtil.getDeviceInfo(context),
                            AppConstants.getApiKey()
                    );

            headers.put("action", "VERIFY_MOBILE_NUMBER");
            HeaderUtil.addSecurityHeaders(headers);

            ApiClient.create().verifyMobileNumber(headers, body)
                    .enqueue(new Callback<ResponseBody>() {

                        @Override
                        public void onResponse(Call<ResponseBody> call,
                                               Response<ResponseBody> response) {

                            if (!response.isSuccessful() ||
                                    response.body() == null) {
                                callback.onError("Server error");
                                return;
                            }

                            try {
                                String raw = response.body().string();
                                Log.d(TAG, "📨 RAW RESPONSE: " + raw);

                                JSONObject wrapper =
                                        new JSONObject(raw);

                                if (wrapper.optInt("response_code") != 1) {
                                    callback.onError("Verification failed");
                                    return;
                                }

                                String decrypted =
                                        GCMUtil.decrypt(
                                                wrapper.getString("response"),
                                                AppConstants.getSecretKeyBytes()
                                        );

                                Log.d(TAG, "🔓 DECRYPTED: " + decrypted);

                                JSONObject data =
                                        new JSONObject(decrypted);

                                if (data.optInt("response_code") == 1) {
                                    callback.onSuccess();
                                } else {
                                    callback.onError("Invalid mobile or customer ID");
                                }

                            } catch (Exception e) {
                                callback.onError("Parsing error");
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call,
                                              Throwable t) {
                            callback.onError("Network error");
                        }
                    });

        } catch (Exception e) {
            callback.onError("Unexpected error");
        }
    }
}
