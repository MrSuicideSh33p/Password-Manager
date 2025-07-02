package com.vichu.thevault.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.vichu.thevault.R;
import com.vichu.thevault.models.CredentialData;
import com.vichu.thevault.utils.AwsS3Helper;

import java.util.Objects;

public class CredentialDetailsActivity extends AppCompatActivity {

    private EditText websiteText, usernameText, passwordText, notesText;
    private AwsS3Helper awsS3Helper;
    private String credentialFile;
    private boolean isEditing = false;
    private MenuItem saveMenuItem, editMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credential_details);

        websiteText = findViewById(R.id.websiteText);
        usernameText = findViewById(R.id.usernameText);
        passwordText = findViewById(R.id.passwordText);
        notesText = findViewById(R.id.notesText);

        awsS3Helper = new AwsS3Helper(this);

        Toolbar toolbar = findViewById(R.id.credentialDetailsToolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        Intent intent = getIntent();
        credentialFile = intent.getStringExtra("credentialFile");

        loadCredentialDetails();

        // Initially disable editing
        setEditingEnabled(false);
    }

    private void loadCredentialDetails() {
        awsS3Helper.fetchCredentialDetails(credentialFile, (credentialData, errorMessage) -> {
            if (errorMessage != null) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show());
            } else {
                runOnUiThread(() -> {
                    websiteText.setText(credentialData.getWebsite());
                    usernameText.setText(credentialData.getUsername());
                    passwordText.setText(credentialData.getPassword());
                    notesText.setText(credentialData.getNotes());
                });
            }
        });
    }

    private void setEditingEnabled(boolean enabled) {
        websiteText.setEnabled(enabled);
        usernameText.setEnabled(enabled);
        passwordText.setEnabled(enabled);
        notesText.setEnabled(enabled);
        isEditing = enabled;
        if (saveMenuItem != null) {
            saveMenuItem.setVisible(enabled);
        }
        if (editMenuItem != null) {
            editMenuItem.setVisible(enabled ? false : true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.credential_details_menu, menu);
        saveMenuItem = menu.findItem(R.id.save_credential);
        editMenuItem = menu.findItem(R.id.edit_credential);
        saveMenuItem.setVisible(false); // Hide save initially
        editMenuItem.setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.edit_credential) {
            setEditingEnabled(true);
            return true;
        } else if (item.getItemId() == R.id.save_credential) {
            saveCredential();
            return true;
        } else if (item.getItemId() == R.id.delete_credential) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Credential")
                .setMessage("Are you sure you want to delete this credential?")
                .setPositiveButton("Delete", (dialog, which) -> deleteCredential())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCredential() {
        awsS3Helper.deleteCredential(credentialFile, success -> {
            if (success) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Credential deleted", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }
        });
    }

    private void saveCredential() {
        String newWebsite = websiteText.getText().toString().trim();
        String newUsername = usernameText.getText().toString().trim();
        String newPassword = passwordText.getText().toString().trim();
        String newNotes = notesText.getText().toString().trim();

        if (newWebsite.isEmpty() || newUsername.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        CredentialData updatedCredential = new CredentialData(newWebsite, newUsername, newPassword, newNotes);
        String updatedFileContent = updatedCredential.toFileFormat();

        awsS3Helper.uploadCredentials(credentialFile, updatedFileContent, (success) -> {
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Credential updated", Toast.LENGTH_SHORT).show();
                    setEditingEnabled(false);

                    // Redirect back to CredentialListActivity
                    Intent intent = new Intent(this, CredentialListActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

}
