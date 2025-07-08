package com.vichu.thevault.activities;

import static com.vichu.thevault.utils.HelperUtils.getMetadataFile;
import static com.vichu.thevault.utils.HelperUtils.showToast;
import static com.vichu.thevault.utils.HelperUtils.getUserFolder;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.vichu.thevault.R;
import com.vichu.thevault.models.CredentialData;
import com.vichu.thevault.utils.AwsS3Helper;
import com.vichu.thevault.utils.EncryptionHelper;

public class AddCredentialsActivity extends AppCompatActivity {

    private EditText websiteInput, usernameInput, passwordInput, privateKeyInput, notesInput;
    private AwsS3Helper awsS3Helper;
    private String user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_credentials);

        Intent intent = getIntent();
        user = intent.getStringExtra("user");

        initViews();
        initToolbar();
        awsS3Helper = new AwsS3Helper(this);
    }

    private void initViews() {
        websiteInput = findViewById(R.id.et_website);
        usernameInput = findViewById(R.id.et_username);
        passwordInput = findViewById(R.id.et_password);
        privateKeyInput = findViewById(R.id.et_private_key);
        notesInput = findViewById(R.id.et_notes);
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.credentialsEntryToolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    @Override
    public void onBackPressed() {
        confirmExitIfNeeded(() -> super.onBackPressed());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.credentials_entry_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            confirmExitIfNeeded(this::finish);
            return true;
        } else if (id == R.id.action_save) {
            saveCredentials();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmExitIfNeeded(Runnable exitAction) {
        if (isAnyFieldFilled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Exit")
                    .setMessage("The text you entered will be lost. Are you sure you want to proceed?")
                    .setPositiveButton("Yes", (dialog, which) -> exitAction.run()) //Close activity
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss()) //Stay on page
                    .show();
        } else {
            exitAction.run();
        }
    }

    private void saveCredentials() {
        String website = websiteInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String privateKey = privateKeyInput.getText().toString().trim();
        String notes = notesInput.getText().toString().trim();

        if (website.isEmpty() || username.isEmpty() || password.isEmpty() || privateKey.isEmpty()) {
            showToast(this, "Please don't leave any fields blank!");
            return;
        }

        String salt = EncryptionHelper.generateSalt();
        String encryptedPassword;
        try {
            encryptedPassword = EncryptionHelper.encrypt(password, privateKey, salt);
        } catch (Exception e) {
            Log.e("EncryptionHelper", "Error encrypting password", e);
            showToast(this, "Encryption failed. Please try again.");
            return;
        }

        CredentialData credentialData = new CredentialData(website, username, encryptedPassword, privateKey, salt, notes);
        String fileContent = credentialData.toFileFormat();
        String fileName = buildFileName(website, username);

        awsS3Helper.uploadCredentials(fileName, user, fileContent, success -> runOnUiThread(() -> {
            if (success) {
                showToast(this, "Credentials successfully saved!");
                navigateToMain();
            } else {
                showToast(this, "Failed to save credentials!");
            }
        }));
    }

    private boolean isAnyFieldFilled() {
        return !websiteInput.getText().toString().trim().isEmpty() ||
                !usernameInput.getText().toString().trim().isEmpty() ||
                !passwordInput.getText().toString().trim().isEmpty() ||
                !privateKeyInput.getText().toString().trim().isEmpty() ||
                !notesInput.getText().toString().trim().isEmpty();
    }

    private String buildFileName(String website, String username) {
        return getUserFolder(user) + website.replaceAll("\\s+", "_") +
                "-" + username.replaceAll("\\s+", "_") + ".txt";
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("user", user);
        startActivity(intent);
        finish();
    }
}
