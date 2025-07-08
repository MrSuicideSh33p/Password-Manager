package com.vichu.thevault.activities;

import static com.vichu.thevault.utils.HelperUtils.USERS_JSON;
import static com.vichu.thevault.utils.HelperUtils.showToast;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.vichu.thevault.R;
import com.vichu.thevault.utils.AwsS3Helper;

import org.json.JSONException;

public class LoginActivity extends AppCompatActivity {

    EditText usernameInput;
    EditText passwordInput;
    Button loginButton;
    Button registerButton;

    private AwsS3Helper awsS3Helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_btn);
        registerButton = findViewById(R.id.register_btn);

        awsS3Helper = new AwsS3Helper(this);

        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                showToast(this, "Please don't leave any fields blank!");
                return;
            }

            awsS3Helper.fetchUserDetails(USERS_JSON, (userDataList, errorMessage) -> runOnUiThread(() -> {

                if (errorMessage != null) {
                    showToast(this, "Error: " + errorMessage);
                    return;
                }

                if (userDataList == null || !userDataList.has(username)) {
                    showToast(this, "This user is not registered!");
                    navigateToRegister();
                    return;
                }

                try {
                    if (userDataList.getString(username).equals(password)) {
                        showToast(this, "User successfully verified!");
                        openMainActivityScreen(username);
                    } else {
                        showToast(this, "Incorrect credentials!");
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }));
        });

        Button registerButton = findViewById(R.id.register_btn);
        registerButton.setOnClickListener(v -> openRegisterActivityScreen());
    }

    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void openMainActivityScreen(String username) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("user", username);
        startActivity(intent);
    }

    private void openRegisterActivityScreen() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }
}
