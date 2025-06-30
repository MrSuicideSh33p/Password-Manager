package com.vichu.passwordmanager.activities;


import android.app.AlertDialog;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class AddPasswordActivity extends AppCompatActivity {

    private EditText websiteInput, usernameInput, passwordInput, notesInput;

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

    private boolean isAnyFieldFilled() {
        return !websiteInput.getText().toString().trim().isEmpty() ||
                !usernameInput.getText().toString().trim().isEmpty() ||
                !passwordInput.getText().toString().trim().isEmpty() ||
                !notesInput.getText().toString().trim().isEmpty();
    }
}
