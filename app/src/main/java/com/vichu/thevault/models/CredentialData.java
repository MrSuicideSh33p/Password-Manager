package com.vichu.thevault.models;

public class CredentialData {
    private String website;
    private String username;
    private String password;
    private String notes;

    public CredentialData(String website, String username, String password, String notes) {
        this.website = website;
        this.username = username;
        this.password = password;
        this.notes = notes;
    }

    public CredentialData(String content) {
        String[] lines = content.split("\n");
        this.website = lines[0];
        this.username = lines[1];
        this.password = lines[2];
        this.notes = lines.length > 3 ? lines[3] : "";
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Converts the credential details into a formatted string for file storage.
     */
    public String toFileFormat() {
        return website + "\n" + username + "\n" + password + "\n" + notes;
    }
}
