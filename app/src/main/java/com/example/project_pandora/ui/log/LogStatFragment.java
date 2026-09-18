package com.example.project_pandora.ui.log;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.project_pandora.databinding.FragmentLogStatBinding;
import com.example.project_pandora.repository.LogRepository;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

public class LogStatFragment extends Fragment {

    private FragmentLogStatBinding binding;
    private LogRepository logRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLogStatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        logRepository = new LogRepository(requireContext());
        loadStats();
    }

    private void loadStats() {
        binding.progressBar.setVisibility(View.VISIBLE);
        logRepository.getLogStats()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        stats -> {
                            binding.progressBar.setVisibility(View.GONE);
                            displayCharts(stats);
                        },
                        error -> {
                            binding.progressBar.setVisibility(View.GONE);
                        }
                );
    }

    @SuppressWarnings("unchecked")
    private void displayCharts(Map<String, Object> stats) {
        List<Map<String, Object>> completionRate = (List<Map<String, Object>>) stats.get("completionRate");
        if (completionRate != null && !completionRate.isEmpty()) {
            setupCompletionRateChart(completionRate);
        }

        List<Map<String, Object>> importantRatio = (List<Map<String, Object>>) stats.get("importantRatio");
        if (importantRatio != null && !importantRatio.isEmpty()) {
            setupImportantRatioChart(importantRatio);
        }

        List<Map<String, Object>> analysisChart = (List<Map<String, Object>>) stats.get("analysisChart");
        if (analysisChart != null && !analysisChart.isEmpty()) {
            setupAnalysisChart(analysisChart);
        }
    }

    private void setupCompletionRateChart(List<Map<String, Object>> data) {
        BarChart chart = new BarChart(requireContext());
        List<BarEntry> entries = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> item = data.get(i);
            Object completedObj = item.get("completed");
            float completed = completedObj != null ? ((Number) completedObj).floatValue() : 0f;
            entries.add(new BarEntry(i, completed));
        }

        BarDataSet dataSet = new BarDataSet(entries, "完成数");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        BarData barData = new BarData(dataSet);
        chart.setData(barData);
        chart.invalidate();

        replaceChart(binding.chartCompletionRate.getId(), chart);
    }

    private void setupImportantRatioChart(List<Map<String, Object>> data) {
        PieChart chart = new PieChart(requireContext());
        List<PieEntry> entries = new ArrayList<>();

        for (Map<String, Object> item : data) {
            String label = (String) item.get("label");
            Object valueObj = item.get("value");
            float value = valueObj != null ? ((Number) valueObj).floatValue() : 0f;
            entries.add(new PieEntry(value, label != null ? label : ""));
        }

        PieDataSet dataSet = new PieDataSet(entries, "重要占比");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        PieData pieData = new PieData(dataSet);
        chart.setData(pieData);
        chart.invalidate();

        replaceChart(binding.chartImportantRatio.getId(), chart);
    }

    private void setupAnalysisChart(List<Map<String, Object>> data) {
        BarChart chart = new BarChart(requireContext());
        List<BarEntry> entries = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> item = data.get(i);
            Object valueObj = item.get("value");
            float value = valueObj != null ? ((Number) valueObj).floatValue() : 0f;
            entries.add(new BarEntry(i, value));
        }

        BarDataSet dataSet = new BarDataSet(entries, "分析数据");
        dataSet.setColors(ColorTemplate.JOYFUL_COLORS);
        BarData barData = new BarData(dataSet);
        chart.setData(barData);
        chart.invalidate();

        replaceChart(binding.chartAnalysis.getId(), chart);
    }

    private void replaceChart(int viewId, android.view.View chart) {
        android.view.ViewGroup parent = (android.view.ViewGroup) binding.getRoot();
        android.view.View old = parent.findViewById(viewId);
        if (old != null) {
            android.view.ViewGroup oldParent = (android.view.ViewGroup) old.getParent();
            int index = oldParent.indexOfChild(old);
            oldParent.removeView(old);
            chart.setId(viewId);
            oldParent.addView(chart, index);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}