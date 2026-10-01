package com.yair.ironbreath;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;

/** Shared palette, typography and small view-building helpers. */
final class Ui {
    static final int BG = 0xFF0C1214;
    static final int BG2 = 0xFF111A1C;
    static final int CARD = 0xFF152023;
    static final int CARD2 = 0xFF1C292C;
    static final int LINE = 0x1FFFFFFF;
    static final int TEXT = 0xFFE8E6DE;
    static final int MUTED = 0xFF8A9792;
    static final int DIM = 0xFF55625E;
    static final int EMBER = 0xFF8CC4AC;
    static final int BRASS = 0xFFCDB68C;
    static final int STEEL = 0xFF86AECB;
    static final int MOSS = 0xFFA8BF8E;
    static final int BLOOD = 0xFFCF8A74;
    static final int VIOLET = 0xFFA3A6D6;

    static Typeface REG, BOLD, SERIF, NUM, NUM_LIGHT;
    static float density = 3f;
    private static Context app;
    private static final HashMap<String, Bitmap> bitmaps = new HashMap<>();

    static void init(Context c) {
        app = c.getApplicationContext();
        density = c.getResources().getDisplayMetrics().density;
        if (REG != null) return;
        try {
            REG = Typeface.createFromAsset(c.getAssets(), "fonts/reg.ttf");
            BOLD = Typeface.createFromAsset(c.getAssets(), "fonts/bold.ttf");
            SERIF = Typeface.createFromAsset(c.getAssets(), "fonts/serif.ttf");
        } catch (Exception e) {
            REG = Typeface.DEFAULT;
            BOLD = Typeface.DEFAULT_BOLD;
            SERIF = Typeface.SERIF;
        }
        NUM = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        NUM_LIGHT = Typeface.create("sans-serif-light", Typeface.NORMAL);
    }

    static int dp(float v) {
        return Math.round(v * density);
    }

    static Bitmap bitmap(int resId, int sample) {
        String key = resId + ":" + sample;
        Bitmap b = bitmaps.get(key);
        if (b == null) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            b = BitmapFactory.decodeResource(app.getResources(), resId, o);
            bitmaps.put(key, b);
        }
        return b;
    }

    static int alpha(int color, float a) {
        return (Math.round(a * 255) << 24) | (color & 0x00FFFFFF);
    }

    static int mix(int c1, int c2, float t) {
        int r = (int) (Color.red(c1) + (Color.red(c2) - Color.red(c1)) * t);
        int g = (int) (Color.green(c1) + (Color.green(c2) - Color.green(c1)) * t);
        int b = (int) (Color.blue(c1) + (Color.blue(c2) - Color.blue(c1)) * t);
        int a = (int) (Color.alpha(c1) + (Color.alpha(c2) - Color.alpha(c1)) * t);
        return Color.argb(a, r, g, b);
    }

    static TextView text(Context c, CharSequence s, float sp, int color, Typeface tf) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(tf);
        t.setIncludeFontPadding(false);
        t.setLineSpacing(0, 1.12f);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        return t;
    }

    /** Centered text that fills its parent's width (children of vertical boxes stretch by default). */
    static TextView centered(Context c, CharSequence s, float sp, int color, Typeface tf) {
        TextView t = text(c, s, sp, color, tf);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    /** Small letter-spaced label, used as a "military stencil" tag. */
    static TextView tag(Context c, String s, int color) {
        TextView t = text(c, s, 11, color, NUM);
        t.setLetterSpacing(0.22f);
        t.setAllCaps(true);
        return t;
    }

    static CharSequence accent(String full, String part, int color) {
        SpannableString sp = new SpannableString(full);
        int i = full.lastIndexOf(part);
        if (i >= 0) sp.setSpan(new ForegroundColorSpan(color), i, i + part.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sp;
    }

    static GradientDrawable round(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    static GradientDrawable round(int color, float radiusDp, int stroke, float strokeDp) {
        GradientDrawable g = round(color, radiusDp);
        g.setStroke(Math.max(1, dp(strokeDp)), stroke);
        return g;
    }

    static GradientDrawable vgrad(int top, int bottom) {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bottom});
    }

    static void clip(View v, final float radiusDp) {
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(radiusDp));
            }
        });
        v.setClipToOutline(true);
    }

    static LinearLayout vbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout hbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    static LinearLayout.LayoutParams lpm(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    static FrameLayout.LayoutParams flp(int w, int h, int gravity) {
        return new FrameLayout.LayoutParams(w, h, gravity);
    }

    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    static ImageView image(Context c, int res, int sample) {
        ImageView iv = new ImageView(c);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setImageBitmap(bitmap(res, sample));
        return iv;
    }

    /** Slow "Ken Burns" drift for background imagery. */
    static void kenBurns(View v, long ms) {
        ObjectAnimator sx = ObjectAnimator.ofFloat(v, "scaleX", 1.02f, 1.14f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(v, "scaleY", 1.02f, 1.14f);
        ObjectAnimator tx = ObjectAnimator.ofFloat(v, "translationX", -dp(6), dp(10));
        for (ObjectAnimator o : new ObjectAnimator[]{sx, sy, tx}) {
            o.setDuration(ms);
            o.setRepeatMode(ValueAnimator.REVERSE);
            o.setRepeatCount(ValueAnimator.INFINITE);
        }
        AnimatorSet s = new AnimatorSet();
        s.playTogether(sx, sy, tx);
        s.start();
    }

    /** Staggered rise-in of the direct children of a container. */
    static void stagger(ViewGroup g, long startDelay) {
        for (int i = 0; i < g.getChildCount(); i++) {
            View v = g.getChildAt(i);
            v.setAlpha(0f);
            v.setTranslationY(dp(28));
            v.animate().alpha(1f).translationY(0).setStartDelay(startDelay + i * 70L)
                    .setDuration(520).setInterpolator(new DecelerateInterpolator(2f)).start();
        }
    }

    static void pop(View v) {
        v.setScaleX(0.6f);
        v.setScaleY(0.6f);
        v.setAlpha(0f);
        v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(420).setInterpolator(new OvershootInterpolator(2f)).start();
    }

    /** Tactile press feedback: shrink slightly while pressed. */
    static void pressable(final View v) {
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        view.animate().scaleX(0.965f).scaleY(0.965f).setDuration(110).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        view.animate().scaleX(1f).scaleY(1f).setDuration(220).setInterpolator(new OvershootInterpolator(3f)).start();
                        break;
                }
                return false;
            }
        });
    }

    /** Solid pill button. */
    static TextView button(Context c, String label, int bg, int fg) {
        TextView b = text(c, label, 16, fg, BOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(22), dp(15), dp(22), dp(15));
        b.setBackground(round(bg, 30));
        b.setClickable(true);
        pressable(b);
        return b;
    }

    static TextView ghostButton(Context c, String label, int color) {
        TextView b = text(c, label, 14, color, BOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(18), dp(11), dp(18), dp(11));
        b.setBackground(round(0x00000000, 24, alpha(color, 0.45f), 1.2f));
        b.setClickable(true);
        pressable(b);
        return b;
    }

    static ColorStateList csl(int c) {
        return ColorStateList.valueOf(c);
    }

    static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        return v;
    }

    static String mmss(long sec) {
        if (sec < 0) sec = 0;
        return (sec / 60) + ":" + (sec % 60 < 10 ? "0" : "") + (sec % 60);
    }

    private Ui() {
    }
}
