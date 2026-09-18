package com.example.project_pandora.ui.ai;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.databinding.ActivityMbtiTestBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MBTITestActivity extends AppCompatActivity {

    private static final String PREF_NAME = "mbti_pref";
    private static final String KEY_HISTORY = "mbti_history";

    private ActivityMbtiTestBinding binding;
    private int currentQuestionIndex = 0;
    private int[] answers;
    private int selectedVersion = 0;

    private static final String[][] ALL_QUESTIONS = {
        {"你喜欢在人群中成为焦点吗？", "喜欢", "不太喜欢"},
        {"你倾向于主动与人交谈吗？", "倾向于主动", "倾向于等待"},
        {"你在社交场合感到精力充沛吗？", "是的", "并非如此"},
        {"聚会结束后你感到兴奋还是疲惫？", "兴奋", "疲惫"},
        {"你喜欢边想边说还是想好再说？", "边想边说", "想好再说"},
        {"你更愿意参与外部活动还是独处思考？", "参与活动", "独处思考"},
        {"你能轻松和陌生人开启对话吗？", "能", "不能"},
        {"你倾向于广泛社交还是深交少数？", "广泛社交", "深交少数"},
        {"空闲时你更想约人出去还是独自放松？", "约人出去", "独自放松"},
        {"你更关注具体事实还是抽象概念？", "具体事实", "抽象概念"},
        {"你更相信经验还是直觉？", "经验", "直觉"},
        {"你通常更关注细节而非整体吗？", "关注细节", "关注整体"},
        {"你更喜欢按说明书操作还是自由发挥？", "按说明书", "自由发挥"},
        {"你更看重过去的经验还是未来的可能？", "过去的经验", "未来的可能"},
        {"你做事喜欢一步一个脚印还是跳跃式？", "一步一个脚印", "跳跃式"},
        {"你更善于记住具体事实还是整体印象？", "具体事实", "整体印象"},
        {"你更喜欢传统做法还是新颖方法？", "传统做法", "新颖方法"},
        {"你解决问题时更依赖已有方案还是灵感？", "已有方案", "灵感"},
        {"做决定时你更依赖逻辑还是感受？", "逻辑", "感受"},
        {"你认为公平比和谐更重要吗？", "公平更重要", "和谐更重要"},
        {"你更善于分析还是体谅他人？", "善于分析", "善于体谅"},
        {"你更看重客观事实还是他人情感？", "客观事实", "他人情感"},
        {"批评别人时你更注重对错还是感受？", "注重对错", "注重感受"},
        {"你做决定时更冷静还是感性？", "冷静", "感性"},
        {"你更看重效率还是人际关系？", "效率", "人际关系"},
        {"你更倾向于直言不讳还是委婉表达？", "直言不讳", "委婉表达"},
        {"面对冲突你更讲道理还是顾及感情？", "讲道理", "顾及感情"},
        {"你喜欢有计划地安排事情吗？", "喜欢", "不喜欢"},
        {"你喜欢列出待办事项清单吗？", "喜欢", "不喜欢"},
        {"你喜欢按时完成任务吗？", "喜欢", "不喜欢"},
        {"你更偏好结构化还是灵活的环境？", "结构化", "灵活"},
        {"你更倾向于提前规划还是临时应变？", "提前规划", "临时应变"},
        {"你喜欢事情有明确结论还是保持开放？", "明确结论", "保持开放"},
        {"你更注重结果还是过程？", "结果", "过程"},
        {"你更习惯按部就班还是随机应变？", "按部就班", "随机应变"},
        {"你更喜欢确定的事情还是可能性？", "确定的事情", "可能性"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMbtiTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        showVersionSelection();
    }

    private void showVersionSelection() {
        binding.layoutVersionSelect.setVisibility(View.VISIBLE);
        binding.layoutQuestion.setVisibility(View.GONE);
        binding.layoutResult.setVisibility(View.GONE);

        binding.btnVersion12.setOnClickListener(v -> startTest(12));
        binding.btnVersion24.setOnClickListener(v -> startTest(24));
        binding.btnVersion36.setOnClickListener(v -> startTest(36));
    }

    private void startTest(int version) {
        selectedVersion = version;
        answers = new int[version];
        currentQuestionIndex = 0;

        binding.layoutVersionSelect.setVisibility(View.GONE);
        binding.layoutQuestion.setVisibility(View.VISIBLE);

        showQuestion();
        binding.btnNext.setOnClickListener(v -> handleNext());
    }

    private int[] getQuestionIndices(int version) {
        int[] indices = new int[version];
        int perDim = version / 4;
        int[][] dimRanges = {{0, 9}, {9, 18}, {18, 27}, {27, 36}};
        int pos = 0;
        for (int d = 0; d < 4; d++) {
            for (int i = 0; i < perDim; i++) {
                indices[pos++] = dimRanges[d][0] + i;
            }
        }
        return indices;
    }

    private void showQuestion() {
        int[] indices = getQuestionIndices(selectedVersion);
        if (currentQuestionIndex >= selectedVersion) {
            showResult();
            return;
        }

        int qIdx = indices[currentQuestionIndex];
        String[] q = ALL_QUESTIONS[qIdx];

        binding.textQuestionNumber.setText(String.format(Locale.getDefault(),
                "第 %d/%d 题", currentQuestionIndex + 1, selectedVersion));
        binding.textQuestion.setText(q[0]);
        binding.radioOptionA.setText(q[1]);
        binding.radioOptionB.setText(q[2]);
        binding.radioGroup.clearCheck();

        if (currentQuestionIndex == selectedVersion - 1) {
            binding.btnNext.setText("提交");
        } else {
            binding.btnNext.setText("下一题");
        }
    }

    private void handleNext() {
        int selectedId = binding.radioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "请选择一个选项", Toast.LENGTH_SHORT).show();
            return;
        }

        answers[currentQuestionIndex] = (selectedId == binding.radioOptionA.getId()) ? 0 : 1;
        currentQuestionIndex++;
        showQuestion();
    }

    private void showResult() {
        binding.layoutQuestion.setVisibility(View.GONE);
        binding.layoutResult.setVisibility(View.VISIBLE);

        int[] indices = getQuestionIndices(selectedVersion);
        int eCount = 0, iCount = 0, sCount = 0, nCount = 0, tCount = 0, fCount = 0, jCount = 0, pCount = 0;

        for (int i = 0; i < selectedVersion; i++) {
            int qIdx = indices[i];
            int ans = answers[i];
            if (qIdx < 9) { if (ans == 0) eCount++; else iCount++; }
            else if (qIdx < 18) { if (ans == 0) sCount++; else nCount++; }
            else if (qIdx < 27) { if (ans == 0) tCount++; else fCount++; }
            else { if (ans == 0) jCount++; else pCount++; }
        }

        String type = (eCount >= iCount ? "E" : "I") +
                (sCount >= nCount ? "S" : "N") +
                (tCount >= fCount ? "T" : "F") +
                (jCount >= pCount ? "J" : "P");

        String nickname = getTypeNickname(type);
        String description = getTypeDescription(type);

        StringBuilder sb = new StringBuilder();
        sb.append("你的MBTI类型：").append(type).append(" — ").append(nickname).append("\n\n");
        sb.append("测试版本：").append(selectedVersion).append("题\n");
        sb.append("维度得分：\n");
        sb.append(String.format("  E=%d  I=%d\n", eCount, iCount));
        sb.append(String.format("  S=%d  N=%d\n", sCount, nCount));
        sb.append(String.format("  T=%d  F=%d\n", tCount, fCount));
        sb.append(String.format("  J=%d  P=%d\n\n", jCount, pCount));
        sb.append("【类型介绍】\n").append(description);

        binding.textResult.setText(sb.toString());

        saveResult(type, nickname, selectedVersion, eCount, iCount, sCount, nCount, tCount, fCount, jCount, pCount);
    }

    private void saveResult(String type, String nickname, int version,
                            int e, int i, int s, int n, int t, int f, int j, int p) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            JSONArray history = new JSONArray(prefs.getString(KEY_HISTORY, "[]"));

            JSONObject result = new JSONObject();
            result.put("type", type);
            result.put("nickname", nickname);
            result.put("version", version);
            result.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));
            result.put("E", e); result.put("I", i);
            result.put("S", s); result.put("N", n);
            result.put("T", t); result.put("F", f);
            result.put("J", j); result.put("P", p);

            history.put(result);
            prefs.edit().putString(KEY_HISTORY, history.toString()).apply();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    static String getTypeNickname(String type) {
        if (type == null) return "未知";
        switch (type) {
            case "INTJ": return "建筑师";
            case "INTP": return "逻辑学家";
            case "ENTJ": return "指挥官";
            case "ENTP": return "辩论家";
            case "INFJ": return "提倡者";
            case "INFP": return "调停者";
            case "ENFJ": return "主人公";
            case "ENFP": return "竞选者";
            case "ISTJ": return "物流师";
            case "ISFJ": return "守卫者";
            case "ESTJ": return "总经理";
            case "ESFJ": return "执政官";
            case "ISTP": return "鉴赏家";
            case "ISFP": return "探险家";
            case "ESTP": return "企业家";
            case "ESFP": return "表演者";
            default: return type;
        }
    }

    static String getTypeDescription(String type) {
        if (type == null) return "未知类型";
        switch (type) {
            case "INTJ": return "INTJ是富有想象力的战略家，一切都有计划。他们工作努力、自信，对知识和能力有强烈的渴望。适合独立思考和长期规划的工作。";
            case "INTP": return "INTP是逻辑学家，对知识有永不知足的渴望。他们喜欢发现事物背后的原理，善于分析和解决复杂问题。适合研究和分析类工作。";
            case "ENTJ": return "ENTJ是天生的领导者，意志坚定且富有魅力。他们善于发现低效之处并加以改进，适合管理和决策类工作。";
            case "ENTP": return "ENTP是聪明的思考者，喜欢智力挑战。他们善于看到各种可能性，思维敏捷，适合创新和创业类工作。";
            case "INFJ": return "INFJ是提倡者，安静而神秘，同时鼓舞人心。他们有深刻的洞察力，关心他人，适合咨询和教育类工作。";
            case "INFP": return "INFP是调停者，诗意而善良。他们有强烈的价值观，追求有意义的生活，适合创作和公益类工作。";
            case "ENFJ": return "ENFJ是主人公，富有魅力和影响力。他们善于激励他人，有强烈的责任感，适合领导和服务类工作。";
            case "ENFP": return "ENFP是竞选者，热情而有创造力。他们善于发现生活中的可能性，社交能力强，适合创意和媒体类工作。";
            case "ISTJ": return "ISTJ是物流师，实际而注重事实。他们可靠有责任心，重视秩序和传统，适合管理和执行类工作。";
            case "ISFJ": return "ISFJ是守卫者，非常专注而温暖。他们善于照顾他人，有强烈的使命感，适合医疗和服务类工作。";
            case "ESTJ": return "ESTJ是总经理，出色的组织者。他们重视秩序和规则，善于管理事务，适合行政和管理类工作。";
            case "ESFJ": return "ESFJ是执政官，非常有同情心。他们善于社交，乐于助人，适合人事和客户服务类工作。";
            case "ISTP": return "ISTP是鉴赏家，大胆而实际的观察者。他们善于使用工具和解决实际问题，适合技术和工程类工作。";
            case "ISFP": return "ISFP是探险家，灵活而有魅力的艺术家。他们善于发现美，喜欢用行动表达自己，适合艺术和设计类工作。";
            case "ESTP": return "ESTP是企业家，聪明而精力充沛。他们善于随机应变，喜欢冒险和挑战，适合销售和创业类工作。";
            case "ESFP": return "ESFP是表演者，自发的且精力充沛。他们善于享受生活，有强烈的审美感，适合表演和娱乐类工作。";
            default: return "未知类型";
        }
    }
}
