package com.example.project_pandora.ui.log;
import com.example.project_pandora.ui.people.PeopleDirectory;

import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.project_pandora.R;
import com.example.project_pandora.core.util.DateCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LogCommentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Map<String, Object>> comments = new ArrayList<>();
    public void setComments(List<Map<String, Object>> values) {
        comments.clear();
        for (Map<String, Object> value : values) {
            if (!"ADMIN".equalsIgnoreCase(PeopleDirectory.text(value, "author_role"))) comments.add(value);
        }
        notifyDataSetChanged();
    }
    @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        LinearLayout row = new LinearLayout(parent.getContext()); row.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(12 * parent.getResources().getDisplayMetrics().density);
        row.setPadding(0, padding, 0, padding);
        for (int i = 0; i < 2; i++) {
            TextView text = new TextView(parent.getContext()); text.setTextSize(i == 0 ? 13 : 16);
            text.setTextColor(ContextCompat.getColor(parent.getContext(), i == 0 ? R.color.color_text_secondary : R.color.color_text_primary));
            text.setTextIsSelectable(i == 1); text.setPadding(0, i == 1 ? padding / 2 : 0, 0, 0);
            row.addView(text, new LinearLayout.LayoutParams(-1, -2));
        }
        row.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
        return new RecyclerView.ViewHolder(row) {};
    }
    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Map<String, Object> comment = comments.get(position); LinearLayout row = (LinearLayout) holder.itemView;
        String time = DateCompat.toLocalDateTime(PeopleDirectory.value(comment, "created_at"));
        ((TextView) row.getChildAt(0)).setText(PeopleDirectory.text(comment, "author_name") + " · " + (time == null ? "" : time));
        ((TextView) row.getChildAt(1)).setText(PeopleDirectory.text(comment, "content"));
    }
    @Override public int getItemCount() { return comments.size(); }
}
