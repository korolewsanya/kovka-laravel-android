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

        TextView tvPrivacyLink = findViewById(R.id.tvPrivacyLink);
        tvPrivacyLink.setOnClickListener(v -> {
            String privacyUrl = Config.BASE_URL + "privacy";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl));
            startActivity(browserIntent);
        });

        if (tokenManager.hasToken()) {
            startActivity(new Intent(this, AdminActivity.class));
            finish();
            return;
        }

        btnLogin.setOnClickListener(v -> performLogin());
    }

    private void performLogin() {
        // Проверка чекбокса
        if (!cbAgree.isChecked()) {
            tvError.setText("Необходимо согласие с политикой конфиденциальности");
            tvError.setVisibility(View.VISIBLE);
            return;
        }

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
                    tokenManager.saveToken(result.data.token);
                    String role = result.data.role != null ? result.data.role : "employee";
                    tokenManager.saveRole(role);

                    Toast.makeText(LoginActivity.this, "Добро пожаловать!", Toast.LENGTH_SHORT).show();

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

                // Если ошибка содержит признак блокировки (429), выводим её как есть
                // ApiClient должен возвращать текст вроде "Слишком много попыток. Подождите..."
                tvError.setText(error);
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }
}