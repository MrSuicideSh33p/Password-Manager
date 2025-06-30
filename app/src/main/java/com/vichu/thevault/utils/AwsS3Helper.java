package com.vichu.thevault.utils;

import android.content.Context;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.vichu.thevault.R;
import com.vichu.thevault.TheVaultApplication;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class AwsS3Helper {

    private static final String TAG = "AwsS3Helper";
    private static final String BUCKET_NAME = "the-vault-bucket";
    private static final String ENDPOINT = "s3.us-east-1.amazonaws.com";

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

    public void uploadCredentials(String fileName, String fileContent, UploadListener listener) {
        new Thread(() -> {
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
        }).start();
    }

    public interface UploadListener {
        void onSuccess(boolean success);
    }

}
