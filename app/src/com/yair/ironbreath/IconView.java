package com.yair.ironbreath;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Hand-drawn vector glyphs on a 24-unit grid (no icon fonts, no generic icon set). */
final class IconView extends View {
    static final int HOME = 0, BREATH = 1, SOUND = 2, MEDITATE = 3, YOGA = 4, PLAY = 5, PAUSE = 6, CLOSE = 7,
            LEFT = 8, RIGHT = 9, VOICE = 10, VIBRATE = 11, PLUS = 12, MINUS = 13, STOP = 14, TIMER = 15,
            FLAME = 16, MOON = 17, BOLT = 18, CHECK = 19, NOTE = 20;

    int type;
    int color;
    float stroke = 2f;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rf = new RectF();

    IconView(Context c, int type, int color) {
        super(c);
        this.type = type;
        this.color = color;
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    void set(int type, int color) {
        this.type = type;
        this.color = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float s = Math.min(getWidth(), getHeight()) / 24f;
        c.save();
        c.translate((getWidth() - 24 * s) / 2f, (getHeight() - 24 * s) / 2f);
        c.scale(s, s);
        p.setColor(color);
        p.setStrokeWidth(stroke);
        p.setStyle(Paint.Style.STROKE);
        path.reset();
        switch (type) {
            case HOME:
                path.moveTo(2, 20);
                path.lineTo(9, 8.5f);
                path.lineTo(13, 14.5f);
                path.lineTo(16, 10.5f);
                path.lineTo(22, 20);
                path.close();
                c.drawPath(path, p);
                c.drawCircle(17.5f, 5, 2, p);
                break;
            case BREATH:
                path.moveTo(3, 9);
                path.lineTo(15, 9);
                rf.set(12, 3, 18, 9);
                path.arcTo(rf, 90, -270);
                path.moveTo(3, 13.5f);
                path.lineTo(20, 13.5f);
                path.moveTo(3, 18);
                path.lineTo(12, 18);
                rf.set(9, 18, 15, 24);
                path.arcTo(rf, -90, 250);
                c.drawPath(path, p);
                break;
            case SOUND: {
                float[] hs = {5, 11, 17, 9, 4};
                for (int i = 0; i < 5; i++) {
                    float x = 4 + i * 4;
                    c.drawLine(x, 12 - hs[i] / 2, x, 12 + hs[i] / 2, p);
                }
                break;
            }
            case MEDITATE:
                c.drawCircle(12, 4.8f, 2.3f, p);
                path.moveTo(12, 8.5f);
                path.lineTo(12, 15);
                path.moveTo(5.5f, 15.5f);
                path.quadTo(8, 10, 12, 10);
                path.quadTo(16, 10, 18.5f, 15.5f);
                path.moveTo(3, 19.5f);
                path.quadTo(12, 14.5f, 21, 19.5f);
                c.drawPath(path, p);
                break;
            case YOGA:
                c.drawCircle(12, 4.5f, 2.2f, p);
                path.moveTo(2.5f, 9);
                path.lineTo(21.5f, 9);
                path.moveTo(12, 7.5f);
                path.lineTo(12, 13.5f);
                path.lineTo(6.5f, 21);
                path.moveTo(12, 13.5f);
                path.lineTo(17, 16.5f);
                path.lineTo(17.5f, 21);
                c.drawPath(path, p);
                break;
            case PLAY:
                p.setStyle(Paint.Style.FILL_AND_STROKE);
                path.moveTo(8, 5);
                path.lineTo(19, 12);
                path.lineTo(8, 19);
                path.close();
                c.drawPath(path, p);
                break;
            case PAUSE:
                p.setStrokeWidth(3.2f);
                c.drawLine(8.5f, 5.5f, 8.5f, 18.5f, p);
                c.drawLine(15.5f, 5.5f, 15.5f, 18.5f, p);
                break;
            case CLOSE:
                c.drawLine(6, 6, 18, 18, p);
                c.drawLine(18, 6, 6, 18, p);
                break;
            case LEFT:
                path.moveTo(15, 5);
                path.lineTo(8, 12);
                path.lineTo(15, 19);
                c.drawPath(path, p);
                break;
            case RIGHT:
                path.moveTo(9, 5);
                path.lineTo(16, 12);
                path.lineTo(9, 19);
                c.drawPath(path, p);
                break;
            case VOICE:
                path.moveTo(3, 9.5f);
                path.lineTo(7, 9.5f);
                path.lineTo(12, 5);
                path.lineTo(12, 19);
                path.lineTo(7, 14.5f);
                path.lineTo(3, 14.5f);
                path.close();
                c.drawPath(path, p);
                rf.set(10, 8, 18, 16);
                c.drawArc(rf, -50, 100, false, p);
                rf.set(9, 4, 23, 20);
                c.drawArc(rf, -50, 100, false, p);
                break;
            case VIBRATE:
                rf.set(8, 4, 16, 20);
                c.drawRoundRect(rf, 2, 2, p);
                c.drawLine(4.5f, 9, 4.5f, 15, p);
                c.drawLine(19.5f, 9, 19.5f, 15, p);
                c.drawLine(1.5f, 10.5f, 1.5f, 13.5f, p);
                c.drawLine(22.5f, 10.5f, 22.5f, 13.5f, p);
                break;
            case PLUS:
                c.drawLine(12, 5, 12, 19, p);
                c.drawLine(5, 12, 19, 12, p);
                break;
            case MINUS:
                c.drawLine(5, 12, 19, 12, p);
                break;
            case STOP:
                p.setStyle(Paint.Style.FILL_AND_STROKE);
                rf.set(6.5f, 6.5f, 17.5f, 17.5f);
                c.drawRoundRect(rf, 2, 2, p);
                break;
            case TIMER:
                c.drawCircle(12, 13, 8, p);
                c.drawLine(12, 13, 12, 8.5f, p);
                c.drawLine(12, 13, 15, 15, p);
                c.drawLine(10, 2.5f, 14, 2.5f, p);
                break;
            case FLAME:
                path.moveTo(12, 2.5f);
                path.cubicTo(16, 7, 19, 10, 19, 14.5f);
                path.cubicTo(19, 18.5f, 16, 21.5f, 12, 21.5f);
                path.cubicTo(8, 21.5f, 5, 18.5f, 5, 14.5f);
                path.cubicTo(5, 11.5f, 7, 9.5f, 8.5f, 8);
                path.cubicTo(9, 10.5f, 10, 11.5f, 11, 12);
                path.cubicTo(11, 8.5f, 11, 5.5f, 12, 2.5f);
                c.drawPath(path, p);
                break;
            case MOON:
                path.moveTo(16.5f, 4.2f);
                rf.set(3, 3, 21, 21);
                path.arcTo(rf, -60, -240);
                path.quadTo(8, 12, 16.5f, 4.2f);
                c.drawPath(path, p);
                break;
            case BOLT:
                path.moveTo(13.5f, 2.5f);
                path.lineTo(5.5f, 13.5f);
                path.lineTo(11.5f, 13.5f);
                path.lineTo(10.5f, 21.5f);
                path.lineTo(18.5f, 10.5f);
                path.lineTo(12.5f, 10.5f);
                path.close();
                c.drawPath(path, p);
                break;
            case CHECK:
                path.moveTo(4.5f, 12.5f);
                path.lineTo(9.5f, 17.5f);
                path.lineTo(19.5f, 6.5f);
                c.drawPath(path, p);
                break;
            case NOTE:
                c.drawLine(9, 17, 9, 4.5f, p);
                c.drawLine(9, 4.5f, 19, 3, p);
                c.drawLine(19, 3, 19, 15.5f, p);
                c.drawCircle(6.5f, 17.5f, 2.5f, p);
                c.drawCircle(16.5f, 16, 2.5f, p);
                break;
        }
        c.restore();
    }
}
