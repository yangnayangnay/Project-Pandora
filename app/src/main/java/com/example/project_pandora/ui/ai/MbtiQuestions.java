package com.example.project_pandora.ui.ai;

import java.util.ArrayList;
import java.util.List;

public class MbtiQuestions {

    public static class Question {
        public final String text;
        public final String optionA;
        public final String optionB;

        public Question(String text, String optionA, String optionB) {
            this.text = text;
            this.optionA = optionA;
            this.optionB = optionB;
        }
    }

    public static List<Question> getQuestions() {
        List<Question> q = new ArrayList<>();

        q.add(new Question("在聚会中，你更倾向于", "主动与多人交谈，包括陌生人", "和少数熟悉的人深入交流"));
        q.add(new Question("你更关注", "具体的事实和细节", "可能性和整体趋势"));
        q.add(new Question("做决定时，你更看重", "逻辑和客观分析", "个人价值观和他人感受"));
        q.add(new Question("你更喜欢", "制定详细计划并按计划执行", "保持灵活，随机应变"));

        q.add(new Question("独处一段时间后，你会感到", "精力充沛，准备好社交", "需要与人交流来恢复能量"));
        q.add(new Question("学习新事物时，你更喜欢", "按部就班，从基础开始", "跳跃式学习，先看大局"));
        q.add(new Question("你认为更重要的是", "公平公正，一视同仁", "和谐共处，因人而异"));
        q.add(new Question("对于截止日期，你通常", "提前完成任务", "在最后期限前赶工完成"));

        q.add(new Question("在团队中，你通常是", "主动发言、推动讨论的人", "倾听思考、适时发言的人"));
        q.add(new Question("你更相信", "经验和已验证的方法", "灵感和新的理论"));
        q.add(new Question("面对冲突时，你倾向于", "讲道理，辨明是非", "寻求和解，维护关系"));
        q.add(new Question("你的生活节奏更像是", "有规律、有结构", "随性、开放式"));

        q.add(new Question("你的注意力更多放在", "外部世界的人和事", "内心世界的想法和感受"));
        q.add(new Question("阅读时你更喜欢", "实用性的、有操作指南的内容", "启发性的、有想象空间的内容"));
        q.add(new Question("你更欣赏", "能力出众、直言不讳的人", "善解人意、温暖体贴的人"));
        q.add(new Question("面对新项目，你会", "先列出任务清单和里程碑", "先构思大方向，边做边调整"));

        q.add(new Question("你的社交风格更接近", "广泛交友，来者不拒", "深交少数，慢热型"));
        q.add(new Question("描述事物时，你更常用", "具体的例子和数据", "比喻和抽象概念"));
        q.add(new Question("你更容易被什么打动", "严密的论证", "感人的故事"));
        q.add(new Question("你的工作空间通常", "整洁有序", "比较随意"));

        q.add(new Question("周末你更倾向于", "参加社交活动或外出", "在家休息或独处"));
        q.add(new Question("解决问题时，你更依赖", "过去的成功经验", "直觉和创造性思维"));
        q.add(new Question("你更讨厌", "不讲逻辑的人", "不近人情的人"));
        q.add(new Question("旅行时你更倾向于", "提前规划好行程路线", "说走就走，随性而行"));

        q.add(new Question("别人眼中你是", "外向、活跃的人", "内向、沉静的人"));
        q.add(new Question("你更擅长", "处理当下的实际问题", "预见未来的发展方向"));
        q.add(new Question("你做事情的驱动力更多来自", "理性判断和目标导向", "内心认同和情感需求"));
        q.add(new Question("你更享受", "完成计划清单的成就感", "探索未知的新鲜感"));

        return q;
    }

    public static String[] getCognitiveFunctions(String mbtiType) {
        switch (mbtiType) {
            case "INTJ": return new String[]{"Ni", "Te", "Fi", "Se", "Ne", "Ti", "Fe", "Si"};
            case "INTP": return new String[]{"Ti", "Ne", "Si", "Fe", "Te", "Ni", "Se", "Fi"};
            case "ENTJ": return new String[]{"Te", "Ni", "Se", "Fi", "Ti", "Ne", "Si", "Fe"};
            case "ENTP": return new String[]{"Ne", "Ti", "Fe", "Si", "Ni", "Te", "Fi", "Se"};
            case "INFJ": return new String[]{"Ni", "Fe", "Ti", "Se", "Ne", "Fi", "Te", "Si"};
            case "INFP": return new String[]{"Fi", "Ne", "Si", "Te", "Fe", "Ni", "Se", "Ti"};
            case "ENFJ": return new String[]{"Fe", "Ni", "Se", "Ti", "Fi", "Ne", "Si", "Te"};
            case "ENFP": return new String[]{"Ne", "Fi", "Te", "Si", "Ni", "Fe", "Ti", "Se"};
            case "ISTJ": return new String[]{"Si", "Te", "Fi", "Ne", "Se", "Ti", "Fe", "Ni"};
            case "ISFJ": return new String[]{"Si", "Fe", "Ti", "Ne", "Se", "Fi", "Te", "Ni"};
            case "ESTJ": return new String[]{"Te", "Si", "Ne", "Fi", "Ti", "Se", "Ni", "Fe"};
            case "ESFJ": return new String[]{"Fe", "Si", "Ne", "Ti", "Fi", "Se", "Ni", "Te"};
            case "ISTP": return new String[]{"Ti", "Se", "Ni", "Fe", "Te", "Si", "Ne", "Fi"};
            case "ISFP": return new String[]{"Fi", "Se", "Ni", "Te", "Fe", "Si", "Ne", "Ti"};
            case "ESTP": return new String[]{"Se", "Ti", "Fe", "Ni", "Si", "Te", "Fi", "Ne"};
            case "ESFP": return new String[]{"Se", "Fi", "Te", "Ni", "Si", "Fe", "Ti", "Ne"};
            default: return new String[]{"?", "?", "?", "?", "?", "?", "?", "?"};
        }
    }

    public static String getFunctionDescription(String func) {
        switch (func) {
            case "Se": return "外倾感觉：关注外部世界的感官体验和当下现实";
            case "Si": return "内倾感觉：依赖个人经验记忆和内在身体感受";
            case "Ne": return "外倾直觉：探索外部世界的可能性和关联";
            case "Ni": return "内倾直觉：洞察事物本质和未来发展趋势";
            case "Te": return "外倾思考：组织外部世界，追求效率和秩序";
            case "Ti": return "内倾思考：构建内在逻辑体系，追求精确理解";
            case "Fe": return "外倾情感：关注他人情感和群体和谐";
            case "Fi": return "内倾情感：忠于内在价值观和真实感受";
            default: return func;
        }
    }
}