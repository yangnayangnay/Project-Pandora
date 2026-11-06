package com.example.project_pandora.core.util;

import java.io.File;
import java.io.IOException;

/** Fits decoded avatar pixels to the upload budget without imposing a source file-size limit. */
public final class AvatarJpegCompressor {
    public interface Image {
        int width();
        int height();
        void writeJpeg(File output, int quality) throws IOException;
        void resize(int width, int height) throws IOException;
    }

    private AvatarJpegCompressor() {}

    public static void compress(Image image, File output) throws IOException {
        compress(image, output, AvatarUploadPolicy.MAX_BYTES);
    }

    /** A smaller budget can exercise the same fallback with real JPEG data in JVM tests. */
    static void compress(Image image, File output, long maxBytes) throws IOException {
        if (maxBytes <= 0) throw new IllegalArgumentException("Invalid avatar byte budget");
        boolean complete = false;
        try {
            while (true) {
                for (int quality = 90; quality >= 50; quality -= 10) {
                    image.writeJpeg(output, quality);
                    long bytes = output.length();
                    if (bytes == 0) throw new IOException("图片处理失败，请重新选择图片");
                    if (bytes <= maxBytes) { complete = true; return; }
                }
                int width = image.width(), height = image.height();
                if (width <= 1 && height <= 1) throw new IOException("图片处理失败，请重新选择图片");
                // Reduce pixels as well as quality; every retry has strictly smaller dimensions.
                int nextWidth = Math.max(1, width * 3 / 4);
                int nextHeight = Math.max(1, height * 3 / 4);
                image.resize(nextWidth, nextHeight);
                if (image.width() != nextWidth || image.height() != nextHeight)
                    throw new IOException("图片缩放失败，请重试");
            }
        } finally {
            if (!complete) output.delete();
        }
    }
}
