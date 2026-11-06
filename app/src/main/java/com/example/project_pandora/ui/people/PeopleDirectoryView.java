package com.example.project_pandora.ui.people;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.project_pandora.core.util.NumberCompat;
import com.example.project_pandora.core.util.RoleDisplayUtil;
import com.example.project_pandora.databinding.ItemPeopleDirectoryBinding;
import com.example.project_pandora.databinding.ViewPeopleDirectoryBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Department navigation, search, rows and request states shared by both personnel screens. */
public class PeopleDirectoryView extends LinearLayout {
    private final ViewPeopleDirectoryBinding binding;
    private final RowsAdapter adapter = new RowsAdapter();
    private List<PeopleDirectory.Department> departments = new ArrayList<>();
    private String departmentKey, rootQuery = "", emptyMessage = "暂无可查看人员";
    private boolean loading, failed, ready;
    private Runnable retry;
    private Consumer<Map<String, Object>> personClick;
    private Consumer<PeopleDirectory.Department> allClick;

    public PeopleDirectoryView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        binding = ViewPeopleDirectoryBinding.inflate(LayoutInflater.from(context), this);
        binding.recyclerPeople.setLayoutManager(new LinearLayoutManager(context));
        binding.recyclerPeople.setAdapter(adapter);
        binding.buttonDepartments.setOnClickListener(v -> back());
        binding.buttonRetry.setOnClickListener(v -> { if (retry != null) retry.run(); });
        binding.buttonAll.setOnClickListener(v -> { if (allClick != null && ready) allClick.accept(currentDepartment()); });
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { render(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    public void configure(String description, String emptyMessage, Runnable retry,
                          Consumer<Map<String, Object>> personClick,
                          @Nullable Consumer<PeopleDirectory.Department> allClick) {
        binding.textDescription.setText(description);
        this.emptyMessage = emptyMessage;
        this.retry = retry;
        this.personClick = personClick;
        this.allClick = allClick;
        render();
    }

    public void showLoading() { loading = true; failed = false; ready = false; render(); }
    public void showError() { loading = false; failed = true; ready = false; render(); }
    public void setPeople(List<Map<String, Object>> people) {
        departments = PeopleDirectory.departments(people);
        if (departmentKey != null && currentDepartment() == null) {
            departmentKey = null; binding.editSearch.setText(rootQuery);
        }
        loading = false; failed = false; ready = true; render();
    }

    @Nullable public PeopleDirectory.Department currentDepartment() {
        for (PeopleDirectory.Department department : departments) if (department.key.equals(departmentKey)) return department;
        return null;
    }

    public boolean back() {
        if (departmentKey == null) return false;
        departmentKey = null;
        binding.editSearch.setText(rootQuery);
        render();
        return true;
    }

    public Bundle saveDirectoryState() {
        Bundle state = new Bundle();
        state.putString("departmentKey", departmentKey);
        state.putString("rootQuery", rootQuery);
        state.putString("query", binding.editSearch.getText().toString());
        return state;
    }
    public void restoreDirectoryState(@Nullable Bundle state) {
        if (state == null) return;
        departmentKey = state.getString("departmentKey");
        rootQuery = state.getString("rootQuery", "");
        binding.editSearch.setText(state.getString("query", ""));
    }

    private void enter(PeopleDirectory.Department department) {
        rootQuery = binding.editSearch.getText().toString();
        departmentKey = department.key;
        // Keep a person search when drilling into a matching department.
        binding.editSearch.setText(department.name.toLowerCase(Locale.ROOT).contains(rootQuery.trim().toLowerCase(Locale.ROOT)) ? "" : rootQuery);
        render();
        binding.recyclerPeople.scrollToPosition(0);
    }

    private void render() {
        if (binding == null) return;
        boolean root = departmentKey == null;
        PeopleDirectory.Department department = currentDepartment();
        binding.buttonDepartments.setVisibility(root ? GONE : VISIBLE);
        binding.textDirectoryTitle.setText(root ? "选择部门" : department == null ? "部门人员" : department.name);
        binding.searchLayout.setHint(root ? "搜索部门、姓名、账号或小组" : "搜索姓名、账号或小组");
        binding.editSearch.setEnabled(ready);
        binding.progressBar.setVisibility(loading ? VISIBLE : GONE);
        binding.buttonRetry.setVisibility(failed ? VISIBLE : GONE);
        binding.buttonAll.setVisibility(allClick == null ? GONE : VISIBLE);
        binding.buttonAll.setEnabled(ready);
        binding.buttonAll.setText(root ? "全部可查看人员" : "该部门全部可查看人员");
        String query = binding.editSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<Row> rows = new ArrayList<>();
        if (ready && root) {
            for (PeopleDirectory.Department group : departments) {
                if (!group.name.toLowerCase(Locale.ROOT).contains(query) && PeopleDirectory.search(group, query).isEmpty()) continue;
                rows.add(new Row(group.name, group.people.size() + " 位人员", true, () -> enter(group)));
            }
        } else if (ready && department != null) {
            for (Map<String, Object> person : PeopleDirectory.search(department, query)) {
                List<String> details = new ArrayList<>();
                String username = PeopleDirectory.text(person, "username");
                if (!username.isEmpty()) details.add(username);
                String role = PeopleDirectory.text(person, "role");
                if (!role.isEmpty()) details.add(RoleDisplayUtil.toChinese(role));
                String team = PeopleDirectory.text(person, "team_name");
                if (!team.isEmpty()) details.add(team);
                String relation = PeopleDirectory.text(person, "relation_kind");
                int depth = NumberCompat.toIntOrDefault(PeopleDirectory.value(person, "depth"), 0);
                if (!relation.isEmpty()) details.add((depth == 1 ? "直属" : depth > 1 ? "间接" : "") + relation);
                rows.add(new Row(PeopleDirectory.name(person), String.join(" · ", details), false,
                        () -> { if (personClick != null) personClick.accept(person); }));
            }
        }
        adapter.setRows(rows);
        int count = 0;
        for (PeopleDirectory.Department group : departments) count += group.people.size();
        binding.textCount.setText(root ? departments.size() + " 个部门 · " + count + " 位人员"
                : (department == null ? 0 : department.people.size()) + " 位人员");
        binding.textCount.setVisibility(ready ? VISIBLE : GONE);
        binding.recyclerPeople.setVisibility(ready ? VISIBLE : GONE);
        binding.textEmpty.setText(failed ? "人员加载失败，请重试" : query.isEmpty() ? emptyMessage : "未找到匹配结果，请尝试其他关键词");
        binding.textEmpty.setVisibility(failed || ready && rows.isEmpty() ? VISIBLE : GONE);
    }

    private static final class Row {
        final String title, detail; final boolean department; final Runnable click;
        Row(String title, String detail, boolean department, Runnable click) {
            this.title = title; this.detail = detail; this.department = department; this.click = click;
        }
    }
    private static final class Holder extends RecyclerView.ViewHolder {
        final ItemPeopleDirectoryBinding binding;
        Holder(ItemPeopleDirectoryBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }
    private static final class RowsAdapter extends RecyclerView.Adapter<Holder> {
        private List<Row> rows = new ArrayList<>();
        void setRows(List<Row> rows) { this.rows = rows; notifyDataSetChanged(); }
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new Holder(ItemPeopleDirectoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            Row row = rows.get(position);
            holder.binding.textName.setText(row.title);
            holder.binding.textDetail.setText(row.detail);
            holder.binding.imageChevron.setVisibility(row.department ? View.VISIBLE : View.INVISIBLE);
            holder.itemView.setOnClickListener(v -> row.click.run());
        }
        @Override public int getItemCount() { return rows.size(); }
    }
}
