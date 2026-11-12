package com.example.project_pandora.ui.aimap;

import com.nlf.calendar.EightChar;
import com.nlf.calendar.Lunar;
import com.nlf.calendar.Solar;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/** Calendar facts and locally curated daily prompts are deliberately kept separate. */
public final class BirthFortuneEngine {
    private BirthFortuneEngine() {}
    public static final String ELEMENTS = "木火土金水";
    private static final String[] THEMES = {"稳步蓄力", "灵感生长", "协作有光", "专注收获", "从容整理", "主动沟通"};
    private static final String[] OUTLOOK = {
            "把节奏放稳，让一件小事先有结果。今天的灵感是：完成比堆积更有力量。",
            "给新想法留一点空间。把脑中的点子写成一页草稿，再挑一个做小范围尝试。",
            "把独自思考带进交流里。一句清楚的反馈，可能让卡住的工作继续前进。",
            "为重要的事留出一段安静时间。先做最有价值的一项，再处理零散消息。",
            "适合梳理信息与收尾。清理一份文档、核对一个节点，让下一步更轻盈。",
            "主动说明期待与边界。与伙伴确认目标后，再一起决定今天先推进什么。"};
    private static final String[] DO = {
            "把大任务拆成两步，先交付可确认的小成果。", "用草图或清单表达想法，邀请一位同伴给反馈。",
            "先对齐负责人和截止时间，再开始协作。", "给重点任务预留一个不被打断的工作时段。",
            "核对资料版本、数字与待办，把遗漏补齐。", "用一句话说清目标，再补充需要的资源。"};
    private static final String[] WATCH = {
            "不要同时开启过多事项；保留一点缓冲时间。", "新想法先验证，避免在条件未明时承诺工期。",
            "有分歧时先复述对方的需求，避免只凭猜测行动。", "专注时也留意必要的同步，别让同伴等待消息。",
            "反复检查也要设结束点，避免细节挤占主要任务。", "重要结论留一份文字记录，避免口头信息遗漏。"};
    private static final String[] ZODIACS = {"鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"};
    private static final String[] ZODIAC_TIPS = {
            "借一点机敏：多准备一个备选方案。", "借一点踏实：把复杂任务一步步落实。",
            "借一点勇气：主动推进那个迟迟没开始的小步骤。", "借一点细腻：照顾沟通里的语气和细节。",
            "借一点远见：先看整体目标，再安排今天。", "借一点沉着：观察清楚，再给出判断。",
            "借一点行动力：先做一个能验证方向的尝试。", "借一点温和：听完不同意见，再协调分工。",
            "借一点巧思：试试更省力的工具或表达。", "借一点条理：用清晰的检查表帮助收尾。",
            "借一点可靠：把答应的事情及时反馈进度。", "借一点松弛：工作间隙留出恢复精力的时间。"};
    private static final String[] SIGNS = {"白羊", "金牛", "双子", "巨蟹", "狮子", "处女", "天秤", "天蝎", "射手", "摩羯", "水瓶", "双鱼"};
    private static final String[] SIGN_TIPS = {
            "先行动，再复盘。开始前多确认一次需求。", "坚持自己的节奏，也给变化留一个出口。",
            "把好奇变成问题清单，一次解决一个。", "体察团队感受，也清楚表达自己的边界。",
            "让热情带动协作，记得把成果与伙伴共享。", "用细致提高质量，也为交付设定明确终点。",
            "照顾各方意见后，为选择定一个截止点。", "深入问题核心，用事实代替推测。",
            "把远处的目标拆成今天能走的一小步。", "稳稳推进计划，也给休息留出位置。",
            "欢迎新角度，用小实验检验大胆想法。", "把感受转成具体表达，让创意落到纸面。"};

    /** All civil input is interpreted in Beijing time; sect 2 changes the day at midnight. */
    public static Result calculate(int year, int month, int day, int hour, int minute, Calendar now) {
        if (now == null) throw new IllegalArgumentException("无法获取当前时间，请稍后重试");
        if (year < 1900 || year > 2100) throw new IllegalArgumentException("出生年份需在1900年至今之间");
        Calendar birth = new GregorianCalendar(TimeZone.getTimeZone("Asia/Shanghai"), Locale.CHINA);
        birth.clear(); birth.setLenient(false); birth.set(year, month - 1, day, hour, minute, 0);
        try { birth.getTimeInMillis(); } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("出生日期或时间无效，请重新选择");
        }
        if (birth.getTimeInMillis() > now.getTimeInMillis()) throw new IllegalArgumentException("出生时间不能晚于现在");
        Calendar today = new GregorianCalendar(TimeZone.getTimeZone("Asia/Shanghai"), Locale.CHINA);
        today.setTimeInMillis(now.getTimeInMillis());
        Solar solar = Solar.fromYmdHms(year, month, day, hour, minute, 0);
        Lunar lunar = solar.getLunar();
        EightChar chart = lunar.getEightChar(); chart.setSect(2);
        String[] pillars = {chart.getYear(), chart.getMonth(), chart.getDay(), chart.getTime()};
        String[] elements = {chart.getYearWuXing(), chart.getMonthWuXing(), chart.getDayWuXing(), chart.getTimeWuXing()};
        int[] counts = new int[5];
        for (String pair : elements) for (int i = 0; i < pair.length(); i++) {
            int index = ELEMENTS.indexOf(pair.charAt(i)); if (index >= 0) counts[index]++;
        }
        String zodiac = lunar.getYearShengXiao(), sign = solar.getXingZuo();
        String date = String.format(Locale.CHINA, "%04d.%02d.%02d", today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH));
        String chartKey = pillars[0] + "/" + pillars[1] + "/" + pillars[2] + "/" + pillars[3];
        int theme = Math.floorMod((chartKey + zodiac + sign + ":" + date).hashCode(), THEMES.length);
        int zi = indexOf(ZODIACS, zodiac), si = indexOf(SIGNS, sign.replace("座", ""));
        return new Result(pillars, elements, counts, lunar.toString(), zodiac, sign + "座", date,
                THEMES[theme], OUTLOOK[theme], DO[theme], WATCH[theme], ZODIAC_TIPS[zi], SIGN_TIPS[si]);
    }

    private static int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(value)) return i;
        throw new IllegalArgumentException("暂无法识别该日期的生肖或星座");
    }

    public static final class Result {
        public final String[] pillars, elements;
        public final int[] elementCounts;
        public final String lunarDate, zodiac, constellation, date, theme, outlook, action, caution, zodiacTip, constellationTip;
        Result(String[] pillars, String[] elements, int[] counts, String lunarDate, String zodiac, String constellation,
               String date, String theme, String outlook, String action, String caution, String zodiacTip, String constellationTip) {
            this.pillars = pillars; this.elements = elements; elementCounts = counts; this.lunarDate = lunarDate;
            this.zodiac = zodiac; this.constellation = constellation; this.date = date; this.theme = theme;
            this.outlook = outlook; this.action = action; this.caution = caution;
            this.zodiacTip = zodiacTip; this.constellationTip = constellationTip;
        }
    }
}
