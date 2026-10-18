package com.example.kovka;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private TextView tvError;
    private CheckBox cbAgree;
    private ApiClient apiClient;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);
        cbAgree = findViewById(R.id.cbAgree);

        apiClient = ApiClient.getInstance(this);
        tokenManager = new TokenManager(this);

        // Обработка клика по ссылке на политику конфиденциальности
        TextView tvPrivacyLink = findViewById(R.id.tvPrivacyLink);
        tvPrivacyLink.setOnClickListener(v -> {
            // ВАЖНО: Укажите здесь реальный URL вашей страницы политики
            // Если вы тестируете локально, это будет IP вашего компьютера
            String privacyUrl = Config.BASE_URL + "privacy";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl));
            startActivity(browserIntent);
        });

        // Если уже есть токен — сразу в AdminActivity
        if (tokenManager.hasToken()) {
            startActivity(new Intent(this, AdminActivity.class));
            finish();
            return;
        }

        btnLogin.setOnClickListener(v -> performLogin());
    }

    private void performLogin() {
        // === ПРОВЕРКА ЧЕКБОКСА СОГЛАСИЯ ===
        if (!cbAgree.isChecked()) {
            tvError.setText("Необходимо согласие с политикой конфиденциальности");
            tvError.setVisibility(View.VISIBLE);
            return;
        }
        // ===================================

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Введите email");
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Введите пароль");
            etPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        tvError.setText("");
        tvError.setVisibility(View.GONE);

        apiClient.login(email, password, new ApiClient.ApiCallback<ApiClient.LoginResponse>() {
            @Override
            public void onSuccess(ApiClient.LoginResponse result) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                if (result.data != null) {
                    // Сохраняем токен
                    tokenManager.saveToken(result.data.token);

                    // Сохраняем роль (admin или employee)
                    String role = result.data.role != null ? result.data.role : "employee";
                    tokenManager.saveRole(role);

                    Toast.makeText(LoginActivity.this, "Добро пожаловать!", Toast.LENGTH_SHORT).show();

                    // Все пользователи идут в AdminActivity
                    Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    tvError.setText("Ошибка: данные не получены");
                    tvError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                tvError.setText(error);
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }
}