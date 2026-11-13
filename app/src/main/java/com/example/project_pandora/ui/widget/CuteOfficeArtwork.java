package com.example.project_pandora.ui.widget;

import android.graphics.*;
import com.example.project_pandora.R;

/** Original vector-drawn office world: shared chibi proportions, soft outlines and pastel islands.
 * Rasterized once off the UI thread; no network assets or font-dependent emoji. */
final class CuteOfficeArtwork {
    private final Canvas canvas;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int ink = Color.rgb(112, 100, 116), cream = Color.rgb(255, 250, 237);

    private CuteOfficeArtwork(Canvas canvas) { this.canvas = canvas; }

    static Bitmap render(int scene) {
        Bitmap bitmap = Bitmap.createBitmap(720, 1600, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap); canvas.scale(2, 2);
        new CuteOfficeArtwork(canvas).scene(scene);
        return bitmap;
    }

    private void scene(int scene) {
        boolean company = scene == R.drawable.company_interior, log = scene == R.drawable.company_workstation;
        boolean sky = scene == R.drawable.company_sky, mine = scene == R.drawable.company_lounge;
        paint.setShader(new LinearGradient(0, 0, 360, 800, 0xfffff3da,
                sky ? 0xffd7e9f8 : mine ? 0xfff6dfe8 : log ? 0xffffe7d3 : company ? 0xffd9ece4 : 0xffe5e3f4,
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, 360, 800, paint); paint.setShader(null);
        // Distinct silhouettes and arrangements, not the same desktop with a different tint.
        // Keep every subject inside the artboard; the middle stays quiet behind page content.
        if (company) collaboration();
        else if (log) journal();
        else if (sky) exploration();
        else if (mine) garden();
        else town();
    }

    private void town() {
        sun(295, 102, 21); cloud(73, 112, .8f);
        building(24, 200, .76f, 0xffc9e6f1); building(255, 505, .8f, 0xffe1d7ed);
        ellipse(18, 697, 157, 751, 0xffcce8d7, false);
        person(70, 650, .72f, 0xffffd9c5, true); plant(133, 726, .75f);
        envelope(301, 295, .65f); cloud(71, 452, .55f);
        sparkle(14, 350, 4, 0xffc4b1dd); sparkle(345, 666, 4, 0xffe8b4a1);
    }

    private void collaboration() {
        // A window, planning board and shared meeting table distinguish the company page.
        rounded(27, 85, 127, 175, 16, cream, true);
        rounded(33, 91, 121, 169, 12, 0xffc9e6f1, false);
        line(77, 92, 77, 168, ink, 2); line(34, 130, 120, 130, ink, 2);
        cloud(78, 143, .43f); sun(101, 109, 9);
        rounded(252, 222, 334, 308, 12, cream, true);
        for (int i = 0; i < 3; i++) {
            rounded(263, 235 + i * 21, 277, 249 + i * 21, 4, 0xffcce8d7, false);
            line(286, 242 + i * 21, 322, 242 + i * 21, 0xffc3b5ce, 3);
        }
        canvas.save(); canvas.translate(31, 607);
        ellipse(-3, 84, 145, 126, 0xffc9e6f1, false);
        person(35, 15, .68f, 0xffffd9c5, true); person(106, 21, .63f, 0xffe1d7ed, false);
        desk(0, 70, 137, 0xffcce8d7); canvas.restore();
        plant(310, 724, 1.2f); envelope(36, 392, .55f);
        sparkle(14, 529, 4, 0xffb8d9be); sparkle(345, 435, 4, 0xffc3b5ce);
    }

    private void journal() {
        // Open planner with sticky notes, a pencil and a small writing desk.
        canvas.save(); canvas.translate(268, 110); canvas.rotate(9);
        rounded(-49, -32, 49, 39, 10, cream, true);
        line(0, -26, 0, 32, 0xffd3baa6, 2);
        for (int i = 0; i < 3; i++) {
            line(-37, -13 + i * 14, -11, -13 + i * 14, 0xffddc4ad, 2);
            line(11, -13 + i * 14, 37, -13 + i * 14, 0xffc8c2df, 2);
        }
        canvas.restore();
        rounded(23, 245, 81, 301, 8, 0xffffd9c5, true);
        line(35, 264, 68, 264, ink, 2); line(35, 277, 61, 277, ink, 2);
        envelope(311, 410, .72f); cup(43, 537, .9f);
        canvas.save(); canvas.translate(217, 649);
        ellipse(-7, 71, 121, 113, 0xffe1d7ed, false);
        person(56, 0, .72f, 0xffcce8d7, false); desk(0, 60, 113, cream);
        canvas.restore(); plant(68, 742, 1f);
        sparkle(14, 169, 4, 0xffd3baa6); sparkle(345, 567, 4, 0xffd3baa6);
    }

    private void exploration() {
        // A cloud observatory, orbit and telescope give AI its own playful skyline.
        cloud(88, 153, 1.25f); person(88, 97, .64f, 0xffe1d7ed, true);
        circle(293, 274, 29, 0xffffd9c5, true); face(293, 274, .65f);
        paint.setStyle(Paint.Style.STROKE); paint.setColor(0xffb8b4cf); paint.setStrokeWidth(2);
        canvas.save(); canvas.rotate(-25, 293, 274); canvas.drawOval(250, 264, 336, 284, paint); canvas.restore();
        paint.setStyle(Paint.Style.FILL);
        cloud(278, 694, 1.25f);
        person(243, 627, .71f, 0xffc9e6f1, false);
        line(298, 648, 287, 695, ink, 2); line(298, 648, 317, 695, ink, 2);
        canvas.save(); canvas.rotate(-28, 302, 636);
        rounded(276, 625, 327, 644, 7, 0xffe1d7ed, true); canvas.restore();
        for (int i = 0; i < 4; i++) sparkle(25 + (i % 2) * 18, 355 + i * 42, 4 + i % 2, 0xffb8b4cf);
        cloud(71, 731, .7f); envelope(306, 93, .6f);
        sparkle(345, 489, 5, 0xffe9ba9e);
    }

    private void garden() {
        // A planted lounge, hanging leaves and a warm tea corner for the personal page.
        line(296, 65, 296, 111, ink, 2); plant(296, 152, 1f);
        cloud(72, 276, .8f); cup(67, 270, 1.1f);
        canvas.save(); canvas.translate(239, 451);
        rounded(-3, 30, 92, 98, 18, 0xffe1d7ed, true);
        rounded(-7, 62, 96, 100, 12, 0xffffd9c5, true);
        person(43, 3, .77f, 0xffcce8d7, true); canvas.restore();
        ellipse(22, 700, 148, 752, 0xffcce8d7, false);
        plant(56, 721, 1.4f); plant(111, 728, .9f);
        envelope(288, 728, .75f); sparkle(15, 477, 4, 0xffc4b1dd);
        sparkle(345, 338, 4, 0xffc4b1dd);
    }

    private void building(float x, float y, float scale, int color) {
        canvas.save(); canvas.translate(x, y); canvas.scale(scale, scale);
        ellipse(-4, 98, 107, 130, 0xffcce8d7, false);
        rounded(4, 0, 92, 112, 16, cream, true); rounded(0, -10, 96, 12, 9, color, true);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++)
            rounded(15 + col * 24, 26 + row * 24, 30 + col * 24, 41 + row * 24, 4, color, false);
        canvas.restore();
    }

    private void sun(float x, float y, float radius) {
        circle(x, y, radius, 0xffffdea0, false);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * i / 4;
            line(x + (float)Math.cos(angle) * (radius + 5), y + (float)Math.sin(angle) * (radius + 5),
                    x + (float)Math.cos(angle) * (radius + 10), y + (float)Math.sin(angle) * (radius + 10), 0xffefbc6a, 2);
        }
        if (radius > 12) face(x, y, .65f);
    }

    private void person(float x, float y, float scale, int shirt, boolean wave) {
        canvas.save(); canvas.translate(x, y); canvas.scale(scale, scale);
        rounded(-18, 54, -3, 89, 7, 0xffb8b4cf, true); rounded(3, 54, 18, 89, 7, 0xffb8b4cf, true);
        rounded(-23, 27, 23, 68, 17, shirt, true);
        if (wave) {
            line(18, 42, 34, 22, ink, 12); line(18, 42, 34, 22, shirt, 8);
            circle(35, 17, 6, 0xffffe7d4, true);
        } else {
            rounded(15, 36, 30, 61, 7, shirt, true); circle(23, 61, 6, 0xffffe7d4, true);
        }
        rounded(-31, 36, -16, 60, 7, shirt, true); circle(-23, 61, 6, 0xffffe7d4, true);
        circle(0, 4, 31, 0xffb8a8a0, true);
        rounded(-28, -16, 28, 31, 23, 0xffffe7d4, true);
        ellipse(-27, -23, 6, -1, 0xffb8a8a0, false);
        ellipse(-7, -23, 27, -3, 0xffb8a8a0, false);
        face(0, 8, .8f);
        rounded(-22, 82, -1, 91, 4, cream, true); rounded(1, 82, 22, 91, 4, cream, true);
        canvas.restore();
    }

    private void desk(float x, float y, float width, int color) {
        line(x + 12, y + 8, x + 12, y + 42, ink, 4); line(x + width - 12, y + 8, x + width - 12, y + 42, ink, 4);
        rounded(x, y, x + width, y + 12, 6, 0xffead3b7, true);
        rounded(x + 27, y - 36, x + 85, y - 2, 6, color, true);
        circle(x + 56, y - 19, 4, cream, false); line(x + 21, y - 1, x + 91, y - 1, ink, 3);
        cup(x + 103, y - 10, .6f);
    }

    private void face(float x, float y, float scale) {
        canvas.save(); canvas.translate(x, y); canvas.scale(scale, scale);
        ellipse(-15, 3, -7, 8, 0xfff3bcb5, false); ellipse(7, 3, 15, 8, 0xfff3bcb5, false);
        circle(-7, 0, 1.8f, ink, false); circle(7, 0, 1.8f, ink, false);
        paint.setColor(ink); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.6f); paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawArc(new RectF(-4, 0, 4, 8), 15, 150, false, paint); paint.setStyle(Paint.Style.FILL);
        canvas.restore();
    }

    private void plant(float x, float y, float scale) {
        canvas.save(); canvas.translate(x, y); canvas.scale(scale, scale);
        line(0, -3, 0, -41, ink, 2);
        ellipse(-18, -39, 0, -20, 0xffb8d9be, true); ellipse(0, -49, 19, -28, 0xffc9e3c2, true);
        rounded(-13, -11, 13, 9, 5, 0xffedc0ae, true); canvas.restore();
    }

    private void cup(float x, float y, float scale) {
        canvas.save(); canvas.translate(x, y); canvas.scale(scale, scale);
        rounded(9, -7, 21, 5, 5, cream, true); rounded(-10, -12, 12, 12, 6, cream, true);
        line(-5, -21, -3, -27, 0xffcfbaa8, 2); line(4, -20, 6, -27, 0xffcfbaa8, 2);
        face(1, -2, .45f); canvas.restore();
    }

    private void envelope(float x, float y, float scale) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(-12); canvas.scale(scale, scale);
        rounded(-24, -16, 24, 16, 6, cream, true);
        line(-22, -12, 0, 3, ink, 1.8f); line(0, 3, 22, -12, ink, 1.8f);
        circle(0, 3, 4, 0xffe8b4b5, false); canvas.restore();
    }

    private void cloud(float x, float y, float scale) {
        canvas.save(); canvas.translate(x, y); canvas.scale(scale, scale);
        rounded(-34, 0, 39, 23, 12, cream, false);
        circle(-14, 1, 17, cream, false); circle(12, -4, 22, cream, false); canvas.restore();
    }
    private void sparkle(float x, float y, float radius, int color) {
        line(x - radius, y, x + radius, y, color, 2.8f); line(x, y - radius, x, y + radius, color, 2.8f);
    }
    private void line(float x1, float y1, float x2, float y2, int color, float width) {
        paint.setColor(color); paint.setStrokeWidth(width); paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(x1, y1, x2, y2, paint);
    }
    private void rounded(float left, float top, float right, float bottom, float radius, int color, boolean outline) {
        paint.setColor(color); paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint);
        if (outline) { paint.setColor(ink); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.8f);
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint); paint.setStyle(Paint.Style.FILL); }
    }
    private void ellipse(float left, float top, float right, float bottom, int color, boolean outline) {
        paint.setColor(color); paint.setStyle(Paint.Style.FILL); canvas.drawOval(left, top, right, bottom, paint);
        if (outline) { paint.setColor(ink); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.8f);
            canvas.drawOval(left, top, right, bottom, paint); paint.setStyle(Paint.Style.FILL); }
    }
    private void circle(float x, float y, float radius, int color, boolean outline) { ellipse(x-radius,y-radius,x+radius,y+radius,color,outline); }
}
