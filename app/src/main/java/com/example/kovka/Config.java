package com.example.kovka;

public class Config {
    // Базовый URL (меняешь здесь один раз)
    public static final String BASE_URL = "http://192.168.1.156/kovka-laravel/public/";

    // Составные пути
    public static final String API_BASE = BASE_URL + "api/";
    public static final String STORAGE_BASE = BASE_URL + "storage/";
    public static final String IMG_BASE = BASE_URL + "img/";

    // Пути для создания, замены, удаления
    public static final String URL_CREATE = API_BASE + "orders";
    public static final String URL_CHANGE = API_BASE + "orders/";
    public static final String URL_DELETE = API_BASE + "orders/";

    public static String getImageUrl(String filename) {
        if (filename != null && filename.startsWith("products/")) {
            return STORAGE_BASE + filename;
        }
        return STORAGE_BASE + "products/" + filename;
    }
}