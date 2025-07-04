package com.vichu.thevault.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.vichu.thevault.R;
import com.vichu.thevault.models.CredentialData;
import com.vichu.thevault.utils.AwsS3Helper;
import com.vichu.thevault.utils.EncryptionHelper;

import java.util.Objects;

public class CredentialDetailsActivity extends AppCompatActivity {

    private EditText websiteText, usernameText, passwordText, notesText;
    private AwsS3Helper awsS3Helper;

    private String credentialFile;
    private String encryptedPassword;
    private String privateKey;
    private String salt;

    private boolean isEditing = false;

    private MenuItem saveMenuItem;
    private MenuItem editMenuItem;
    private MenuItem encryptCredentialItem;
    private MenuItem decryptCredentialItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credential_details);

        initViews();
        initToolbar();

        credentialFile = getIntent().getStringExtra("credentialFile");
        awsS3Helper = new AwsS3Helper(this);

        loadCredentialDetails();
        setEditingEnabled(false); // Disable fields by default
    }

    private void initViews() {
        websiteText = findViewById(R.id.websiteText);
        usernameText = findViewById(R.id.usernameText);
        passwordText = findViewById(R.id.passwordText);
        notesText = findViewById(R.id.notesText);
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.credentialDetailsToolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadCredentialDetails() {
        awsS3Helper.fetchCredentialDetails(credentialFile, (credentialData, errorMessage) -> {
            runOnUiThread(() -> {
                if (errorMessage != null) {
                    showToast("Error: " + errorMessage);
                    return;
                }

                websiteText.setText(credentialData.getWebsite());
                usernameText.setText(credentialData.getUsername());
                passwordText.setText(credentialData.getPassword());
                notesText.setText(credentialData.getNotes());

                encryptedPassword = credentialData.getPassword();
                privateKey = credentialData.getPrivateKey();
                salt = credentialData.getSalt();
            });
        });
    }

    private void setEditingEnabled(boolean enabled) {
        websiteText.setEnabled(enabled);
        usernameText.setEnabled(enabled);
        passwordText.setText(encryptedPassword);
        passwordText.setEnabled(enabled);
        notesText.setEnabled(enabled);

        isEditing = enabled;

        if (saveMenuItem != null)
            saveMenuItem.setVisible(enabled);
        if (editMenuItem != null)
            editMenuItem.setVisible(!enabled);
        if (editMenuItem != null) {
            decryptCredentialItem.setVisible(!enabled);
            encryptCredentialItem.setVisible(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.credential_details_menu, menu);
        saveMenuItem = menu.findItem(R.id.save_credential);
        editMenuItem = menu.findItem(R.id.edit_credential);
        decryptCredentialItem = menu.findItem(R.id.decrypt_credential);
        encryptCredentialItem = menu.findItem(R.id.encrypt_credential);

        saveMenuItem.setVisible(false);
        editMenuItem.setVisible(true);
        decryptCredentialItem.setVisible(true);
        encryptCredentialItem.setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.edit_credential) {
            setEditingEnabled(true);
        } else if (id == R.id.save_credential) {
            saveCredential();
        } else if (id == R.id.decrypt_credential) {
            promptPrivateKeyForDecryption();
        } else if (id == R.id.encrypt_credential) {
            showEncryptedPassword();
        } else if (id == R.id.delete_credential) {
            confirmDelete();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showEncryptedPassword() {
        passwordText.setText(encryptedPassword);
        decryptCredentialItem.setVisible(true);
        encryptCredentialItem.setVisible(false);
    }

    private void promptPrivateKeyForDecryption() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("Verification")
                .setMessage("Enter your private key")
                .setView(input)
                .setPositiveButton("Decrypt", (dialog, which) -> {
                    String keyInput = input.getText().toString().trim();
                    if (keyInput.isEmpty()) {
                        showToast("Private key cannot be empty!");
                        return;
                    }

                    try {
                        String decryptedPassword = EncryptionHelper.decrypt(encryptedPassword, keyInput, salt);
                        passwordText.setText(decryptedPassword);
                        decryptCredentialItem.setVisible(false);
                        encryptCredentialItem.setVisible(true);
                    } catch (Exception e) {
                        showToast("Decryption failed. Invalid key!");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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
                    showToast("Credential deleted");
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
            showToast("All fields are required!");
            return;
        }

        checkAndUpdatePassword(newPassword, finalPassword -> {
            if (finalPassword == null) {
                showToast("Password not updated. Verification failed or cancelled.");
                return;
            }

            CredentialData updatedCredential = new CredentialData(
                    newWebsite, newUsername, finalPassword, privateKey, salt, newNotes
            );

            awsS3Helper.uploadCredentials(credentialFile, updatedCredential.toFileFormat(), success -> {
                runOnUiThread(() -> {
                    if (success) {
                        showToast("Credential updated");
                        setEditingEnabled(false);
                        startActivity(new Intent(this, CredentialListActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                        finish();
                    } else {
                        showToast("Update failed");
                    }
                });
            });
        });
    }

    private void checkAndUpdatePassword(String newPassword, PasswordCallback callback) {
        if (newPassword.equals(encryptedPassword)) {
            callback.onPasswordReady(newPassword);
            return;
        }

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("Verification")
                .setMessage("Enter your private key to update password")
                .setView(input)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String keyInput = input.getText().toString().trim();
                    if (keyInput.isEmpty()) {
                        showToast("Private key cannot be empty!");
                        callback.onPasswordReady(null);
                        return;
                    }

                    if (!keyInput.equals(privateKey)) {
                        showToast("Encryption failed. Invalid key!");
                        callback.onPasswordReady(null);
                        return;
                    }

                    try {
                        encryptedPassword = EncryptionHelper.encrypt(newPassword, privateKey, salt);
                        callback.onPasswordReady(encryptedPassword);
                    } catch (Exception e) {
                        Log.e("EncryptionHelper", "Encryption error: " + e.getMessage(), e);
                        callback.onPasswordReady(null);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> callback.onPasswordReady(null))
                .show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    interface PasswordCallback {
        void onPasswordReady(String finalPassword);
    }
}
