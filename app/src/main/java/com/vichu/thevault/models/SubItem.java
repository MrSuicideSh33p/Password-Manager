package com.vichu.thevault.models;

public class SubItem {
    private final String title;
    private final Runnable action;

    public SubItem(String title, Runnable action) {
        this.title = title;
        this.action = action;
    }

    public String getTitle() {
        return title;
    }

    public Runnable getAction() {
        return action;
    }
}

