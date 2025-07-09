package com.vichu.thevault.activities;

import static com.vichu.thevault.utils.HelperUtils.DISPLAY_NAME_FIELD;
import static com.vichu.thevault.utils.HelperUtils.USERS_JSON;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.vichu.thevault.R;
import com.vichu.thevault.utils.AwsS3Helper;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private AwsS3Helper awsS3Helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        awsS3Helper = new AwsS3Helper(this);

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        String username = intent.getStringExtra("user");
        setWelcomeText(username);

        LinearLayout addCredentialSection = findViewById(R.id.add_credentials_section);
        addCredentialSection.setOnClickListener(v -> openAddCredentialsScreen(username));

        LinearLayout viewCredentialSection = findViewById(R.id.view_credentials_section);
        viewCredentialSection.setOnClickListener(v -> openViewCredentialsScreen(username));

        // Initialize DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Setup the Drawer Toggle
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Handle Navigation Drawer Clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_display_name) {
                openSetDisplayNameScreen(username);
            } else if (id == R.id.nav_reset_password) {
                openResetPasswordScreen(username);
            } else if (id == R.id.nav_add_credentials) {
                openAddCredentialsScreen(username);
            } else if (id == R.id.nav_view_credentials) {
                openViewCredentialsScreen(username);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setWelcomeText(String user) {
        awsS3Helper.fetchUserDetails(USERS_JSON, (userData, errorMessage) -> runOnUiThread(() -> {
            String displayName = user;
            if (errorMessage == null) {
                String existingDisplayName = userData.get(user).get(DISPLAY_NAME_FIELD).asText(null);
                if (existingDisplayName != null) {
                    displayName = existingDisplayName;
                }
            }

            TextView welcomeText = findViewById(R.id.welcome_text);
            welcomeText.setText(new StringBuilder()
                    .append("Welcome ")
                    .append(displayName.replaceAll("@.*", ""))
                    .append("!")
                    .toString());
        }));
    }

    private void openSetDisplayNameScreen(String username) {
        Intent intent = new Intent(this, DisplayNameActivity.class);
        intent.putExtra("user", username);
        startActivity(intent);
    }

    private void openResetPasswordScreen(String username) {
        Intent intent = new Intent(this, ResetPasswordActivity.class);
        intent.putExtra("user", username);
        startActivity(intent);
    }

    private void openAddCredentialsScreen(String username) {
        Intent intent = new Intent(this, AddCredentialsActivity.class);
        intent.putExtra("user", username);
        startActivity(intent);
    }

    private void openViewCredentialsScreen(String username) {
        Intent intent = new Intent(this, CredentialListActivity.class);
        intent.putExtra("user", username);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            Snackbar.make(findViewById(android.R.id.content), "Are you sure you want to logout?", Snackbar.LENGTH_LONG)
                    .setAction("Logout", v -> logout())
                    .setDuration(5000)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
