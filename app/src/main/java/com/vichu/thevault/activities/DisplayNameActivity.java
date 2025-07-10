package com.vichu.thevault.activities;

import static com.vichu.thevault.utils.HelperUtils.DISPLAY_NAME_FIELD;
import static com.vichu.thevault.utils.HelperUtils.USERS_JSON;
import static com.vichu.thevault.utils.HelperUtils.showToast;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.vichu.thevault.R;
import com.vichu.thevault.utils.AwsS3Helper;

public class DisplayNameActivity extends AppCompatActivity {

    EditText displayNameInput;
    Button displayNameButton;

    private String user;
    private ProgressBar progressBar;
    private AwsS3Helper awsS3Helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_name_activity);
        initToolbar();

        displayNameInput = findViewById(R.id.display_name_input);
        displayNameButton = findViewById(R.id.display_name_btn);

        user = getIntent().getStringExtra("user");
        progressBar = findViewById(R.id.progressBar);
        awsS3Helper = new AwsS3Helper(this);

        awsS3Helper.fetchUserDetails(USERS_JSON, (userData, errorMessage) -> runOnUiThread(() -> {
            if (errorMessage == null && userData != null && userData.has(user)) {
                String existingDisplayName = userData.get(user).get(DISPLAY_NAME_FIELD).asText(null);
                if (existingDisplayName != null) {
                    displayNameInput.setText(existingDisplayName);
                }
            }
        }));

        displayNameButton.setOnClickListener(v -> {
            String displayName = displayNameInput.getText().toString().trim();

            if (displayName.isEmpty()) {
                showToast(this, "Display name can not be empty!");
                return;
            }

            if (displayName.length() > 50) {
                showToast(this, "Display name can't exceed 50 characters!");
                return;
            }

            displayNameButton.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            awsS3Helper.updateUserDetails(user, DISPLAY_NAME_FIELD, displayName, success -> runOnUiThread(() -> {
                if (success) {
                    showToast(this, "Display name updated successfully!");
                    displayNameButton.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    navigateToMain();
                } else {
                    showToast(this, "Failed to update display name!");
                    displayNameButton.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                }
            }));
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            navigateToMain();
        }
        return super.onOptionsItemSelected(item);
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.displayNameToolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("user", user);
        startActivity(intent);
        finish();
    }
}
