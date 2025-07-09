package com.vichu.thevault.activities;

import static com.vichu.thevault.utils.HelperUtils.TAG;
import static com.vichu.thevault.utils.HelperUtils.USERS_JSON;
import static com.vichu.thevault.utils.HelperUtils.showToast;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.vichu.thevault.R;
import com.vichu.thevault.utils.AwsS3Helper;

import org.json.JSONException;

public class ResetPasswordActivity extends AppCompatActivity {

    EditText currentPasswordInput, newPasswordInput, confirmNewPasswordInput;
    Button changePasswordButton;

    private String user;
    private AwsS3Helper awsS3Helper;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reset_password_activity);

        awsS3Helper = new AwsS3Helper(this);
        progressBar = findViewById(R.id.progressBar);
        user = getIntent().getStringExtra("user");

        currentPasswordInput = findViewById(R.id.current_password_input);
        newPasswordInput = findViewById(R.id.new_password_input);
        confirmNewPasswordInput = findViewById(R.id.confirm_new_password_input);
        changePasswordButton = findViewById(R.id.change_password_btn);

        changePasswordButton.setOnClickListener(v -> {

            String currentPassword = currentPasswordInput.getText().toString().trim();
            String newPassword = newPasswordInput.getText().toString().trim();
            String confirmNewPassword = confirmNewPasswordInput.getText().toString().trim();

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
                showToast(this, "Please don't leave any fields blank!");
                return;
            }

            changePasswordButton.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            awsS3Helper.fetchUserDetails(USERS_JSON, (userDataList, errorMessage) -> runOnUiThread(() -> {

                if (errorMessage != null) {
                    showToast(this, "Error: " + errorMessage);
                    changePasswordButton.setEnabled(true);
                    return;
                }

                try {
                    if (!userDataList.getString(user).equals(currentPassword)) {
                        updateUI( "The current password entered is incorrect!");
                        return;
                    }

                    if (!newPassword.equals(confirmNewPassword)) {
                        updateUI("The passwords do not match!");
                        return;
                    }

                    if (currentPassword.equals(newPassword)) {
                        updateUI("New password must be different from the current one!");
                        return;
                    }

                    if (newPassword.length() < 8) {
                        updateUI("Password must be at least 8 characters!");
                        return;
                    }

                    awsS3Helper.updateUserDetails(user, newPassword, success -> runOnUiThread(() -> {
                        if (success) {
                            showToast(this, "Password updated successfully!");
                            progressBar.setVisibility(View.GONE);
                            navigateToMain();
                        } else {
                            updateUI("Failed to update password!");
                        }
                        changePasswordButton.setEnabled(true);
                    }));

                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing user.json content: " + e.getMessage());
                    updateUI( "Internal server error!");
                }
            }));
        });
    }

    private void updateUI(String message) {
        changePasswordButton.setEnabled(true);
        progressBar.setVisibility(View.GONE);
        showToast(this, message);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("user", user);
        startActivity(intent);
        finish();
    }
}
