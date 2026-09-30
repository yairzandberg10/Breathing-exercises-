package com.yair.ironbreath;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** Meditation visual: a slowly breathing aura, a turning mandala of dots and a progress ring. */
final class AuraView extends View {
    int color = Ui.EMBER;
    float progress;
    boolean paused;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private RadialGradient core;
    private int coreColor;
    private float phase;
    private long last;

    AuraView(Context c) {
        super(c);
        p.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas c) {
        long now = System.currentTimeMillis();
        float dt = last == 0 ? 0.016f : Math.min(0.05f, (now - last) / 1000f);
        last = now;
        if (!paused) phase += dt;
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float R = Math.min(cx, cy) - Ui.dp(4);
        // 4s in, 6s out breathing guide
        float cyc = phase % 10f;
        float b = cyc < 4 ? BreathView.ease(cyc / 4f) : 1f - BreathView.ease((cyc - 4f) / 6f);

        if (core == null || coreColor != color) {
            coreColor = color;
            core = new RadialGradient(cx, cy, R * 0.55f,
                    new int[]{Ui.alpha(Ui.mix(color, 0xFFFFFFFF, 0.5f), 0.9f), Ui.alpha(color, 0.45f), Ui.alpha(color, 0f)},
                    new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP);
        }
        p.setStyle(Paint.Style.FILL);
        c.save();
        float sc = 0.75f + 0.3f * b;
        c.scale(sc, sc, cx, cy);
        p.setShader(core);
        c.drawCircle(cx, cy, R * 0.55f, p);
        p.setShader(null);
        c.restore();

        // concentric breathing rings
        p.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < 4; i++) {
            float rr = R * (0.3f + i * 0.12f) * (0.85f + 0.15f * b);
            p.setStrokeWidth(Ui.dp(1));
            p.setColor(Ui.alpha(color, 0.35f - i * 0.07f));
            c.drawCircle(cx, cy, rr, p);
        }

        // mandala: two counter-rotating rings of dots
        p.setStyle(Paint.Style.FILL);
        for (int ring = 0; ring < 2; ring++) {
            int n = ring == 0 ? 12 : 24;
            float rr = R * (ring == 0 ? 0.52f : 0.68f) * (0.95f + 0.05f * b);
            float rot = phase * (ring == 0 ? 0.08f : -0.05f);
            for (int i = 0; i < n; i++) {
                double a = rot + i * Math.PI * 2 / n;
                p.setColor(Ui.alpha(ring == 0 ? Ui.BRASS : color, ring == 0 ? 0.7f : 0.45f));
                c.drawCircle(cx + (float) Math.cos(a) * rr, cy + (float) Math.sin(a) * rr, Ui.dp(ring == 0 ? 2.6f : 1.4f), p);
            }
        }

        // progress ring
        float pr = R * 0.9f;
        rf.set(cx - pr, cy - pr, cx + pr, cy + pr);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Ui.dp(2));
        p.setColor(0x1AFFFFFF);
        c.drawCircle(cx, cy, pr, p);
        p.setStrokeWidth(Ui.dp(3));
        p.setColor(Ui.mix(color, 0xFFFFFFFF, 0.2f));
        c.drawArc(rf, -90, 360 * progress, false, p);
        postInvalidateOnAnimation();
    }
}
