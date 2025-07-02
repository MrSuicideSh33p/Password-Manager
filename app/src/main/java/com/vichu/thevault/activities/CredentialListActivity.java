package com.vichu.thevault.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.vichu.thevault.R;
import com.vichu.thevault.utils.AwsS3Helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CredentialListActivity extends AppCompatActivity {

    private MenuItem searchItem;
    private TextView progressText;
    private ListView credentialListView;
    private ImageView emptyStateImage;
    private ProgressBar progressBar;
    private ArrayAdapter<String> adapter;
    private final List<String> credentialNames = new ArrayList<>();
    private final List<String> reusableCredentialNames = new ArrayList<>(); // Store original list separately
    private final List<String> credentialFiles = new ArrayList<>();
    private final List<Integer> filteredIndices = new ArrayList<>();
    private AwsS3Helper awsS3Helper;
    private static final int REQUEST_CODE_CREDENTIAL_DETAILS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credential_list);
        emptyStateImage = findViewById(R.id.emptyStateImage);
        credentialListView = findViewById(R.id.credentialListView);

        Toolbar toolbar = findViewById(R.id.credentialListToolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressText = findViewById(R.id.progressText);
        progressBar = findViewById(R.id.progressBar);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, credentialNames);
        credentialListView.setAdapter(adapter);

        awsS3Helper = new AwsS3Helper(this);

        fetchCredentials();

        credentialListView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(CredentialListActivity.this, CredentialDetailsActivity.class);
            int originalIndex = (filteredIndices.isEmpty()) ? position : filteredIndices.get(position);
            intent.putExtra("credentialFile", credentialFiles.get(originalIndex));
            startActivityForResult(intent, REQUEST_CODE_CREDENTIAL_DETAILS);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.credential_list_menu, menu);
        searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setIconified(false);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false; // No need to handle submit separately
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    resetList(); // Show all credentials when empty
                } else {
                    filterCredentials(newText); // Perform fuzzy search
                }
                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            searchView.setQuery("", false);  // Clear search bar text
            searchItem.collapseActionView(); // Collapse search view
            return false;
        });

        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                searchView.setQuery("", false);  // Clear text when losing focus
                searchView.setIconified(false);
            }
        });

        return true;
    }

    public void filterCredentials(String query) {
        credentialNames.clear();
        filteredIndices.clear(); // Reset indices tracking

        for (int i = 0; i < reusableCredentialNames.size(); i++) {
            if (reusableCredentialNames.get(i).toLowerCase().contains(query.toLowerCase())) {
                credentialNames.add(reusableCredentialNames.get(i));
                filteredIndices.add(i);  // Store the original index
            }
        }

        adapter.notifyDataSetChanged();
    }

    public void resetList() {
        credentialNames.clear();
        credentialNames.addAll(reusableCredentialNames);
        adapter.notifyDataSetChanged();
    }

    private void fetchCredentials() {
        progressBar.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);

        awsS3Helper.fetchCredentialList(new AwsS3Helper.S3CredentialFetchListener() {
            @Override
            public void onSuccess(List<String> names, List<String> files) {
                credentialNames.clear();
                credentialFiles.clear();
                credentialNames.addAll(names);
                credentialFiles.addAll(files);

                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    progressText.setVisibility(View.GONE);

                    if (credentialNames.isEmpty()) {
                        emptyStateImage.setVisibility(View.VISIBLE);
                        credentialListView.setVisibility(View.GONE);
                    } else {
                        reusableCredentialNames.clear();
                        reusableCredentialNames.addAll(credentialNames);
                        emptyStateImage.setVisibility(View.GONE);
                        credentialListView.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(CredentialListActivity.this, "Error fetching credentials: " + error, Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    progressText.setVisibility(View.GONE);
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CREDENTIAL_DETAILS && resultCode == RESULT_OK) {
            fetchCredentials();  // Refresh credentials after an update or delete
        }
    }
}
