package com.example.project_pandora.ui.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * Round27 REQ-27-11: 16Personalities 风格 60 题中文版题库（四维度各 15 题）。
 * 四要素结构：text / optionA / optionB / dimension（"EI"/"SN"/"TF"/"JP"）。
 * 计分语义（与 MBTITestActivity 既有编码 1=选A、0=选B 配合）：
 *   选 A（ans==1）→ 该维度第一字母（E/S/T/J）+1；选 B（ans==0）→ 第二字母（I/N/F/P）+1。
 * 题目按 EI/SN/TF/JP 循环穿插排列（16P 风格），维度归属由 dimension 字段显式携带，
 * 顺序不承担计分语义；静态常量无网络加载。
 */
public class MbtiQuestions {

    public static class Question {
        public final String text;
        public final String optionA;
        public final String optionB;
        public final String dimension;

        public Question(String text, String optionA, String optionB, String dimension) {
            this.text = text;
            this.optionA = optionA;
            this.optionB = optionB;
            this.dimension = dimension;
        }
    }

    public static List<Question> getQuestions() {
        List<Question> q = new ArrayList<>();

        // ---- 第 1 轮（1~4 题：EI/SN/TF/JP）----
        q.add(new Question("参加聚会时，你通常会", "主动跟很多人聊天，包括陌生人", "只跟少数熟人深聊，或提前离场", "EI"));
        q.add(new Question("你更愿意阅读的内容是", "有具体数据和操作步骤的实用指南", "充满想象力和未来感的科幻小说", "SN"));
        q.add(new Question("朋友意见不合找你评理时，你会", "客观分析谁对谁错，就事论事", "先安抚双方情绪，照顾每个人的感受", "TF"));
        q.add(new Question("面对旅行，你的习惯是", "提前做好详细行程表并严格执行", "订个大概方向，到了再说，随遇而安", "JP"));

        // ---- 第 2 轮（5~8 题）----
        q.add(new Question("连续忙碌一整天的社交活动后，你感到", "意犹未尽，精力更旺盛", "身心俱疲，急需独处充电", "EI"));
        q.add(new Question("学习一项新技能时，你倾向于", "循序渐进，先把基础打牢", "先看整体框架，凭兴趣跳跃式探索", "SN"));
        q.add(new Question("做重大决定时，你更依赖", "逻辑分析和利弊清单", "内心价值观和对他人的影响", "TF"));
        q.add(new Question("你的书桌或工作台通常是", "物品分类摆放，井井有条", "随手放置，“创意性”凌乱", "JP"));

        // ---- 第 3 轮（9~12 题）----
        q.add(new Question("在小组讨论中，你的角色更像", "打破沉默、推动话题的发起者", "先倾听消化，被点名才发言的思考者", "EI"));
        q.add(new Question("别人向你描述一个方案时，你首先注意到", "其中的细节漏洞和数据错误", "方案背后的创意和潜在可能性", "SN"));
        q.add(new Question("以下哪句话更让你不舒服", "“你这个人太不讲逻辑了”", "“你这个人太不近人情了”", "TF"));
        q.add(new Question("接到一个为期一个月的任务，你会", "立刻拆解成周计划，倒排工期", "先构思大方向，临近期限再冲刺", "JP"));

        // ---- 第 4 轮（13~16 题）----
        q.add(new Question("周末的黄金时光，你更愿意", "约朋友聚餐、逛街或参加活动", "宅家看书、打游戏或独处放空", "EI"));
        q.add(new Question("你更相信", "亲身验证过的经验和事实", "一闪而过的直觉和灵感", "SN"));
        q.add(new Question("评价一个人时，你更看重", "他做事的能力和水平", "他待人的真诚和温度", "TF"));
        q.add(new Question("对于“计划赶不上变化”，你的态度是", "所以要做得更周密，留好缓冲", "所以干脆少做计划，见招拆招", "JP"));

        // ---- 第 5 轮（17~20 题）----
        q.add(new Question("电梯里遇到不太熟的同事，你会", "主动搭话寒暄几句", "低头看手机，祈祷对方别开口", "EI"));
        q.add(new Question("解释一个复杂概念时，你习惯", "用具体案例和数据说明", "打比方、讲类比，勾勒整体图景", "SN"));
        q.add(new Question("团队里有人犯了错，你的第一反应是", "分析错误原因，指出问题所在", "先关心他的状态，怕他太难自责", "TF"));
        q.add(new Question("你的待办清单风格是", "按优先级排好，完成一项划掉一项", "想到什么写什么，经常不看完", "JP"));

        // ---- 第 6 轮（21~24 题）----
        q.add(new Question("新环境里认识新朋友，对你来说", "是件令人兴奋的轻松事", "需要鼓起勇力的负担事", "EI"));
        q.add(new Question("你更欣赏哪种老师/前辈", "干货满满、逻辑严谨的实战派", "天马行空、启发思维的梦想家", "SN"));
        q.add(new Question("看电影时最打动你的是", "精巧严密、经得起推敲的剧情", "真挚动人、引起共鸣的情感", "TF"));
        q.add(new Question("出门旅行前一晚，你的行李箱状态是", "早就列好清单收拾妥当", "睡前才开始随手塞，或到了再买", "JP"));

        // ---- 第 7 轮（25~28 题）----
        q.add(new Question("电话铃响起时，你的第一感觉是", "有人找我，挺好的，接", "又来事了，有点烦，看看是谁", "EI"));
        q.add(new Question("拿到一份新报告，你会", "逐字核对数据和事实", "先翻结论，瞄一眼大趋势", "SN"));
        q.add(new Question("朋友遭遇感情问题来倾诉，你会", "帮他理性分析这段关系值不值得", "先陪他难过，倾听和拥抱更重要", "TF"));
        q.add(new Question("你的日常作息", "固定时间起床睡觉，规律稳定", "随心情和状态，弹性很大", "JP"));

        // ---- 第 8 轮（29~32 题）----
        q.add(new Question("头脑风暴时，你", "边说边想，说出来才知道自己在想什么", "先安静想清楚，再一次性表达", "EI"));
        q.add(new Question("你觉得更有价值的是", "解决眼前切实存在的问题", "探索十年后可能改变世界的构想", "SN"));
        q.add(new Question("如果必须批评同事，你会", "直接指出事实，对事不对人", "委婉迂回，照顾对方面子", "TF"));
        q.add(new Question("对于截止日期，你通常", "提前完成才安心，厌恶赶工", "deadline 是第一生产力，压线交付", "JP"));

        // ---- 第 9 轮（33~36 题）----
        q.add(new Question("你在社交软件上的状态是", "群里活跃，消息秒回", "常年潜水，能不回就不回", "EI"));
        q.add(new Question("参观博物馆时，你更喜欢", "仔细阅读每件展品的说明文字", "走马观花，感受整体氛围和联想", "SN"));
        q.add(new Question("你更讨厌别人说你", "“感情用事，不讲道理”", "“冷血动物，没有温度”", "TF"));
        q.add(new Question("约朋友吃饭，你喜欢", "提前几天定好时间地点", "当天临时起意，“出来吗？”", "JP"));

        // ---- 第 10 轮（37~40 题）----
        q.add(new Question("独处对你而言是", "偶尔需要，但久了会无聊", "像呼吸一样自然的能量来源", "EI"));
        q.add(new Question("你更擅长的思维方式是", "记住细节、精确执行", "跳跃联想、举一反三", "SN"));
        q.add(new Question("下属把事情搞砸了但情有可原，你会", "照样按规则处理，一视同仁", "酌情从轻，法理不外乎人情", "TF"));
        q.add(new Question("你的电脑文件管理方式是", "目录层级清晰，命名规范", "全扔桌面或下载文件夹，靠搜索", "JP"));

        // ---- 第 11 轮（41~44 题）----
        q.add(new Question("在公司年会等大型场合，你", "如鱼得水，主动串场互动", "全程安静吃饭，盼着早点散场", "EI"));
        q.add(new Question("别人评价你更像", "脚踏实地的实干家", "仰望星空的梦想家", "SN"));
        q.add(new Question("做决定最让你痛苦的是", "明明逻辑都对，却莫名不踏实", "明知感情用事，还是想任性一次", "TF"));
        q.add(new Question("项目执行中途出现更好的思路，你会", "评估扰动成本，大概率按原计划走", "果断调整方向，计划就是用来改的", "JP"));

        // ---- 第 12 轮（45~48 题）----
        q.add(new Question("长时间独处之后，你最想", "找人聚一聚，说说见闻", "继续独处，享受安静", "EI"));
        q.add(new Question("听别人讲理论课时，你希望", "多讲案例、步骤和实操", "多讲理念、愿景和可能性", "SN"));
        q.add(new Question("网购收到瑕疵品，你的处理方式是", "列清事实，据理力争索赔", "嫌麻烦，情绪上来先给差评", "TF"));
        q.add(new Question("你的周末计划通常", "早有安排，按部就班执行", "睡醒再说，看心情临时决定", "JP"));

        // ---- 第 13 轮（49~52 题）----
        q.add(new Question("被人当众表扬时，你", "大方接受，享受这份关注", "浑身不自在，恨不得钻地缝", "EI"));
        q.add(new Question("面对全新领域，你的学习路径是", "从官方文档和基础教程啃起", "直接上手乱试，遇到问题再补课", "SN"));
        q.add(new Question("你认为好的管理者应该", "赏罚分明，制度至上", "以人为本，人情练达", "TF"));
        q.add(new Question("整理房间的时机是", "定期打扫，维持整洁是刚需", "乱到忍无可忍才爆发式整理", "JP"));

        // ---- 第 14 轮（53~56 题）----
        q.add(new Question("和刚认识的人聊天，你更可能", "主动分享自己的经历，快速热络", "多问对方，保持礼貌距离", "EI"));
        q.add(new Question("你记忆最深的是", "具体发生过的事件细节", "当时的氛围和整体感觉", "SN"));
        q.add(new Question("“情义”和“道理”冲突时，你偏向", "道理，规则面前人人平等", "情义，人非草木孰能无情", "TF"));
        q.add(new Question("如果人生是一场旅行，你是", "按攻略打卡的规划师", "说走就走的自由行走", "JP"));

        // ---- 第 15 轮（57~60 题）----
        q.add(new Question("疲惫时的充电方式，你选择", "约上三五好友，热闹一场", "关掉手机，一个人静静", "EI"));
        q.add(new Question("读新闻时你更关注", "确凿的数据、时间和当事人", "这件事背后反映的趋势和意义", "SN"));
        q.add(new Question("给好友提职业建议时，你会", "理性分析行业前景和性价比", "首先问他自己真正热爱什么", "TF"));
        q.add(new Question("十年后的生活，你希望是", "目标清晰，按现在的规划稳步实现", "保持开放，谁知道明天会怎样", "JP"));

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