package com.vichu.thevault.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.vichu.thevault.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        LinearLayout addCredentialSection = findViewById(R.id.add_credentials_section);
        addCredentialSection.setOnClickListener(v -> openAddCredentialsScreen());

        LinearLayout viewCredentialSection = findViewById(R.id.view_credentials_section);
        viewCredentialSection.setOnClickListener(v -> openViewCredentialsScreen());
    }

    private void openAddCredentialsScreen() {
        Intent intent = new Intent(this, AddCredentialsActivity.class);
        startActivity(intent);
    }

    private void openViewCredentialsScreen() {
        Intent intent = new Intent(this, CredentialListActivity.class);
        startActivity(intent);
    }

}
