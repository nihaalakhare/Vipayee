package com.example.vipayee.activities;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vipayee.AppConstants;
import com.example.vipayee.R;
import com.example.vipayee.api.ApiClient;
import com.example.vipayee.crypto.GCMUtil;
import com.example.vipayee.model.MiniStatementAdapter;
import com.example.vipayee.model.TransactionModel;
import com.example.vipayee.utils.BaseActivity;
import com.example.vipayee.utils.HeaderUtil;
import com.example.vipayee.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MiniStatementActivity extends BaseActivity {

    private static final String TAG = "MINI_STATEMENT";

    private RecyclerView rv;
    private TextView tvLoading;

    private final List<TransactionModel> list = new ArrayList<>();
    private final Handler handler = new Handler();
    private int dotCount = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_mini_statement);

        rv = findViewById(R.id.rvMiniStatement);
        tvLoading = findViewById(R.id.tvLoading);

        rv.setLayoutManager(new LinearLayoutManager(this));

        startLoadingAnimation();
        loadMiniStatement();
    }

    /* -------------------- Loading UI -------------------- */

    private void startLoadingAnimation() {
        tvLoading.setVisibility(View.VISIBLE);
        rv.setVisibility(View.GONE);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dotCount = (dotCount + 1) % 4;
                StringBuilder dots = new StringBuilder();
                for (int i = 0; i < dotCount; i++) dots.append(".");
                tvLoading.setText("Loading" + dots);
                handler.postDelayed(this, 500);
            }
        }, 500);
    }

    private void stopLoadingAnimation() {
        handler.removeCallbacksAndMessages(null);
        tvLoading.setVisibility(View.GONE);
        rv.setVisibility(View.VISIBLE);
    }

    /* -------------------- API Call -------------------- */

    private void loadMiniStatement() {
        try {
            SessionManager session = new SessionManager(this);

            // ---- Plain payload ----
            JSONObject payload = new JSONObject();
            payload.put("acc_no", session.getPrimaryAccNo());

            // ---- Encrypt ----
            String encrypted = GCMUtil.encrypt(
                    payload.toString(),
                    AppConstants.getSecretKeyBytes()
            );

            // ---- IMPORTANT: backend expects "data" ----
            JSONObject wrapper = new JSONObject();
            wrapper.put("d", encrypted);

            RequestBody body = RequestBody.create(
                    MediaType.parse("text/plain"),
                    wrapper.toString()
            );

            Map<String, String> headers = HeaderUtil.baseHeaders(
                    session.getDeviceInfo(),
                    AppConstants.getApiKey()
            );

            HeaderUtil.addSecurityHeaders(headers);
            headers.put("auth_token", session.getAuthToken());
            headers.put("action", "MINI_STATEMENT");

            ApiClient.create()
                    .balanceEnquiry(headers, body)
                    .enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            handleResponse(response);
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Log.e(TAG, "API FAILED", t);
                            stopLoadingAnimation();
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "REQUEST BUILD ERROR", e);
            stopLoadingAnimation();
        }
    }

    /* -------------------- Response Handling -------------------- */

    private void handleResponse(Response<ResponseBody> response) {
        try {
            String raw;

            if (response.isSuccessful() && response.body() != null) {
                raw = response.body().string();
            } else if (response.errorBody() != null) {
                raw = response.errorBody().string();
            } else {
                throw new Exception("Empty server response");
            }

            Log.e(TAG, "RAW RESPONSE: " + raw);
            Log.e(TAG, "HTTP CODE: " + response.code());

            JSONObject json = new JSONObject(raw);

            if (!json.has("response")) {
                throw new Exception("Missing 'response' field in JSON");
            }

            String decrypted = GCMUtil.decrypt(
                    json.getString("response"),
                    AppConstants.getSecretKeyBytes()
            );

            Log.e(TAG, "DECRYPTED RESPONSE: " + decrypted);

            parseMiniStatement(decrypted);

        } catch (Exception e) {
            Log.e(TAG, "PARSE / DECRYPT ERROR", e);
            stopLoadingAnimation();
        }
    }

    /* -------------------- Business Parsing -------------------- */

    private void parseMiniStatement(String decrypted) throws Exception {

        JSONArray table = new JSONObject(decrypted)
                .getJSONObject("response")
                .getJSONArray("Table");

        list.clear();

        for (int i = 0; i < table.length(); i++) {

            JSONObject obj = table.getJSONObject(i);
            TransactionModel t = new TransactionModel();

            t.type = obj.optInt("TransactionType") == 1 ? "DEBIT" : "CREDIT";
            t.amount = obj.optString("TransactionAmountNew")
                    .replace("Rs.", "")
                    .trim();
            t.date = obj.optString("TransactionDate");
            t.remark = extractRemark(obj.optString("Remarks"));

            list.add(t);
        }

        rv.setAdapter(new MiniStatementAdapter(list));
        stopLoadingAnimation();
    }

    /* -------------------- Helpers -------------------- */

    private String extractRemark(String r) {
        if (r == null) return "Bank Transaction";
        r = r.toLowerCase();
        if (r.contains("atm")) return "ATM Withdrawal";
        if (r.contains("upi")) return "UPI Transaction";
        if (r.contains("cash")) return "Cash Deposit";
        if (r.contains("int posted")) return "Interest Credit";
        return "Bank Transaction";
    }
}





//package com.example.vipayee.activities;
//
//import android.os.Bundle;
//import android.util.Log;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.vipayee.AppConstants;
//import com.example.vipayee.R;
//import com.example.vipayee.api.ApiClient;
//import com.example.vipayee.crypto.AESGCMUtil;
//import com.example.vipayee.model.MiniStatementAdapter;
//import com.example.vipayee.model.TransactionModel;
//import com.example.vipayee.utils.HeaderUtil;
//import com.example.vipayee.utils.SessionManager;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//import okhttp3.MediaType;
//import okhttp3.RequestBody;
//import okhttp3.ResponseBody;
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//
//public class MiniStatementActivity extends AppCompatActivity {
//
//    private static final String TAG = "MINI_STATEMENT";
//
//    private RecyclerView rv;
//    private final List<TransactionModel> list = new ArrayList<>();
//
//    @Override
//    protected void onCreate(Bundle b) {
//        super.onCreate(b);
//        setContentView(R.layout.activity_mini_statement);
//
//        rv = findViewById(R.id.rvMiniStatement);
//        rv.setLayoutManager(new LinearLayoutManager(this));
//
//        loadMiniStatement();
//    }
//
//    private void loadMiniStatement() {
//        try {
//            SessionManager session = new SessionManager(this);
//
//            JSONObject payload = new JSONObject();
//            payload.put("acc_no", "016100100006637");
//
//            String encrypted = AESGCMUtil.encrypt(
//                    payload.toString(),
//                    AppConstants.getSecretKeyBytes()
//            );
//
//            JSONObject wrapper = new JSONObject();
//            wrapper.put("d", encrypted);
//
//            RequestBody body = RequestBody.create(
//                    MediaType.parse("application/json; charset=utf-8"),
//                    wrapper.toString()
//            );
//
//            Map<String, String> headers = HeaderUtil.baseHeaders(
//                    session.getDeviceInfo(),
//                    AppConstants.getApiKey()
//            );
//
//            HeaderUtil.addSecurityHeaders(headers);
//            headers.put("auth_token", session.getAuthToken());
//            headers.put("action", "MINI_STATEMENT");
//
//            ApiClient.create()
//                    .balanceEnquiry(headers, body)
//                    .enqueue(new Callback<ResponseBody>() {
//                        @Override
//                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                            handleResponse(response);
//                        }
//
//                        @Override
//                        public void onFailure(Call<ResponseBody> call, Throwable t) {
//                            Log.e(TAG, "API FAILED", t);
//                        }
//                    });
//
//        } catch (Exception e) {
//            Log.e(TAG, "ERROR", e);
//        }
//    }
//
//    private void handleResponse(Response<ResponseBody> response) {
//        try {
//            String decrypted = AESGCMUtil.decrypt(
//                    new JSONObject(response.body().string()).getString("response"),
//                    AppConstants.getSecretKeyBytes()
//            );
//
//            JSONArray table = new JSONObject(decrypted)
//                    .getJSONObject("response")
//                    .getJSONArray("Table");
//
//            for (int i = 0; i < table.length(); i++) {
//
//                JSONObject obj = table.getJSONObject(i);
//                TransactionModel t = new TransactionModel();
//
//                // CREDIT / DEBIT
//                t.type = obj.optInt("TransactionType") == 1
//                        ? "DEBIT"
//                        : "CREDIT";
//
//                // Amount → remove "Rs."
//                t.amount = obj
//                        .optString("TransactionAmountNew")
//                        .replace("Rs.", "")
//                        .trim();
//
//                // Date
//                t.date = obj.optString("TransactionDate");
//
//                // Remark
//                t.remark = extractRemark(obj.optString("Remarks"));
//
//                list.add(t);
//            }
//
//            rv.setAdapter(new MiniStatementAdapter(list));
//
//        } catch (Exception e) {
//            Log.e(TAG, "PARSE ERROR", e);
//        }
//    }
//
//    private String extractRemark(String r) {
//        r = r.toLowerCase();
//        if (r.contains("atm")) return "ATM Withdrawal";
//        if (r.contains("upi")) return "UPI Transaction";
//        if (r.contains("cash")) return "Cash Deposit";
//        if (r.contains("int posted")) return "Interest Credit";
//        return "Bank Transaction";
//    }
//}
