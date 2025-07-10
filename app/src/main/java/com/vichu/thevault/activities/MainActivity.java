package com.vichu.thevault.activities;

import static com.vichu.thevault.utils.HelperUtils.DISPLAY_NAME_FIELD;
import static com.vichu.thevault.utils.HelperUtils.USERS_JSON;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.vichu.thevault.R;
import com.vichu.thevault.adapters.DrawerAdapter;
import com.vichu.thevault.models.DrawerItem;
import com.vichu.thevault.models.SubItem;
import com.vichu.thevault.utils.AwsS3Helper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String user;
    private AwsS3Helper awsS3Helper;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private RecyclerView drawerRecyclerView;
    private DrawerAdapter drawerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        awsS3Helper = new AwsS3Helper(this);
        user = getIntent().getStringExtra("user");

        setupToolbar();
        setupDrawerLayout();
        setWelcomeText();
        setupMainSection();
        setupDrawerItems();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Show hamburger icon
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    private void setupDrawerLayout() {
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerRecyclerView = findViewById(R.id.drawer_recycler);

        // Setup the Drawer Toggle
        toggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setWelcomeText() {
        awsS3Helper.fetchUserDetails(USERS_JSON, (userData, errorMessage) -> runOnUiThread(() -> {
            String displayName = user;
            if (errorMessage == null) {
                String existingDisplayName = userData.get(user).get(DISPLAY_NAME_FIELD).asText(null);
                if (existingDisplayName != null) {
                    displayName = existingDisplayName;
                }
            }
            TextView welcomeText = findViewById(R.id.welcome_text);
            welcomeText.setText("Welcome " + displayName.replaceAll("@.*", "") + "!");
        }));
    }

    private void setupMainSection() {
        LinearLayout addCredentialSection = findViewById(R.id.add_credentials_section);
        LinearLayout viewCredentialSection = findViewById(R.id.view_credentials_section);

        addCredentialSection.setOnClickListener(v -> openAddCredentialsScreen(user));
        viewCredentialSection.setOnClickListener(v -> openViewCredentialsScreen(user));
    }

    private void setupDrawerItems() {
        drawerRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<DrawerItem> drawerItems = new ArrayList<>();

        List<SubItem> profileItems = new ArrayList<>();
        profileItems.add(new SubItem("Display Name", () -> openSetDisplayNameScreen(user)));
        profileItems.add(new SubItem("Reset Password", () -> openResetPasswordScreen(user)));

        List<SubItem> credentialItems = new ArrayList<>();
        credentialItems.add(new SubItem("Add Credentials", () -> openAddCredentialsScreen(user)));
        credentialItems.add(new SubItem("View Credentials", () -> openViewCredentialsScreen(user)));

        drawerItems.add(new DrawerItem("My Profile", profileItems));
        drawerItems.add(new DrawerItem("Manage Credentials", credentialItems));

        drawerAdapter = new DrawerAdapter(drawerItems);
        drawerRecyclerView.setAdapter(drawerAdapter);
    }

    private void openSetDisplayNameScreen(String username) {
        startActivity(new Intent(this, DisplayNameActivity.class).putExtra("user", username));
    }

    private void openResetPasswordScreen(String username) {
        startActivity(new Intent(this, ResetPasswordActivity.class).putExtra("user", username));
    }

    private void openAddCredentialsScreen(String username) {
        startActivity(new Intent(this, AddCredentialsActivity.class).putExtra("user", username));
    }

    private void openViewCredentialsScreen(String username) {
        startActivity(new Intent(this, CredentialListActivity.class).putExtra("user", username));
    }

    private void logout() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle toggle first
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }

        if (item.getItemId() == R.id.action_logout) {
            Snackbar.make(findViewById(android.R.id.content), "Are you sure you want to logout?", Snackbar.LENGTH_LONG)
                    .setAction("Logout", v -> logout())
                    .setDuration(5000)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
