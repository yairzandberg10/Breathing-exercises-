package com.yair.ironbreath;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import java.util.Random;

/**
 * The main breathing visual: a dive-watch bezel whose ticks fill with session progress, a glowing
 * phase arc, a molten orb that expands and contracts with the breath, and particles that get pulled
 * in on the inhale and pushed out on the exhale. Box breathing traces a square instead.
 */
final class BreathView extends View {
    interface Driver {
        void frame(BreathView v, long nowMs);
    }

    Driver driver;
    int color = Ui.EMBER;
    int mode = Content.MODE_ORB;
    float scale = 0.55f, phaseProgress, totalProgress;
    int phaseType = -1;
    int boxSide;
    String label = "", big = "", sub = "";
    boolean compact;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private RadialGradient orbShader, haloShader, bgShader;
    private int shaderColor;
    private float cx, cy, R;
    private static final int PN = 70;
    private final float[] pa = new float[PN], pr = new float[PN], ps = new float[PN], pv = new float[PN];
    private final float[] echo = new float[4];
    private final float[] echoA = new float[4];
    private int lastType = -1;
    private long last;

    BreathView(Context c) {
        super(c);
        Random r = new Random(5);
        for (int i = 0; i < PN; i++) {
            pa[i] = r.nextFloat() * 6.2832f;
            pr[i] = 0.5f + r.nextFloat() * 0.5f;
            ps[i] = 0.5f + r.nextFloat();
            pv[i] = r.nextFloat();
        }
        tp.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        cx = w / 2f;
        cy = h / 2f;
        R = Math.min(w, h) / 2f - Ui.dp(6);
        shaderColor = 0;
    }

    private void shaders() {
        if (shaderColor == color && orbShader != null) return;
        shaderColor = color;
        float orbR = R * 0.62f;
        orbShader = new RadialGradient(cx - orbR * 0.25f, cy - orbR * 0.3f, orbR * 1.25f,
                new int[]{Ui.mix(color, 0xFFFFF4E0, 0.55f), color, Ui.mix(color, 0xFF000000, 0.55f), Ui.mix(color, 0xFF000000, 0.8f)},
                new float[]{0f, 0.35f, 0.8f, 1f}, Shader.TileMode.CLAMP);
        haloShader = new RadialGradient(cx, cy, orbR * 1.6f,
                new int[]{Ui.alpha(color, 0.55f), Ui.alpha(color, 0.18f), Ui.alpha(color, 0f)},
                new float[]{0.55f, 0.72f, 1f}, Shader.TileMode.CLAMP);
        bgShader = new RadialGradient(cx, cy, R * 1.2f,
                new int[]{Ui.alpha(color, 0.16f), Ui.alpha(color, 0.05f), 0x00000000},
                new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP);
    }

    @Override
    protected void onDraw(Canvas c) {
        long now = System.currentTimeMillis();
        float dt = last == 0 ? 0.016f : Math.min(0.05f, (now - last) / 1000f);
        last = now;
        if (driver != null) driver.frame(this, now);
        shaders();
        float t = now / 1000f;

        // ambient glow
        p.setShader(bgShader);
        p.setStyle(Paint.Style.FILL);
        c.drawCircle(cx, cy, R * 1.2f, p);
        p.setShader(null);

        if (compact) {
            drawOrb(c, scale, t);
            postInvalidateOnAnimation();
            return;
        }
        // bezel ticks, filled up to session progress
        int ticks = 72;
        for (int i = 0; i < ticks; i++) {
            double a = -Math.PI / 2 + i * Math.PI * 2 / ticks;
            boolean major = i % 6 == 0;
            float r1 = R * (major ? 0.905f : 0.93f), r2 = R * 0.985f;
            boolean lit = (float) i / ticks < totalProgress;
            p.setStrokeWidth(Ui.dp(major ? 2.4f : 1.3f));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(lit ? Ui.mix(color, 0xFFFFFFFF, 0.2f) : (major ? Ui.alpha(Ui.BRASS, 0.55f) : 0x33FFFFFF));
            float cs = (float) Math.cos(a), sn = (float) Math.sin(a);
            c.drawLine(cx + cs * r1, cy + sn * r1, cx + cs * r2, cy + sn * r2, p);
        }

        // rotating dotted inner ring
        c.save();
        c.rotate(t * 6f, cx, cy);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x26FFFFFF);
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2 / 48;
            c.drawCircle(cx + (float) Math.cos(a) * R * 0.77f, cy + (float) Math.sin(a) * R * 0.77f, Ui.dp(i % 4 == 0 ? 1.6f : 0.9f), p);
        }
        c.restore();

        // phase arc with glow
        float ar = R * 0.845f;
        rf.set(cx - ar, cy - ar, cx + ar, cy + ar);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(Ui.dp(3));
        p.setColor(0x14FFFFFF);
        c.drawCircle(cx, cy, ar, p);
        if (phaseProgress > 0) {
            float sweep = 360f * Math.min(1f, phaseProgress);
            p.setColor(Ui.alpha(color, 0.12f));
            p.setStrokeWidth(Ui.dp(12));
            c.drawArc(rf, -90, sweep, false, p);
            p.setColor(Ui.alpha(color, 0.25f));
            p.setStrokeWidth(Ui.dp(7));
            c.drawArc(rf, -90, sweep, false, p);
            p.setColor(Ui.mix(color, 0xFFFFFFFF, 0.25f));
            p.setStrokeWidth(Ui.dp(3));
            c.drawArc(rf, -90, sweep, false, p);
            double ea = Math.toRadians(-90 + sweep);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFFFF4E0);
            c.drawCircle(cx + (float) Math.cos(ea) * ar, cy + (float) Math.sin(ea) * ar, Ui.dp(4), p);
        }

        // echo rings when a new exhale starts
        if (phaseType != lastType) {
            if (phaseType == Content.OUT) {
                for (int i = 0; i < echo.length; i++) {
                    if (echoA[i] <= 0) {
                        echo[i] = scale;
                        echoA[i] = 1f;
                        break;
                    }
                }
            }
            lastType = phaseType;
        }
        p.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < echo.length; i++) {
            if (echoA[i] > 0) {
                echo[i] += dt * 0.35f;
                echoA[i] -= dt * 0.55f;
                p.setStrokeWidth(Ui.dp(1.5f));
                p.setColor(Ui.alpha(color, Math.max(0, echoA[i]) * 0.6f));
                c.drawCircle(cx, cy, R * 0.62f * echo[i], p);
            }
        }

        float orbScale = mode == Content.MODE_BOX ? scale * 0.62f : scale;

        // particles
        boolean inhale = phaseType == Content.IN || phaseType == Content.IN2;
        boolean exhale = phaseType == Content.OUT;
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < PN; i++) {
            float minR = orbScale * 0.62f + 0.04f;
            if (inhale) pr[i] -= dt * 0.22f * ps[i];
            else if (exhale) pr[i] += dt * 0.16f * ps[i];
            else pr[i] += (float) Math.sin(t * 2 + pv[i] * 6) * dt * 0.01f;
            pa[i] += dt * (0.08f + pv[i] * 0.12f) * (inhale ? 2.2f : 1f);
            if (pr[i] < minR) pr[i] = 0.72f + pv[i] * 0.12f;
            if (pr[i] > 0.84f) pr[i] = minR + 0.02f;
            float rr = pr[i] * R;
            float px = cx + (float) Math.cos(pa[i]) * rr, py = cy + (float) Math.sin(pa[i]) * rr;
            float depth = (pr[i] - minR) / Math.max(0.05f, 0.84f - minR);
            float a = (inhale ? 1f - depth : exhale ? depth : 0.5f) * 0.55f + 0.1f;
            p.setColor(Ui.alpha(Ui.mix(color, 0xFFFFFFFF, 0.4f), Math.max(0, Math.min(1, a))));
            c.drawCircle(px, py, Ui.dp(0.8f + ps[i] * 1.2f), p);
        }

        // box track
        if (mode == Content.MODE_BOX) {
            float half = R * 0.5f;
            rf.set(cx - half, cy - half, cx + half, cy + half);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Ui.dp(2));
            p.setColor(0x22FFFFFF);
            c.drawRoundRect(rf, Ui.dp(10), Ui.dp(10), p);
            if (phaseType >= 0) {
                // corners: start bottom-right, go up (inhale), across top (hold), down (exhale), across bottom (hold)
                float[][] corner = {{cx + half, cy + half}, {cx + half, cy - half}, {cx - half, cy - half}, {cx - half, cy + half}};
                int s = boxSide & 3;
                float[] a0 = corner[s], a1 = corner[(s + 1) & 3];
                float pp = Math.min(1f, phaseProgress);
                float dx = a0[0] + (a1[0] - a0[0]) * pp, dy = a0[1] + (a1[1] - a0[1]) * pp;
                p.setStrokeWidth(Ui.dp(4));
                p.setColor(Ui.mix(color, 0xFFFFFFFF, 0.2f));
                for (int k = 0; k < s; k++) {
                    float[] b0 = corner[k], b1 = corner[k + 1];
                    p.setColor(Ui.alpha(color, 0.45f));
                    c.drawLine(b0[0], b0[1], b1[0], b1[1], p);
                }
                p.setColor(Ui.mix(color, 0xFFFFFFFF, 0.2f));
                c.drawLine(a0[0], a0[1], dx, dy, p);
                p.setStyle(Paint.Style.FILL);
                p.setColor(Ui.alpha(color, 0.3f));
                c.drawCircle(dx, dy, Ui.dp(12), p);
                p.setColor(0xFFFFF4E0);
                c.drawCircle(dx, dy, Ui.dp(5), p);
            }
        }

        drawOrb(c, orbScale, t);

        // text
        {
            tp.setTypeface(Ui.BOLD);
            tp.setColor(0xFFFFFFFF);
            tp.setTextSize(Ui.dp(24));
            tp.setShadowLayer(Ui.dp(8), 0, 0, 0x99000000);
            if (label.length() > 0) c.drawText(label, cx, cy - R * 0.06f, tp);
            if (big.length() > 0) {
                tp.setTypeface(Ui.NUM);
                tp.setTextSize(Ui.dp(46));
                c.drawText(big, cx, cy + R * 0.2f, tp);
            }
            if (sub != null && sub.length() > 0) {
                tp.setTypeface(Ui.REG);
                tp.setTextSize(Ui.dp(13));
                tp.setColor(0xDDFFFFFF);
                c.drawText(sub, cx, cy + R * 0.33f, tp);
            }
            tp.clearShadowLayer();
        }
        postInvalidateOnAnimation();
    }

    private void drawOrb(Canvas c, float orbScale, float t) {
        p.setStyle(Paint.Style.FILL);
        c.save();
        c.scale(orbScale, orbScale, cx, cy);
        p.setShader(haloShader);
        c.drawCircle(cx, cy, R * 0.62f * 1.6f, p);
        p.setShader(orbShader);
        c.drawCircle(cx, cy, R * 0.62f, p);
        p.setShader(null);
        // molten surface bands
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Ui.dp(1.2f) / Math.max(0.2f, orbScale));
        for (int k = 0; k < 3; k++) {
            float rr = R * 0.62f * (0.35f + 0.2f * k + 0.04f * (float) Math.sin(t * 0.8f + k));
            p.setColor(Ui.alpha(0xFFFFFFFF, 0.06f + 0.03f * k));
            c.drawCircle(cx + (float) Math.sin(t * 0.4f + k) * Ui.dp(3), cy, rr, p);
        }
        c.restore();
    }

    // ------------------------------------------------------------------ preview helper
    static float target(int type, float from) {
        switch (type) {
            case Content.IN: return 1f;
            case Content.IN2: return 1.12f;
            case Content.OUT: return 0.42f;
            default: return from;
        }
    }

    static float ease(float x) {
        return (float) (0.5 - 0.5 * Math.cos(Math.PI * Math.max(0, Math.min(1, x))));
    }

    /** Orb scale of a technique at time t (seconds), used by the small live previews. */
    static float previewScale(Content.Technique tech, float t) {
        float total = 0;
        int n = Math.min(tech.round.size(), 12);
        for (int i = 0; i < n; i++) total += dur(tech.round.get(i));
        t = t % total;
        float s = 0.42f;
        for (int i = 0; i < n; i++) {
            Content.Phase ph = tech.round.get(i);
            float d = dur(ph);
            float tgt = target(ph.type, s);
            if (t < d) return s + (tgt - s) * ease(t / d);
            s = tgt;
            t -= d;
        }
        return s;
    }

    private static float dur(Content.Phase p) {
        return p.type == Content.OPEN ? 3f : Math.max(0.35f, p.sec);
    }
}
