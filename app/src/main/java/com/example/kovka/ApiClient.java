package com.example.kovka;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static ApiClient instance;
    private RequestQueue requestQueue;
    private TokenManager tokenManager;
    private Context context;

    private ApiClient(Context context) {
        this.context = context.getApplicationContext();
        requestQueue = Volley.newRequestQueue(this.context);
        tokenManager = new TokenManager(this.context);
    }

    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context);
        }
        return instance;
    }

    public TokenManager getTokenManager() {
        return tokenManager;
    }

    // МЕТОД ДЛЯ ВХОДА
    public void login(String email, String password, ApiCallback<LoginResponse> callback) {
        String url = Config.API_BASE + "login";

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("email", email);
            jsonBody.put("password", password);

            // === ДОБАВЛЯЕМ ПЕРЕДАЧУ СОГЛАСИЯ С ПОЛИТИКОЙ КОНФИДЕНЦИАЛЬНОСТИ===
            // Передаем 1 (единицу), что означает "true" для валидации Laravel
            jsonBody.put("agree", 1);
            // ===================================

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                    response -> {
                        try {
                            Log.d(TAG, "Login успех: " + response.toString());

                            LoginResponse loginResponse = new LoginResponse();
                            loginResponse.error = response.optBoolean("error", false);
                            loginResponse.message = response.optString("message", "");

                            // Получаем данные
                            JSONObject data = response.optJSONObject("data");
                            if (data != null) {
                                loginResponse.data = new LoginData();
                                loginResponse.data.token = data.optString("token", "");
                                loginResponse.data.role = data.optString("role", "");

                                // Сохраняем токен
                                if (!loginResponse.data.token.isEmpty()) {
                                    tokenManager.saveToken(loginResponse.data.token);
                                }
                            }

                            if (!loginResponse.error && loginResponse.data != null && !loginResponse.data.token.isEmpty()) {
                                callback.onSuccess(loginResponse);
                            } else {
                                callback.onError(loginResponse.message != null ? loginResponse.message : "Ошибка входа");
                            }
                        } catch (Exception e) {
                            callback.onError("Ошибка обработки ответа: " + e.getMessage());
                        }
                    },
                    error -> {
                        String message = "Ошибка соединения. Попробуйте позже.";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String responseBody = new String(error.networkResponse.data, "UTF-8");
                                Log.e(TAG, "Ошибка ответа: " + responseBody);
                                JSONObject jsonError = new JSONObject(responseBody);
                                if (jsonError.has("message")) {
                                    message = jsonError.getString("message");
                                }
                            } catch (Exception e) {
                            }
                        }
                        callback.onError(message);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Accept", "application/json");
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            request.setRetryPolicy(new DefaultRetryPolicy(
                    30000,
                    0,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(request);

        } catch (Exception e) {
            callback.onError("Ошибка: " + e.getMessage());
        }
    }

    // GET запрос
    public void get(String endpoint, ApiCallback<JSONObject> callback) {
        String url = Config.API_BASE + endpoint;
        makeRequest(Request.Method.GET, url, null, callback);
    }

    // POST запрос
    public void post(String endpoint, JSONObject body, ApiCallback<JSONObject> callback) {
        String url = Config.API_BASE + endpoint;
        makeRequest(Request.Method.POST, url, body, callback);
    }

    // PUT запрос
    public void put(String endpoint, JSONObject body, ApiCallback<JSONObject> callback) {
        String url = Config.API_BASE + endpoint;
        makeRequest(Request.Method.PUT, url, body, callback);
    }

    // DELETE запрос
    public void delete(String endpoint, ApiCallback<JSONObject> callback) {
        String url = Config.API_BASE + endpoint;
        makeRequest(Request.Method.DELETE, url, null, callback);
    }

    // ОСНОВНОЙ МЕТОД ЗАПРОСА
    private void makeRequest(int method, String url, JSONObject body, ApiCallback<JSONObject> callback) {
        String token = tokenManager.getToken();

        Log.d(TAG, "Запрос: " + method + " " + url);
        if (body != null) {
            Log.d(TAG, "Тело: " + body.toString());
        }

        JsonObjectRequest request = new JsonObjectRequest(method, url, body,
                response -> {
                    Log.d(TAG, "Успешный ответ: " + response.toString());
                    if (callback != null) {
                        callback.onSuccess(response);
                    }
                },
                error -> {
                    Log.e(TAG, "Ошибка: " + error.getMessage());

                    String errorMsg = "Ошибка соединения";
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        Log.e(TAG, "Код ошибки: " + statusCode);

                        if (statusCode >= 200 && statusCode < 300) {
                            Log.d(TAG, "Запрос успешен, но парсинг ответа не удался");
                            if (callback != null) {
                                try {
                                    JSONObject fakeResponse = new JSONObject();
                                    fakeResponse.put("success", true);
                                    fakeResponse.put("message", "Заказ создан");
                                    callback.onSuccess(fakeResponse);
                                } catch (Exception e) {
                                    callback.onSuccess(new JSONObject());
                                }
                            }
                            return;
                        }
                    }

                    if (callback != null) {
                        callback.onError(errorMsg);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = tokenManager.getToken();
                if (token != null && !token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                }
                headers.put("Accept", "application/json");
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    // ВНУТРЕННИЙ КЛАСС ДЛЯ ОТВЕТА ВХОДА
    public static class LoginResponse {
        public boolean error;
        public String message;
        public LoginData data;
    }

    public static class LoginData {
        public String token;
        public String role;
    }
}