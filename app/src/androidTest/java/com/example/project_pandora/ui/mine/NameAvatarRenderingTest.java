package com.example.project_pandora.ui.mine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.widget.ImageView;
import androidx.core.content.ContextCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.project_pandora.R;
import de.hdodenhof.circleimageview.CircleImageView;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class NameAvatarRenderingTest {
    @Test public void nameRemainsVisibleInCircularAndStandardAvatarsAtBothProfileSizes() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            for (int dp : new int[]{64, 96}) for (boolean circular : new boolean[]{true, false}) {
                ImageView view = circular ? new CircleImageView(context) : new ImageView(context);
                view.setImageDrawable(new NameAvatarDrawable(context, "王小明"));
                int size = Math.round(dp * context.getResources().getDisplayMetrics().density);
                int spec = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY);
                view.measure(spec, spec); view.layout(0, 0, size, size);
                Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                view.draw(new Canvas(bitmap));
                int textColor = ContextCompat.getColor(context, R.color.color_on_accent);
                int foregroundPixels = 0;
                for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
                    if (bitmap.getPixel(x, y) == textColor) foregroundPixels++;
                }
                assertTrue("Name text disappeared in " + view.getClass().getSimpleName(), foregroundPixels > size);
            }
        });
    }
}
