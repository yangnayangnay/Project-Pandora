package com.example.project_pandora.ui.mine;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.project_pandora.R;
import com.example.project_pandora.databinding.ActivityProfileBinding;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/** These view assertions require a device; compiling this source is not a device test pass. */
@RunWith(AndroidJUnit4.class)
public class ProfileReadOnlyTest {
    @Test public void accountPageContainsOnlyDisplayValuesAndKeepsAvatarInteractive() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = new ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext(), R.style.Theme_Project_Pandora);
            ActivityProfileBinding binding = ActivityProfileBinding.inflate(LayoutInflater.from(context));
            assertFalse(hasEditableField(binding.getRoot()));
            for (View value : new View[]{binding.textUsername, binding.textRealName, binding.textEmail, binding.textPhone}) {
                assertFalse(value.isClickable()); assertFalse(value.isFocusable());
            }
            assertTrue(binding.textAccountMaintenance.getText().toString().contains("管理员统一维护"));
            assertTrue(binding.imageAvatar.isClickable()); assertTrue(binding.imageAvatar.isFocusable());
            assertEquals("修改头像", binding.imageAvatar.getContentDescription().toString());
        });
    }

    private boolean hasEditableField(View view) {
        if (view instanceof EditText) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) if (hasEditableField(group.getChildAt(index))) return true;
        }
        return false;
    }
}
