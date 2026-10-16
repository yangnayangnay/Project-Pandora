package com.pandora.backend.top10;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Round19 REQ-19-07: 公司十大事项加权排序服务。
 *
 * 综合评分 = 紧急度权重(0.6) × 紧急度等级 + 截止时间权重(0.4) × 时间紧迫因子
 *
 * 紧急度等级映射: 高=3, 中=2, 低=1, null=1
 * 时间紧迫因子映射 (差值 = deadline - 当前日期):
 *   已过期 → 5
 *   ≤1 天 → 4
 *   ≤3 天 → 3
 *   ≤7 天 → 2
 *   >7 天 → 1
 *   null → 1
 *
 * 排序规则: 主排序综合评分降序，同分按 rank_order 升序兜底。
 */
@Service
public class Top10SortService {

    @Value("${pandora.top10.urgency-weight:0.6}")
    private double urgencyWeight;

    @Value("${pandora.top10.time-weight:0.4}")
    private double timeWeight;

    /**
     * 为每条事项计算综合评分并按降序排列，同分按 rank_order 升序兜底。
     *
     * @param items 原始事项列表（Map 中含 urgency_level、deadline、rank_order 字段）
     * @return 排序后列表（每条 Map 附加 composite_score 字段，Double 类型）
     */
    public List<Map<String, Object>> sortAndScore(List<Map<String, Object>> items) {
        if (items == null) return java.util.Collections.emptyList();

        Date now = new Date();
        for (Map<String, Object> item : items) {
            String urgencyLevel = getStr(item, "urgency_level");
            Date deadline = getDate(item, "deadline");
            int urgency = calcUrgencyLevel(urgencyLevel);
            int factor = calcTimeUrgencyFactor(deadline, now);
            double score = calcCompositeScore(urgency, factor);
            item.put("composite_score", score);
        }

        items.sort(Comparator
                .comparingDouble((Map<String, Object> item) -> getDouble(item, "composite_score")).reversed()
                .thenComparingInt(item -> getInt(item, "rank_order")));

        return items;
    }

    /**
     * 紧急度等级映射: 高=3, 中=2, 低=1, null/空=1
     * 兼容英文 HIGH/MEDIUM/LOW（数据库可能存储英文）
     */
    private int calcUrgencyLevel(String urgencyLevel) {
        if (urgencyLevel == null || urgencyLevel.isEmpty()) return 1;
        switch (urgencyLevel) {
            case "高": return 3;
            case "中": return 2;
            case "低": return 1;
            default:
                // 兼容英文 HIGH/MEDIUM/LOW
                String upper = urgencyLevel.toUpperCase();
                if ("HIGH".equals(upper)) return 3;
                if ("MEDIUM".equals(upper) || "MID".equals(upper)) return 2;
                if ("LOW".equals(upper)) return 1;
                return 1;
        }
    }

    /**
     * 时间紧迫因子映射:
     *   已过期 → 5
     *   ≤1 天 → 4
     *   ≤#3 天 → 3
     *   ≤7 天 → 2
     *   >7 天 → 1
     *   null → 1
     */
    private int calcTimeUrgencyFactor(Date deadline, Date now) {
        if (deadline == null) return 1;
        long diffMillis = deadline.getTime() - now.getTime();
        // 按天计算差值（向上取整到天边界，截止日期当天视为1天内）
        long diffDays = diffMillis / (1000L * 60 * 60 * 24);
        if (diffMillis < 0) {
            // 已过期
            return 5;
        } else if (diffDays <= 1) {
            return 4;
        } else if (diffDays <= 3) {
            return 3;
        } else if (diffDays <= 7) {
            return 2;
        } else {
            return 1;
        }
    }

    /**
     * 综合评分 = urgencyWeight × 紧急度等级 + timeWeight × 时间紧迫因子
     */
    private double calcCompositeScore(int urgency, int factor) {
        return urgencyWeight * urgency + timeWeight * factor;
    }

    private String getStr(Map<String, Object> item, String key) {
        Object val = item.get(key);
        if (val == null) {
            // H2 返回大写列名兜底
            val = item.get(key.toUpperCase());
        }
        return val != null ? val.toString() : null;
    }

    private Date getDate(Map<String, Object> item, String key) {
        Object val = item.get(key);
        if (val == null) {
            val = item.get(key.toUpperCase());
        }
        if (val == null) return null;
        if (val instanceof Date) return (Date) val;
        if (val instanceof java.sql.Date) return new Date(((java.sql.Date) val).getTime());
        if (val instanceof java.sql.Timestamp) return new Date(((java.sql.Timestamp) val).getTime());
        // 尝试解析字符串 yyyy-MM-dd
        try {
            String s = val.toString();
            if (s.length() >= 10) {
                return java.sql.Date.valueOf(s.substring(0, 10));
            }
        } catch (Exception ignored) {}
        return null;
    }

    private double getDouble(Map<String, Object> item, String key) {
        Object val = item.get(key);
        if (val == null) val = item.get(key.toUpperCase());
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val != null) {
            try { return Double.parseDouble(val.toString()); } catch (Exception ignored) {}
        }
        return 0.0;
    }

    private int getInt(Map<String, Object> item, String key) {
        Object val = item.get(key);
        if (val == null) val = item.get(key.toUpperCase());
        if (val instanceof Number) return ((Number) val).intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString()); } catch (Exception ignored) {}
        }
        return 0;
    }
}