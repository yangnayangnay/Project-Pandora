package com.example.project_pandora.core.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import static org.junit.Assert.*;

/** Standalone JVM tests: desktop JPEG encoding is unavailable in Android's compilation API. */
public class AvatarJpegCompressorTest {
    @Rule public TemporaryFolder files = new TemporaryFolder();

    @Test public void productionBudgetProducesAReadableJpegWithoutEnlargingTheImage() throws Exception {
        JpegImage image = new JpegImage(640, 480);
        File output = files.newFile("avatar.jpg");
        AvatarJpegCompressor.compress(image, output);
        assertTrue(AvatarUploadPolicy.accepts(output.length()));
        BufferedImage decoded = ImageIO.read(output);
        assertNotNull(decoded);
        assertEquals(640, decoded.getWidth()); assertEquals(480, decoded.getHeight());
        assertEquals(0, image.resizes);
    }

    @Test public void qualityCanFitTheBudgetWhilePreservingTheOriginalDimensions() throws Exception {
        JpegImage image = new JpegImage(384, 384);
        File probe = files.newFile("probe.jpg");
        image.writeJpeg(probe, 90); long highQuality = probe.length();
        image.writeJpeg(probe, 50); long lowQuality = probe.length();
        assertTrue(highQuality > lowQuality);
        long budget = (highQuality + lowQuality) / 2;
        File output = files.newFile("avatar.jpg");
        AvatarJpegCompressor.compress(image, output, budget);
        assertTrue(output.length() > 0 && output.length() <= budget);
        assertEquals(0, image.resizes);
        assertEquals(384, ImageIO.read(output).getWidth());
    }

    @Test public void resolutionShrinksUntilAnOtherwiseOversizedJpegFits() throws Exception {
        JpegImage image = new JpegImage(512, 384);
        File output = files.newFile("avatar.jpg");
        image.writeJpeg(output, 50);
        long budget = 4096;
        assertTrue("quality alone cannot fit this image", output.length() > budget);
        AvatarJpegCompressor.compress(image, output, budget);
        assertTrue(output.length() > 0 && output.length() <= budget);
        assertTrue(image.resizes > 0);
        BufferedImage decoded = ImageIO.read(output);
        assertNotNull(decoded);
        assertTrue(decoded.getWidth() < 512 && decoded.getHeight() < 384);
        assertEquals(image.width(), decoded.getWidth()); assertEquals(image.height(), decoded.getHeight());
    }

    @Test public void encoderErrorsRemovePartialFiles() throws Exception {
        File output = files.newFile("partial.jpg");
        AvatarJpegCompressor.Image broken = new JpegImage(2, 2) {
            @Override public void writeJpeg(File file, int quality) throws IOException {
                Files.write(file.toPath(), new byte[]{1, 2, 3});
                throw new IOException("encoder failed");
            }
        };
        try { AvatarJpegCompressor.compress(broken, output); fail("encoding must fail"); }
        catch (IOException expected) { assertEquals("encoder failed", expected.getMessage()); }
        assertFalse(output.exists());
    }

    @Test public void anEmptyEncoderResultCannotBeUploaded() throws Exception {
        File output = files.newFile("empty.jpg");
        AvatarJpegCompressor.Image empty = new JpegImage(2, 2) {
            @Override public void writeJpeg(File file, int quality) throws IOException {
                Files.write(file.toPath(), new byte[0]);
            }
        };
        try { AvatarJpegCompressor.compress(empty, output); fail("empty result must fail"); }
        catch (IOException expected) { assertFalse(output.exists()); }
    }

    @Test(timeout = 5000) public void anImpossibleBudgetTerminatesWithoutLeavingAnOversizedFile() throws Exception {
        File output = files.newFile("too-small.jpg");
        try { AvatarJpegCompressor.compress(new JpegImage(1, 1), output, 1); fail("budget cannot fit JPEG headers"); }
        catch (IOException expected) { assertFalse(output.exists()); }
    }

    private static class JpegImage implements AvatarJpegCompressor.Image {
        private BufferedImage pixels;
        int resizes;

        JpegImage(int width, int height) {
            pixels = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Random random = new Random(20261104);
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) pixels.setRGB(x, y, random.nextInt());
        }

        @Override public int width() { return pixels.getWidth(); }
        @Override public int height() { return pixels.getHeight(); }

        @Override public void writeJpeg(File output, int quality) throws IOException {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            try (FileOutputStream file = new FileOutputStream(output);
                 ImageOutputStream stream = new MemoryCacheImageOutputStream(file)) {
                writer.setOutput(stream);
                ImageWriteParam options = writer.getDefaultWriteParam();
                options.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                options.setCompressionQuality(quality / 100f);
                writer.write(null, new IIOImage(pixels, null, null), options);
            } finally { writer.dispose(); }
        }

        @Override public void resize(int width, int height) {
            BufferedImage smaller = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = smaller.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.drawImage(pixels, 0, 0, width, height, null);
            } finally { graphics.dispose(); }
            pixels.flush(); pixels = smaller; resizes++;
        }
    }
}
