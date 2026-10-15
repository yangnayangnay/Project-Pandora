package com.example.project_pandora.ui.mine;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;

import java.io.File;

public class AvatarHelper {

    public static final int REQUEST_ALBUM = 10001;
    public static final int REQUEST_CAMERA = 10002;

    public static void pickFromAlbum(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activity.startActivityForResult(intent, REQUEST_ALBUM);
    }

    public static void pickFromCamera(Activity activity) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            File photoFile = new File(activity.getCacheDir(), "avatar_camera_tmp.jpg");
            Uri photoUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            activity.startActivityForResult(intent, REQUEST_CAMERA);
        }
    }

    public static Uri handleActivityResult(int requestCode, int resultCode, Intent data, Activity activity) {
        if (resultCode != Activity.RESULT_OK) return null;

        if (requestCode == REQUEST_ALBUM) {
            if (data != null) return data.getData();
        } else if (requestCode == REQUEST_CAMERA) {
            File photoFile = new File(activity.getCacheDir(), "avatar_camera_tmp.jpg");
            if (photoFile.exists()) {
                return Uri.fromFile(photoFile);
            }
        }
        return null;
    }
}