package com.example.project_pandora.ui.view;

/** Hosts the company month/week/day task UI in read-only person mode. */
public final class CompanyPersonTasksActivity extends com.example.project_pandora.ui.PandoraActivity {
    private com.example.project_pandora.databinding.ActivityCompanyPersonTasksBinding binding;
    @Override protected void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        binding = com.example.project_pandora.databinding.ActivityCompanyPersonTasksBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        long userId = getIntent().getLongExtra("userId", -1L);
        if (userId <= 0) { finish(); return; }
        String name = getIntent().getStringExtra("personName");
        binding.toolbar.setTitle(name == null || name.isBlank() ? "人员任务" : name + "的任务");
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { finish(); }
        });
        if (state == null) getSupportFragmentManager().beginTransaction()
                .replace(binding.fragmentContainer.getId(), ViewFragment.forPerson(userId)).commit();
    }
}
