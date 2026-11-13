package com.example.project_pandora.ui.widget;

import android.graphics.Bitmap;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.project_pandora.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Source compilation only in this change; device execution requires a separately authorized APK. */
@RunWith(AndroidJUnit4.class)
public class CuteOfficeArtworkTest {
    @Test public void allPageScenesHaveDistinctOfficeArtworkAtTheSameResolution() {
        int[] scenes = {R.drawable.home_office_building,R.drawable.company_interior,R.drawable.company_workstation,
                R.drawable.company_sky,R.drawable.company_lounge};
        java.util.Set<Integer> hashes = new java.util.HashSet<>();
        for (int scene : scenes) {
            Bitmap bitmap = CuteOfficeArtwork.render(scene);
            assertEquals(720, bitmap.getWidth()); assertEquals(1600, bitmap.getHeight());
            int[] pixels = new int[720 * 1600]; bitmap.getPixels(pixels, 0, 720, 0, 0, 720, 1600);
            hashes.add(java.util.Arrays.hashCode(pixels)); bitmap.recycle();
        }
        assertEquals(scenes.length, hashes.size());
    }
}
