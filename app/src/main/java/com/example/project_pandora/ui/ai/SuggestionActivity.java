package com.example.project_pandora.ui.ai;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.R;
import com.example.project_pandora.databinding.ActivitySuggestionBinding;
import com.example.project_pandora.repository.AIRepository;

import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import com.example.project_pandora.core.util.BackToolbarHelper;

public class SuggestionActivity extends AppCompatActivity {

    private ActivitySuggestionBinding binding;
    private AIRepository aiRepository;
    /** Round28 REQ-28-06: 更新互斥标志（onCreate 自动加载与手动更新两入口统一管理） */
    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuggestionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        aiRepository = new AIRepository(this);

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        // Round28 REQ-28-05: toolbar 更新分析菜单项
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_update_analysis) {
                refreshSuggestion();
                return true;
            }
            return false;
        });

        loadSuggestion();
    }

    private void loadSuggestion() {
        isUpdating = true;
        binding.progressBar.setVisibility(View.VISIBLE);
        aiRepository.getSuggestion()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        suggestion -> {
                            isUpdating = false;
                            binding.progressBar.setVisibility(View.GONE);
                            displaySuggestion(suggestion);
                        },
                        error -> {
                            isUpdating = false;
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "获取建议失败", Toast.LENGTH_SHORT).show();
                            binding.textAiSource.setText("— 本地兜底生成 —");
                            binding.textAiSource.setVisibility(View.VISIBLE);
                        }
                );
    }

    /**
     * Round28 REQ-28-05/06: 手动更新分析。
     * isUpdating 互斥：进行中后到触发直接忽略；更新中菜单项禁用 + progressBar 可见。
     * 成功：整页覆盖 + Toast"分析已更新"；失败：独立分支仅 Toast"更新失败，请稍后重试"，
     * 不触碰任何展示区（保留原内容完整，与首次加载失败分支严格分流）。
     */
    private void refreshSuggestion() {
        if (isUpdating) return;
        isUpdating = true;
        setUpdateMenuEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);
        aiRepository.getSuggestion()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        suggestion -> {
                            isUpdating = false;
                            binding.progressBar.setVisibility(View.GONE);
                            setUpdateMenuEnabled(true);
                            displaySuggestion(suggestion);
                            Toast.makeText(this, "分析已更新", Toast.LENGTH_SHORT).show();
                        },
                        error -> {
                            isUpdating = false;
                            binding.progressBar.setVisibility(View.GONE);
                            setUpdateMenuEnabled(true);
                            Toast.makeText(this, "更新失败，请稍后重试", Toast.LENGTH_SHORT).show();
                        }
                );
    }

    private void setUpdateMenuEnabled(boolean enabled) {
        MenuItem item = binding.toolbar.getMenu().findItem(R.id.action_update_analysis);
        if (item != null) {
            item.setEnabled(enabled);
        }
    }

    private void displaySuggestion(Map<String, Object> suggestion) {
        displayUserInfo(suggestion.get("userInfo"));
        displayTaskStats(suggestion.get("taskStats"), suggestion.get("logStats"));

        Object workAdvice = suggestion.get("workAdvice");
        Object careerAdvice = suggestion.get("careerAdvice");
        Object developmentPlan = suggestion.get("developmentPlan");

        binding.textWorkAdvice.setText(workAdvice != null ? workAdvice.toString() : "暂无建议");
        binding.textCareerAdvice.setText(careerAdvice != null ? careerAdvice.toString() : "暂无建议");
        binding.textDevelopmentPlan.setText(developmentPlan != null ? developmentPlan.toString() : "暂无计划");

        bindAdviceSource(suggestion.get("source"));
    }

    /**
     * Round24 REQ-24-07: AI 建议来源标识——严格按响应 source 字段驱动（禁止固定文案）：
     * EXTERNAL → "— AI 生成 —"；LOCAL（后端 suggestion 实际取值）/LOCAL_FALLBACK → "— 本地兜底生成 —"；无 source 时隐藏
     */
    private void bindAdviceSource(Object source) {
        String s = source != null ? source.toString() : null;
        if ("EXTERNAL".equals(s)) {
            binding.textAiSource.setText("— AI 生成 —");
            binding.textAiSource.setVisibility(View.VISIBLE);
        } else if ("LOCAL".equals(s) || "LOCAL_FALLBACK".equals(s)) {
            binding.textAiSource.setText("— 本地兜底生成 —");
            binding.textAiSource.setVisibility(View.VISIBLE);
        } else {
            binding.textAiSource.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("unchecked")
    private void displayUserInfo(Object infoObj) {
        if (infoObj instanceof Map) {
            Map<String, Object> info = (Map<String, Object>) infoObj;
            String username = getStr(info, "username");
            String role = getRoleDisplay(getStr(info, "role"));
            String realName = getStr(info, "realName");
            String deptId = getStr(info, "departmentId");
            String teamId = getStr(info, "teamId");

            StringBuilder sb = new StringBuilder();
            sb.append("用户名：").append(username).append("\n");
            sb.append("姓名：").append(realName).append("\n");
            sb.append("角色：").append(role);
            if (!deptId.equals("null") && !deptId.isEmpty()) sb.append("\n部门ID：").append(getNum(info, "departmentId"));
            if (!teamId.equals("null") && !teamId.isEmpty()) sb.append("\n团队ID：").append(getNum(info, "teamId"));
            binding.textUserInfo.setText(sb.toString());
        } else {
            binding.textUserInfo.setText("暂无用户信息");
        }
    }

    @SuppressWarnings("unchecked")
    private void displayTaskStats(Object statsObj, Object logObj) {
        StringBuilder sb = new StringBuilder();
        if (statsObj instanceof Map) {
            Map<String, Object> stats = (Map<String, Object>) statsObj;
            sb.append("总任务数：").append(getNum(stats, "totalTasks")).append("\n");
            sb.append("已完成：").append(getNum(stats, "completedTasks")).append("\n");
            sb.append("进行中：").append(getNum(stats, "inProgressTasks")).append("\n");
            sb.append("待处理：").append(getNum(stats, "pendingTasks")).append("\n");
            sb.append("已逾期：").append(getNum(stats, "overdueTasks")).append("\n");
            sb.append("完成率：").append(getNum(stats, "completionRate")).append("%");
        } else {
            sb.append("暂无任务数据");
        }
        if (logObj instanceof Map) {
            Map<String, Object> logs = (Map<String, Object>) logObj;
            sb.append("\n工作日志：").append(getNum(logs, "logCount")).append(" 条");
        }
        binding.textTaskStats.setText(sb.toString());
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private String getNum(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return "0";
        if (val instanceof Number) {
            double d = ((Number) val).doubleValue();
            if (d == (int) d) return String.valueOf((int) d);
            return String.valueOf(Math.round(d * 100) / 100.0);
        }
        return val.toString();
    }

    private String getRoleDisplay(String role) {
        return com.example.project_pandora.core.util.RoleDisplayUtil.toChinese(role);
    }
}