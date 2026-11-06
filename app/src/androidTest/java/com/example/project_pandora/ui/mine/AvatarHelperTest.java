package com.example.project_pandora.ui.mine;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.project_pandora.core.util.AvatarUploadPolicy;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Source compilation only unless APK/device testing is explicitly authorized. */
@RunWith(AndroidJUnit4.class)
public class AvatarHelperTest {
    @Test public void originalImageOverFiveMegabytesIsPreparedWithoutRejectingItsSize() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File original = AvatarHelper.createCameraFile(context);
        File output = null;
        Bitmap source = Bitmap.createBitmap(1536, 1536, Bitmap.Config.ARGB_8888);
        try {
            Random random = new Random(20261104);
            int[] row = new int[1536];
            for (int y = 0; y < 1536; y++) {
                for (int x = 0; x < row.length; x++) row[x] = 0xff000000 | random.nextInt(0x1000000);
                source.setPixels(row, 0, row.length, 0, y, row.length, 1);
            }
            try (var stream = new FileOutputStream(original)) {
                assertTrue(source.compress(Bitmap.CompressFormat.PNG, 100, stream));
            }
            long originalBytes = original.length();
            assertTrue("the original really exceeds 5MB", originalBytes > AvatarUploadPolicy.MAX_BYTES);
            source.recycle();
            output = AvatarHelper.prepareForUpload(context, AvatarHelper.cameraUri(context, original));
            assertTrue("the prepared upload fits 5MB", AvatarUploadPolicy.accepts(output.length()));
            assertEquals("never modify the selected original", originalBytes, original.length());
            var bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(output.getPath(), bounds);
            assertEquals("image/jpeg", bounds.outMimeType);
            assertTrue(bounds.outWidth > 0 && bounds.outHeight > 0);
        } finally {
            if (!source.isRecycled()) source.recycle();
            original.delete(); if (output != null) output.delete();
        }
    }

    @Test public void cameraContractGrantsReadWriteAccessAndKeepsTheOutputUri() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File camera = AvatarHelper.createCameraFile(context);
        try {
            Uri uri = AvatarHelper.cameraUri(context, camera);
            Intent intent = new AvatarHelper.TakeAvatarPicture().createIntent(context, uri);
            assertEquals("content", uri.getScheme());
            assertEquals(uri, intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT));
            assertEquals(uri, intent.getClipData().getItemAt(0).getUri());
            assertTrue((intent.getFlags() & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0);
            assertTrue((intent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
        } finally { camera.delete(); }
    }
    @Test public void pngBytesAreUploadedAsRealJpegAndCameraOrientationIsCorrected() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File camera = AvatarHelper.createCameraFile(context);
        File output = null;
        Bitmap source = Bitmap.createBitmap(320, 160, Bitmap.Config.ARGB_8888);
        try {
            try (var stream = new FileOutputStream(camera)) { source.compress(Bitmap.CompressFormat.PNG, 100, stream); }
            output = AvatarHelper.prepareForUpload(context, AvatarHelper.cameraUri(context, camera));
            var options = new BitmapFactory.Options(); options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(output.getPath(), options);
            assertEquals("image/jpeg", options.outMimeType);
            assertTrue(AvatarUploadPolicy.accepts(output.length()));
            output.delete(); output = null;
            try (var stream = new FileOutputStream(camera)) { source.compress(Bitmap.CompressFormat.JPEG, 90, stream); }
            var exif = new ExifInterface(camera.getPath());
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_90)); exif.saveAttributes();
            output = AvatarHelper.prepareForUpload(context, AvatarHelper.cameraUri(context, camera));
            BitmapFactory.decodeFile(output.getPath(), options);
            assertEquals(160, options.outWidth); assertEquals(320, options.outHeight);
        } finally { source.recycle(); camera.delete(); if (output != null) output.delete(); }
    }
}
