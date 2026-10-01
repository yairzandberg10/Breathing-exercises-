package com.yair.ironbreath;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Animated emblem for each soundscape. It moves only while its sound is playing. */
final class SoundIconView extends View {
    final int id;
    boolean active;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rf = new RectF();
    private float t;
    private long last;

    SoundIconView(Context c, int id) {
        super(c);
        this.id = id;
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    void setActive(boolean a) {
        active = a;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        long now = System.currentTimeMillis();
        if (active) t += last == 0 ? 0 : Math.min(0.05f, (now - last) / 1000f);
        last = now;
        float w = getWidth(), h = getHeight();
        float u = Math.min(w, h) / 48f;
        c.save();
        c.translate((w - 48 * u) / 2, (h - 48 * u) / 2);
        c.scale(u, u);
        int col = Content.SOUND_COLORS[id];
        int main = active ? Ui.mix(col, 0xFFFFFFFF, 0.15f) : Ui.alpha(col, 0.75f);
        p.setColor(main);
        p.setStrokeWidth(2.2f);
        p.setStyle(Paint.Style.STROKE);
        path.reset();
        switch (id) {
            case Content.S_RAIN:
            case Content.S_STORM: {
                path.moveTo(10, 22);
                path.cubicTo(4, 22, 4, 13, 11, 13);
                path.cubicTo(12, 5, 25, 4, 28, 11);
                path.cubicTo(36, 8, 42, 15, 38, 22);
                path.close();
                c.drawPath(path, p);
                if (id == Content.S_RAIN) {
                    for (int i = 0; i < 6; i++) {
                        float x = 11 + i * 5;
                        float y = 26 + ((t * 30 + i * 7.3f) % 18);
                        p.setAlpha((int) (255 * (1 - (y - 26) / 18f)));
                        c.drawLine(x, y, x - 1.5f, y + 4, p);
                    }
                } else {
                    float flash = active ? (float) Math.max(0, Math.sin(t * 1.7) * Math.sin(t * 5.3)) : 0.4f;
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(Ui.alpha(0xFFEFF3D8, 0.35f + 0.65f * flash));
                    path.reset();
                    path.moveTo(24, 22);
                    path.lineTo(17, 34);
                    path.lineTo(23, 34);
                    path.lineTo(20, 44);
                    path.lineTo(30, 30);
                    path.lineTo(24, 30);
                    path.lineTo(28, 22);
                    path.close();
                    c.drawPath(path, p);
                }
                break;
            }
            case Content.S_OCEAN:
                for (int k = 0; k < 3; k++) {
                    path.reset();
                    float y0 = 16 + k * 9;
                    for (int x = 4; x <= 44; x += 2) {
                        float y = y0 + (float) Math.sin(x * 0.28f + t * (1.6f + k * 0.4f) + k) * (3 - k * 0.6f);
                        if (x == 4) path.moveTo(x, y);
                        else path.lineTo(x, y);
                    }
                    p.setAlpha(255 - k * 60);
                    c.drawPath(path, p);
                }
                break;
            case Content.S_FIRE:
                for (int k = 0; k < 3; k++) {
                    float sc = 1f - k * 0.28f;
                    float fl = active ? (float) Math.sin(t * (7 + k * 3)) * 1.5f : 0;
                    path.reset();
                    path.moveTo(24 - 11 * sc, 42);
                    path.cubicTo(24 - 14 * sc, 30, 24 - 4 * sc + fl, 24, 24 + fl * 0.5f, 42 - 34 * sc);
                    path.cubicTo(24 + 6 * sc - fl, 24, 24 + 14 * sc, 30, 24 + 11 * sc, 42);
                    path.close();
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(Ui.mix(col, 0xFFFFE0A0, k * 0.4f));
                    p.setAlpha(active ? 230 : 150);
                    c.drawPath(path, p);
                }
                p.setStyle(Paint.Style.STROKE);
                p.setColor(Ui.alpha(0xFF6A3A20, 1f));
                c.drawLine(10, 44, 38, 40, p);
                c.drawLine(10, 40, 38, 44, p);
                break;
            case Content.S_WIND:
                for (int k = 0; k < 3; k++) {
                    float off = (t * 14 + k * 11) % 12;
                    float y = 14 + k * 10;
                    float x0 = 4 + off, x1 = 34 + off * 0.3f;
                    path.reset();
                    path.moveTo(x0, y);
                    path.lineTo(x1, y);
                    rf.set(x1 - 4, y - 8, x1 + 4, y);
                    path.arcTo(rf, 90, -250);
                    p.setAlpha(255 - k * 50);
                    c.drawPath(path, p);
                }
                break;
            case Content.S_NIGHT:
                path.moveTo(30, 6);
                rf.set(12, 6, 36, 30);
                path.arcTo(rf, -80, -240);
                path.quadTo(20, 18, 30, 6);
                p.setStyle(Paint.Style.FILL);
                c.drawPath(path, p);
                for (int i = 0; i < 6; i++) {
                    float tw = 0.5f + 0.5f * (float) Math.sin(t * 3 + i * 1.7f);
                    p.setColor(Ui.alpha(0xFFFFFFFF, active ? tw : 0.5f));
                    c.drawCircle(6 + i * 7.5f, 36 + (i % 2) * 5, 1.4f, p);
                }
                break;
            case Content.S_BOWLS:
                rf.set(8, 14, 40, 44);
                c.drawArc(rf, 0, 180, false, p);
                c.drawLine(6, 29, 42, 29, p);
                for (int k = 0; k < 3; k++) {
                    float ph = ((t * 0.5f + k / 3f) % 1f);
                    float r = 6 + ph * 18;
                    p.setAlpha((int) (255 * (1 - ph) * (active ? 1 : 0.5f)));
                    rf.set(24 - r, 26 - r * 0.6f, 24 + r, 26 + r * 0.2f);
                    c.drawArc(rf, 200, 140, false, p);
                }
                break;
            case Content.S_DRONE:
                for (int k = 0; k < 4; k++) {
                    path.reset();
                    for (int x = 4; x <= 44; x += 2) {
                        float y = 24 + (float) Math.sin(x * (0.1f + k * 0.05f) + t * (0.8f + k * 0.3f)) * (10 - k * 2);
                        if (x == 4) path.moveTo(x, y);
                        else path.lineTo(x, y);
                    }
                    p.setAlpha(230 - k * 45);
                    c.drawPath(path, p);
                }
                break;
            case Content.S_BROWN: {
                path.reset();
                java.util.Random r = new java.util.Random(active ? (long) (t * 12) : 3);
                float y = 24;
                for (int x = 4; x <= 44; x += 2) {
                    y += (r.nextFloat() - 0.5f) * 6;
                    y += (24 - y) * 0.2f;
                    if (x == 4) path.moveTo(x, y);
                    else path.lineTo(x, y);
                }
                c.drawPath(path, p);
                break;
            }
            default: {
                float f = id == Content.S_ALPHA ? 0.5f : id == Content.S_THETA ? 0.36f : 0.22f;
                for (int k = 0; k < 2; k++) {
                    path.reset();
                    for (int x = 4; x <= 44; x += 2) {
                        float y = 24 + (float) Math.sin(x * f * (1 + k * 0.12f) + t * 3) * 9;
                        if (x == 4) path.moveTo(x, y);
                        else path.lineTo(x, y);
                    }
                    p.setColor(k == 0 ? main : Ui.alpha(0xFFFFFFFF, 0.55f));
                    c.drawPath(path, p);
                }
            }
        }
        c.restore();
        if (active) postInvalidateOnAnimation();
    }
}
