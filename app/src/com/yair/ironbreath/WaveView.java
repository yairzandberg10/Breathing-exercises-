package com.yair.ironbreath;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Layered, flowing waveform tinted by whatever soundscapes are currently mixed in. */
final class WaveView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private float energy, t;
    private long last;
    int[] colors = new int[0];
    float[] levels = new float[0];

    WaveView(Context c) {
        super(c);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas c) {
        long now = System.currentTimeMillis();
        float dt = last == 0 ? 0.016f : Math.min(0.05f, (now - last) / 1000f);
        last = now;
        t += dt;
        float target = 0;
        for (float l : levels) target += l;
        target = Math.min(1.4f, target);
        energy += (target - energy) * Math.min(1, dt * 2.5f);
        float w = getWidth(), h = getHeight(), mid = h / 2f;
        int n = Math.max(1, colors.length);
        for (int k = 0; k < n + 1; k++) {
            int col = colors.length == 0 ? 0x33FFFFFF : (k < colors.length ? colors[k] : Ui.BRASS);
            float amp = (h * 0.38f) * (0.12f + energy * 0.7f) * (1f - k * 0.12f);
            float f = 1.3f + k * 0.55f;
            float sp = 0.9f + k * 0.35f;
            path.reset();
            for (int x = 0; x <= (int) w; x += Ui.dp(3)) {
                float xn = x / w;
                float env = (float) Math.sin(Math.PI * xn);
                float y = mid + (float) (Math.sin(xn * f * 6.283 + t * sp + k) * 0.7 + Math.sin(xn * f * 13.1 - t * sp * 1.7) * 0.3) * amp * env;
                if (x == 0) path.moveTo(x, y);
                else path.lineTo(x, y);
            }
            p.setStrokeWidth(Ui.dp(k == 0 ? 2.4f : 1.4f));
            p.setColor(Ui.alpha(col, k == 0 ? 0.95f : 0.5f));
            c.drawPath(path, p);
        }
        postInvalidateOnAnimation();
    }
}
