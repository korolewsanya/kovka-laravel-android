package com.example.kovka;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WorkReportsActivity extends AppCompatActivity {
    private static final String TAG = "IMG_DEBUG";
    private static final String JSON_URL = Config.API_BASE + "work-reports";
    private static final int REQUEST_SAVE_REPORT = 1;
    private static final int REQUEST_UPDATE_REPORT = 2;

    ListView listView;
    ArrayList<JSONObject> infoList;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_reports);

        listView = findViewById(R.id.listView);
        progressBar = findViewById(R.id.progressBar);
        loadJSONFromURL(JSON_URL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadJSONFromURL(JSON_URL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadJSONFromURL(JSON_URL);
            Toast.makeText(this, "Данные обновлены", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadJSONFromURL(String url) {
        progressBar.setVisibility(View.VISIBLE);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    try {
                        JSONObject object = new JSONObject(response);
                        boolean error = object.optBoolean("error", false);
                        if (!error) {
                            JSONArray jsonArray = object.getJSONArray("data");
                            infoList = getArrayListFromJSONArray(jsonArray);
                            WorkReportAdapter adapter = new WorkReportAdapter(getApplicationContext(),
                                    R.layout.work_report, R.id.date, infoList);
                            listView.setAdapter(adapter);

                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    JSONObject selected = infoList.get(position);
                                    final String recordId = selected.optString("id");
                                    final String currentTask = selected.optString("task");
                                    final String currentReport = selected.optString("report");
                                    final String imageFileName = selected.optString("image");
                                    final String date = selected.optString("date");

                                    Log.d(TAG, "id=" + recordId + " image=[" + imageFileName + "] len=" +
                                            (imageFileName == null ? -1 : imageFileName.length()));

                                    AlertDialog.Builder builder = new AlertDialog.Builder(WorkReportsActivity.this);
                                    builder.setTitle("Действие с отчетом");
                                    builder.setItems(new String[]{"Изменить", "Удалить", "Посмотреть изображение"},
                                            (dialog, which) -> {
                                                if (which == 0) {
                                                    // открываем WorkReportsSaveActivity с ID
                                                    Intent intent = new Intent(WorkReportsActivity.this, WorkReportsSaveActivity.class);
                                                    intent.putExtra("idi", recordId);
                                                    intent.putExtra("task", currentTask);
                                                    intent.putExtra("report", currentReport);
                                                    intent.putExtra("date", date);
                                                    intent.putExtra("image", imageFileName);
                                                    startActivityForResult(intent, REQUEST_UPDATE_REPORT);
                                                } else if (which == 1) {
                                                    showDeleteConfirmation(recordId);
                                                } else if (which == 2) {
                                                    if (imageFileName != null
                                                            && !imageFileName.trim().isEmpty()
                                                            && !"null".equalsIgnoreCase(imageFileName.trim())) {
                                                        showImageDialog(imageFileName);
                                                    } else {
                                                        Toast.makeText(WorkReportsActivity.this,
                                                                "Для этого отчета нет изображения", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                    builder.show();
                                }
                            });
                        } else {
                            String message = object.optString("message", "Ошибка загрузки");
                            Toast.makeText(WorkReportsActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(WorkReportsActivity.this, "Ошибка данных", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(WorkReportsActivity.this, "Ошибка соединения. Попробуйте позже.", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                TokenManager tokenManager = new TokenManager(getApplicationContext());
                String token = tokenManager.getToken();
                if (token != null && !token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                }
                headers.put("Accept", "application/json");
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    private void showImageDialog(String imageFileName) {
        if (imageFileName == null) imageFileName = "";
        imageFileName = imageFileName.trim();

        if (imageFileName.isEmpty() || "null".equalsIgnoreCase(imageFileName)) {
            Toast.makeText(this, "Для этого отчета нет изображения", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullUrl;

        if (imageFileName.startsWith("data:image")) {
            // base64 data-URI — Glide грузит напрямую
            fullUrl = imageFileName;
        } else if (imageFileName.startsWith("http://") || imageFileName.startsWith("https://")) {
            // уже полный URL
            fullUrl = imageFileName;
        } else {
            String baseUrl = Config.STORAGE_BASE;
            if (!baseUrl.endsWith("/")) baseUrl += "/";

            while (imageFileName.startsWith("/")) {
                imageFileName = imageFileName.substring(1);
            }

            // ПРОВЕРКА НА "products/": если уже есть — не добавляем, если нет — добавляем
            String path;
            if (imageFileName.startsWith("products/")) {
                path = imageFileName;
            } else {
                path = "products/" + imageFileName;
            }

            // кодируем пробелы/кириллицу в имени файла (не трогаем уже закодированное)
            if (!path.matches(".*%[0-9A-Fa-f]{2}.*")) {
                path = android.net.Uri.encode(path, "/");
            }

            fullUrl = baseUrl + path;
        }

        Log.d(TAG, "open image fullUrl=" + fullUrl);

        Intent intent = new Intent(WorkReportsActivity.this, ImageFullScreenActivity.class);
        intent.putExtra(ImageFullScreenActivity.EXTRA_IMAGE_URL, fullUrl);
        intent.putExtra(ImageFullScreenActivity.EXTRA_IMAGE_NAME, imageFileName);
        startActivity(intent);
    }

    private void showDeleteConfirmation(final String id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Удаление");
        builder.setMessage("Вы действительно хотите удалить этот отчет?");
        builder.setPositiveButton("Да", (dialog, which) -> deleteReport(id));
        builder.setNegativeButton("Нет", null);
        builder.show();
    }

    private void deleteReport(final String id) {
        String url = Config.API_BASE + "work-reports/" + id;

        StringRequest request = new StringRequest(Request.Method.DELETE, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (!json.getBoolean("error")) {
                            Toast.makeText(WorkReportsActivity.this, "Отчет удален", Toast.LENGTH_SHORT).show();
                            loadJSONFromURL(JSON_URL);
                        } else {
                            Toast.makeText(WorkReportsActivity.this, "Ошибка: " + json.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    Toast.makeText(WorkReportsActivity.this, "Ошибка соединения", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                TokenManager tokenManager = new TokenManager(getApplicationContext());
                String token = tokenManager.getToken();
                if (token != null && !token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                }
                headers.put("Accept", "application/json");
                return headers;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    private ArrayList<JSONObject> getArrayListFromJSONArray(JSONArray jsonArray) {
        ArrayList<JSONObject> aList = new ArrayList<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                aList.add(jsonArray.getJSONObject(i));
            }
        } catch (JSONException js) {
            js.printStackTrace();
        }
        return aList;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.work_report, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.ot) {
            // создание нового отчета - без ID
            Intent intent = new Intent(this, WorkReportsSaveActivity.class);
            startActivityForResult(intent, REQUEST_SAVE_REPORT);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}