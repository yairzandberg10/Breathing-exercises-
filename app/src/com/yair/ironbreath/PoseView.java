package com.yair.ironbreath;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * An articulated, broad-shouldered figure that morphs smoothly between yoga poses. Poses are joint
 * coordinates (see {@link Content.Pose}); transitions interpolate every joint with easing.
 */
final class PoseView extends View {
    private static final String FRONT = ",warrior2,triangle,tree,lotus,sideBend,";
    private final float[] from = new float[30], to = new float[30], cur = new float[30];
    private float wFrom = 1, wTo = 1;
    private long animStart;
    private long animDur = 1000;
    int color = Ui.EMBER;
    boolean showMat = true;
    boolean breathe = true;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rf = new RectF();
    private float s, gx, gy;

    PoseView(Context c) {
        super(c);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        setPose(Content.pose("mountain"), false);
    }

    void setPose(Content.Pose pose, boolean animate) {
        float w = FRONT.contains("," + pose.key + ",") ? 1.55f : 1f;
        if (animate) {
            System.arraycopy(cur, 0, from, 0, 30);
            wFrom = currentWidth();
            animStart = System.currentTimeMillis();
        } else {
            System.arraycopy(pose.pts, 0, from, 0, 30);
            wFrom = w;
            animStart = 0;
        }
        System.arraycopy(pose.pts, 0, to, 0, 30);
        wTo = w;
        if (!animate) System.arraycopy(pose.pts, 0, cur, 0, 30);
        invalidate();
    }

    private float progress() {
        if (animStart == 0) return 1f;
        return Math.min(1f, (System.currentTimeMillis() - animStart) / (float) animDur);
    }

    private float currentWidth() {
        float e = BreathView.ease(progress());
        return wFrom + (wTo - wFrom) * e;
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        s = Math.min(w / 1.3f, h / 1.28f);
        gx = w / 2f;
        gy = h * 0.88f;
        float e = BreathView.ease(progress());
        float t = System.currentTimeMillis() / 1000f;
        float bob = breathe ? (float) Math.sin(t * 2 * Math.PI / 5.5) * 0.006f : 0;
        for (int i = 0; i < 30; i++) {
            cur[i] = from[i] + (to[i] - from[i]) * e;
        }
        float tw = 0.058f * currentWidth();

        // floor glow + mat
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(gx, gy, s * 0.8f, new int[]{Ui.alpha(color, 0.22f), 0x00000000}, null, Shader.TileMode.CLAMP));
        rf.set(gx - s * 0.8f, gy - s * 0.25f, gx + s * 0.8f, gy + s * 0.25f);
        c.drawOval(rf, p);
        p.setShader(null);
        if (showMat) {
            rf.set(gx - s * 0.62f, gy - s * 0.005f, gx + s * 0.62f, gy + s * 0.022f);
            p.setShader(new LinearGradient(rf.left, 0, rf.right, 0, new int[]{Ui.alpha(color, 0.15f), Ui.alpha(color, 0.55f), Ui.alpha(color, 0.15f)}, null, Shader.TileMode.CLAMP));
            c.drawRoundRect(rf, s * 0.01f, s * 0.01f, p);
            p.setShader(null);
        }

        int back = Ui.mix(color, Ui.BG, 0.5f);
        int front = color;
        int bright = Ui.mix(color, 0xFFFFFFFF, 0.18f);
        float lw = s * 0.052f;

        // glow pass
        p.setStyle(Paint.Style.STROKE);
        p.setColor(0x00000000);
        drawLimbs(c, lw * 1.8f, lw * 2.2f, bob, true);

        // back limbs
        p.setColor(back);
        limb(c, 7, 8, lw * 1.2f, 0);
        limb(c, 8, 9, lw * 0.95f, 0);
        limb(c, 9, 12, lw * 0.55f, 0);
        limb(c, 2, 3, lw * 0.9f, bob);
        limb(c, 3, 4, lw * 0.75f, bob);

        // torso (tapered, bent through the mid-spine point)
        float sx = X(2), sy = Y(2, bob), hx = X(7), hy = Y(7, 0), mx = X(14), my = Y(14, bob * 0.5f);
        float dx = sx - hx, dy = sy - hy;
        float len = (float) Math.sqrt(dx * dx + dy * dy) + 0.0001f;
        float nx = -dy / len, ny = dx / len;
        float ts = tw * s;
        path.reset();
        path.moveTo(sx + nx * ts, sy + ny * ts);
        path.quadTo(mx + nx * ts * 0.95f, my + ny * ts * 0.95f, hx + nx * ts * 0.68f, hy + ny * ts * 0.68f);
        path.lineTo(hx - nx * ts * 0.68f, hy - ny * ts * 0.68f);
        path.quadTo(mx - nx * ts * 0.95f, my - ny * ts * 0.95f, sx - nx * ts, sy - ny * ts);
        path.close();
        p.setStyle(Paint.Style.FILL_AND_STROKE);
        p.setStrokeWidth(lw * 0.5f);
        p.setShader(new LinearGradient(sx, sy, hx, hy, bright, Ui.mix(color, Ui.BG, 0.2f), Shader.TileMode.CLAMP));
        c.drawPath(path, p);
        p.setShader(null);

        // neck + head
        p.setStyle(Paint.Style.STROKE);
        p.setColor(front);
        limb(c, 1, 2, lw * 0.85f, bob);
        p.setStyle(Paint.Style.FILL);
        p.setColor(bright);
        c.drawCircle(X(0), Y(0, bob), s * 0.056f, p);

        // front limbs
        p.setStyle(Paint.Style.STROKE);
        p.setColor(front);
        limb(c, 7, 10, lw * 1.25f, 0);
        limb(c, 10, 11, lw, 0);
        limb(c, 11, 13, lw * 0.58f, 0);
        p.setColor(bright);
        limb(c, 2, 5, lw * 0.95f, bob);
        limb(c, 5, 6, lw * 0.8f, bob);

        if (animStart != 0 || breathe) postInvalidateOnAnimation();
    }

    private void drawLimbs(Canvas c, float lw, float tw, float bob, boolean all) {
        int[][] segs = {{7, 8}, {8, 9}, {7, 10}, {10, 11}, {2, 3}, {3, 4}, {2, 5}, {5, 6}, {2, 14}, {14, 7}, {1, 2}};
        p.setStrokeWidth(lw);
        for (int[] sg : segs) {
            c.drawLine(X(sg[0]), Y(sg[0], bob), X(sg[1]), Y(sg[1], bob), p);
        }
        c.drawCircle(X(0), Y(0, bob), s * 0.1f, p);
    }

    private void limb(Canvas c, int a, int b, float width, float bob) {
        p.setStrokeWidth(width);
        c.drawLine(X(a), Y(a, bob), X(b), Y(b, bob), p);
    }

    private float X(int j) {
        return gx + cur[j * 2] * s;
    }

    private float Y(int j, float bob) {
        // only the upper body rises with the breath
        float lift = (j <= 6 || j == 14) ? bob : 0;
        return gy - (cur[j * 2 + 1] + lift) * s;
    }
}
