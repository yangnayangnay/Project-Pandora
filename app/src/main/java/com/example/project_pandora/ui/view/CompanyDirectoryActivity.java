package com.example.project_pandora.ui.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import com.example.project_pandora.ui.PandoraActivity;
import com.example.project_pandora.core.exception.BusinessException;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.util.NumberCompat;
import com.example.project_pandora.data.remote.CompanyDirectoryApi;
import com.example.project_pandora.databinding.ActivityCompanyDirectoryBinding;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.SerialDisposable;

public final class CompanyDirectoryActivity extends PandoraActivity {
    private ActivityCompanyDirectoryBinding binding;
    private CompanyDirectoryApi api;
    private final SerialDisposable request = new SerialDisposable();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); binding = ActivityCompanyDirectoryBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot());
        api = NetworkClient.getInstance(this).createService(CompanyDirectoryApi.class);
        binding.toolbar.setNavigationOnClickListener(v -> navigateBack());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { navigateBack(); }
        });
        binding.peopleDirectory.configure("按部门查看，也可搜索姓名、账号或小组。人员范围由现有组织权限决定。",
                "暂无可查看人员", this::load, this::openPerson, null);
        if (state != null) binding.peopleDirectory.restoreDirectoryState(state.getBundle("directory"));
        load();
    }

    private void navigateBack() {
        if (binding != null && binding.peopleDirectory.back()) return;
        finish();
    }

    private void openPerson(java.util.Map<String, Object> person) {
        Long id = NumberCompat.toLong(com.example.project_pandora.ui.people.PeopleDirectory.value(person, "id"));
        if (id == null) {
            Toast.makeText(this, "人员信息无效，请刷新后重试", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, CompanyPersonActivity.class).putExtra("userId", id.longValue()));
    }

    private void load() {
        binding.peopleDirectory.showLoading();
        request.set(api.list().subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(response -> {
                    if (!response.isSuccess() || response.getData() == null) throw new BusinessException(response.getCode(), response.getMessage());
                    binding.peopleDirectory.setPeople(response.getData());
                }, error -> binding.peopleDirectory.showError()));
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBundle("directory", binding.peopleDirectory.saveDirectoryState()); super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() { request.dispose(); super.onDestroy(); }
}
