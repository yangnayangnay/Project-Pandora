package com.example.project_pandora.ui.view;

/** Permission-scoped profile with explicit links to the person's work views. */
public final class CompanyPersonActivity extends com.example.project_pandora.ui.PandoraActivity {
    private com.example.project_pandora.databinding.ActivityCompanyPersonBinding binding;
    private final io.reactivex.rxjava3.disposables.SerialDisposable request = new io.reactivex.rxjava3.disposables.SerialDisposable();
    private long userId;
    private String personName = "";

    @Override protected void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        binding = com.example.project_pandora.databinding.ActivityCompanyPersonBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        userId = getIntent().getLongExtra("userId", -1L);
        if (userId <= 0) { finish(); return; }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { finish(); }
        });
        binding.btnPersonTasks.setOnClickListener(v -> open(CompanyPersonTasksActivity.class));
        binding.btnPersonLogs.setOnClickListener(v -> open(CompanyPersonLogsActivity.class));
        load();
    }

    private void open(Class<?> target) {
        startActivity(new android.content.Intent(this, target).putExtra("userId", userId).putExtra("personName", personName));
    }

    private void load() {
        binding.progress.setVisibility(android.view.View.VISIBLE);
        binding.message.setText("正在加载人员资料…");
        binding.btnPersonTasks.setEnabled(false);
        binding.btnPersonLogs.setEnabled(false);
        com.example.project_pandora.data.remote.CompanyDirectoryApi api =
                com.example.project_pandora.core.network.NetworkClient.getInstance(this)
                        .createService(com.example.project_pandora.data.remote.CompanyDirectoryApi.class);
        request.set(api.detail(userId, null, null, null)
                .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread()).subscribe(response -> {
                    if (!response.isSuccess() || response.getData() == null)
                        throw new com.example.project_pandora.core.exception.BusinessException(response.getCode(), response.getMessage());
                    java.util.Map<String, Object> person = map(response.getData().get("person"));
                    personName = text(person, "real_name");
                    if (personName.isEmpty()) personName = text(person, "username");
                    binding.toolbar.setTitle(personName.isEmpty() ? "人员详情" : personName);
                    binding.textPerson.setText(shown(personName));
                    com.example.project_pandora.ui.mine.AvatarLoader.load(binding.imageAvatar, text(person, "avatar"), personName);
                    binding.textPhone.setText(shown(text(person, "phone")));
                    binding.textEmail.setText(shown(text(person, "email")));
                    binding.textDepartment.setText(shown(text(person, "department_name")));
                    binding.textTeam.setText(shown(text(person, "team_name")));
                    boolean founder = com.example.project_pandora.ui.people.PeopleDirectory.isFounder(person);
                    boolean hasTeam = com.example.project_pandora.ui.people.PeopleDirectory.hasTeam(person);
                    binding.labelDepartment.setVisibility(founder ? android.view.View.GONE : android.view.View.VISIBLE);
                    binding.textDepartment.setVisibility(founder ? android.view.View.GONE : android.view.View.VISIBLE);
                    binding.labelTeam.setVisibility(hasTeam ? android.view.View.VISIBLE : android.view.View.GONE);
                    binding.textTeam.setVisibility(hasTeam ? android.view.View.VISIBLE : android.view.View.GONE);
                    binding.textRole.setText(shown(com.example.project_pandora.core.util.RoleDisplayUtil.toChinese(text(person, "role"))));
                    binding.textSignature.setText(shown(text(person, "signature")));
                    binding.progress.setVisibility(android.view.View.GONE);
                    binding.message.setText("");
                    binding.btnPersonTasks.setEnabled(true);
                    binding.btnPersonLogs.setEnabled(true);
                }, error -> {
                    binding.progress.setVisibility(android.view.View.GONE);
                    binding.message.setText("人员资料加载失败，点击重试");
                    binding.message.setOnClickListener(v -> load());
                }));
    }

    @SuppressWarnings("unchecked") private static java.util.Map<String, Object> map(Object value) {
        return value instanceof java.util.Map ? (java.util.Map<String, Object>) value : java.util.Collections.emptyMap();
    }
    private static String text(java.util.Map<String, Object> row, String key) {
        return java.util.Objects.toString(com.example.project_pandora.ui.people.PeopleDirectory.value(row, key), "").trim();
    }
    private static String shown(String value) { return value == null || value.isEmpty() ? "未设置" : value; }
    @Override protected void onDestroy() { request.dispose(); binding = null; super.onDestroy(); }
}
