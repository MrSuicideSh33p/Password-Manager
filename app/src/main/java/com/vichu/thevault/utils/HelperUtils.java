package com.vichu.thevault.utils;

import android.content.Context;
import android.widget.Toast;

public class HelperUtils {

    public static final String USERS_JSON = "credentials/user.json";

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
}
