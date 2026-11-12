package com.example.project_pandora.ui.task;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import com.example.project_pandora.ui.PandoraActivity;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.util.NumberCompat;
import com.example.project_pandora.core.util.TaskAssignees;
import com.example.project_pandora.data.remote.TaskApi;
import com.example.project_pandora.databinding.ActivityTaskAssigneesBinding;
import com.example.project_pandora.repository.TaskRepository;
import com.example.project_pandora.ui.people.PeopleDirectory;
import com.google.android.material.chip.Chip;
import java.util.*;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/** Full-height department/team/search selection; opening it never changes the form selection. */
public final class TaskAssigneesActivity extends PandoraActivity {
    private ActivityTaskAssigneesBinding binding;
    private final LinkedHashSet<Long> selected = new LinkedHashSet<>();
    private final List<Map<String, Object>> people = new ArrayList<>();
    private final CompositeDisposable requests = new CompositeDisposable();
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        binding = ActivityTaskAssigneesBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot());
        long[] ids = state == null ? getIntent().getLongArrayExtra("selectedIds") : state.getLongArray("selectedIds");
        if (ids != null) for (long id : ids) if (id > 0) selected.add(id);
        binding.toolbar.setNavigationOnClickListener(v -> back());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { back(); }
        });
        binding.peopleDirectory.setSelectionMode(true);
        binding.peopleDirectory.setAllOption("全选 / 取消当前部门人员", false);
        binding.peopleDirectory.configure("按部门、团队查找，或直接搜索姓名 / 账号；已选人员在底部，可点 × 移除。",
                "暂无可选择人员", this::load, person -> {
                    Long id = NumberCompat.toLong(PeopleDirectory.value(person, "id"));
                    if (id != null) { if (!selected.add(id)) selected.remove(id); renderSelection(); }
                }, department -> {
                    List<Map<String, Object>> rows = department == null ? people : department.people;
                    boolean all = !rows.isEmpty();
                    for (Map<String, Object> row : rows) if (!selected.contains(NumberCompat.toLong(PeopleDirectory.value(row, "id")))) all = false;
                    for (Map<String, Object> row : rows) {
                        Long id = NumberCompat.toLong(PeopleDirectory.value(row, "id"));
                        if (id != null) { if (all) selected.remove(id); else selected.add(id); }
                    }
                    renderSelection();
                });
        if (state != null) binding.peopleDirectory.restoreDirectoryState(state.getBundle("directory"));
        binding.btnClearSelection.setOnClickListener(v -> { selected.clear(); renderSelection(); });
        binding.btnConfirmSelection.setOnClickListener(v -> {
            if (selected.isEmpty()) return;
            ArrayList<String> names = new ArrayList<>(); for (long id : selected) names.add(name(id));
            setResult(Activity.RESULT_OK, new Intent().putExtra("selectedIds", ids()).putStringArrayListExtra("selectedNames", names)); finish();
        });
        renderSelection(); load();
    }
    private long[] ids() { long[] result = new long[selected.size()]; int i = 0; for (long id : selected) result[i++] = id; return result; }
    private String name(long id) {
        for (Map<String, Object> person : people) if (Objects.equals(id, NumberCompat.toLong(PeopleDirectory.value(person, "id")))) return PeopleDirectory.name(person);
        return "用户 " + id;
    }
    private void load() {
        binding.peopleDirectory.showLoading(); binding.btnConfirmSelection.setEnabled(false);
        requests.add(new TaskRepository(this).listAssignees().observeOn(AndroidSchedulers.mainThread()).subscribe(rows -> {
            people.clear(); people.addAll(rows);
            long taskId = getIntent().getLongExtra("taskId", -1);
            if (taskId <= 0) { finishLoad(); return; }
            TaskApi api = NetworkClient.getInstance(this).createService(TaskApi.class);
            requests.add(api.getTask(taskId).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(response -> {
                if (response.isSuccess() && response.getData() != null) {
                    for (Map<String, Object> person : TaskAssignees.people(response.getData())) {
                        Long id = NumberCompat.toLong(PeopleDirectory.value(person, "id"));
                        if (id == null || !selected.contains(id)) continue;
                        boolean found = false; for (Map<String, Object> existing : people) if (Objects.equals(id, NumberCompat.toLong(PeopleDirectory.value(existing, "id")))) found = true;
                        if (!found) { person.put("department_name", "原任务负责人"); people.add(person); }
                    }
                }
                finishLoad();
            }, error -> finishLoad()));
        }, error -> binding.peopleDirectory.showError()));
    }
    private void finishLoad() { binding.peopleDirectory.setPeople(people); renderSelection(); }
    private void renderSelection() {
        binding.peopleDirectory.setSelectedIds(selected);
        binding.textSelectedCount.setText("已选 " + selected.size() + " 人");
        binding.btnConfirmSelection.setEnabled(!selected.isEmpty());
        binding.selectedChips.removeAllViews();
        for (long id : selected) {
            Chip chip = new Chip(this); chip.setText(name(id)); chip.setCheckable(false); chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> { selected.remove(id); renderSelection(); }); binding.selectedChips.addView(chip);
        }
    }
    private void back() { if (!binding.peopleDirectory.back()) finish(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putLongArray("selectedIds", ids()); state.putBundle("directory", binding.peopleDirectory.saveDirectoryState()); super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() { requests.dispose(); super.onDestroy(); }
}
