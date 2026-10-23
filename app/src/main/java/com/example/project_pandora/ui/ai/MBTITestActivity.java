package com.example.project_pandora.ui.ai;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.R;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.data.remote.AIApi;
import com.example.project_pandora.databinding.ActivityMbtiTestBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MBTITestActivity extends AppCompatActivity {

    private ActivityMbtiTestBinding binding;
    private AIApi aiApi;

    private List<MbtiQuestions.Question> questions;
    private final List<Integer> answers = new ArrayList<>();
    private int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMbtiTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        aiApi = NetworkClient.getInstance(this).createService(AIApi.class);
        questions = MbtiQuestions.getQuestions();

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        binding.btnOptionA.setOnClickListener(v -> selectAnswer(1));
        binding.btnOptionB.setOnClickListener(v -> selectAnswer(0));
        binding.btnRetry.setOnClickListener(v -> restartTest());

        loadExistingReport();
    }

    /**
     * Round24 REQ-24-08: 进入先查 GET /ai/mbti/report 分流——
     * 已测账号直达结果页（btnRetry 复用既有 restartTest 语义，文案"重新测试"）；
     * NOT_FOUND("暂无MBTI报告")进答题；其他网络异常 Toast 提示后进答题（不白屏、给出路）
     */
    private void loadExistingReport() {
        binding.layoutQuestion.setVisibility(View.GONE);
        binding.layoutLoading.setVisibility(View.VISIBLE);
        binding.layoutResult.setVisibility(View.GONE);

        aiApi.getMbtiReport()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (isDestroyed() || isFinishing()) return;
                            if (response.isSuccess() && response.getData() != null) {
                                // 直达回显态：GET /ai/mbti/report 响应无 source 字段，
                                // 为禁止伪装为 AI 生成，此态省略来源标识展示
                                displayResult(response.getData());
                                binding.textAiSource.setVisibility(View.GONE);
                            } else {
                                Toast.makeText(this, "报告加载失败，可直接测试", Toast.LENGTH_SHORT).show();
                                showQuestion();
                            }
                        },
                        error -> {
                            if (isDestroyed() || isFinishing()) return;
                            if (error instanceof retrofit2.HttpException
                                    && ((retrofit2.HttpException) error).code() == 404) {
                                showQuestion();
                            } else {
                                Toast.makeText(this, "报告加载失败，可直接测试", Toast.LENGTH_SHORT).show();
                                showQuestion();
                            }
                        });
    }

    private void showQuestion() {
        binding.layoutQuestion.setVisibility(View.VISIBLE);
        binding.layoutLoading.setVisibility(View.GONE);
        binding.layoutResult.setVisibility(View.GONE);

        MbtiQuestions.Question q = questions.get(currentIndex);
        binding.textProgress.setText(String.format("第 %d / %d 题", currentIndex + 1, questions.size()));
        binding.progressBarQuestion.setMax(questions.size());
        binding.progressBarQuestion.setProgress(currentIndex + 1);
        binding.textQuestion.setText(q.text);
        binding.btnOptionA.setText(q.optionA);
        binding.btnOptionB.setText(q.optionB);
    }

    private void selectAnswer(int answer) {
        answers.add(answer);
        currentIndex++;

        if (currentIndex < questions.size()) {
            showQuestion();
        } else {
            submitTest();
        }
    }

    private void submitTest() {
        binding.layoutQuestion.setVisibility(View.GONE);
        binding.layoutLoading.setVisibility(View.VISIBLE);
        binding.layoutResult.setVisibility(View.GONE);

        String personalityType;
        try {
            personalityType = MbtiCalculator.calculate(answers);
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            restartTest();
            return;
        }

        Map<String, Object> request = new java.util.HashMap<>();
        request.put("personalityType", personalityType);
        request.put("answers", answers);

        aiApi.generateMbtiReport(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess() && response.getData() != null) {
                                displayResult(response.getData());
                                bindReportSource(response.getData().get("source"));
                            } else {
                                Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                                Map<String, Object> localResult = new java.util.HashMap<>();
                                localResult.put("personalityType", personalityType);
                                localResult.put("reportContent", "AI报告生成失败，以下为本地分析结果。\n\n" +
                                        "MBTI类型：" + personalityType + "\n" +
                                        "类型描述：" + getTypeDescription(personalityType) + "\n\n" +
                                        "如需详细AI分析报告，请检查网络连接后重新测试。");
                                displayResult(localResult);
                                bindReportSource("LOCAL_FALLBACK");
                            }
                        },
                        error -> {
                            Toast.makeText(this, "AI报告生成失败，显示本地结果", Toast.LENGTH_SHORT).show();
                            Map<String, Object> localResult = new java.util.HashMap<>();
                            localResult.put("personalityType", personalityType);
                            localResult.put("reportContent", "AI报告生成失败，以下为本地分析结果。\n\n" +
                                    "MBTI类型：" + personalityType + "\n" +
                                    "类型描述：" + getTypeDescription(personalityType) + "\n\n" +
                                    "如需详细AI分析报告，请检查网络连接后重新测试。");
                            displayResult(localResult);
                            bindReportSource("LOCAL_FALLBACK");
                        }
                );
    }

    private void displayResult(Map<String, Object> data) {
        binding.layoutQuestion.setVisibility(View.GONE);
        binding.layoutLoading.setVisibility(View.GONE);
        binding.layoutResult.setVisibility(View.VISIBLE);

        String type = data.get("personalityType") != null ? data.get("personalityType").toString() : "????";
        binding.textMbtiType.setText(type);
        binding.textMbtiDesc.setText(MBTITestActivity.getTypeDescription(type));

        String[] functions = MbtiQuestions.getCognitiveFunctions(type);
        StringBuilder funcSb = new StringBuilder();
        String[] labels = {"主导", "辅助", "第三", "劣势", "第五", "第六", "第七", "第八"};
        for (int i = 0; i < functions.length; i++) {
            funcSb.append(labels[i]).append("：").append(functions[i])
                    .append(" — ").append(MbtiQuestions.getFunctionDescription(functions[i]))
                    .append("\n\n");
        }
        binding.textFunctions.setText(funcSb.toString().trim());

        String report = data.get("reportContent") != null ? data.get("reportContent").toString() : "暂无报告内容";
        binding.textReport.setText(report);
    }

    private void restartTest() {
        answers.clear();
        currentIndex = 0;
        showQuestion();
    }

    /**
     * Round24 REQ-24-07: MBTI 报告来源标识——严格按响应 source 字段驱动（禁止固定文案）：
     * EXTERNAL → "— AI 生成 —"；LOCAL（generate 既有取值）/LOCAL_FALLBACK/网络失败本地兜底 → "— 本地兜底生成 —"
     */
    private void bindReportSource(Object source) {
        String s = source != null ? source.toString() : null;
        if ("EXTERNAL".equals(s)) {
            binding.textAiSource.setText("— AI 生成 —");
        } else {
            binding.textAiSource.setText("— 本地兜底生成 —");
        }
        binding.textAiSource.setVisibility(View.VISIBLE);
    }

    public static String getTypeDescription(String type) {
        switch (type) {
            case "INTJ": return "建筑师：富有想象力且战略性的思考者，一切皆有计划。";
            case "INTP": return "逻辑学家：具有创造力的发明家，对知识有不可抑制的渴望。";
            case "ENTJ": return "指挥官：大胆、富有想象力且意志坚强的领导者，总能找到或创造解决方案。";
            case "ENTP": return "辩论家：聪明好奇的思想者，无法抗拒智力上的挑战。";
            case "INFJ": return "提倡者：安静而神秘，同时鼓舞人心且不知疲倦的理想主义者。";
            case "INFP": return "调停者：诗意、善良的利他主义者，总是热切地为正义事业提供帮助。";
            case "ENFJ": return "主人公：富有魅力、鼓舞人心的领导者，能够使听众为之着迷。";
            case "ENFP": return "竞选者：热情、有同情心且富有创造力的社会人士，总是能找到微笑的理由。";
            case "ISTJ": return "物流师：务实且注重事实的人，其可靠性不容置疑。";
            case "ISFJ": return "守卫者：非常专注而温暖的守护者，时刻准备着保护爱着的人们。";
            case "ESTJ": return "总经理：出色的管理者，在管理事物或人员方面无与伦比。";
            case "ESFJ": return "执政官：极有同情心、好交际且受欢迎的人们，总是乐于助人。";
            case "ISTP": return "鉴赏家：大胆而实际的实验家，擅长使用各种工具。";
            case "ISFP": return "探险家：灵活有魅力的艺术家，时刻准备探索和体验新事物。";
            case "ESTP": return "企业家：聪明、精力充沛且感知敏锐的人，真正享受生活在边缘地带。";
            case "ESFP": return "表演者：自发的、精力充沛而热情的表演者——生活在他们周围永远不会无聊。";
            default: return "未知类型：" + type;
        }
    }
}
