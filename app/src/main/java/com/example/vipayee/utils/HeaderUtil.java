package com.example.vipayee.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HeaderUtil {

    /**
     * Base headers required by ALL APIs
     * ❗ DO NOT SET Content-Type HERE
     */
    public static Map<String, String> baseHeaders(
            String deviceInfo,
            String apiKey
    ) {
        Map<String, String> headers = new HashMap<>();

        headers.put("api_key", apiKey);
        headers.put("device_info", deviceInfo);
        headers.put("Accept", "application/json");

        return headers;
    }

    /**
     * Adds security headers (required by authenticate + secure APIs)
     */
    public static void addSecurityHeaders(Map<String, String> headers) {
        headers.put("nonce", UUID.randomUUID().toString());
        headers.put("req_ts", String.valueOf(System.currentTimeMillis() / 1000));
    }
}




//package com.example.vipayee.utils;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//public class HeaderUtil {
//    public static Map<String, String> generateHeaders(String deviceInfo, String apiKey) {
//
//        Map<String, String> headers = new HashMap<>();
//        headers.put("api_key", apiKey);
//        headers.put("nonce", UUID.randomUUID().toString());
//        headers.put("req_ts", String.valueOf(System.currentTimeMillis() / 1000));
//        headers.put("device_info", deviceInfo);
//
//        // IMPORTANT
//        headers.put("Content-Type", "text/plain");
//        headers.put("Accept", "application/json");
//
//        return headers;
//    }
//
//}

