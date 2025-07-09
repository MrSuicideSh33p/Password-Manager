package com.vichu.thevault.utils;

import android.content.Context;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HelperUtils {

    public static final String PASSWORD_FIELD = "password";
    public static final String DISPLAY_NAME_FIELD = "displayName";
    public static final String USERS_JSON = "credentials/user.json";
    public static final String TAG = "AwsS3Helper";
    public static final String BUCKET_NAME = "the-vault-bucket";
    public static final String ENDPOINT = "s3.us-east-1.amazonaws.com";

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static String getWelcomeText(String username) {
        return new StringBuilder()
                .append("Welcome ")
                .append(username.replaceAll("@.*", ""))
                .append("!")
                .toString();
    }

    public static String getUserFolder(String username) {
        return new StringBuilder()
                .append("credentials/")
                .append(username)
                .append("/")
                .toString();
    }

    public static String getMetadataFile(String username) {
        return new StringBuilder()
                .append("credentials/")
                .append(username)
                .append("/")
                .append("metadata.json")
                .toString();
    }

    public static String readInputStream(InputStream inputStream) throws IOException {
        return readInputStream(inputStream, false);
    }

    public static String readInputStream(InputStream inputStream, boolean preserveNewLines) throws IOException {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
                if (preserveNewLines)
                    content.append("\n");
            }
            return content.toString();
        }
    }
}
