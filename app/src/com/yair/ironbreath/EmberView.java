package com.yair.ironbreath;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import java.util.Random;

/** Glowing embers drifting upward, with an optional celebratory burst. */
final class EmberView extends View {
    private static final int MAX = 90;
    private final float[] x = new float[MAX], y = new float[MAX], vx = new float[MAX], vy = new float[MAX],
            life = new float[MAX], size = new float[MAX], seed = new float[MAX];
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rnd = new Random();
    private long last;
    int color = Ui.EMBER;
    int ambient = 34;

    EmberView(Context c) {
        super(c);
    }

    private void spawn(int i, boolean anywhere) {
        int w = Math.max(1, getWidth()), h = Math.max(1, getHeight());
        x[i] = rnd.nextFloat() * w;
        y[i] = anywhere ? rnd.nextFloat() * h : h + rnd.nextFloat() * Ui.dp(20);
        vx[i] = (rnd.nextFloat() - 0.5f) * Ui.dp(10);
        vy[i] = -Ui.dp(14 + rnd.nextFloat() * 36);
        life[i] = 0.4f + rnd.nextFloat() * 0.6f;
        size[i] = Ui.dp(0.8f + rnd.nextFloat() * 2.2f);
        seed[i] = rnd.nextFloat() * 10f;
    }

    /** Fountain of sparks from a point, used when a session is completed. */
    void burst(float cx, float cy) {
        for (int i = 0; i < MAX; i++) {
            double a = rnd.nextFloat() * Math.PI * 2;
            float sp = Ui.dp(60 + rnd.nextFloat() * 220);
            x[i] = cx;
            y[i] = cy;
            vx[i] = (float) Math.cos(a) * sp;
            vy[i] = (float) Math.sin(a) * sp - Ui.dp(60);
            life[i] = 1f;
            size[i] = Ui.dp(1.2f + rnd.nextFloat() * 2.5f);
            seed[i] = rnd.nextFloat() * 10f;
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        for (int i = 0; i < ambient; i++) spawn(i, true);
    }

    @Override
    protected void onDraw(Canvas c) {
        long now = System.nanoTime();
        float dt = last == 0 ? 0.016f : Math.min(0.05f, (now - last) / 1e9f);
        last = now;
        float t = now / 1e9f;
        for (int i = 0; i < MAX; i++) {
            if (life[i] <= 0) {
                if (i < ambient) spawn(i, false);
                else continue;
            }
            x[i] += (vx[i] + (float) Math.sin(t * 1.3f + seed[i]) * Ui.dp(8)) * dt;
            y[i] += vy[i] * dt;
            if (i >= ambient) vy[i] += Ui.dp(40) * dt;
            life[i] -= dt * (i < ambient ? 0.12f : 0.45f);
            if (y[i] < -Ui.dp(10)) life[i] = 0;
            float flick = 0.65f + 0.35f * (float) Math.sin(t * 9 + seed[i] * 3);
            float a = Math.max(0, Math.min(1, life[i])) * flick;
            p.setColor(Ui.alpha(color, a * 0.25f));
            c.drawCircle(x[i], y[i], size[i] * 3.2f, p);
            p.setColor(Ui.alpha(Ui.mix(color, 0xFFFFE2B0, 0.5f), a));
            c.drawCircle(x[i], y[i], size[i], p);
        }
        postInvalidateOnAnimation();
    }
}
