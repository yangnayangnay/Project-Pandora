package com.example.project_pandora.ui.task;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import com.example.project_pandora.ui.PandoraActivity;
import com.example.project_pandora.core.util.NumberCompat;
import com.example.project_pandora.databinding.ActivityTaskFilterPeopleBinding;
import com.example.project_pandora.repository.TaskRepository;
import com.example.project_pandora.ui.people.PeopleDirectory;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.SerialDisposable;

/** Department/team/search picker used by the task-details assignee filter. */
public final class TaskFilterPeopleActivity extends PandoraActivity {
    public static final String EXTRA_ID = "assigneeId";
    public static final String EXTRA_NAME = "assigneeName";
    private ActivityTaskFilterPeopleBinding binding;
    private final SerialDisposable request = new SerialDisposable();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        binding = ActivityTaskFilterPeopleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> navigateBack());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { navigateBack(); }
        });
        binding.peopleDirectory.configure("按部门和团队查找负责人，也可直接搜索姓名或账号。", "暂无可筛选人员",
                this::load, person -> {
                    Long id = NumberCompat.toLong(PeopleDirectory.value(person, "id"));
                    if (id == null) return;
                    setResult(Activity.RESULT_OK, new Intent().putExtra(EXTRA_ID, id).putExtra(EXTRA_NAME, PeopleDirectory.name(person)));
                    finish();
                }, department -> {
                    setResult(Activity.RESULT_OK, new Intent().putExtra(EXTRA_ID, -1L).putExtra(EXTRA_NAME, "全部负责人"));
                    finish();
                });
        binding.peopleDirectory.setAllOption("全部负责人", true);
        if (state != null) binding.peopleDirectory.restoreDirectoryState(state.getBundle("directory"));
        load();
    }

    private void navigateBack() {
        if (!binding.peopleDirectory.back()) finish();
    }
    private void load() {
        binding.peopleDirectory.showLoading();
        request.set(new TaskRepository(this).listAssignees().observeOn(AndroidSchedulers.mainThread())
                .subscribe(binding.peopleDirectory::setPeople, error -> binding.peopleDirectory.showError()));
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBundle("directory", binding.peopleDirectory.saveDirectoryState()); super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() { request.dispose(); binding = null; super.onDestroy(); }
}
