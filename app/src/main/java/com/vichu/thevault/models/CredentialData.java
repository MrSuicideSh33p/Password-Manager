package com.vichu.thevault.models;

public class CredentialData {
    private final String website;
    private final String username;
    private final String password;
    private final String notes;
    private final String privateKey;
    private final String salt;

    public CredentialData(String website, String username, String password, String privateKey, String salt, String notes) {
        this.website = website;
        this.username = username;
        this.password = password;
        this.notes = notes;
        this.privateKey = privateKey;
        this.salt = salt;
    }

    public CredentialData(String content) {
        String[] lines = content.split("\n");
        this.website = lines[0];
        this.username = lines[1];
        this.password = lines[2];
        this.privateKey = lines[3];
        this.salt = lines[4];
        this.notes = lines.length > 5 ? lines[5] : "";
    }

    public String getWebsite() {
        return website;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getSalt() {
        return salt;
    }

    public String getNotes() {
        return notes;
    }

    /**
     * Converts the credential details into a formatted string for file storage.
     */
    public String toFileFormat() {
        return website + "\n" + username + "\n" + password + "\n" + privateKey + "\n" + salt + "\n" + notes;
    }
}
