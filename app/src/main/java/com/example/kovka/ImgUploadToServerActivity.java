package com.example.kovka;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ImgUploadToServerActivity extends AppCompatActivity {
    ImageView imageView;
    EditText editTextTags;
    ProgressBar progressBar;
    Bitmap selectedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img_upload_to_server);

        imageView = findViewById(R.id.imageView);
        editTextTags = findViewById(R.id.editTextTags);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.buttonUploadImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (editTextTags.getText().toString().trim().isEmpty()) {
                    editTextTags.setError("Введите название изображения");
                    editTextTags.requestFocus();
                    Toast.makeText(ImgUploadToServerActivity.this, "Пожалуйста, заполните поле", Toast.LENGTH_LONG).show();
                    return;
                }
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, 100);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                imageView.setImageBitmap(selectedBitmap);
                uploadBitmap(selectedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Ошибка чтения изображения", Toast.LENGTH_LONG).show();
            }
        }
    }

    public byte[] getFileDataFromDrawable(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private void uploadBitmap(final Bitmap bitmap) {
        final String tags = editTextTags.getText().toString().trim();

        if (bitmap == null) {
            Toast.makeText(this, "Изображение не выбрано", Toast.LENGTH_LONG).show();
            return;
        }

        // 1. ПРОВЕРЯЕМ НАЛИЧИЕ ТОКЕНА
        TokenManager tokenManager = new TokenManager(this);
        String token = tokenManager.getToken();

        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Вы не авторизованы. Войдите в систему.", Toast.LENGTH_LONG).show();
            return;
        }

        byte[] imageData = getFileDataFromDrawable(bitmap);

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        if (imageData.length == 0) {
            Toast.makeText(this, "Ошибка: изображение пустое", Toast.LENGTH_LONG).show();
            return;
        }

        // 2. СОЗДАЕМ ЗАПРОС И ПЕРЕДАЕМ ТОКЕН В ЗАГОЛОВКЕ
        VolleyMultipartRequest volleyMultipartRequest = new VolleyMultipartRequest(
                Request.Method.POST,
                EndPoints.UPLOAD_URL,
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        try {
                            String json = new String(response.data, "UTF-8");
                            JSONObject obj = new JSONObject(json);
                            boolean error = obj.optBoolean("error", false);
                            if (!error) {
                                String message = obj.optString("message", "Файл успешно загружен");
                                Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.TOP, 0, 300);
                                toast.show();
                            } else {
                                String errMsg = obj.optString("message", "Ошибка загрузки");
                                Toast.makeText(getApplicationContext(), errMsg, Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Ошибка обработки ответа", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }

                        if (error.networkResponse != null) {
                            if (error.networkResponse.data != null) {
                                try {
                                    String responseBody = new String(error.networkResponse.data);
                                    JSONObject jsonError = new JSONObject(responseBody);
                                    String serverMessage = jsonError.optString("message", "");
                                    if (!serverMessage.isEmpty()) {
                                        Toast.makeText(getApplicationContext(), serverMessage, Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        Toast.makeText(getApplicationContext(), "Ошибка соединения. Попробуйте позже.", Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            // 3. ДОБАВЛЯЕМ ЗАГОЛОВОК С ТОКЕНОМ
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token); // <--- ВОТ ОН!
                return headers;
            }
        };

        volleyMultipartRequest.addStringParam("tags", tags);

        String fileName = tags + ".jpg";
        volleyMultipartRequest.addByteParam("image", new VolleyMultipartRequest.DataPart(fileName, imageData));

        Volley.newRequestQueue(this).add(volleyMultipartRequest);
    }
}