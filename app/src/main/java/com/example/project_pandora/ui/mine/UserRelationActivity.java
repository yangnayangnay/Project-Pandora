package com.example.project_pandora.ui.mine;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import com.example.project_pandora.ui.PandoraActivity;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.core.util.NumberCompat;
import com.example.project_pandora.core.util.RoleDisplayUtil;
import com.example.project_pandora.data.remote.UserApi;
import com.example.project_pandora.databinding.ActivityUserRelationBinding;
import com.example.project_pandora.ui.login.LoginActivity;
import com.example.project_pandora.ui.people.PeopleDirectory;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.SerialDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.HttpException;

public class UserRelationActivity extends PandoraActivity {
    private ActivityUserRelationBinding binding;
    private final SerialDisposable request = new SerialDisposable();
    private final List<Map<String, Object>> people = new ArrayList<>();
    private int relationTab;
    private boolean loaded;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        binding = ActivityUserRelationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        BackToolbarHelper.setupBack(binding.toolbar, this::back);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { back(); }
        });
        binding.peopleDirectory.configure("按部门查看与你有上下级关系的人员，支持搜索姓名、账号或小组。", "暂无人员关系数据", this::loadRelations,
                this::showPerson, null);
        if (state != null) {
            relationTab = state.getInt("relationTab", 0);
            binding.peopleDirectory.restoreDirectoryState(state.getBundle("directory"));
        }
        for (String title : new String[]{"全部关系", "上级", "下级"}) binding.tabsRelations.addTab(binding.tabsRelations.newTab().setText(title));
        binding.tabsRelations.getTabAt(Math.max(0, Math.min(2, relationTab))).select();
        binding.tabsRelations.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                relationTab = tab.getPosition();
                if (loaded) renderPeople();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        loadRelations();
    }
    private void loadRelations() {
        loaded = false;
        binding.peopleDirectory.showLoading();
        request.set(NetworkClient.getInstance(this).createService(UserApi.class).getUserRelations()
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(response -> {
                    if (!response.isSuccess() || response.getData() == null) { binding.peopleDirectory.showError(); return; }
                    people.clear();
                    append(response.getData().get("superiors"), "上级");
                    append(response.getData().get("subordinates"), "下级");
                    loaded = true;
                    renderPeople();
                }, error -> {
                    if (error instanceof HttpException && ((HttpException) error).code() == 401) {
                        TokenManager.getInstance(this).clear();
                        startActivity(new Intent(this, LoginActivity.class)); finish();
                    } else binding.peopleDirectory.showError();
                }));
    }
    @SuppressWarnings("unchecked") private void append(Object raw, String kind) {
        if (!(raw instanceof List)) return;
        for (Object value : (List<?>) raw) if (value instanceof Map) {
            Map<String, Object> person = new HashMap<>((Map<String, Object>) value);
            if (!PeopleDirectory.isMobilePerson(person)) continue;
            person.put("relation_kind", kind);
            people.add(person);
        }
    }
    private void renderPeople() {
        List<Map<String, Object>> visible = new ArrayList<>();
        for (Map<String, Object> person : people) {
            String kind = PeopleDirectory.text(person, "relation_kind");
            if (relationTab == 0 || relationTab == 1 && kind.equals("上级") || relationTab == 2 && kind.equals("下级")) visible.add(person);
        }
        binding.peopleDirectory.setPeople(visible);
    }
    private void showPerson(Map<String, Object> person) {
        int depth = NumberCompat.toIntOrDefault(PeopleDirectory.value(person, "depth"), 0);
        String relation = PeopleDirectory.text(person, "relation_kind");
        String detail = "账号：" + display(PeopleDirectory.text(person, "username"))
                + "\n职务：" + RoleDisplayUtil.toChinese(PeopleDirectory.text(person, "role"))
                + "\n部门：" + display(PeopleDirectory.text(person, "department_name"))
                + "\n小组：" + display(PeopleDirectory.text(person, "team_name"))
                + "\n关系：" + (depth == 1 ? "直属" : depth > 1 ? "间接" : "") + relation
                + (depth > 0 ? "（第" + depth + "层）" : "");
        new AlertDialog.Builder(this).setTitle(PeopleDirectory.name(person)).setMessage(detail).setPositiveButton("关闭", null).show();
    }
    private String display(String value) { return value.isEmpty() ? "未设置" : value; }
    private void back() { if (!binding.peopleDirectory.back()) finish(); }
    @Override protected void onSaveInstanceState(@NonNull Bundle state) {
        state.putInt("relationTab", relationTab);
        state.putBundle("directory", binding.peopleDirectory.saveDirectoryState());
        super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() { request.dispose(); super.onDestroy(); }
}
