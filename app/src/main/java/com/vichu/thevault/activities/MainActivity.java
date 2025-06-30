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

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        LinearLayout addContactSection = findViewById(R.id.add_credentials_section);
        addContactSection.setOnClickListener(v -> openAddCredentialsScreen());
    }

    private void openAddCredentialsScreen() {
        Intent intent = new Intent(this, AddCredentialsActivity.class);
        startActivity(intent);
    }

}
