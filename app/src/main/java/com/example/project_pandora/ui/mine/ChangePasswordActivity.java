package com.example.project_pandora.ui.mine;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.util.BackToolbarHelper;
import com.example.project_pandora.data.remote.UserApi;
import com.example.project_pandora.databinding.ActivityChangePasswordBinding;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class ChangePasswordActivity extends AppCompatActivity {
    private ActivityChangePasswordBinding binding;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        BackToolbarHelper.setupBack(binding.toolbar, this::finish);
        binding.btnChangePassword.setOnClickListener(v -> save());
    }

    private void save() {
        String oldPassword = binding.editOldPassword.getText().toString();
        String newPassword = binding.editNewPassword.getText().toString();
        String confirmation = binding.editConfirmPassword.getText().toString();
        binding.layoutOldPassword.setError(null);
        binding.layoutNewPassword.setError(null);
        binding.layoutConfirmPassword.setError(null);
        if (oldPassword.isEmpty()) { binding.layoutOldPassword.setError("请输入原密码"); return; }
        if (newPassword.length() < 6 || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            binding.layoutNewPassword.setError("至少6位，UTF-8长度不超过72字节"); return;
        }
        if (newPassword.equals(oldPassword)) { binding.layoutNewPassword.setError("新密码不能与原密码相同"); return; }
        if (!newPassword.equals(confirmation)) { binding.layoutConfirmPassword.setError("两次输入的新密码不一致"); return; }
        Map<String, String> request = new HashMap<>();
        request.put("oldPassword", oldPassword); request.put("newPassword", newPassword);
        binding.btnChangePassword.setEnabled(false);
        binding.btnChangePassword.setText("正在修改…");
        disposables.add(NetworkClient.getInstance(this).createService(UserApi.class).changePassword(request)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if (response.isSuccess()) {
                        binding.editOldPassword.setText(""); binding.editNewPassword.setText(""); binding.editConfirmPassword.setText("");
                        Toast.makeText(this, "密码已修改，下次登录请使用新密码", Toast.LENGTH_LONG).show(); finish();
                    } else { restoreButton(); Toast.makeText(this, response.getMessage(), Toast.LENGTH_LONG).show(); }
                }, error -> {
                    restoreButton(); Toast.makeText(this, "修改失败，请检查原密码和网络连接", Toast.LENGTH_LONG).show();
                }));
    }

    private void restoreButton() { binding.btnChangePassword.setEnabled(true); binding.btnChangePassword.setText("确认修改"); }
    @Override protected void onDestroy() { disposables.clear(); super.onDestroy(); }
}
