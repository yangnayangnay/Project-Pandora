package com.example.project_pandora.ui.mine;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.project_pandora.MainActivity;
import com.example.project_pandora.R;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.sync.SyncManager;
import com.example.project_pandora.data.remote.UserApi;
import com.example.project_pandora.databinding.FragmentMineBinding;
import com.example.project_pandora.repository.SyncRepository;

import com.example.project_pandora.ui.login.LoginActivity;

import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MineFragment extends Fragment {

    private FragmentMineBinding binding;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMineBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        com.example.project_pandora.ui.widget.CompanyBackgrounds.bind(view,
                com.example.project_pandora.ui.widget.CompanyBackgrounds.Scene.MINE);
        binding.toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        AvatarLoader.load(binding.imageAvatar, null, "");

        binding.imageAvatar.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProfileActivity.class)));

        binding.btnUserProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProfileActivity.class)));

        // Round19 REQ-19-10: 移除专注模式入口（保留FocusActivity源码）
        binding.btnFocus.setVisibility(View.GONE);

        String role = TokenManager.getInstance(requireContext()).getRole();
        binding.btnResetRequests.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ResetRequestsActivity.class)));
        binding.btnHierarchyManage.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), HierarchyManageActivity.class)));
        if ("ADMIN".equals(role)) {
            binding.btnResetRequests.setVisibility(View.VISIBLE);
            binding.btnHierarchyManage.setVisibility(View.VISIBLE);
        }

        binding.btnTaskReviews.setOnClickListener(v -> startActivity(new Intent(requireContext(), com.example.project_pandora.ui.task.TaskReviewsActivity.class)));
        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));

        // R37 REQ-37-B: 主题三选一（点选即全局生效+持久化；无记录默认跟随系统）
        setupThemeToggle();

        TokenManager tm = TokenManager.getInstance(requireContext());
        if (tm.isLoggedIn()) {
            binding.btnLogout.setVisibility(View.VISIBLE);
        }

        binding.btnLogout.setOnClickListener(v -> handleLogout());

        String relationRole = tm.getRole();
        binding.btnUserRelation.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), UserRelationActivity.class)));
        if (relationRole != null && !"ADMIN".equals(relationRole)) {
            binding.btnUserRelation.setVisibility(View.VISIBLE);
        }
    }

    private void handleLogout() {
        SyncRepository repo = new SyncRepository(requireContext());
        disposables.add(repo.hasUnsyncedData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        hasUnsynced -> {
                            if (hasUnsynced) {
                                new AlertDialog.Builder(requireContext())
                                        .setTitle("退出登录")
                                        .setMessage("有未同步数据，退出后数据将保留在本地，下次登录后可继续同步")
                                        .setPositiveButton("确认退出", (d, w) -> doLogout())
                                        .setNegativeButton("取消", null)
                                        .show();
                            } else {
                                new AlertDialog.Builder(requireContext())
                                        .setTitle("退出登录")
                                        .setMessage("确认退出登录?")
                                        .setPositiveButton("确认", (d, w) -> doLogout())
                                        .setNegativeButton("取消", null)
                                        .show();
                            }
                        },
                        error -> doLogout()
                ));
    }

    private void doLogout() {
        SyncManager.getInstance().destroy();
        TokenManager.getInstance(requireContext()).clear();
        com.example.project_pandora.notification.PollingForegroundService.stop(requireContext());
        startActivity(new Intent(requireContext(), LoginActivity.class));
        requireActivity().finishAffinity();
    }

    private void loadUserInfo() {
        UserApi userApi = NetworkClient.getInstance(requireContext()).createService(UserApi.class);
        disposables.add(userApi.getCurrentUser()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (binding == null) return;
                            if (response.isSuccess() && response.getData() != null) {
                                Map<String, Object> data = response.getData();
                                String realName = getStr(data, "realName");
                                String username = getStr(data, "username");
                                String displayName = (!realName.isEmpty()) ? realName : (!username.isEmpty() ? username : "未知用户");
                                binding.textUsername.setText(displayName);
                                String currentRole = getStr(data, "role");
                                boolean admin = "ADMIN".equals(currentRole);
                                binding.btnResetRequests.setVisibility(admin ? View.VISIBLE : View.GONE);
                                binding.btnHierarchyManage.setVisibility(admin ? View.VISIBLE : View.GONE);
                                binding.btnUserRelation.setVisibility(!currentRole.isEmpty() && !admin ? View.VISIBLE : View.GONE);
                                binding.textRole.setText(com.example.project_pandora.core.util.RoleDisplayUtil.toChinese(getStr(data, "role")));

                                String avatar = getStr(data, "avatar");
                                AvatarLoader.load(binding.imageAvatar, avatar, displayName);
                            }
                        },
                        error -> {
                            if (binding == null) return;
                            binding.textUsername.setText("加载失败");
                        }
                ));
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    @Override public void onResume() {
        super.onResume();
        if (binding != null) loadUserInfo();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        binding = null;
    }
    /**
     * R37 REQ-37-B2/B3/B6: 主题三态单选绑定——回显持久值+点选保存并即时应用。
     */
    private void setupThemeToggle() {
        int mode = com.example.project_pandora.core.config.ThemePrefs.getMode(requireContext());
        if (mode == com.example.project_pandora.core.config.ThemePrefs.MODE_ALWAYS_DAY) {
            binding.toggleThemeMode.check(binding.btnThemeDay.getId());
        } else if (mode == com.example.project_pandora.core.config.ThemePrefs.MODE_ALWAYS_NIGHT) {
            binding.toggleThemeMode.check(binding.btnThemeNight.getId());
        } else {
            binding.toggleThemeMode.check(binding.btnThemeSystem.getId());
        }
        binding.toggleThemeMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            int newMode;
            if (checkedId == binding.btnThemeDay.getId()) {
                newMode = com.example.project_pandora.core.config.ThemePrefs.MODE_ALWAYS_DAY;
            } else if (checkedId == binding.btnThemeNight.getId()) {
                newMode = com.example.project_pandora.core.config.ThemePrefs.MODE_ALWAYS_NIGHT;
            } else {
                newMode = com.example.project_pandora.core.config.ThemePrefs.MODE_FOLLOW_SYSTEM;
            }
            com.example.project_pandora.core.config.ThemePrefs.saveMode(requireContext(), newMode);
            com.example.project_pandora.core.config.ThemePrefs.applyTheme(requireContext());
        });
    }

}
