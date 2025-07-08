package com.vichu.thevault.activities;

import static com.vichu.thevault.utils.HelperUtils.getWelcomeText;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.vichu.thevault.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        String username = intent.getStringExtra("user");

        TextView welcomeText = findViewById(R.id.welcome_text);
        welcomeText.setText(getWelcomeText(username));

        LinearLayout addCredentialSection = findViewById(R.id.add_credentials_section);
        addCredentialSection.setOnClickListener(v -> openAddCredentialsScreen(username));

        LinearLayout viewCredentialSection = findViewById(R.id.view_credentials_section);
        viewCredentialSection.setOnClickListener(v -> openViewCredentialsScreen(username));
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
            logout();
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
