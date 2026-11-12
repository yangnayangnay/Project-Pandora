package com.example.project_pandora.ui.mine;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.project_pandora.ui.PandoraActivity;
import androidx.core.content.ContextCompat;

import com.example.project_pandora.R;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.data.remote.UserApi;
import com.example.project_pandora.databinding.ActivityProfileBinding;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Collections;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import com.example.project_pandora.core.util.BackToolbarHelper;

public class ProfileActivity extends PandoraActivity {

    private ActivityProfileBinding binding;
    private UserApi userApi;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private String originalAvatar;
    private String avatarDisplayName = "";
    private File pendingCameraFile;
    private boolean avatarUploading;
    private int avatarRevision;
    private final ActivityResultLauncher<PickVisualMediaRequest> albumPicker = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> { if (uri != null) uploadAvatar(uri, "album", null); });
    private final ActivityResultLauncher<Uri> cameraPicker = registerForActivityResult(new AvatarHelper.TakeAvatarPicture(), success -> {
        File capture = pendingCameraFile;
        pendingCameraFile = null;
        if (Boolean.TRUE.equals(success) && capture != null && capture.length() > 0) {
            uploadAvatar(AvatarHelper.cameraUri(this, capture), "camera", capture);
        } else if (capture != null) {
            capture.delete();
            if (Boolean.TRUE.equals(success)) Toast.makeText(this, "未能读取拍摄的照片，请重试", Toast.LENGTH_SHORT).show();
        }
    });
    private final ActivityResultLauncher<String> cameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchCamera();
                else new AlertDialog.Builder(this).setTitle("相机权限未开启")
                        .setMessage("拍照需要相机权限。你可以在系统设置中开启权限，或从相册选择头像。")
                        .setPositiveButton("前往设置", (dialog, which) -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + getPackageName()))))
                        .setNegativeButton("取消", null).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AvatarLoader.load(binding.imageAvatar, null, "");
        if (savedInstanceState != null && savedInstanceState.containsKey("avatarCameraPath")) {
            pendingCameraFile = new File(savedInstanceState.getString("avatarCameraPath"));
        }

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        userApi = NetworkClient.getInstance(this).createService(UserApi.class);

        android.text.InputFilter[] filters = binding.editSignature.getFilters();
        android.text.InputFilter[] signatureFilters = java.util.Arrays.copyOf(filters, filters.length + 1);
        signatureFilters[filters.length] = new SignatureLineFilter(binding.editSignature,
                () -> binding.signatureLayout.setError("个人签名最多 4 行（含自动换行）"));
        binding.editSignature.setFilters(signatureFilters);
        binding.editSignature.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable value) { binding.signatureLayout.setError(signatureError()); }
        });
        binding.editSignature.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r - l != or - ol) binding.signatureLayout.setError(signatureError());
        });

        loadUserInfo();

        binding.imageAvatar.setOnClickListener(v -> showAvatarPickerDialog());
        binding.btnSaveSignature.setOnClickListener(v -> saveSignature());
    }

    private void loadUserInfo() {
        int loadingAvatarRevision = avatarRevision;
        disposables.add(userApi.getCurrentUser()
                .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess() && response.getData() != null) {
                                Map<String, Object> data = response.getData();
                                boolean currentAvatar = loadingAvatarRevision == avatarRevision;
                                if (currentAvatar) originalAvatar = getStr(data, "avatar");
                                avatarDisplayName = com.example.project_pandora.ui.people.PeopleDirectory.name(data);

                                binding.textUsername.setText(display(data, "username"));
                                binding.textRealName.setText(display(data, "realName"));
                                binding.textEmail.setText(display(data, "email"));
                                binding.textPhone.setText(display(data, "phone"));
                                // Preserve older, longer signatures for review rather than
                                // silently truncating them when loading the new 20-char editor.
                                android.text.InputFilter[] signatureFilters = binding.editSignature.getFilters();
                                binding.editSignature.setFilters(new android.text.InputFilter[0]);
                                binding.editSignature.setText(getStr(data, "signature"));
                                binding.editSignature.setFilters(signatureFilters);
                                binding.textRole.setText(com.example.project_pandora.core.util.RoleDisplayUtil.toChinese(getStr(data, "role")));
                                // Round26 观察F: 部门/团队显示名称（/me 已补 LEFT JOIN；空值显示占位"-"）
                                String deptName = getStr(data, "departmentName");
                                binding.textDepartment.setText(deptName.isEmpty() ? "-" : deptName);
                                String teamName = getStr(data, "teamName");
                                binding.textTeam.setText(teamName.isEmpty() ? "-" : teamName);

                                if (currentAvatar) loadAvatar(originalAvatar);
                            }
                        },
                        error -> Toast.makeText(this, "加载失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private String display(Map<String, Object> map, String key) {
        String value = getStr(map, key);
        return value.trim().isEmpty() ? "未设置" : value;
    }

    private void saveSignature() {
        String validationError = signatureError();
        if (validationError != null) { binding.signatureLayout.setError(validationError); return; }
        String signature = com.example.project_pandora.core.util.SignatureRules.normalize(binding.editSignature.getText()).trim();
        binding.signatureLayout.setError(null); binding.btnSaveSignature.setEnabled(false);
        binding.textSignatureStatus.setText("正在保存…");
        disposables.add(userApi.updateCurrentUser(Collections.singletonMap("signature", signature))
                .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(response -> {
                    binding.btnSaveSignature.setEnabled(true);
                    binding.textSignatureStatus.setText(response.isSuccess() ? "已保存" : "保存失败：" + response.getMessage());
                }, error -> {
                    binding.btnSaveSignature.setEnabled(true);
                    binding.textSignatureStatus.setText("保存失败，请检查网络后重试");
                }));
    }

    private String signatureError() {
        String value = com.example.project_pandora.core.util.SignatureRules.normalize(binding.editSignature.getText());
        if (value.trim().length() > com.example.project_pandora.core.util.SignatureRules.MAX_LENGTH) return "个人签名最多 20 字";
        return SignatureLineFilter.fits(binding.editSignature, value) ? null : "个人签名最多 4 行（含自动换行）";
    }


    private void loadAvatar(String avatarUrl) {
        AvatarLoader.load(binding.imageAvatar, avatarUrl, avatarDisplayName);
    }

    private void showAvatarPickerDialog() {
        if (avatarUploading) return;
        new AlertDialog.Builder(this)
                .setTitle("修改头像")
                .setItems(new String[]{"从相册选择", "拍照"}, (dialog, which) -> {
                    if (which == 0) {
                        try {
                            albumPicker.launch(new PickVisualMediaRequest.Builder()
                                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
                        } catch (ActivityNotFoundException error) {
                            Toast.makeText(this, "未找到可用的图片选择器", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            launchCamera();
                        } else {
                            cameraPermission.launch(Manifest.permission.CAMERA);
                        }
                    }
                })
                .show();
    }

    private void launchCamera() {
        try {
            pendingCameraFile = AvatarHelper.createCameraFile(this);
            cameraPicker.launch(AvatarHelper.cameraUri(this, pendingCameraFile));
        } catch (IOException | ActivityNotFoundException | SecurityException error) {
            if (pendingCameraFile != null) pendingCameraFile.delete();
            pendingCameraFile = null;
            Toast.makeText(this, "无法打开相机，请检查相机应用和权限，或从相册选择", Toast.LENGTH_LONG).show();
        }
    }

    private void uploadAvatar(Uri imageUri, String imageSource, File capture) {
        avatarUploading = true;
        binding.imageAvatar.setEnabled(false);
        binding.progressAvatarUpload.setVisibility(View.VISIBLE);
        binding.textAvatarHint.setText("正在处理并上传头像…");
        disposables.add(io.reactivex.rxjava3.core.Observable.using(
                () -> AvatarHelper.prepareForUpload(getApplicationContext(), imageUri),
                file -> userApi.uploadAvatar(MultipartBody.Part.createFormData("file", file.getName(),
                                RequestBody.create(MediaType.parse("image/jpeg"), file)),
                        RequestBody.create(MediaType.parse("text/plain"), imageSource)),
                file -> file.delete())
                .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> {
                    if (capture != null) capture.delete();
                    avatarUploading = false;
                    if (!isDestroyed()) {
                        binding.imageAvatar.setEnabled(true);
                        binding.progressAvatarUpload.setVisibility(View.GONE);
                        binding.textAvatarHint.setText(R.string.avatar_upload_hint);
                    }
                })
                .subscribe(response -> {
                    if (response.isSuccess() && response.getData() != null && !getStr(response.getData(), "avatarUrl").isEmpty()) {
                        originalAvatar = getStr(response.getData(), "avatarUrl");
                        avatarRevision++;
                        AvatarLoader.load(binding.imageAvatar, originalAvatar, avatarDisplayName, true, displayed -> {
                            if (!isDestroyed()) Toast.makeText(this, displayed ? "头像更新成功"
                                    : "头像已上传，但图片读取失败，请检查服务器图片访问后重试", Toast.LENGTH_LONG).show();
                        });
                    } else Toast.makeText(this, "头像上传失败: " + response.getMessage(), Toast.LENGTH_LONG).show();
                }, error -> Toast.makeText(this, error instanceof IOException ? error.getMessage()
                        : "头像上传失败，请检查网络后重试", Toast.LENGTH_LONG).show()));
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        if (pendingCameraFile != null) state.putString("avatarCameraPath", pendingCameraFile.getAbsolutePath());
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}
