package com.yair.ironbreath;

import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;

final class HomeScreen extends Screen {
    private TextView streakV, minV, holdV, greetV;
    private LinearLayout col;
    private boolean first = true;

    HomeScreen(MainActivity a) {
        super(a);
    }

    @Override
    View create() {
        ScrollView sv = new ScrollView(a);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        col = Ui.vbox(a);
        sv.addView(col);

        int sh = (int) (a.getResources().getDisplayMetrics().heightPixels / Ui.density);
        col.addView(hero(), Ui.lp(Ui.MATCH, Ui.dp(Math.max(420, sh * 0.6f))));
        col.addView(statsStrip(), Ui.lpm(Ui.MATCH, Ui.WRAP, 16, -28, 16, 0));
        col.addView(sectionTitle("מומלץ עכשיו"), Ui.lpm(Ui.MATCH, Ui.WRAP, 20, 34, 20, 12));
        col.addView(protocolCard(), Ui.lpm(Ui.MATCH, Ui.dp(170), 16, 0, 16, 0));
        col.addView(sosCard(), Ui.lpm(Ui.MATCH, Ui.WRAP, 16, 12, 16, 0));
        col.addView(sectionTitle("מה בא לך היום?"), Ui.lpm(Ui.MATCH, Ui.WRAP, 20, 34, 20, 12));
        col.addView(arsenal(), Ui.lpm(Ui.MATCH, Ui.WRAP, 16, 0, 16, 0));
        col.addView(quoteCard(), Ui.lpm(Ui.MATCH, Ui.WRAP, 16, 30, 16, 120));
        return sv;
    }

    private View hero() {
        FrameLayout f = new FrameLayout(a);
        ImageView img = Ui.image(a, R.drawable.img_hero, 1);
        f.addView(img, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        Ui.kenBurns(img, 30000);
        View scrim = new View(a);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x660C1214, 0x000C1214, 0x000C1214, 0xCC0C1214, 0xFF0C1214}));
        f.addView(scrim, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        LinearLayout txt = Ui.vbox(a);
        txt.setPadding(Ui.dp(24), 0, Ui.dp(24), Ui.dp(52));
        greetV = Ui.text(a, "", 16, 0xD9EDE6DA, Ui.REG);
        txt.addView(greetV);
        TextView h = Ui.text(a, Ui.accent("שליטה מתחילה\nבנשימה.", "בנשימה.", Ui.EMBER), 42, 0xFFFFFFFF, Ui.BOLD);
        h.setLineSpacing(0, 0.95f);
        h.setPadding(0, Ui.dp(10), 0, 0);
        h.setShadowLayer(Ui.dp(14), 0, Ui.dp(2), 0x99000000);
        txt.addView(h);
        f.addView(txt, new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP, Gravity.BOTTOM));
        return f;
    }

    private static String greeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h < 5) return "לילה שקט, אלוף";
        if (h < 12) return "בוקר טוב, אלוף";
        if (h < 17) return "צהריים טובים, אלוף";
        if (h < 21) return "ערב טוב, אלוף";
        return "לילה טוב, אלוף";
    }

    private View statsStrip() {
        LinearLayout row = Ui.hbox(a);
        row.setBackground(Ui.round(0xF515171D, 20));
        row.setElevation(Ui.dp(8));
        row.setPadding(0, Ui.dp(16), 0, Ui.dp(16));
        streakV = stat(row, "ימי רצף", Ui.EMBER);
        row.addView(vline(), Ui.lp(1, Ui.dp(36)));
        minV = stat(row, "דקות אימון", Ui.TEXT);
        row.addView(vline(), Ui.lp(1, Ui.dp(36)));
        holdV = stat(row, "שיא עצירה", Ui.TEXT);
        return row;
    }

    private View vline() {
        View v = new View(a);
        v.setBackgroundColor(0x1AFFFFFF);
        return v;
    }

    private TextView stat(LinearLayout row, String label, int color) {
        LinearLayout c = Ui.vbox(a);
        TextView v = Ui.centered(a, "0", 28, color, Ui.NUM);
        c.addView(v);
        TextView l = Ui.centered(a, label, 12, Ui.MUTED, Ui.REG);
        l.setPadding(0, Ui.dp(4), 0, 0);
        c.addView(l);
        row.addView(c, Ui.weight(1));
        return v;
    }

    private View sectionTitle(String he) {
        return Ui.text(a, he, 20, Ui.TEXT, Ui.BOLD);
    }

    private Content.Technique todays() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h >= 5 && h < 11) return Content.technique("power");
        if (h >= 11 && h < 17) return Content.technique("box");
        if (h >= 17 && h < 21) return Content.technique("coherent");
        return Content.technique("478");
    }

    private View protocolCard() {
        final Content.Technique t = todays();
        FrameLayout f = new FrameLayout(a);
        Ui.clip(f, 22);
        f.setBackgroundColor(Ui.CARD);
        int img = t.cat == Content.CAT_SLEEP ? R.drawable.img_lake : t.cat == Content.CAT_ENERGY ? R.drawable.img_dunes : R.drawable.img_ocean;
        ImageView iv = Ui.image(a, img, 2);
        f.addView(iv, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        View scrim = new View(a);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, new int[]{0xF20C1214, 0xA00C1214, 0x100C1214}));
        f.addView(scrim, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        LinearLayout txt = Ui.vbox(a);
        txt.setGravity(Gravity.CENTER_VERTICAL);
        txt.setPadding(Ui.dp(20), 0, Ui.dp(20), 0);
        TextView name = Ui.text(a, t.name, 25, 0xFFFFFFFF, Ui.BOLD);
        txt.addView(name);
        TextView tag = Ui.text(a, t.tagline, 14, 0xCCEDE6DA, Ui.REG);
        tag.setPadding(0, Ui.dp(6), 0, Ui.dp(10));
        txt.addView(tag);
        txt.addView(Ui.text(a, t.minutes(t.rounds) + " דקות", 13, t.color, Ui.BOLD));
        f.addView(txt, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        FrameLayout play = new FrameLayout(a);
        play.setBackground(Ui.round(Ui.EMBER, 30));
        play.setElevation(Ui.dp(6));
        play.addView(new IconView(a, IconView.PLAY, 0xFF0B1A16), new FrameLayout.LayoutParams(Ui.dp(22), Ui.dp(22), Gravity.CENTER));
        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams(Ui.dp(54), Ui.dp(54), Gravity.LEFT | Gravity.CENTER_VERTICAL);
        pl.setMargins(Ui.dp(20), 0, 0, 0);
        f.addView(play, pl);

        f.setClickable(true);
        Ui.pressable(f);
        f.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                a.push(new BreathSession(a, t));
            }
        });
        return f;
    }

    private View sosCard() {
        LinearLayout r = Ui.hbox(a);
        r.setPadding(Ui.dp(16), Ui.dp(14), Ui.dp(16), Ui.dp(14));
        r.setBackground(Ui.round(0xFF1E2224, 20));
        FrameLayout ic = new FrameLayout(a);
        ic.setBackground(Ui.round(Ui.alpha(Ui.BLOOD, 0.18f), 22));
        ic.addView(new IconView(a, IconView.BOLT, Ui.BLOOD), new FrameLayout.LayoutParams(Ui.dp(20), Ui.dp(20), Gravity.CENTER));
        r.addView(ic, Ui.lp(Ui.dp(44), Ui.dp(44)));
        LinearLayout t = Ui.vbox(a);
        t.setPadding(Ui.dp(14), 0, 0, 0);
        t.addView(Ui.text(a, "לחוץ עכשיו?", 17, Ui.TEXT, Ui.BOLD));
        TextView s = Ui.text(a, "דקה אחת של נשימה שמורידה דופק", 13, Ui.MUTED, Ui.REG);
        s.setPadding(0, Ui.dp(4), 0, 0);
        t.addView(s);
        r.addView(t, Ui.weight(1));
        r.setClickable(true);
        Ui.pressable(r);
        r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                a.push(new BreathSession(a, Content.technique("sigh")));
            }
        });
        ValueAnimator va = ValueAnimator.ofFloat(0, 1);
        va.setDuration(1300);
        va.setRepeatCount(ValueAnimator.INFINITE);
        final View icf = ic;
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator an) {
                float x = (float) an.getAnimatedValue();
                float beat = (float) (Math.exp(-Math.pow((x - 0.1) * 14, 2)) + 0.6 * Math.exp(-Math.pow((x - 0.3) * 14, 2)));
                icf.setScaleX(1 + beat * 0.1f);
                icf.setScaleY(1 + beat * 0.1f);
            }
        });
        va.start();
        return r;
    }

    private View arsenal() {
        LinearLayout grid = Ui.vbox(a);
        int[] imgs = {R.drawable.img_ocean, R.drawable.img_forest, R.drawable.img_cliff, R.drawable.img_dunes};
        String[] titles = {"נשימה", "סאונד", "מדיטציה", "יוגה"};
        String[] subs = {Content.TECHNIQUES.size() + " תרגילים", Content.S_COUNT + " צלילים", Content.MEDITATIONS.size() + " מדיטציות", Content.ROUTINES.size() + " אימונים"};
        final int[] tabsFor = {MainActivity.TAB_BREATH, MainActivity.TAB_SOUND, MainActivity.TAB_MEDITATE, MainActivity.TAB_YOGA};
        LinearLayout row = null;
        for (int i = 0; i < 4; i++) {
            if (i % 2 == 0) {
                row = Ui.hbox(a);
                grid.addView(row, Ui.lpm(Ui.MATCH, Ui.dp(170), 0, i == 0 ? 0 : 12, 0, 0));
            }
            FrameLayout tile = new FrameLayout(a);
            Ui.clip(tile, 22);
            tile.setBackgroundColor(Ui.CARD);
            ImageView iv = Ui.image(a, imgs[i], 2);
            tile.addView(iv, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
            View sc = new View(a);
            sc.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x000C1214, 0x200C1214, 0xE60C1214}));
            tile.addView(sc, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
            LinearLayout tx = Ui.vbox(a);
            tx.setPadding(Ui.dp(16), 0, Ui.dp(16), Ui.dp(14));
            tx.addView(Ui.text(a, titles[i], 22, 0xFFFFFFFF, Ui.BOLD));
            TextView st = Ui.text(a, subs[i], 12, 0xB3EDE6DA, Ui.REG);
            st.setPadding(0, Ui.dp(3), 0, 0);
            tx.addView(st);
            tile.addView(tx, new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP, Gravity.BOTTOM));
            final int target = tabsFor[i];
            tile.setClickable(true);
            Ui.pressable(tile);
            tile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    a.showTab(target);
                }
            });
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, Ui.MATCH, 1);
            tl.setMarginStart(i % 2 == 1 ? Ui.dp(12) : 0);
            row.addView(tile, tl);
        }
        return grid;
    }

    private View quoteCard() {
        String[] q = Content.QUOTES[(int) (Stats.today() % Content.QUOTES.length)];
        LinearLayout c = Ui.vbox(a);
        c.setPadding(Ui.dp(22), Ui.dp(22), Ui.dp(22), Ui.dp(22));
        c.setBackground(Ui.round(Ui.CARD, 22));
        TextView tq = Ui.text(a, "״" + q[0] + "״", 19, Ui.TEXT, Ui.SERIF);
        tq.setLineSpacing(0, 1.3f);
        tq.setPadding(0, 0, 0, Ui.dp(12));
        c.addView(tq);
        c.addView(Ui.text(a, "— " + q[1], 14, Ui.BRASS, Ui.BOLD));
        return c;
    }

    @Override
    void onShow() {
        if (greetV == null) return;
        greetV.setText(greeting());
        count(streakV, a.stats.streak());
        count(minV, a.stats.minutes());
        int hold = a.stats.bestHold();
        holdV.setText(hold == 0 ? "—" : Ui.mmss(hold));
        if (first) {
            first = false;
            Ui.stagger(col, 120);
        }
    }

    private void count(final TextView tv, final int to) {
        ValueAnimator va = ValueAnimator.ofInt(0, to);
        va.setDuration(Math.min(1400, 400 + to * 20L));
        va.setInterpolator(new DecelerateInterpolator(2f));
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator an) {
                tv.setText(String.valueOf(an.getAnimatedValue()));
            }
        });
        va.start();
    }
}
