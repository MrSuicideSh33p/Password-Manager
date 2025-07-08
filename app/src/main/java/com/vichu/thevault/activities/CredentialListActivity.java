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

    private static final int REQUEST_CODE_CREDENTIAL_DETAILS = 1;

    private MenuItem searchItem;
    private TextView progressText;
    private ListView credentialListView;
    private ImageView emptyStateImage;
    private ProgressBar progressBar;

    private ArrayAdapter<String> adapter;

    private final List<String> credentialNames = new ArrayList<>();
    private final List<String> reusableCredentialNames = new ArrayList<>();
    private final List<String> credentialFiles = new ArrayList<>();
    private final List<Integer> filteredIndices = new ArrayList<>();

    private AwsS3Helper awsS3Helper;
    private String user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credential_list);

        Intent intent = getIntent();
        user = intent.getStringExtra("user");

        initViews();
        initToolbar();
        initListView();

        awsS3Helper = new AwsS3Helper(this);
        fetchCredentials();
    }

    private void initViews() {
        emptyStateImage = findViewById(R.id.emptyStateImage);
        credentialListView = findViewById(R.id.credentialListView);
        progressText = findViewById(R.id.progressText);
        progressBar = findViewById(R.id.progressBar);
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.credentialListToolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initListView() {
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, credentialNames);
        credentialListView.setAdapter(adapter);

        credentialListView.setOnItemClickListener((parent, view, position, id) -> {
            int originalIndex = filteredIndices.isEmpty() ? position : filteredIndices.get(position);
            String selectedFile = credentialFiles.get(originalIndex);

            Intent intent = new Intent(this, CredentialDetailsActivity.class);
            intent.putExtra("credentialFile", selectedFile);
            intent.putExtra("user", user);
            startActivityForResult(intent, REQUEST_CODE_CREDENTIAL_DETAILS);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.credential_list_menu, menu);
        searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        setupSearchView(searchView);
        return true;
    }

    private void setupSearchView(SearchView searchView) {
        searchView.setIconified(false);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
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
    }

    private void filterCredentials(String query) {
        credentialNames.clear();
        filteredIndices.clear(); // Reset indices tracking

        for (int i = 0; i < reusableCredentialNames.size(); i++) {
            String name = reusableCredentialNames.get(i);
            if (name.toLowerCase().contains(query.toLowerCase())) {
                credentialNames.add(name);
                filteredIndices.add(i);  // Store the original index
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void resetList() {
        credentialNames.clear();
        credentialNames.addAll(reusableCredentialNames);
        adapter.notifyDataSetChanged();
    }

    private void fetchCredentials() {
        progressBar.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);

        awsS3Helper.fetchCredentialList(user, new AwsS3Helper.S3CredentialFetchListener() {
            @Override
            public void onSuccess(List<String> names, List<String> files) {
                runOnUiThread(() -> {
                    credentialNames.clear();
                    credentialFiles.clear();

                    credentialNames.addAll(names);
                    credentialFiles.addAll(files);
                    reusableCredentialNames.clear();
                    reusableCredentialNames.addAll(names);

                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    progressText.setVisibility(View.GONE);

                    updateEmptyStateUI(names.isEmpty());
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

    private void updateEmptyStateUI(boolean isEmpty) {
        emptyStateImage.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        credentialListView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CREDENTIAL_DETAILS && resultCode == RESULT_OK) {
            fetchCredentials();  // Refresh credentials after an update or delete
        }
    }
}
