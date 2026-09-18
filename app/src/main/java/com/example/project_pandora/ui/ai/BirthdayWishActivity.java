package com.example.project_pandora.ui.ai;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project_pandora.R;
import com.example.project_pandora.databinding.ActivityBirthdayWishBinding;

import java.util.Calendar;
import java.util.Locale;

public class BirthdayWishActivity extends AppCompatActivity {

    private static final String PREF_NAME = "birthday_pref";
    private static final String KEY_MONTH = "birth_month";
    private static final String KEY_DAY = "birth_day";
    private static final int NOT_SET = -1;

    private ActivityBirthdayWishBinding binding;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBirthdayWishBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        setupSpinners();
        loadSavedBirthday();

        binding.btnSaveBirthday.setOnClickListener(v -> saveBirthday());
    }

    private void setupSpinners() {
        String[] months = new String[12];
        for (int i = 0; i < 12; i++) {
            months[i] = String.valueOf(i + 1);
        }
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, months);
        binding.spinnerMonth.setAdapter(monthAdapter);

        String[] days = new String[31];
        for (int i = 0; i < 31; i++) {
            days[i] = String.valueOf(i + 1);
        }
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, days);
        binding.spinnerDay.setAdapter(dayAdapter);
    }

    private void loadSavedBirthday() {
        int month = prefs.getInt(KEY_MONTH, NOT_SET);
        int day = prefs.getInt(KEY_DAY, NOT_SET);

        if (month != NOT_SET && day != NOT_SET) {
            binding.spinnerMonth.setSelection(month - 1);
            binding.spinnerDay.setSelection(day - 1);
            showWish(month, day);
        } else {
            binding.textBirthdayContent.setText(R.string.birthday_not_set);
        }
    }

    private void saveBirthday() {
        int month = binding.spinnerMonth.getSelectedItemPosition() + 1;
        int day = binding.spinnerDay.getSelectedItemPosition() + 1;

        prefs.edit()
                .putInt(KEY_MONTH, month)
                .putInt(KEY_DAY, day)
                .apply();

        Toast.makeText(this, R.string.birthday_saved, Toast.LENGTH_SHORT).show();
        showWish(month, day);
    }

    private void showWish(int birthMonth, int birthDay) {
        Calendar today = Calendar.getInstance();
        int todayMonth = today.get(Calendar.MONTH) + 1;
        int todayDay = today.get(Calendar.DAY_OF_MONTH);

        if (todayMonth == birthMonth && todayDay == birthDay) {
            binding.textBirthdayContent.setText(R.string.birthday_today);
            return;
        }

        Calendar next = Calendar.getInstance();
        next.set(Calendar.MONTH, birthMonth - 1);
        next.set(Calendar.DAY_OF_MONTH, birthDay);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);

        if (next.before(todayCal)) {
            next.add(Calendar.YEAR, 1);
        }

        long diffDays = (next.getTimeInMillis() - todayCal.getTimeInMillis()) / (1000 * 60 * 60 * 24);

        if (diffDays > 0 && diffDays <= 365) {
            String text = getString(R.string.birthday_days_left, (int) diffDays);
            binding.textBirthdayContent.setText(text);
        } else {
            String text = getString(R.string.birthday_passed, (int) diffDays);
            binding.textBirthdayContent.setText(text);
        }
    }
}