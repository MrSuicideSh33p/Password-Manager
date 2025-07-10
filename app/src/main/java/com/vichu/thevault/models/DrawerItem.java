package com.vichu.thevault.models;

import java.util.List;

public class DrawerItem {
    private String title;
    private boolean isExpanded;
    private List<SubItem> subItems;

    public DrawerItem(String title, List<SubItem> subItems) {
        this.title = title;
        this.subItems = subItems;
        this.isExpanded = false;
    }

    public String getTitle() {
        return title;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public List<SubItem> getSubItems() {
        return subItems;
    }
}


