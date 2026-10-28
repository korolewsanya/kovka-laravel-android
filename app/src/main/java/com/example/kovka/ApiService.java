package com.example.kovka;

import android.content.Context;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

public class ApiService {
    private static ApiService instance;
    private RequestQueue requestQueue;
    private Gson gson;
    private TokenManager tokenManager; // <-- ДОБАВЛЕНО

    private ApiService(Context context) {
        requestQueue = Volley.newRequestQueue(context);
        gson = new Gson();
        tokenManager = new TokenManager(context); // <-- ДОБАВЛЕНО
    }

    public static synchronized ApiService getInstance(Context context) {
        if (instance == null) {
            instance = new ApiService(context);
        }
        return instance;
    }

    // <-- ДОБАВЛЕН ВСПОМОГАТЕЛЬНЫЙ МЕТОД ДЛЯ ЗАГОЛОВКОВ -->
    private Map<String, String> getAuthHeaders() {
        Map<String, String> headers = new HashMap<>();
        String token = tokenManager.getToken();
        if (token != null && !token.isEmpty()) {
            headers.put("Authorization", "Bearer " + token);
        }
        return headers;
    }

    public interface ImageListCallback {
        void onSuccess(List<ImageModel> images);
        void onError(String error);
    }

    public interface SimpleCallback {
        void onSuccess(ApiResponse response);
        void onError(String error);
    }

    // GET /api/images - список всех изображений
    public void getImages(ImageListCallback callback) {
        String url = EndPoints.IMAGES_URL;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        boolean error = response.optBoolean("error", false);
                        if (!error) {
                            Object dataObj = response.get("data");
                            String dataJson = dataObj.toString();
                            Type listType = new TypeToken<List<ImageModel>>(){}.getType();
                            List<ImageModel> images = gson.fromJson(dataJson, listType);
                            callback.onSuccess(images);
                        } else {
                            String message = response.optString("message", "Ошибка загрузки");
                            callback.onError(message);
                        }
                    } catch (Exception e) {
                        callback.onError("Ошибка парсинга: " + e.getMessage());
                    }
                },
                error -> {
                    String errorMsg = "Ошибка соединения";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data);
                            JSONObject jsonError = new JSONObject(responseBody);
                            errorMsg = jsonError.optString("message", errorMsg);
                        } catch (Exception e) {}
                    }
                    callback.onError(errorMsg);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    // DELETE /api/images/delete?filename=xxx - удаление изображения
    public void deleteImage(String filename, SimpleCallback callback) {
        String url = EndPoints.DELETE_URL + "?filename=" + filename;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.DELETE,
                url,
                null,
                response -> {
                    try {
                        ApiResponse apiResponse = new ApiResponse();
                        apiResponse.setError(response.optBoolean("error", false));
                        apiResponse.setMessage(response.optString("message", ""));
                        callback.onSuccess(apiResponse);
                    } catch (Exception e) {
                        callback.onError("Ошибка парсинга");
                    }
                },
                error -> {
                    String errorMsg = "Ошибка соединения";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data);
                            JSONObject jsonError = new JSONObject(responseBody);
                            errorMsg = jsonError.optString("message", errorMsg);
                        } catch (Exception e) {}
                    }
                    callback.onError(errorMsg);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    // PUT /api/images/rename - переименование изображения
    public void renameImage(String oldName, String newName, SimpleCallback callback) {
        String url = EndPoints.RENAME_URL;

        JSONObject params = new JSONObject();
        try {
            params.put("old_name", oldName);
            params.put("new_name", newName);
        } catch (Exception e) {}

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                params,
                response -> {
                    try {
                        ApiResponse apiResponse = new ApiResponse();
                        apiResponse.setError(response.optBoolean("error", false));
                        apiResponse.setMessage(response.optString("message", ""));

                        JSONObject data = response.optJSONObject("data");
                        if (data != null) {
                            apiResponse.setNewName(data.optString("new_name", ""));
                        }

                        callback.onSuccess(apiResponse);
                    } catch (Exception e) {
                        callback.onError("Ошибка парсинга");
                    }
                },
                error -> {
                    String errorMsg = "Ошибка соединения";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data);
                            JSONObject jsonError = new JSONObject(responseBody);
                            errorMsg = jsonError.optString("message", errorMsg);
                        } catch (Exception e) {}
                    }
                    callback.onError(errorMsg);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    public static String getBaseUrl() {
        return Config.STORAGE_BASE + "products/";
    }

    // GET /api/finances - список всех финансов
    public void getFinances(FinanceListCallback callback) {
        String url = Config.API_BASE + "finances";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        boolean error = response.optBoolean("error", false);
                        if (!error) {
                            Object dataObj = response.get("data");
                            String dataJson = dataObj.toString();
                            Type listType = new TypeToken<List<FinanceModel>>(){}.getType();
                            List<FinanceModel> finances = gson.fromJson(dataJson, listType);
                            callback.onSuccess(finances);
                        } else {
                            String message = response.optString("message", "Ошибка загрузки");
                            callback.onError(message);
                        }
                    } catch (Exception e) {
                        callback.onError("Ошибка парсинга: " + e.getMessage());
                    }
                },
                error -> {
                    String errorMsg = "Ошибка соединения";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data);
                            JSONObject jsonError = new JSONObject(responseBody);
                            errorMsg = jsonError.optString("message", errorMsg);
                        } catch (Exception e) {}
                    }
                    callback.onError(errorMsg);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    // POST /api/finances - создание записи
    public void createFinance(FinanceModel finance, SimpleCallback callback) {
        String url = Config.API_BASE + "finances";

        JSONObject params = new JSONObject();
        try {
            if (finance.getDate() != null) params.put("date", finance.getDate());
            params.put("income", finance.getIncome());
            params.put("expense", finance.getExpense());
            params.put("profit", finance.getProfit());
            if (finance.getNote() != null) params.put("note", finance.getNote());
        } catch (Exception e) {}

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                params,
                response -> {
                    try {
                        ApiResponse apiResponse = new ApiResponse();
                        apiResponse.setError(response.optBoolean("error", false));
                        apiResponse.setMessage(response.optString("message", ""));

                        JSONObject data = response.optJSONObject("data");
                        if (data != null) {
                            FinanceModel created = gson.fromJson(data.toString(), FinanceModel.class);
                            apiResponse.setData(created);
                        }

                        callback.onSuccess(apiResponse);
                    } catch (Exception e) {
                        callback.onError("Ошибка парсинга");
                    }
                },
                error -> {
                    String errorMsg = "Ошибка соединения";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data);
                            JSONObject jsonError = new JSONObject(responseBody);
                            errorMsg = jsonError.optString("message", errorMsg);
                        } catch (Exception e) {}
                    }
                    callback.onError(errorMsg);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    // PUT /api/finances/{id} - обновление записи
    public void updateFinance(int id, FinanceModel finance, SimpleCallback callback) {
        String url = Config.API_BASE + "finances/" + id;

        JSONObject params = new JSONObject();
        try {
            if (finance.getDate() != null) params.put("date", finance.getDate());
            params.put("income", finance.getIncome());
            params.put("expense", finance.getExpense());
            params.put("profit", finance.getProfit());
            if (finance.getNote() != null) params.put("note", finance.getNote());
        } catch (Exception e) {}

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                params,
                response -> {
                    try {
                        ApiResponse apiResponse = new ApiResponse();
                        apiResponse.setError(response.optBoolean("error", false));
                        apiResponse.setMessage(response.optString("message", ""));

                        JSONObject data = response.optJSONObject("data");
                        if (data != null) {
                            FinanceModel updated = gson.fromJson(data.toString(), FinanceModel.class);
                            apiResponse.setData(updated);
                        }

                        callback.onSuccess(apiResponse);
                    } catch (Exception e) {
                        callback.onError("Ошибка парсинга");
                    }
                },
                error -> {
                    String errorMsg = "Ошибка соединения";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data);
                            JSONObject jsonError = new JSONObject(responseBody);
                            errorMsg = jsonError.optString("message", errorMsg);
                        } catch (Exception e) {}
                    }
                    callback.onError(errorMsg);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    // DELETE /api/finances/{id} - удаление записи
    public void deleteFinance(int id, SimpleCallback callback) {
        String url = Config.API_BASE + "finances/" + id;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.DELETE,
                url,
                null,
                response -> {
                    try {
                        ApiResponse apiResponse = new ApiResponse();
                        apiResponse.setError(response.optBoolean("error", false));
                        apiResponse.setMessage(response.optString("message", ""));
                        callback.onSuccess(apiResponse);
                    } catch (Exception e) {
                        callback.onError("Ошибка парсинга");
                    }
                },
                error -> {
                    String errorMsg = "Ошибка соединения";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data);
                            JSONObject jsonError = new JSONObject(responseBody);
                            errorMsg = jsonError.optString("message", errorMsg);
                        } catch (Exception e) {}
                    }
                    callback.onError(errorMsg);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    // Добавьте интерфейс
    public interface FinanceListCallback {
        void onSuccess(List<FinanceModel> finances);
        void onError(String error);
    }
}