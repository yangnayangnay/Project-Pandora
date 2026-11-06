package com.example.project_pandora.ui.mine;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import com.example.project_pandora.core.util.AvatarUploadPolicy;
import com.example.project_pandora.core.util.AvatarJpegCompressor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class AvatarHelper {
    private AvatarHelper() {}
    /** Explicit URI grants also work with camera apps that require ClipData. */
    public static final class TakeAvatarPicture extends ActivityResultContracts.TakePicture {
        @NonNull @Override public Intent createIntent(@NonNull Context context, @NonNull Uri uri) {
            Intent intent = super.createIntent(context, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.setClipData(ClipData.newRawUri("avatar", uri));
            return intent;
        }
    }
    public static File createCameraFile(Context context) throws IOException {
        return File.createTempFile("camera-", ".jpg", avatarDirectory(context));
    }
    public static Uri cameraUri(Context context, File file) {
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
    }
    private static File avatarDirectory(Context context) throws IOException {
        File directory = new File(context.getCacheDir(), "avatars");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("无法创建头像临时文件");
        return directory;
    }
    private static InputStream open(Context context, Uri uri) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);
        if (input == null) throw new IOException("无法读取图片，请重新选择");
        return input;
    }
    /** Original file size is unrestricted; sample pixels and encode to <=5MB off the UI thread. */
    public static File prepareForUpload(Context context, Uri uri) throws IOException {
        Bitmap bitmap = null;
        BitmapImage image = null;
        File output = null;
        boolean complete = false;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream input = open(context, uri)) { BitmapFactory.decodeStream(input, null, options); }
            if (options.outWidth <= 0 || options.outHeight <= 0) throw new IOException("无法识别这张图片，请选择其他图片");
            options.inSampleSize = AvatarUploadPolicy.sampleSize(options.outWidth, options.outHeight);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            try (InputStream input = open(context, uri)) { bitmap = BitmapFactory.decodeStream(input, null, options); }
            if (bitmap == null) throw new IOException("图片解码失败，请选择其他图片");
            int orientation = ExifInterface.ORIENTATION_NORMAL;
            try (InputStream input = open(context, uri)) {
                orientation = new ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            } catch (IOException ignored) { /* Images without EXIF retain their decoded orientation. */ }
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: matrix.setScale(-1, 1); break;
                case ExifInterface.ORIENTATION_ROTATE_180: matrix.setRotate(180); break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL: matrix.setScale(1, -1); break;
                case ExifInterface.ORIENTATION_TRANSPOSE: matrix.setRotate(90); matrix.postScale(-1, 1); break;
                case ExifInterface.ORIENTATION_ROTATE_90: matrix.setRotate(90); break;
                case ExifInterface.ORIENTATION_TRANSVERSE: matrix.setRotate(270); matrix.postScale(-1, 1); break;
                case ExifInterface.ORIENTATION_ROTATE_270: matrix.setRotate(270); break;
                default: break;
            }
            if (!matrix.isIdentity()) {
                Bitmap oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (oriented != bitmap) { bitmap.recycle(); bitmap = oriented; }
            }
            int largest = Math.max(bitmap.getWidth(), bitmap.getHeight());
            if (largest > AvatarUploadPolicy.MAX_DIMENSION) {
                float scale = (float) AvatarUploadPolicy.MAX_DIMENSION / largest;
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, Math.max(1, Math.round(bitmap.getWidth() * scale)),
                        Math.max(1, Math.round(bitmap.getHeight() * scale)), true);
                if (scaled != bitmap) { bitmap.recycle(); bitmap = scaled; }
            }
            image = new BitmapImage(bitmap);
            bitmap = null; // BitmapImage now owns the pixels, including resized replacements.
            output = File.createTempFile("upload-", ".jpg", avatarDirectory(context));
            AvatarJpegCompressor.compress(image, output);
            complete = true;
            return output;
        } catch (OutOfMemoryError error) {
            throw new IOException("暂时无法处理这张图片，请稍后重试", error);
        } finally {
            if (bitmap != null) bitmap.recycle();
            if (image != null) image.close();
            if (!complete && output != null) output.delete();
        }
    }

    private static final class BitmapImage implements AvatarJpegCompressor.Image, AutoCloseable {
        private Bitmap bitmap;

        BitmapImage(Bitmap bitmap) { this.bitmap = bitmap; }
        @Override public int width() { return bitmap.getWidth(); }
        @Override public int height() { return bitmap.getHeight(); }

        @Override public void writeJpeg(File output, int quality) throws IOException {
            try (FileOutputStream stream = new FileOutputStream(output)) {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream))
                    throw new IOException("图片压缩失败，请重试");
            }
        }

        @Override public void resize(int width, int height) {
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, width, height, true);
            if (scaled != bitmap) { bitmap.recycle(); bitmap = scaled; }
        }

        @Override public void close() { bitmap.recycle(); }
    }
}
