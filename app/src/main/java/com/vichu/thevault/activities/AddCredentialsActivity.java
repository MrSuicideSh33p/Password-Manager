package com.vichu.thevault.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.vichu.thevault.R;
import com.vichu.thevault.models.CredentialData;
import com.vichu.thevault.utils.AwsS3Helper;

public class AddCredentialsActivity extends AppCompatActivity {

    private EditText websiteInput, usernameInput, passwordInput, notesInput;
    private AwsS3Helper awsS3Helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_credentials);

        Toolbar toolbar = findViewById(R.id.credentialsEntryToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        // Initialize UI elements
        websiteInput = findViewById(R.id.et_website);
        usernameInput = findViewById(R.id.et_username);
        passwordInput = findViewById(R.id.et_password);
        notesInput = findViewById(R.id.et_notes);
        awsS3Helper = new AwsS3Helper(this);
    }

    @Override
    public void onBackPressed() {
        if (isAnyFieldFilled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Exit")
                    .setMessage("The text you entered will be lost. Are you sure you want to proceed?")
                    .setPositiveButton("Yes", (dialog, which) -> super.onBackPressed()) // Go back
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss()) // Stay on page
                    .show();
        } else {
            super.onBackPressed(); // No changes, exit normally
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.credentials_entry_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            confirmExit();
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            saveCredentials(); // Handle save button
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmExit() {
        if (isAnyFieldFilled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Exit")
                    .setMessage("The text you entered will be lost. Are you sure you want to proceed?")
                    .setPositiveButton("Yes", (dialog, which) -> finish()) // Close activity
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss()) // Stay on page
                    .show();
        } else {
            finish(); // Exit if no fields are filled
        }
    }

    private void saveCredentials() {
        String website = websiteInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String notes = notesInput.getText().toString().trim();

        if (website.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(AddCredentialsActivity.this, "Please don't leave any fields blank!", Toast.LENGTH_SHORT).show();
            return;
        }

        CredentialData credentialData = new CredentialData(website, username, password, notes);
        String credentialsContent = credentialData.toFileFormat();
        String fileName = "credentials/" + website.replaceAll("\\s+", "_") + "-" + username.replaceAll("\\s", "_") + ".txt"; // Ensure a valid file name

        awsS3Helper.uploadCredentials(fileName, credentialsContent, success -> {
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Credentials successfully saved!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Failed to save credentials!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private boolean isAnyFieldFilled() {
        return !websiteInput.getText().toString().trim().isEmpty() ||
                !usernameInput.getText().toString().trim().isEmpty() ||
                !passwordInput.getText().toString().trim().isEmpty() ||
                !notesInput.getText().toString().trim().isEmpty();
    }
}
