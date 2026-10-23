package com.pandora.backend.ai;

import com.pandora.backend.common.ErrorCode;
import com.pandora.backend.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LocalFallbackAIClient {

    private static final Logger log = LoggerFactory.getLogger(LocalFallbackAIClient.class);

    /**
     * Round24 T3.1: 64 卦名常量表——与 App 端 HexagramTexts.HEXAGRAM_NAMES 同一份 64 名单。
     * 编号规则与 DivinationEngine 同源：本卦编号 = (下卦数 - 1) × 8 + 上卦数（先天卦数 1~8）。
     */
    private static final String[] HEXAGRAM_NAMES = {
        "乾", "夬", "大有", "大壮", "小畜", "需", "大畜", "泰",
        "履", "兑", "睽", "归妹", "中孚", "节", "损", "临",
        "同人", "革", "离", "丰", "家人", "既济", "贲", "明夷",
        "无妄", "随", "噬嗑", "震", "益", "屯", "颐", "复",
        "姤", "大过", "鼎", "恒", "巽", "井", "蛊", "升",
        "讼", "困", "未济", "解", "涣", "坎", "蒙", "师",
        "遁", "咸", "旅", "小过", "渐", "蹇", "艮", "谦",
        "否", "萃", "晋", "豫", "观", "比", "剥", "坤"
    };

    /**
     * Round24 T3.2: 8 组八卦属性基础模板（按上卦分组，乾一兑二离三震四巽五坎六艮七坤八）。
     * 全部为积极正向阐释，无凶兆/恐吓/宿命论表述。
     */
    private static final String[] TRIGRAM_TEMPLATES = {
        /* 1 乾(天/刚健) 进取开拓 */
        "天道刚健、自强不息的气息。当前正处于开拓进取的上升期，坚定的意志与持续的行动将为您打开新局面，适合主动设立目标、争取表现机会。",
        /* 2 兑(泽/喜悦) 喜悦沟通 */
        "和悦沟通、以诚待人的气息。近期人际氛围融洽，是表达想法、增进协作的好时机，真诚的交流将为您赢得支持与好运。",
        /* 3 离(火/光明) 光明愿景 */
        "光明磊落、前景可期的气息。如同太阳当空，您的能力正被更多人看见，保持热情与专注，清晰的愿景将引领您走向更大的舞台。",
        /* 4 震(雷/行动) 行动突破 */
        "雷声振动、奋发有为的气息。变化与机遇正在靠近，果断行动、敢于尝试新事物，将在动态中抢占先机、实现突破。",
        /* 5 巽(风/顺势) 柔顺借势 */
        "清风入微、柔顺借势的气息。循序渐进、顺势而为是当下的最佳策略，以灵活变通的方式推进目标，细水长流终将汇聚成势。",
        /* 6 坎(水/坚韧) 坚韧磨砺 */
        "水流不息、坚韧不拔的气息。当前正是磨砺心志、积累内功的阶段，保持信心与耐心，每一次坚持都在为将来的顺利积蓄力量。",
        /* 7 艮(山/沉稳) 沉稳积累 */
        "山岳稳重、知止而安的气息。稳扎稳打、厚积薄发是当下的主旋律，守住节奏、沉淀实力，坚实的积累将支撑您行稳致远。",
        /* 8 坤(地/包容) 厚德承载 */
        "大地包容、厚德载物的气息。以宽厚之心待人处事，注重配合与奉献，您的踏实与包容将赢得信任，福泽自然绵长。"
    };

    /**
     * Round24 T3.2: 6 爻爻位提示句（初爻萌芽蓄势/二爻得中稳健/三爻审慎过渡/四爻临近突破/五爻得位大吉/上爻功成守成）。
     */
    private static final String[] LINE_HINTS = {
        /* 初爻(1) 萌芽蓄势 */
        "眼下正值萌芽蓄势之时，宜低调准备、积蓄能量，为下一步成长打好基础。",
        /* 二爻(2) 得中稳健 */
        "眼下居于得中之位，稳步前行最为适宜，保持稳健的节奏自有吉祥相伴。",
        /* 三爻(3) 审慎过渡 */
        "眼下处于过渡阶段，多一分审慎与观察，谨慎行事便可平稳迈入新阶段。",
        /* 四爻(4) 临近突破 */
        "已临近突破关口，做好准备、把握时机，即将迎来水到渠成的进展。",
        /* 五爻(5) 得位大吉 */
        "正处得位大吉之位，是施展才华、成就事情的黄金时刻，宜积极进取、大展身手。",
        /* 上爻(6) 功成守成 */
        "功成之际重在守成，珍惜成果、谦逊自持，让圆满的局面延续更长久的美好。"
    };

    /** 固定收尾：理性看待红线文案（娱乐定位声明，不可省略） */
    private static final String DIVINATION_CLOSING = "请理性看待占卜结果，仅供娱乐与文化体验。";

    /** 无卦象上下文（或卦名无法识别）时回归的既有固定兜底文案（两参调用兼容） */
    private static final String DIVINATION_DEFAULT_FALLBACK =
            "基于本地算法的卦象解读：卦象蕴含积极向上的能量，预示您正处于稳步向前的阶段。"
                    + "建议近期专注核心目标、合理规划时间，保持积极心态与开放姿态；遇到变化时顺势而为、主动沟通，"
                    + "您的能力与努力将被更多人看见，好消息正在靠近。" + DIVINATION_CLOSING;

    public String call(String prompt, String analysisType) {
        return call(prompt, analysisType, null);
    }

    /**
     * Round24 T3.1: 带上下文本地兜底重载。
     * DIVINATION 类型按 context 中的本卦名/动爻生成 48 组（8 卦属性 × 6 爻位）差异化文案；
     * 其余类型或 context=null 时行为与原两参调用完全一致。
     */
    public String call(String prompt, String analysisType, Map<String, Object> context) {
        log.info("使用本地兜底AI: analysisType={}, hasContext={}", analysisType, context != null);
        return generateLocalResult(prompt, analysisType, context);
    }

    private String generateLocalResult(String prompt, String analysisType, Map<String, Object> context) {
        switch (analysisType) {
            case "MBTI_ANALYSIS":
                return "基于本地算法的MBTI分析结果：根据您的答题情况，您倾向于内向、直觉、情感、判断型(INFJ)。您具有深刻的洞察力和同理心，适合从事需要创造力和人文关怀的工作。";
            case "KEYWORD_EXTRACTION":
            case "KEYWORD_ANALYSIS":
                return extractKeywordsLocal(prompt);
            case "SUGGESTION":
                return "基于本地算法的工作建议：1.合理规划每日任务优先级 2.保持工作日志的习惯 3.定期复盘和调整工作方向 4.注重团队协作和沟通 5.持续学习新技能";
            case "DIVINATION":
                // Round24 T3.2: 卦名差异化兜底（8 卦属性 × 6 爻位 = 48 组，积极基调）
                return generateDivinationFallback(context);
            case "LOG_AUTO_ANALYSIS":
                return "基于本地算法的日志分析：根据您的工作日志内容，主要工作集中在项目推进和任务执行方面。建议保持良好的工作节奏，定期总结复盘，关注重点工作事项的优先级管理。结合您的性格特点，建议发挥自身优势，合理分配精力，在团队协作中主动沟通协调。";
            default:
                return "本地兜底AI分析结果：已处理您的请求（" + analysisType + "）。由于外部AI服务暂不可用，此结果由本地算法生成。";
        }
    }

    /**
     * Round24 T3.2: DIVINATION 分卦差异化兜底文案。
     * 结构 = "[卦名]卦象揭示…" + 上卦八卦属性正向阐释 + 爻位提示（动爻已提供时） + 固定收尾红线。
     */
    private String generateDivinationFallback(Map<String, Object> context) {
        if (context == null) {
            return DIVINATION_DEFAULT_FALLBACK;
        }
        Object originalNameObj = context.get("originalName");
        if (originalNameObj == null || originalNameObj.toString().trim().isEmpty()) {
            return DIVINATION_DEFAULT_FALLBACK;
        }
        String originalName = originalNameObj.toString().trim();

        Integer upperTrigram = resolveUpperTrigramIndex(originalName);
        if (upperTrigram == null) {
            // 卦名不在 64 卦名表内：回归固定兜底，不抛错（放行语义）
            return DIVINATION_DEFAULT_FALLBACK;
        }

        Integer movingLine = null;
        Object movingLineObj = context.get("movingLine");
        if (movingLineObj instanceof Number) {
            int line = ((Number) movingLineObj).intValue();
            if (line >= 1 && line <= 6) {
                movingLine = line;
            }
        }

        StringBuilder text = new StringBuilder();
        text.append(originalName).append("卦象揭示").append(TRIGRAM_TEMPLATES[upperTrigram - 1]);
        if (movingLine != null) {
            text.append(LINE_HINTS[movingLine - 1]);
        }
        text.append(DIVINATION_CLOSING);
        return text.toString();
    }

    /**
     * Round24 T3.1: 本卦名 → 上卦八卦索引（乾1兑2离3震4巽5坎6艮7坤8）。
     * 编号规则与 App 端 DivinationEngine 同源：本卦编号 = (下卦数-1)×8 + 上卦数，
     * 故 64 卦名表 0-based 索引 i 的上卦数 = i % 8 + 1（如"姤"i=32 → 上卦乾1，"天风姤"=巽下乾上）。
     */
    private Integer resolveUpperTrigramIndex(String hexagramName) {
        if (hexagramName == null) {
            return null;
        }
        for (int i = 0; i < HEXAGRAM_NAMES.length; i++) {
            if (HEXAGRAM_NAMES[i].equals(hexagramName)) {
                return i % 8 + 1;
            }
        }
        return null;
    }

    private String extractKeywordsLocal(String text) {
        String[] commonWords = {"工作", "任务", "项目", "会议", "报告", "计划", "总结", "分析", "开发", "设计"};
        StringBuilder result = new StringBuilder("关键词提取结果（本地算法）：");
        int count = 0;
        for (String word : commonWords) {
            if (text != null && text.contains(word)) {
                if (count > 0) result.append("、");
                result.append(word);
                count++;
            }
            if (count >= 5) break;
        }
        if (count == 0) {
            result.append("未提取到明显关键词，建议补充更多工作描述。");
        }
        return result.toString();
    }
}