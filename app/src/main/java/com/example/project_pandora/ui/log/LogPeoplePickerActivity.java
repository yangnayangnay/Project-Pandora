package com.example.project_pandora.ui.log;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import com.example.project_pandora.ui.PandoraActivity;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.core.util.NumberCompat;
import com.example.project_pandora.databinding.ActivityLogPeoplePickerBinding;
import com.example.project_pandora.repository.LogRepository;
import com.example.project_pandora.ui.people.PeopleDirectory;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.SerialDisposable;

public class LogPeoplePickerActivity extends PandoraActivity {
    public static final String EXTRA_USER = "logFilterUserId", EXTRA_DEPARTMENT = "logFilterDepartmentId", EXTRA_LABEL = "logFilterLabel";
    private ActivityLogPeoplePickerBinding binding;
    private final SerialDisposable request = new SerialDisposable();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        binding = ActivityLogPeoplePickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        BackToolbarHelper.setupBack(binding.toolbar, this::back);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { back(); }
        });
        binding.peopleDirectory.configure("先选择部门，再按姓名、账号或小组查找人员。仅展示你有权查看日志的人员。", "暂无可查看人员", this::load,
                person -> {
                    Long id = NumberCompat.toLong(PeopleDirectory.value(person, "id"));
                    PeopleDirectory.Department department = binding.peopleDirectory.currentDepartment();
                    if (id != null) choose(id, department == null || department.key.startsWith("name:") ? null : department.id,
                            (department == null ? "" : department.name + " · ") + PeopleDirectory.name(person));
                }, department -> {
                    if (department != null && department.key.startsWith("name:")) {
                        Toast.makeText(this, "当前服务端缺少部门编号，请更新后端后重试", Toast.LENGTH_LONG).show();
                        return;
                    }
                    choose(null, department == null ? null : department.id, department == null ? "全部可查看人员" : department.name + " · 全部人员");
                });
        if (state != null) binding.peopleDirectory.restoreDirectoryState(state.getBundle("directory"));
        load();
    }
    private void load() {
        binding.peopleDirectory.showLoading();
        request.set(new LogRepository(this).getSubordinates().observeOn(AndroidSchedulers.mainThread())
                .subscribe(binding.peopleDirectory::setPeople, error -> binding.peopleDirectory.showError()));
    }
    private void back() { if (!binding.peopleDirectory.back()) finish(); }
    private void choose(Long userId, Long departmentId, String label) {
        Intent result = new Intent();
        if (userId != null) result.putExtra(EXTRA_USER, userId.longValue());
        if (departmentId != null) result.putExtra(EXTRA_DEPARTMENT, departmentId.longValue());
        result.putExtra(EXTRA_LABEL, label);
        setResult(RESULT_OK, result);
        finish();
    }
    @Override protected void onSaveInstanceState(@NonNull Bundle state) {
        state.putBundle("directory", binding.peopleDirectory.saveDirectoryState());
        super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() { request.dispose(); super.onDestroy(); }
}
