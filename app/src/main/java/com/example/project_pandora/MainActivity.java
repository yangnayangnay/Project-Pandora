package com.example.project_pandora;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.project_pandora.core.crash.CrashHandler;
import com.example.project_pandora.core.logger.PandoraLogger;
import com.example.project_pandora.core.theme.ThemeManager;
import com.example.project_pandora.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ThemeManager themeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        PandoraLogger.init(this);
        CrashHandler.register(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContainer, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        themeManager = new ThemeManager(this);
        themeManager.applyTheme();

        setupBottomNavigation();
        adjustBottomNavHeight();
    }

    private void setupBottomNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        }
    }

    private void adjustBottomNavHeight() {
        binding.bottomNavigation.post(() -> {
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int maxHeight = (int) (screenHeight * 0.12);

            int widthSpec = View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            binding.bottomNavigation.measure(widthSpec, heightSpec);
            int desiredHeight = binding.bottomNavigation.getMeasuredHeight();

            if (desiredHeight > maxHeight && maxHeight > 0) {
                ViewGroup.LayoutParams params = binding.bottomNavigation.getLayoutParams();
                params.height = maxHeight;
                binding.bottomNavigation.setLayoutParams(params);
            }
        });
    }
}
