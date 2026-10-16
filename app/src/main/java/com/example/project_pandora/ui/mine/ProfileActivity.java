package com.example.project_pandora.ui.mine;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.example.project_pandora.R;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.data.remote.UserApi;
import com.example.project_pandora.databinding.ActivityProfileBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import com.example.project_pandora.core.util.BackToolbarHelper;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private UserApi userApi;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_REGEX = Pattern.compile("^1[3-9]\\d{9}$");

    private String originalUsername, originalRealName, originalEmail, originalPhone, originalAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BackToolbarHelper.setupBack(binding.toolbar, this::finish);

        userApi = NetworkClient.getInstance(this).createService(UserApi.class);

        loadUserInfo();

        binding.btnSave.setOnClickListener(v -> saveProfile());

        binding.imageAvatar.setOnClickListener(v -> showAvatarPickerDialog());
    }

    private void loadUserInfo() {
        disposables.add(userApi.getCurrentUser()
                .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess() && response.getData() != null) {
                                Map<String, Object> data = response.getData();
                                originalUsername = getStr(data, "username");
                                originalRealName = getStr(data, "realName");
                                originalEmail = getStr(data, "email");
                                originalPhone = getStr(data, "phone");
                                originalAvatar = getStr(data, "avatar");

                                binding.editUsername.setText(originalUsername);
                                binding.editRealName.setText(originalRealName);
                                binding.editEmail.setText(originalEmail);
                                binding.editPhone.setText(originalPhone);
                                binding.textRole.setText(com.example.project_pandora.core.util.RoleDisplayUtil.toChinese(getStr(data, "role")));
                                binding.textDepartment.setText(getNumAsIntStr(data, "departmentId"));
                                binding.textTeam.setText(getNumAsIntStr(data, "teamId"));

                                loadAvatar(originalAvatar);
                            }
                        },
                        error -> Toast.makeText(this, "加载失败: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                ));
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private String getNumAsIntStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return "";
        if (val instanceof Number) return String.valueOf(((Number) val).intValue());
        return val.toString();
    }

    private void loadAvatar(String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            String baseUrl = NetworkClient.getBaseUrl();
            if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            String fullUrl = baseUrl + avatarUrl;
            Glide.with(this).load(fullUrl).placeholder(R.mipmap.ic_launcher).error(R.mipmap.ic_launcher).into(binding.imageAvatar);
        } else {
            binding.imageAvatar.setImageResource(R.mipmap.ic_launcher);
        }
    }

    private void saveProfile() {
        String username = binding.editUsername.getText().toString().trim();
        String realName = binding.editRealName.getText().toString().trim();
        String email = binding.editEmail.getText().toString().trim();
        String phone = binding.editPhone.getText().toString().trim();

        if (username.isEmpty()) {
            binding.layoutUsername.setError("用户名不能为空");
            return;
        }
        binding.layoutUsername.setError(null);

        if (realName.isEmpty()) {
            binding.layoutRealName.setError("真实姓名不能为空");
            return;
        }
        binding.layoutRealName.setError(null);

        if (!email.isEmpty() && !EMAIL_REGEX.matcher(email).matches()) {
            binding.layoutEmail.setError("邮箱格式不正确");
            return;
        }
        binding.layoutEmail.setError(null);

        if (!phone.isEmpty() && !PHONE_REGEX.matcher(phone).matches()) {
            binding.layoutPhone.setError("手机号格式不正确");
            return;
        }
        binding.layoutPhone.setError(null);

        Map<String, Object> request = new HashMap<>();
        request.put("username", username);
        request.put("realName", realName);
        request.put("email", email);
        request.put("phone", phone);

        disposables.add(userApi.updateProfile(request)
                .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess()) {
                                originalUsername = username;
                                originalRealName = realName;
                                originalEmail = email;
                                originalPhone = phone;
                                Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
                                // TASK-R18-019: 修改成功后主动触发当前页面展示刷新（账号信息实时生效）
                                // 清除 TokenManager 中可能存在的姓名缓存副本（如有），重新拉取最新数据
                                loadUserInfo();
                            } else {
                                rollbackFields();
                                Toast.makeText(this, "保存失败: " + response.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        },
                        error -> {
                            rollbackFields();
                            Toast.makeText(this, "保存失败，已恢复原信息", Toast.LENGTH_SHORT).show();
                        }
                ));
    }

    private void rollbackFields() {
        binding.editUsername.setText(originalUsername);
        binding.editRealName.setText(originalRealName);
        binding.editEmail.setText(originalEmail);
        binding.editPhone.setText(originalPhone);
    }

    private void showAvatarPickerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("修改头像")
                .setItems(new String[]{"从相册选择", "拍照"}, (dialog, which) -> {
                    if (which == 0) {
                        AvatarHelper.pickFromAlbum(this);
                    } else {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            AvatarHelper.pickFromCamera(this);
                        } else {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 200);
                        }
                    }
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            AvatarHelper.pickFromCamera(this);
        } else {
            Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri imageUri = AvatarHelper.handleActivityResult(requestCode, resultCode, data, this);
        if (imageUri != null) {
            uploadAvatar(imageUri);
        }
    }

    private void uploadAvatar(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            File tempFile = new File(getCacheDir(), "avatar_upload_tmp.jpg");
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();

            if (tempFile.length() > 2 * 1024 * 1024) {
                Toast.makeText(this, "图片过大，请选择小于2MB的图片", Toast.LENGTH_SHORT).show();
                tempFile.delete();
                return;
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), tempFile);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", tempFile.getName(), requestFile);
            RequestBody source = RequestBody.create(MediaType.parse("text/plain"), "album");

            disposables.add(userApi.uploadAvatar(body, source)
                    .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            response -> {
                                if (response.isSuccess() && response.getData() != null) {
                                    String avatarUrl = getStr(response.getData(), "avatarUrl");
                                    originalAvatar = avatarUrl;
                                    loadAvatar(avatarUrl);
                                    Toast.makeText(this, "头像更新成功", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "头像上传失败: " + response.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                                tempFile.delete();
                            },
                            error -> {
                                Toast.makeText(this, "头像上传失败", Toast.LENGTH_SHORT).show();
                                tempFile.delete();
                            }
                    ));
        } catch (Exception e) {
            Toast.makeText(this, "图片处理失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}
