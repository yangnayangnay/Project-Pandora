package com.example.project_pandora.ui.mine;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.project_pandora.R;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.core.util.PermissionLevel;
import com.example.project_pandora.data.remote.UserApi;
import com.example.project_pandora.databinding.ActivityUserRelationBinding;
import com.example.project_pandora.model.UserRelationItem;
import com.example.project_pandora.ui.login.LoginActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.HttpException;

public class UserRelationActivity extends AppCompatActivity {

    private ActivityUserRelationBinding binding;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final UserRelationAdapter superiorsAdapter = new UserRelationAdapter();
    private final UserRelationAdapter subordinatesAdapter = new UserRelationAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserRelationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        binding.recyclerSuperiors.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerSuperiors.setAdapter(superiorsAdapter);
        binding.recyclerSubordinates.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerSubordinates.setAdapter(subordinatesAdapter);

        binding.btnRetry.setOnClickListener(v -> loadRelations());

        loadRelations();
    }

    private void loadRelations() {
        binding.textError.setVisibility(View.GONE);
        binding.btnRetry.setVisibility(View.GONE);
        binding.textEmpty.setVisibility(View.GONE);

        UserApi userApi = NetworkClient.getInstance(this).createService(UserApi.class);
        disposables.add(userApi.getUserRelations()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess() && response.getData() != null) {
                                Map<String, Object> data = response.getData();
                                List<UserRelationItem> superiors = parseList(data.get("superiors"));
                                List<UserRelationItem> subordinates = parseList(data.get("subordinates"));

                                Collections.sort(superiors, PermissionLevel.descendingComparator());
                                Collections.sort(subordinates, PermissionLevel.descendingComparator());

                                superiorsAdapter.setItems(superiors);
                                subordinatesAdapter.setItems(subordinates);

                                String role = TokenManager.getInstance(this).getRole();
                                if ("FOUNDER".equals(role)) {
                                    binding.superiorsSection.setVisibility(View.GONE);
                                } else if ("EMPLOYEE".equals(role) || "ADMIN".equals(role)) {
                                    binding.subordinatesSection.setVisibility(View.GONE);
                                }

                                if (superiors.isEmpty() && subordinates.isEmpty()) {
                                    binding.textEmpty.setVisibility(View.VISIBLE);
                                }
                            } else {
                                binding.textError.setVisibility(View.VISIBLE);
                                binding.btnRetry.setVisibility(View.VISIBLE);
                            }
                        },
                        error -> {
                            if (error instanceof HttpException && ((HttpException) error).code() == 401) {
                                TokenManager.getInstance(this).clear();
                                startActivity(new Intent(this, LoginActivity.class));
                                finish();
                            } else {
                                binding.textError.setVisibility(View.VISIBLE);
                                binding.btnRetry.setVisibility(View.VISIBLE);
                            }
                        }
                ));
    }

    @SuppressWarnings("unchecked")
    private List<UserRelationItem> parseList(Object raw) {
        List<UserRelationItem> result = new ArrayList<>();
        if (raw instanceof List) {
            for (Object item : (List<?>) raw) {
                if (item instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) item;
                    // Round26: id/depth 解析统一收敛至 NumberCompat（原 id 为裸强转、depth 缺省 0，语义保持）
                    Long id = com.example.project_pandora.core.util.NumberCompat.toLong(m.get("id"));
                    String realName = getStr(m, "realName");
                    String role = getStr(m, "role");
                    String deptName = getStr(m, "departmentName");
                    String teamName = getStr(m, "teamName");
                    int depth = com.example.project_pandora.core.util.NumberCompat.toIntOrDefault(m.get("depth"), 0);
                    result.add(new UserRelationItem(id, realName, role, deptName, teamName, depth));
                }
            }
        }
        return result;
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}