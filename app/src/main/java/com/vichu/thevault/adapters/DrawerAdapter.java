package com.vichu.thevault.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.vichu.thevault.R;
import com.vichu.thevault.models.DrawerItem;
import com.vichu.thevault.models.SubItem;

import java.util.ArrayList;
import java.util.List;

public class DrawerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CHILD = 1;

    private final List<Object> displayList = new ArrayList<>();
    private final List<DrawerItem> originalList;

    public DrawerAdapter(List<DrawerItem> drawerItems) {
        this.originalList = drawerItems;
        buildDisplayList();
    }

    private void buildDisplayList() {
        displayList.clear();
        for (DrawerItem item : originalList) {
            displayList.add(item);
            if (item.isExpanded()) {
                displayList.addAll(item.getSubItems());
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return displayList.get(position) instanceof DrawerItem ? TYPE_HEADER : TYPE_CHILD;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.drawer_item, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.drawer_subitem, parent, false);
            return new ChildViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            DrawerItem item = (DrawerItem) displayList.get(position);
            HeaderViewHolder hvh = (HeaderViewHolder) holder;
            hvh.title.setText(item.getTitle());
            hvh.itemView.setOnClickListener(v -> {
                item.setExpanded(!item.isExpanded());
                buildDisplayList();
            });
        } else {
            SubItem sub = (SubItem) displayList.get(position);
            ChildViewHolder cvh = (ChildViewHolder) holder;
            cvh.title.setText(sub.getTitle());
            cvh.itemView.setOnClickListener(v -> sub.getAction().run());
        }
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        HeaderViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.item_title);
        }
    }

    static class ChildViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ChildViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.subitem_title);
        }
    }
}
