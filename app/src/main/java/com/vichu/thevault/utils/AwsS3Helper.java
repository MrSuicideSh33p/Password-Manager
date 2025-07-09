package com.vichu.thevault.utils;

import static com.vichu.thevault.utils.HelperUtils.BUCKET_NAME;
import static com.vichu.thevault.utils.HelperUtils.ENDPOINT;
import static com.vichu.thevault.utils.HelperUtils.TAG;
import static com.vichu.thevault.utils.HelperUtils.USERS_JSON;
import static com.vichu.thevault.utils.HelperUtils.getMetadataFile;
import static com.vichu.thevault.utils.HelperUtils.getUserFolder;
import static com.vichu.thevault.utils.HelperUtils.readInputStream;

import android.content.Context;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.vichu.thevault.R;
import com.vichu.thevault.models.CredentialData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AwsS3Helper {

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private AmazonS3 s3Client;
    public AwsS3Helper(Context context) {
        initializeS3Client(context);
    }

    private void initializeS3Client(Context context) {
        try {
            // Load credentials from raw/aws_credentials.properties
            Properties properties = new Properties();
            InputStream credentialsStream = context.getResources().openRawResource(R.raw.aws_credentials);
            properties.load(credentialsStream);

            String accessKey = properties.getProperty("AWS_ACCESS_KEY");
            String secretKey = properties.getProperty("AWS_SECRET_KEY");

            if (accessKey == null || secretKey == null) {
                throw new IllegalStateException("AWS credentials not found in properties file.");
            }

            s3Client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
            s3Client.setRegion(Region.getRegion(Regions.US_EAST_1));
            s3Client.setEndpoint(ENDPOINT);
            TransferNetworkLossHandler.getInstance(context);
            Log.d(TAG, "Amazon S3 client initialized successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AWS credentials: " + e.getMessage(), e);
        }
    }

    public void uploadCredentials(String fileName, String user, String fileContent, UploadListener listener) {
        executor.execute(() -> {
            try {
                if (fileName == null || fileName.isEmpty()) {
                    throw new IllegalArgumentException("Filename cannot be null or empty");
                }

                InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(fileContent.length());
                Log.d("S3_UPLOAD", "Uploading file: " + fileName);

                s3Client.putObject(BUCKET_NAME, fileName, inputStream, metadata);
                Log.d("S3_UPLOAD", "File uploaded successfully: " + fileName);

                // Extract credential name from fileContent
                String websiteName = extractWebsiteName(fileContent);
                // Update metadata file
                updateMetadataFile(user, fileName, websiteName);

                listener.onSuccess(true);
            } catch (AmazonServiceException e) {
                Log.e("S3_UPLOAD", "AWS Service error: " + e.getErrorMessage());
                listener.onSuccess(false);
            } catch (AmazonClientException e) {
                Log.e("S3_UPLOAD", "AWS Client error: " + e.getMessage());
                listener.onSuccess(false);
            } catch (IllegalArgumentException e) {
                Log.e("S3_UPLOAD", "Invalid Argument: " + e.getMessage());
                listener.onSuccess(false);
            } catch (Exception e) {
                Log.e("S3_UPLOAD", "Unexpected error: " + e.getMessage());
                listener.onSuccess(false);
            }
        });
    }

    private String extractWebsiteName(String fileContent) {
        return fileContent.split("\n")[0].trim(); // Assuming first line is the website
    }

    // Update metadata.json
    private void updateMetadataFile(String user, String fileName, String websiteName) {

        try {
            // Check if metadata.json exists
            String existingMetadataJson = "{}"; // Default empty JSON
            String metadataKey = getMetadataFile(user);

            try (S3Object metadataObject = s3Client.getObject(BUCKET_NAME, metadataKey)) {
                existingMetadataJson = new String(metadataObject.getObjectContent().readAllBytes(), StandardCharsets.UTF_8);
                Log.d("S3_METADATA", "Existing metadata loaded successfully.");
            } catch (AmazonS3Exception e) {
                if (e.getStatusCode() == 404) {
                    Log.w("S3_METADATA", "Metadata file not found. Creating a new one.");
                } else {
                    throw e;
                }
            }

            // Convert existing metadata to JSON object
            JSONObject metadataJson = new JSONObject(existingMetadataJson);
            metadataJson.put(fileName, websiteName);

            // Upload updated metadata
            InputStream updatedMetadataStream = new ByteArrayInputStream(metadataJson.toString().getBytes(StandardCharsets.UTF_8));
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(metadataJson.toString().length());

            s3Client.putObject(BUCKET_NAME, metadataKey, updatedMetadataStream, metadata);
            Log.d("S3_METADATA", "Metadata file updated successfully.");

        } catch (Exception e) {
            Log.e("S3_METADATA", "Failed to update metadata file: " + e.getMessage());
        }
    }

    // Create folder for user and update user.json
    //TODO check if this can also use fileContent and toFileFormat from model class
    public void registerUser(String userFolder, String username, String password, CreateListener listener) {
        executor.execute(() -> {
            try {
                // Check if user.json exists
                String existingUserJson = "{}"; // Default empty JSON

                try(S3Object userObject = s3Client.getObject(BUCKET_NAME, USERS_JSON)) {
                    existingUserJson = new String(userObject.getObjectContent().readAllBytes(), StandardCharsets.UTF_8);
                    Log.d("S3_USER", "Existing user.json loaded successfully.");
                } catch (AmazonS3Exception e) {
                    if (e.getStatusCode() == 404) {
                        Log.w("S3_USER", "User.json file not found. Creating a new one.");
                    } else {
                        listener.onSuccess(false);
                        throw e;
                    }
                }

                // Convert existing user to JSON object
                JSONObject userJson = new JSONObject(existingUserJson);
                userJson.put(username, password);

                // Upload updated user.json
                InputStream updatedUserStream = new ByteArrayInputStream(userJson.toString().getBytes(StandardCharsets.UTF_8));
                ObjectMetadata userMetadata = new ObjectMetadata();
                userMetadata.setContentLength(userJson.toString().length());

                s3Client.putObject(BUCKET_NAME, USERS_JSON, updatedUserStream, userMetadata);
                Log.d("S3_USER", "User.json file updated successfully.");

                try {
                    //Create user folder
                    ObjectMetadata userFolderMetadata = new ObjectMetadata();
                    userFolderMetadata.setContentLength(0L);

                    InputStream inputStream = new ByteArrayInputStream(new byte[0]);
                    PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME,
                            userFolder, inputStream, userFolderMetadata);

                    s3Client.putObject(putObjectRequest);
                    Log.d("S3_USER", "User folder successfully created.");
                    listener.onSuccess(true);
                } catch (Exception e) {
                    Log.e("S3_USER", "Failed to create user folder:" + e.getMessage());
                    listener.onSuccess(false);
                }
            } catch (Exception e) {
                Log.e("S3_USER", "Failed to update user.json file:" + e.getMessage());
                listener.onSuccess(false);
            }
        });
    }

    public void fetchCredentialList(String user, S3CredentialFetchListener listener) {
        executor.execute(() -> {
            try {
                Map<String, String> metadataMap = new HashMap<>();
                String metadataFile = getMetadataFile(user);

                // Try fetching metadata.json
                try (S3Object metadataObject = s3Client.getObject(BUCKET_NAME, metadataFile)) {
                    String jsonString = readInputStream(metadataObject.getObjectContent());
                    // Convert JSON to Map
                    JSONObject metadataJson = new JSONObject(jsonString);
                    for (Iterator<String> it = metadataJson.keys(); it.hasNext(); ) {
                        String key = it.next();
                        metadataMap.put(key, metadataJson.getString(key));
                    }
                    Log.d(TAG, "Metadata loaded successfully: " + metadataMap.size() + " credentials.");
                } catch (AmazonS3Exception e) {
                    Log.e(TAG, "metadata.json not found. Proceeding without it.");
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing metadata.json: " + e.getMessage());
                }

                // Fetch all credential files from S3
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(BUCKET_NAME).withPrefix(getUserFolder(user));
                ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
                List<String> names = new ArrayList<>();
                List<String> files = new ArrayList<>();

                for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    String fileName = objectSummary.getKey();

                    // Ignore json files and folder itself
                    if (fileName.equals(metadataFile) || fileName.equals(USERS_JSON) || fileName.endsWith("/"))
                        continue;

                    files.add(fileName);

                    // Retrieve the credential name from metadata.json
                    if (metadataMap.containsKey(fileName)) {
                        names.add(metadataMap.get(fileName));
                    } else {
                        names.add(fileName.substring(fileName.lastIndexOf('/') + 1)); // Fallback to UUID
                    }
                }

                Log.d(TAG, "Fetched " + files.size() + " credentials from S3.");
                listener.onSuccess(names, files);
            } catch (Exception e) {
                Log.e(TAG, "Error fetching credentials: " + e.getMessage());
                listener.onError(e.getMessage());
            }
        });
    }

    public void fetchCredentialDetails(String fileName, CredentialDataListener listener) {
        executor.execute(() -> {
            try (S3Object s3Object = s3Client.getObject(BUCKET_NAME, fileName)) {
                String content = readInputStream(s3Object.getObjectContent(), true);
                CredentialData credentialData = new CredentialData(content);
                listener.onResult(credentialData, null);
            } catch (Exception e) {
                listener.onResult(null, e.getMessage());
            }
        });
    }

    public void fetchUserDetails(String fileName, UserDataListener listener) {
        executor.execute(() -> {
            try {
                boolean bool = s3Client.doesObjectExist(BUCKET_NAME, fileName);
                if (!bool) {
                    listener.onResult(null, null);
                    return;
                }
                S3Object s3Object = s3Client.getObject(BUCKET_NAME, fileName);
                String content = readInputStream(s3Object.getObjectContent());
                s3Object.close();
                listener.onResult(new JSONObject(content), null);
            } catch (Exception e) {
                listener.onResult(null, e.getMessage());
            }
        });
    }

    //Update User Details
    public void updateUserDetails(String username, String password, CreateListener listener) {
        executor.execute(() -> {
            try {
                String existingUserJson = "{}"; // Default empty JSON

                try(S3Object userObject = s3Client.getObject(BUCKET_NAME, USERS_JSON)) {
                    existingUserJson = new String(userObject.getObjectContent().readAllBytes(), StandardCharsets.UTF_8);
                    Log.d("S3_USER", "Existing user.json loaded successfully.");
                } catch (AmazonS3Exception e) {
                    listener.onSuccess(false);
                    throw e;
                }

                // Convert existing user to JSON object
                JSONObject userJson = new JSONObject(existingUserJson);
                userJson.put(username, password);

                // Upload updated user.json
                InputStream updatedUserStream = new ByteArrayInputStream(userJson.toString().getBytes(StandardCharsets.UTF_8));
                ObjectMetadata userMetadata = new ObjectMetadata();
                userMetadata.setContentLength(userJson.toString().length());

                s3Client.putObject(BUCKET_NAME, USERS_JSON, updatedUserStream, userMetadata);
                Log.d("S3_USER", "User.json file updated successfully.");
                listener.onSuccess(true);
            } catch (Exception e) {
                Log.e("S3_USER", "Failed to update user.json file:" + e.getMessage());
                listener.onSuccess(false);
            }
        });
    }

    public void deleteCredential(String fileName, DeleteListener listener) {
        executor.execute(() -> {
            try {
                s3Client.deleteObject(new DeleteObjectRequest(BUCKET_NAME, fileName));
                listener.onSuccess(true);
            } catch (Exception e) {
                listener.onSuccess(false);
            }
        });
    }

    public interface UploadListener {
        void onSuccess(boolean success);
    }

    public interface S3CredentialFetchListener {
        void onSuccess(List<String> names, List<String> files);
        void onError(String error);
    }

    public interface CredentialDataListener {
        void onResult(CredentialData credentialData, String errorMessage);
    }

    public interface UserDataListener {
        void onResult(JSONObject userDataList, String errorMessage);
    }

    public interface CreateListener {
        void onSuccess(boolean success);
    }

    public interface DeleteListener {
        void onSuccess(boolean success);
    }

}
