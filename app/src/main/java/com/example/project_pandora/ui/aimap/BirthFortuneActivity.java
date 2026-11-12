package com.example.project_pandora.ui.aimap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import com.example.project_pandora.R;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.databinding.ActivityBirthFortuneBinding;
import com.example.project_pandora.ui.PandoraActivity;
import com.example.project_pandora.ui.widget.CompanyBackgrounds;
import com.example.project_pandora.ui.widget.QuickDatePickerDialog;
import com.example.project_pandora.ui.widget.WheelTimePickerDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BirthFortuneActivity extends PandoraActivity {
    private ActivityBirthFortuneBinding binding;
    private final ExecutorService calculator = Executors.newSingleThreadExecutor();
    private int year, month, day, hour = -1, minute;
    private int requestVersion, stage = -1;
    private boolean busy, stopped, restoredReading;
    private BirthFortuneEngine.Result result;
    private ValueAnimator ritual;
    private AlertDialog dateDialog;
    private WheelTimePickerDialog timeDialog;
    private final boolean[] opened = new boolean[3];
    private static final String[] STAGES = {"排定四柱", "遇见生肖", "点亮星座", "收拢今日灵感"};
    private static final String[] CENTERS = {"四柱", "生肖", "星座", "今日"};

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        binding = ActivityBirthFortuneBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot());
        CompanyBackgrounds.bind(binding.getRoot(), CompanyBackgrounds.Scene.AI);
        BackToolbarHelper.setupBack(binding.toolbar, this::finish);
        if (state != null) {
            year = state.getInt("birth_year"); month = state.getInt("birth_month"); day = state.getInt("birth_day");
            hour = state.getInt("birth_hour", -1); minute = state.getInt("birth_minute");
            restoredReading = state.getBoolean("reading");
            boolean[] saved = state.getBooleanArray("opened");
            if (saved != null) System.arraycopy(saved, 0, opened, 0, Math.min(saved.length, opened.length));
        }
        updateInputs();
        binding.birthDate.setOnClickListener(v -> selectDate());
        binding.birthTime.setOnClickListener(v -> selectTime());
        binding.beginReading.setOnClickListener(v -> calculate(false));
        binding.skipRitual.setOnClickListener(v -> { cancelRitual(); showResult(); });
        if (restoredReading && year > 0 && hour >= 0) calculate(true);
    }

    private Calendar beijingNow() { return Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")); }
    private void selectDate() {
        if (dateDialog != null && dateDialog.isShowing()) return;
        Calendar selected = beijingNow();
        if (year > 0) selected.set(year, month - 1, day);
        else selected.set(2000, Calendar.JANUARY, 1);
        dateDialog = QuickDatePickerDialog.create(this, selected, 1900, chosen -> {
            year = chosen.get(Calendar.YEAR); month = chosen.get(Calendar.MONTH) + 1; day = chosen.get(Calendar.DAY_OF_MONTH);
            invalidateReading(); updateInputs();
        });
        dateDialog.show();
    }

    private void selectTime() {
        if (timeDialog != null && timeDialog.isShowing()) return;
        timeDialog = new WheelTimePickerDialog(this, getString(R.string.fortune_birth_time), hour < 0 ? 12 : hour, minute, (h, m) -> {
            hour = h; minute = m; invalidateReading(); updateInputs();
        });
        timeDialog.show();
    }

    private void updateInputs() {
        binding.birthDate.setText(year == 0 ? getString(R.string.fortune_pick_date) : String.format(Locale.CHINA, "%04d年%02d月%02d日", year, month, day));
        binding.birthTime.setText(hour < 0 ? getString(R.string.fortune_pick_time) : String.format(Locale.CHINA, "%02d:%02d", hour, minute));
    }

    private void invalidateReading() {
        result = null; restoredReading = false; requestVersion++;
        for (int i = 0; i < opened.length; i++) opened[i] = false;
        binding.results.setVisibility(View.GONE); binding.inputError.setVisibility(View.GONE);
        binding.beginReading.setText(R.string.fortune_begin);
    }

    private void setBusy(boolean value) {
        busy = value; binding.birthDate.setEnabled(!value); binding.birthTime.setEnabled(!value);
        binding.beginReading.setEnabled(!value);
        binding.beginReading.setText(value ? "正在排盘…" : getString(R.string.fortune_begin));
    }

    private void calculate(boolean restore) {
        if (busy) return;
        if (year == 0 || hour < 0) { error("请先选择出生日期和出生时间"); return; }
        binding.inputError.setVisibility(View.GONE); setBusy(true);
        if (!restore) for (int i = 0; i < opened.length; i++) opened[i] = false;
        binding.results.setVisibility(View.GONE);
        int version = ++requestVersion;
        int y = year, mo = month, d = day, h = hour, mi = minute;
        Calendar now = beijingNow();
        calculator.execute(() -> {
            try {
                BirthFortuneEngine.Result value = BirthFortuneEngine.calculate(y, mo, d, h, mi, now);
                runOnUiThread(() -> {
                    if (version != requestVersion || isFinishing() || isDestroyed()) return;
                    result = value;
                    if (restore || stopped || !motionEnabled()) showResult(); else playRitual();
                });
            } catch (RuntimeException invalid) {
                runOnUiThread(() -> {
                    if (version != requestVersion || isFinishing() || isDestroyed()) return;
                    setBusy(false); restoredReading = false;
                    error(invalid instanceof IllegalArgumentException ? invalid.getMessage() : "排盘暂未成功，请检查日期后重试");
                });
            }
        });
    }

    private void error(String message) {
        binding.inputError.setText(message); binding.inputError.setVisibility(View.VISIBLE);
        binding.inputError.requestFocus();
    }

    private boolean motionEnabled() {
        if (Build.VERSION.SDK_INT >= 26) return ValueAnimator.areAnimatorsEnabled();
        return android.provider.Settings.Global.getFloat(getContentResolver(),
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f;
    }
    private void playRitual() {
        binding.ritualCard.setVisibility(View.VISIBLE); stage = -1;
        binding.contentScroll.post(() -> { if (!stopped) binding.contentScroll.smoothScrollTo(0, binding.ritualCard.getTop()); });
        ritual = ValueAnimator.ofFloat(0, 1); ritual.setDuration(4400);
        ritual.setInterpolator(new android.view.animation.LinearInterpolator());
        ritual.addUpdateListener(value -> {
            float p = (float) value.getAnimatedValue(); int next = Math.min(3, (int) (p * 4));
            binding.fortuneOrbit.setProgress(p, CENTERS[next]);
            if (stage != next) {
                stage = next; binding.ritualTitle.setText((next + 1) + " / 4 · " + STAGES[next]);
                String[] captions = {"让出生时刻落在四季的坐标里", "农历" + result.zodiac + "年 · 向传统意象借一点灵感",
                        result.constellation + " · 看见另一种行动视角", "停一停，再翻开今天的工作提醒"};
                binding.ritualCaption.setText(captions[next]);
                binding.ritualCaption.setAlpha(0); binding.ritualCaption.animate().alpha(1).setDuration(240).start();
            }
        });
        ritual.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;
            @Override public void onAnimationCancel(Animator animation) { cancelled = true; }
            @Override public void onAnimationEnd(Animator animation) { ritual = null; if (!cancelled) showResult(); }
        }); ritual.start();
    }

    private void cancelRitual() {
        if (ritual != null) ritual.cancel();
        binding.ritualCaption.animate().cancel();
    }

    private void showResult() {
        if (result == null) return;
        setBusy(false); restoredReading = true;
        binding.ritualCard.setVisibility(View.GONE); binding.results.removeAllViews(); binding.results.setVisibility(View.VISIBLE);
        binding.results.addView(text("二 · 你的生辰图", 20, R.color.color_text_primary, true));
        TextView date = text(result.lunarDate + " · 北京时间", 13, R.color.color_company_photo_secondary, false);
        date.setPadding(0, dp(8), 0, dp(4)); binding.results.addView(date);
        buildChart();
        addReveal(0, "生肖 · " + result.zodiac, "生肖锦囊", result.zodiacTip,
                R.color.fortune_zodiac_surface, R.color.fortune_zodiac_ink, "圆融");
        addReveal(1, "星座 · " + result.constellation, "星座行动", result.constellationTip,
                R.color.fortune_star_surface, R.color.fortune_star_ink, "星轨");
        LinearLayout today = section("今日运势 · " + result.theme, R.color.fortune_chart_surface, R.color.fortune_chart_ink, "日光");
        today.addView(text(result.date + " · 每日灵感", 13, R.color.fortune_chart_ink, false));
        TextView outlook = text(result.outlook, 15, R.color.fortune_chart_ink, false);
        outlook.setPadding(0, dp(10), 0, dp(4)); today.addView(outlook);
        addReveal(2, "工作提醒 · 今日一签", "工作注意事项", "宜做\n" + result.action + "\n\n留意\n" + result.caution,
                R.color.fortune_work_surface, R.color.fortune_work_ink, "山形");
        MaterialButton again = new MaterialButton(this); again.setText(R.string.fortune_edit); again.setTextSize(16);
        again.setCornerRadius(dp(18)); again.setMinHeight(dp(52));
        again.setTextColor(color(R.color.color_on_accent)); again.setBackgroundTintList(ColorStateList.valueOf(color(R.color.color_accent)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(16);
        binding.results.addView(again, params);
        again.setOnClickListener(v -> {
            binding.results.setVisibility(View.GONE); restoredReading = false;
            binding.contentScroll.smoothScrollTo(0, 0); binding.birthDate.requestFocus();
        });
    }

    private void buildChart() {
        LinearLayout chart = section("四柱八字", R.color.fortune_chart_surface, R.color.fortune_chart_ink, "四时");
        TextView hint = text("点选四柱，了解每一柱的位置含义", 13, R.color.fortune_chart_ink, false); chart.addView(hint);
        String[] names = {"年柱", "月柱", "日柱", "时柱"};
        String[] meanings = {"年柱：以立春为年界，展示出生时节气所属的干支年。", "月柱：按十二节分月，与农历初一换月不同。",
                "日柱：按六十甲子逐日轮转，本页采用零点换日。", "时柱：将一天分为十二时辰，按北京时间计算。"};
        TextView selected = text(meanings[0], 13, R.color.fortune_chart_ink, false);
        // Two columns remain readable at large font sizes; no fixed text height.
        for (int row = 0; row < 2; row++) {
            LinearLayout line = new LinearLayout(this); line.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < 2; col++) {
                int index = row * 2 + col;
                MaterialButton pillar = new MaterialButton(this); pillar.setText(names[index] + "  " + result.pillars[index] + "\n" + result.elements[index]);
                pillar.setTextSize(17); pillar.setAllCaps(false); pillar.setCornerRadius(dp(16)); pillar.setMinHeight(dp(72));
                pillar.setTextColor(color(R.color.fortune_chart_ink)); pillar.setBackgroundTintList(ColorStateList.valueOf(color(R.color.color_surface)));
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1); p.setMargins(0, dp(8), col == 0 ? dp(8) : 0, 0);
                line.addView(pillar, p);
                pillar.setOnClickListener(v -> selected.setText(meanings[index]));
            }
            chart.addView(line, new LinearLayout.LayoutParams(-1, -2));
        }
        selected.setPadding(0, dp(12), 0, dp(8)); selected.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); chart.addView(selected);
        StringBuilder counts = new StringBuilder("表层五行  ");
        for (int i = 0; i < 5; i++) counts.append(BirthFortuneEngine.ELEMENTS.charAt(i)).append(' ').append(result.elementCounts[i]).append("  ");
        chart.addView(text(counts.toString().trim(), 14, R.color.fortune_chart_ink, true));
        chart.addView(text("仅统计四柱干支的八个字，不含藏干、旺衰或喜用神判断。", 12, R.color.fortune_chart_ink, false));
    }

    private void addReveal(int index, String title, String subject, String content, int surface, int ink, String motif) {
        LinearLayout section = section(title, surface, ink, motif);
        TextView body = text(content, 15, ink, false); body.setVisibility(opened[index] ? View.VISIBLE : View.GONE);
        body.setPadding(0, dp(8), 0, dp(8)); section.addView(body);
        MaterialButton flip = new MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle);
        flip.setText(opened[index] ? "收起" + subject : "翻开" + subject); flip.setTextSize(14); flip.setMinHeight(dp(48));
        flip.setCornerRadius(dp(16)); flip.setTextColor(color(ink)); section.addView(flip);
        flip.setOnClickListener(v -> {
            opened[index] = !opened[index]; body.animate().cancel(); body.setVisibility(opened[index] ? View.VISIBLE : View.GONE);
            flip.setText(opened[index] ? "收起" + subject : "翻开" + subject);
            if (opened[index] && motionEnabled()) { body.setAlpha(0); body.animate().alpha(1).setDuration(260).start(); }
            else body.setAlpha(1);
        });
    }

    private LinearLayout section(String title, int surface, int ink, String motif) {
        MaterialCardView card = new MaterialCardView(this); card.setRadius(dp(22)); card.setCardElevation(dp(2));
        card.setStrokeWidth(0); card.setCardBackgroundColor(color(surface));
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(18), dp(18), dp(18), dp(18));
        LinearLayout heading = new LinearLayout(this); heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView titleView = text(title, 17, ink, true); heading.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1));
        // Code-drawn line motifs share the entry illustration's visual language.
        FortuneMotifView symbol = new FortuneMotifView(this, ink, motif); symbol.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        heading.addView(symbol, new LinearLayout.LayoutParams(dp(36), dp(36)));
        content.addView(heading); card.addView(content);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(14);
        binding.results.addView(card, p); return content;
    }

    private TextView text(String value, int size, int ink, boolean bold) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color(ink));
        view.setLineSpacing(dp(4), 1);
        if (bold) { view.setTypeface(view.getTypeface(), Typeface.BOLD); androidx.core.view.ViewCompat.setAccessibilityHeading(view, true); }
        return view;
    }
    private int color(int id) { return ContextCompat.getColor(this, id); }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override protected void onStart() { super.onStart(); stopped = false; }
    @Override protected void onStop() {
        stopped = true;
        if (ritual != null) { cancelRitual(); showResult(); }
        if (dateDialog != null) dateDialog.dismiss();
        if (timeDialog != null) timeDialog.dismiss();
        super.onStop();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("birth_year", year); state.putInt("birth_month", month); state.putInt("birth_day", day);
        state.putInt("birth_hour", hour); state.putInt("birth_minute", minute);
        state.putBoolean("reading", restoredReading || busy); state.putBooleanArray("opened", opened);
        super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() {
        requestVersion++; cancelRitual(); calculator.shutdownNow(); super.onDestroy();
    }

    private static final class FortuneMotifView extends View {
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final String motif;
        FortuneMotifView(android.content.Context context, int ink, String motif) {
            super(context); this.motif = motif; paint.setColor(ContextCompat.getColor(context, ink));
            paint.setStyle(android.graphics.Paint.Style.STROKE); paint.setStrokeWidth(1.5f);
        }
        @Override protected void onDraw(android.graphics.Canvas c) {
            super.onDraw(c); int save = c.save(); c.scale(getWidth() / 36f, getHeight() / 36f);
            if ("山形".equals(motif)) {
                android.graphics.Path p = new android.graphics.Path(); p.moveTo(3, 29); p.lineTo(14, 9); p.lineTo(23, 29);
                p.moveTo(20, 23); p.lineTo(26, 15); p.lineTo(33, 29); c.drawPath(p, paint);
            } else if ("星轨".equals(motif)) {
                c.drawCircle(18, 18, 12, paint); c.drawLine(8, 24, 18, 10, paint); c.drawLine(18, 10, 28, 23, paint);
                c.drawCircle(18, 10, 2, paint); c.drawCircle(8, 24, 2, paint); c.drawCircle(28, 23, 2, paint);
            } else {
                c.drawCircle(18, 18, 10, paint); c.drawCircle(18, 18, 14, paint);
                if (!"圆融".equals(motif)) for (int i = 0; i < 8; i++) {
                    c.drawLine(18, 1, 18, 5, paint); c.rotate(45, 18, 18);
                }
            }
            c.restoreToCount(save);
        }
    }
}
