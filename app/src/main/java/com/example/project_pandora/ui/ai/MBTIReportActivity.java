package com.example.project_pandora.ui.ai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.databinding.ActivityMbtiReportBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

public class MBTIReportActivity extends AppCompatActivity {

    private static final String PREF_NAME = "mbti_pref";
    private static final String KEY_HISTORY = "mbti_history";

    private ActivityMbtiReportBinding binding;
    private HistoryAdapter adapter;
    private final List<JSONObject> historyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMbtiReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new HistoryAdapter();
        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerHistory.setAdapter(adapter);

        binding.btnRetake.setOnClickListener(v -> {
            startActivity(new Intent(this, MBTITestActivity.class));
            finish();
        });

        binding.btnStartTest.setOnClickListener(v -> {
            startActivity(new Intent(this, MBTITestActivity.class));
            finish();
        });

        binding.btnDeleteAll.setOnClickListener(v -> deleteAllHistory());

        loadHistory();
    }

    private void loadHistory() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_HISTORY, "[]");

        historyList.clear();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                historyList.add(array.getJSONObject(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Collections.reverse(historyList);

        if (historyList.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.layoutResult.setVisibility(View.GONE);
            binding.layoutHistory.setVisibility(View.GONE);
            return;
        }

        binding.layoutEmpty.setVisibility(View.GONE);
        binding.layoutResult.setVisibility(View.VISIBLE);

        showLatestResult(historyList.get(0));

        if (historyList.size() > 1) {
            binding.layoutHistory.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        } else {
            binding.layoutHistory.setVisibility(View.GONE);
        }
    }

    private void showLatestResult(JSONObject result) {
        try {
            String type = result.getString("type");
            String nickname = result.getString("nickname");
            String date = result.getString("date");
            int version = result.getInt("version");

            binding.textMbtiType.setText(type);
            binding.textMbtiNickname.setText(nickname);
            binding.textGeneratedAt.setText("测试时间: " + date + " | " + version + "题版");

            int e = result.getInt("E"), i = result.getInt("I");
            int s = result.getInt("S"), n = result.getInt("N");
            int t = result.getInt("T"), f = result.getInt("F");
            int j = result.getInt("J"), p = result.getInt("P");

            String desc = MBTITestActivity.getTypeDescription(type);
            binding.textReportContent.setText(desc);

            binding.textScores.setText(String.format(
                    "维度得分：\n  E=%d  I=%d\n  S=%d  N=%d\n  T=%d  F=%d\n  J=%d  P=%d",
                    e, i, s, n, t, f, j, p));
        } catch (Exception e) {
            binding.textMbtiType.setText("解析错误");
        }
    }

    private void deleteHistoryItem(int index) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            JSONArray array = new JSONArray(prefs.getString(KEY_HISTORY, "[]"));
            int realIndex = array.length() - 1 - index;
            if (realIndex >= 0 && realIndex < array.length()) {
                JSONArray newArray = new JSONArray();
                for (int i = 0; i < array.length(); i++) {
                    if (i != realIndex) newArray.put(array.get(i));
                }
                prefs.edit().putString(KEY_HISTORY, newArray.toString()).apply();
                loadHistory();
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteAllHistory() {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .edit().remove(KEY_HISTORY).apply();
        loadHistory();
        Toast.makeText(this, "已清空所有记录", Toast.LENGTH_SHORT).show();
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

        @NonNull
        @Override
        public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(32, 24, 32, 24);
            tv.setTextSize(14);
            return new HistoryViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
            try {
                JSONObject item = historyList.get(position);
                String text = String.format("%s — %s（%s，%d题）\n%s",
                        item.getString("type"),
                        item.getString("nickname"),
                        item.getString("date"),
                        item.getInt("version"),
                        MBTITestActivity.getTypeDescription(item.getString("type")));
                ((TextView) holder.itemView).setText(text);

                holder.itemView.setOnLongClickListener(v -> {
                    deleteHistoryItem(position);
                    return true;
                });
            } catch (Exception e) {
                ((TextView) holder.itemView).setText("解析错误");
            }
        }

        @Override
        public int getItemCount() {
            return historyList.size() > 1 ? historyList.size() - 1 : 0;
        }

        class HistoryViewHolder extends RecyclerView.ViewHolder {
            HistoryViewHolder(View itemView) { super(itemView); }
        }
    }
}
