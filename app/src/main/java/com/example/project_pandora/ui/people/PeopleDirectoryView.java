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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.function.Consumer;

/** Department navigation, search, rows and request states shared by both personnel screens. */
public class PeopleDirectoryView extends LinearLayout {
    private final ViewPeopleDirectoryBinding binding;
    private final RowsAdapter adapter = new RowsAdapter();
    private List<PeopleDirectory.Department> departments = new ArrayList<>();
    private String departmentKey, teamKey, rootQuery = "", departmentQuery = "", emptyMessage = "暂无可查看人员";
    private boolean loading, failed, ready;
    private Runnable retry;
    private Consumer<Map<String, Object>> personClick;
    private Consumer<PeopleDirectory.Department> allClick;
    private boolean allOptionRootOnly;
    private String allOptionLabel;
    private final java.util.Set<Long> selectedIds = new LinkedHashSet<>();
    private boolean selectionMode;

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
    public void setAllOption(String label, boolean rootOnly) {
        allOptionLabel = label; allOptionRootOnly = rootOnly; render();
    }
    public void setSelectionMode(boolean enabled) { selectionMode = enabled; render(); }
    public void setSelectedIds(Collection<Long> ids) { selectedIds.clear(); if (ids != null) selectedIds.addAll(ids); render(); }
    public void showError() { loading = false; failed = true; ready = false; render(); }
    public void setPeople(List<Map<String, Object>> people) {
        departments = PeopleDirectory.departments(people);
        if (departmentKey != null && currentDepartment() == null) {
            departmentKey = null; teamKey = null; binding.editSearch.setText(rootQuery);
        }
        loading = false; failed = false; ready = true; render();
    }

    @Nullable public PeopleDirectory.Department currentDepartment() {
        for (PeopleDirectory.Department department : departments) if (department.key.equals(departmentKey)) return department;
        return null;
    }

    public boolean back() {
        if (teamKey != null) {
            teamKey = null;
            binding.editSearch.setText(departmentQuery);
            render();
            return true;
        }
        if (departmentKey == null) return false;
        departmentKey = null;
        binding.editSearch.setText(rootQuery);
        render();
        return true;
    }

    public Bundle saveDirectoryState() {
        Bundle state = new Bundle();
        state.putString("departmentKey", departmentKey);
        state.putString("teamKey", teamKey);
        state.putString("rootQuery", rootQuery);
        state.putString("departmentQuery", departmentQuery);
        state.putString("query", binding.editSearch.getText().toString());
        return state;
    }
    public void restoreDirectoryState(@Nullable Bundle state) {
        if (state == null) return;
        departmentKey = state.getString("departmentKey");
        teamKey = state.getString("teamKey");
        rootQuery = state.getString("rootQuery", "");
        departmentQuery = state.getString("departmentQuery", "");
        binding.editSearch.setText(state.getString("query", ""));
    }

    private void enter(PeopleDirectory.Department department) {
        rootQuery = binding.editSearch.getText().toString();
        departmentKey = department.key;
        teamKey = null;
        // Keep a person search when drilling into a matching department.
        binding.editSearch.setText(department.name.toLowerCase(Locale.ROOT).contains(rootQuery.trim().toLowerCase(Locale.ROOT)) ? "" : rootQuery);
        render();
        binding.recyclerPeople.scrollToPosition(0);
    }

    private void enterTeam(String key) {
        departmentQuery = binding.editSearch.getText().toString();
        teamKey = key;
        binding.editSearch.setText("");
        render();
        binding.recyclerPeople.scrollToPosition(0);
    }

    private static String teamKey(Map<String, Object> person) {
        String name = PeopleDirectory.text(person, "team_name");
        return name.isEmpty() ? "未分团队" : name;
    }

    private void render() {
        if (binding == null) return;
        boolean root = departmentKey == null;
        boolean team = teamKey != null;
        PeopleDirectory.Department department = currentDepartment();
        binding.buttonDepartments.setVisibility(root ? GONE : VISIBLE);
        binding.buttonDepartments.setText(team ? "返回团队列表" : "返回全部部门");
        binding.textDirectoryTitle.setText(root ? "选择部门" : team ? teamKey : department == null ? "选择团队" : department.name + " · 选择团队");
        binding.searchLayout.setHint(root ? "搜索部门、姓名、账号或团队" : team ? "搜索姓名或账号" : "搜索团队或人员");
        binding.editSearch.setEnabled(ready);
        binding.progressBar.setVisibility(loading ? VISIBLE : GONE);
        binding.buttonRetry.setVisibility(failed ? VISIBLE : GONE);
        binding.buttonAll.setVisibility(allClick == null || team || allOptionRootOnly && !root ? GONE : VISIBLE);
        binding.buttonAll.setEnabled(ready);
        binding.buttonAll.setText(allOptionLabel != null ? allOptionLabel
                : root ? "全部可查看人员" : "该部门全部可查看人员");
        String query = binding.editSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<Row> rows = new ArrayList<>();
        if (ready && !query.isEmpty()) {
            for (PeopleDirectory.Department group : departments) {
                if (!root && group != department) continue;
                List<Map<String, Object>> matches = group.name.toLowerCase(Locale.ROOT).contains(query)
                        ? group.people : PeopleDirectory.search(group, query);
                for (Map<String, Object> person : matches) {
                    if (team && !teamKey(person).equals(teamKey)) continue;
                    rows.add(personRow(person, group.name));
                }
            }
        } else if (ready && root) {
            for (PeopleDirectory.Department group : departments) {
                if (!group.name.toLowerCase(Locale.ROOT).contains(query) && PeopleDirectory.search(group, query).isEmpty()) continue;
                rows.add(new Row(group.name, group.people.size() + " 位人员", true, () -> enter(group)));
            }
        } else if (ready && department != null && !team) {
            java.util.Map<String, java.util.List<Map<String, Object>>> teams = new java.util.LinkedHashMap<>();
            for (Map<String, Object> person : department.people)
                teams.computeIfAbsent(teamKey(person), key -> new ArrayList<>()).add(person);
            java.util.List<String> names = new ArrayList<>(teams.keySet());
            names.sort(String::compareTo);
            for (String name : names) {
                boolean match = name.toLowerCase(Locale.ROOT).contains(query);
                if (!match) for (Map<String, Object> person : teams.get(name)) {
                    String searchable = PeopleDirectory.name(person) + " " + PeopleDirectory.text(person, "username");
                    if (searchable.toLowerCase(Locale.ROOT).contains(query)) { match = true; break; }
                }
                if (match) rows.add(new Row(name, teams.get(name).size() + " 位人员", true, () -> enterTeam(name)));
            }
        } else if (ready && department != null) {
            for (Map<String, Object> person : PeopleDirectory.search(department, query)) {
                if (!teamKey(person).equals(teamKey)) continue;
                rows.add(personRow(person, department.name));
            }
        }
        adapter.setRows(rows);
        int count = 0;
        for (PeopleDirectory.Department group : departments) count += group.people.size();
        int levelCount = department == null ? 0 : department.people.size();
        if (team && department != null) {
            levelCount = 0;
            for (Map<String, Object> person : department.people) if (teamKey(person).equals(teamKey)) levelCount++;
        }
        binding.textCount.setText(!query.isEmpty() ? "找到 " + rows.size() + " 位人员" : root ? departments.size() + " 个部门 · " + count + " 位人员" : levelCount + " 位人员");
        binding.textCount.setVisibility(ready ? VISIBLE : GONE);
        binding.recyclerPeople.setVisibility(ready ? VISIBLE : GONE);
        binding.textEmpty.setText(failed ? "人员加载失败，请重试" : query.isEmpty() ? emptyMessage : "未找到匹配结果，请尝试其他关键词");
        binding.textEmpty.setVisibility(failed || ready && rows.isEmpty() ? VISIBLE : GONE);
    }

    private Row personRow(Map<String, Object> person, String departmentName) {
        List<String> details = new ArrayList<>();
        for (String part : new String[]{PeopleDirectory.text(person, "username"), departmentName,
                PeopleDirectory.text(person, "team_name"), RoleDisplayUtil.toChinese(PeopleDirectory.text(person, "role"))})
            if (!part.isEmpty()) details.add(part);
        String relation = PeopleDirectory.text(person, "relation_kind");
        int depth = NumberCompat.toIntOrDefault(PeopleDirectory.value(person, "depth"), 0);
        if (!relation.isEmpty()) details.add((depth == 1 ? "直属" : depth > 1 ? "间接" : "") + relation);
        Row row = new Row(PeopleDirectory.name(person), String.join(" · ", details), false,
                () -> { if (personClick != null) personClick.accept(person); });
        Long id = NumberCompat.toLong(PeopleDirectory.value(person, "id"));
        row.selectable = selectionMode; row.selected = id != null && selectedIds.contains(id);
        row.avatar = PeopleDirectory.text(person, "avatar");
        return row;
    }

    private static final class Row {
        boolean selectable, selected;
        String avatar;
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
            holder.binding.imageAvatar.setVisibility(row.department ? View.GONE : View.VISIBLE);
            if (!row.department) com.example.project_pandora.ui.mine.AvatarLoader.load(holder.binding.imageAvatar, row.avatar, row.title);
            else com.bumptech.glide.Glide.with(holder.binding.imageAvatar).clear(holder.binding.imageAvatar);
            holder.binding.imageChevron.setVisibility(row.selectable ? View.GONE : View.VISIBLE);
            holder.binding.checkSelected.setVisibility(row.selectable ? View.VISIBLE : View.GONE);
            holder.binding.checkSelected.setChecked(row.selected);
            androidx.core.view.ViewCompat.setStateDescription(holder.itemView, row.selectable ? (row.selected ? "已选择" : "未选择") : null);
            holder.itemView.setClickable(true);
            holder.itemView.setContentDescription(row.department
                    ? row.title + "，" + row.detail + "，点击查看部门人员"
                    : row.title + "，" + row.detail + (row.selectable ? (row.selected ? "，点击取消选择" : "，点击选择") : "，点击查看人员详情"));
            holder.itemView.setOnClickListener(v -> row.click.run());
        }
        @Override public int getItemCount() { return rows.size(); }
    }
}
