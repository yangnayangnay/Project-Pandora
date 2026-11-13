package com.example.project_pandora.ui.people;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.project_pandora.R;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Compile without packaging; execution is deferred until device testing is authorized. */
@RunWith(AndroidJUnit4.class)
public class PeopleDirectoryViewTest {
    @Test public void founderIsACompanyPersonAndHeadIsDirectlyUnderDepartment() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = new ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext(), R.style.Theme_Project_Pandora);
            PeopleDirectoryView view = new PeopleDirectoryView(context, null);
            java.util.concurrent.atomic.AtomicReference<Map<String,Object>> opened = new java.util.concurrent.atomic.AtomicReference<>();
            view.configure("测试", "暂无人员", () -> {}, opened::set, null);
            view.setPeople(List.of(Map.of("id", 1, "realName", "创始人甲", "role", "FOUNDER"),
                    Map.of("id", 2, "realName", "部门负责人", "role", "DEPT_HEAD", "departmentId", 10, "departmentName", "研发", "teamName", "过期团队"),
                    Map.of("id", 3, "realName", "员工", "role", "EMPLOYEE", "departmentId", 10, "departmentName", "研发", "teamName", "客户端")));
            RecyclerView recycler = view.findViewById(R.id.recyclerPeople);
            @SuppressWarnings("unchecked") RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = (RecyclerView.Adapter<RecyclerView.ViewHolder>) recycler.getAdapter();
            assertEquals(2, adapter.getItemCount());
            RecyclerView.ViewHolder row = adapter.createViewHolder(recycler, 0);
            adapter.bindViewHolder(row, 0); row.itemView.performClick(); assertEquals("FOUNDER", opened.get().get("role"));
            adapter.bindViewHolder(row, 1); row.itemView.performClick(); assertEquals("研发", view.currentDepartment().name);
            assertEquals(2, adapter.getItemCount());
            adapter.bindViewHolder(row, 0); row.itemView.performClick(); assertEquals("DEPT_HEAD", opened.get().get("role"));
            assertTrue(view.back()); assertFalse(view.back());
        });
    }
    @Test public void rootSearchOpensPersonDirectlyAndSelectionUsesCheckbox() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = new ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext(), R.style.Theme_Project_Pandora);
            PeopleDirectoryView view = new PeopleDirectoryView(context, null);
            java.util.concurrent.atomic.AtomicReference<Map<String, Object>> opened = new java.util.concurrent.atomic.AtomicReference<>();
            view.configure("测试", "暂无人员", () -> {}, opened::set, department -> {});
            view.setPeople(List.of(Map.of("id", 1, "departmentId", 10, "departmentName", "研发", "realName", "张三", "username", "zhang"),
                    Map.of("id", 2, "departmentId", 20, "departmentName", "产品", "realName", "李四", "username", "li")));
            EditText search = view.findViewById(R.id.editSearch);
            RecyclerView recycler = view.findViewById(R.id.recyclerPeople);
            search.setText("张三");
            assertEquals(1, recycler.getAdapter().getItemCount());
            @SuppressWarnings("unchecked") RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = (RecyclerView.Adapter<RecyclerView.ViewHolder>) recycler.getAdapter();
            RecyclerView.ViewHolder holder = adapter.createViewHolder(recycler, 0);
            adapter.bindViewHolder(holder, 0); holder.itemView.performClick();
            assertEquals("张三", PeopleDirectory.name(opened.get()));
            assertEquals("张三", search.getText().toString());
            assertFalse(view.back());
            view.setSelectionMode(true); view.setSelectedIds(List.of(1L));
            adapter.bindViewHolder(holder, 0);
            assertEquals(View.GONE, holder.itemView.findViewById(R.id.imageChevron).getVisibility());
            assertTrue(((android.widget.CheckBox) holder.itemView.findViewById(R.id.checkSelected)).isChecked());
            assertTrue(holder.itemView.getContentDescription().toString().contains("取消选择"));
            view.showLoading(); assertFalse(view.findViewById(R.id.buttonAll).isEnabled());
            view.showError(); assertEquals(View.VISIBLE, view.findViewById(R.id.buttonRetry).getVisibility());
            assertFalse(view.findViewById(R.id.buttonAll).isEnabled());
        });
    }
}
