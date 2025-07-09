package com.vichu.thevault.activities;

import static com.vichu.thevault.utils.HelperUtils.USERS_JSON;
import static com.vichu.thevault.utils.HelperUtils.getUserFolder;
import static com.vichu.thevault.utils.HelperUtils.showToast;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.vichu.thevault.R;
import com.vichu.thevault.utils.AwsS3Helper;

public class RegisterActivity extends AppCompatActivity {

    EditText usernameInput, passwordInput, confirmPasswordInput;
    Button registerButton, loginButton;

    private AwsS3Helper awsS3Helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);
        awsS3Helper = new AwsS3Helper(this);

        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        registerButton = findViewById(R.id.register_btn);
        loginButton = findViewById(R.id.login_btn);

        loginButton.setOnClickListener(v -> navigateToLogin());

        registerButton.setOnClickListener(v -> {

            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                showToast(this, "Please don't leave any fields blank!");
                return;
            }

            if (!password.equals(confirmPassword)) {
                showToast(this, "The passwords do not match!");
                return;
            }

            if (password.length() < 8) {
                showToast(this, "Password must be at least 8 characters!");
                return;
            }

            awsS3Helper.fetchUserDetails(USERS_JSON, (userDataList, errorMessage) -> runOnUiThread(() -> {

                if (errorMessage != null) {
                    showToast(this, "Error: " + errorMessage);
                    return;
                }

                if (userDataList != null && userDataList.has(username)) {
                        showToast(this, "This user is already registered!");
                        navigateToLogin();
                        return;
                }

                awsS3Helper.registerUser(getUserFolder(username), username, password, success -> runOnUiThread(() -> {
                    if (success) {
                        showToast(this, "User successfully registered!");
                        navigateToLogin();
                    } else {
                        showToast(this, "Failed to register user!");
                    }
                }));
            }));
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
