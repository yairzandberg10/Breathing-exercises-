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

        col.addView(hero(), Ui.lp(Ui.MATCH, Ui.dp(500)));
        col.addView(statsStrip(), Ui.lpm(Ui.MATCH, Ui.WRAP, 16, -34, 16, 0));
        col.addView(sectionTitle("הפרוטוקול של היום", "TODAY"), Ui.lpm(Ui.MATCH, Ui.WRAP, 20, 30, 20, 12));
        col.addView(protocolCard(), Ui.lpm(Ui.MATCH, Ui.dp(200), 16, 0, 16, 0));
        col.addView(sosCard(), Ui.lpm(Ui.MATCH, Ui.WRAP, 16, 14, 16, 0));
        col.addView(sectionTitle("הארסנל שלך", "ARSENAL"), Ui.lpm(Ui.MATCH, Ui.WRAP, 20, 30, 20, 12));
        col.addView(arsenal(), Ui.lpm(Ui.MATCH, Ui.WRAP, 16, 0, 16, 0));
        col.addView(quoteCard(), Ui.lpm(Ui.MATCH, Ui.WRAP, 16, 26, 16, 0));
        TextView foot = Ui.tag(a, "IRON BREATH · נשום. התמקד. שלוט.", Ui.DIM);
        foot.setGravity(Gravity.CENTER);
        col.addView(foot, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 30, 0, 110));
        return sv;
    }

    private View hero() {
        FrameLayout f = new FrameLayout(a);
        ImageView img = Ui.image(a, R.drawable.img_hero, 1);
        f.addView(img, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        Ui.kenBurns(img, 26000);
        View scrim = new View(a);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xAA0B0C10, 0x000B0C10, 0x400B0C10, 0xE60B0C10, 0xFF0B0C10}));
        f.addView(scrim, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        EmberView em = new EmberView(a);
        f.addView(em, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        LinearLayout top = Ui.hbox(a);
        top.setPadding(Ui.dp(22), a.statusBar + Ui.dp(14), Ui.dp(22), 0);
        IconView mark = new IconView(a, IconView.FLAME, Ui.EMBER);
        top.addView(mark, Ui.lp(Ui.dp(20), Ui.dp(20)));
        TextView brand = Ui.tag(a, "  IRON BREATH", Ui.TEXT);
        top.addView(brand);
        View sp = new View(a);
        top.addView(sp, Ui.weight(1));
        TextView date = Ui.tag(a, dayName(), Ui.BRASS);
        top.addView(date);
        f.addView(top, new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP, Gravity.TOP));

        LinearLayout txt = Ui.vbox(a);
        txt.setPadding(Ui.dp(22), 0, Ui.dp(22), Ui.dp(58));
        greetV = Ui.text(a, "", 16, 0xD9EDE6DA, Ui.REG);
        txt.addView(greetV);
        TextView h = Ui.text(a, Ui.accent("שליטה\nמתחילה\nבנשימה.", "בנשימה.", Ui.EMBER), 50, 0xFFFFFFFF, Ui.BOLD);
        h.setLineSpacing(0, 0.92f);
        h.setPadding(0, Ui.dp(8), 0, Ui.dp(12));
        h.setShadowLayer(Ui.dp(14), 0, Ui.dp(2), 0x99000000);
        txt.addView(h);
        TextView s = Ui.text(a, "נשימה · סאונד · מדיטציה · יוגה.\nבנוי לגברים שרוצים ראש שקט וגוף חזק.", 15, 0xCCEDE6DA, Ui.REG);
        txt.addView(s);
        f.addView(txt, new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP, Gravity.BOTTOM));
        return f;
    }

    private static String dayName() {
        String[] d = {"יום ראשון", "יום שני", "יום שלישי", "יום רביעי", "יום חמישי", "יום שישי", "שבת"};
        return d[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1];
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
        row.setBackground(Ui.round(0xF215171D, 20, 0x22FFFFFF, 1));
        row.setElevation(Ui.dp(8));
        row.setPadding(0, Ui.dp(16), 0, Ui.dp(16));
        streakV = stat(row, "ימי רצף", Ui.EMBER);
        row.addView(vline(), Ui.lp(1, Ui.dp(40)));
        minV = stat(row, "דקות אימון", Ui.TEXT);
        row.addView(vline(), Ui.lp(1, Ui.dp(40)));
        holdV = stat(row, "שיא עצירה", Ui.STEEL);
        return row;
    }

    private View vline() {
        View v = new View(a);
        v.setBackgroundColor(0x1FFFFFFF);
        return v;
    }

    private TextView stat(LinearLayout row, String label, int color) {
        LinearLayout c = Ui.vbox(a);
        c.setGravity(Gravity.CENTER);
        TextView v = Ui.text(a, "0", 30, color, Ui.NUM);
        c.addView(v);
        TextView l = Ui.text(a, label, 12, Ui.MUTED, Ui.REG);
        l.setPadding(0, Ui.dp(4), 0, 0);
        c.addView(l);
        row.addView(c, Ui.weight(1));
        return v;
    }

    private View sectionTitle(String he, String en) {
        LinearLayout r = Ui.hbox(a);
        View bar = new View(a);
        bar.setBackground(Ui.round(Ui.EMBER, 2));
        r.addView(bar, Ui.lpm(Ui.dp(4), Ui.dp(20), 0, 0, 10, 0));
        r.addView(Ui.text(a, he, 21, Ui.TEXT, Ui.BOLD));
        View sp = new View(a);
        r.addView(sp, Ui.weight(1));
        r.addView(Ui.tag(a, en, Ui.DIM));
        return r;
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
        Ui.clip(f, 24);
        f.setBackgroundColor(Ui.CARD);
        int img = t.cat == Content.CAT_SLEEP ? R.drawable.img_lake : t.cat == Content.CAT_ENERGY ? R.drawable.img_dunes : R.drawable.img_ocean;
        ImageView iv = Ui.image(a, img, 2);
        f.addView(iv, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        View scrim = new View(a);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, new int[]{0xF20B0C10, 0xB30B0C10, 0x200B0C10}));
        f.addView(scrim, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        BreathView orb = new BreathView(a);
        orb.compact = true;
        orb.color = t.color;
        orb.driver = new BreathView.Driver() {
            @Override
            public void frame(BreathView v, long now) {
                v.scale = BreathView.previewScale(t, now / 1000f);
                v.phaseType = -1;
            }
        };
        f.addView(orb, new FrameLayout.LayoutParams(Ui.dp(150), Ui.dp(150), Gravity.LEFT | Gravity.CENTER_VERTICAL));

        LinearLayout txt = Ui.vbox(a);
        txt.setPadding(Ui.dp(20), Ui.dp(18), Ui.dp(20), Ui.dp(18));
        txt.addView(Ui.tag(a, "PROTOCOL · " + t.en, t.color));
        TextView name = Ui.text(a, t.name, 27, 0xFFFFFFFF, Ui.BOLD);
        name.setPadding(0, Ui.dp(8), 0, Ui.dp(4));
        txt.addView(name);
        txt.addView(Ui.text(a, t.tagline, 14, 0xCCEDE6DA, Ui.REG));
        View sp = new View(a);
        txt.addView(sp, new LinearLayout.LayoutParams(1, 0, 1));
        LinearLayout chips = Ui.hbox(a);
        chips.addView(chip(t.pattern, t.color));
        chips.addView(chip("~" + t.minutes(t.rounds) + " דק׳", Ui.TEXT), Ui.lpm(Ui.WRAP, Ui.WRAP, 8, 0, 0, 0));
        txt.addView(chips);
        f.addView(txt, new FrameLayout.LayoutParams(Ui.dp(230), Ui.MATCH, Gravity.RIGHT));

        FrameLayout play = new FrameLayout(a);
        play.setBackground(Ui.round(Ui.EMBER, 30));
        play.setElevation(Ui.dp(6));
        IconView pi = new IconView(a, IconView.PLAY, 0xFF140A05);
        play.addView(pi, new FrameLayout.LayoutParams(Ui.dp(22), Ui.dp(22), Gravity.CENTER));
        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams(Ui.dp(52), Ui.dp(52), Gravity.LEFT | Gravity.BOTTOM);
        pl.setMargins(Ui.dp(16), 0, 0, Ui.dp(16));
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

    private TextView chip(String s, int color) {
        TextView c = Ui.text(a, s, 12, color, Ui.NUM);
        c.setPadding(Ui.dp(10), Ui.dp(5), Ui.dp(10), Ui.dp(5));
        c.setBackground(Ui.round(0x66000000, 14, Ui.alpha(color, 0.4f), 1));
        return c;
    }

    private View sosCard() {
        LinearLayout r = Ui.hbox(a);
        r.setPadding(Ui.dp(16), Ui.dp(14), Ui.dp(16), Ui.dp(14));
        r.setBackground(Ui.round(0xFF1A1112, 20, Ui.alpha(Ui.BLOOD, 0.45f), 1));
        FrameLayout ic = new FrameLayout(a);
        ic.setBackground(Ui.round(Ui.alpha(Ui.BLOOD, 0.2f), 22));
        ic.addView(new IconView(a, IconView.BOLT, Ui.BLOOD), new FrameLayout.LayoutParams(Ui.dp(22), Ui.dp(22), Gravity.CENTER));
        r.addView(ic, Ui.lp(Ui.dp(44), Ui.dp(44)));
        LinearLayout t = Ui.vbox(a);
        t.setPadding(Ui.dp(14), 0, Ui.dp(8), 0);
        t.addView(Ui.text(a, "לחוץ עכשיו? 60 שניות.", 17, Ui.TEXT, Ui.BOLD));
        TextView s = Ui.text(a, "אנחה פיזיולוגית – הדרך המהירה ביותר להוריד דופק", 13, Ui.MUTED, Ui.REG);
        s.setPadding(0, Ui.dp(4), 0, 0);
        t.addView(s);
        r.addView(t, Ui.weight(1));
        r.addView(new IconView(a, IconView.LEFT, Ui.BLOOD), Ui.lp(Ui.dp(20), Ui.dp(20)));
        r.setClickable(true);
        Ui.pressable(r);
        r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                a.push(new BreathSession(a, Content.technique("sigh")));
            }
        });
        // subtle heartbeat pulse on the icon
        ValueAnimator va = ValueAnimator.ofFloat(0, 1);
        va.setDuration(1100);
        va.setRepeatCount(ValueAnimator.INFINITE);
        final View icf = ic;
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator an) {
                float x = (float) an.getAnimatedValue();
                float beat = (float) (Math.exp(-Math.pow((x - 0.1) * 14, 2)) + 0.6 * Math.exp(-Math.pow((x - 0.3) * 14, 2)));
                icf.setScaleX(1 + beat * 0.12f);
                icf.setScaleY(1 + beat * 0.12f);
            }
        });
        va.start();
        return r;
    }

    private View arsenal() {
        LinearLayout grid = Ui.vbox(a);
        int[] imgs = {R.drawable.img_ocean, R.drawable.img_forest, R.drawable.img_cliff, R.drawable.img_dunes};
        String[] titles = {"נשימה", "סאונד", "מדיטציה", "יוגה"};
        String[] subs = {Content.TECHNIQUES.size() + " פרוטוקולים", Content.S_COUNT + " נופי צליל", Content.MEDITATIONS.size() + " אימוני ראש", Content.ROUTINES.size() + " סדרות · " + Content.POSES.size() + " תנוחות"};
        int[] colors = {Ui.STEEL, Ui.EMBER, Ui.BRASS, Ui.MOSS};
        int[] icons = {IconView.BREATH, IconView.SOUND, IconView.MEDITATE, IconView.YOGA};
        final int[] tabsFor = {MainActivity.TAB_BREATH, MainActivity.TAB_SOUND, MainActivity.TAB_MEDITATE, MainActivity.TAB_YOGA};
        LinearLayout row = null;
        for (int i = 0; i < 4; i++) {
            if (i % 2 == 0) {
                row = Ui.hbox(a);
                grid.addView(row, Ui.lpm(Ui.MATCH, Ui.dp(i == 0 ? 210 : 180), 0, i == 0 ? 0 : 12, 0, 0));
            }
            FrameLayout tile = new FrameLayout(a);
            Ui.clip(tile, 22);
            tile.setBackgroundColor(Ui.CARD);
            ImageView iv = Ui.image(a, imgs[i], 2);
            tile.addView(iv, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
            View sc = new View(a);
            sc.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x330B0C10, 0x000B0C10, 0xF00B0C10}));
            tile.addView(sc, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
            TextView num = Ui.text(a, "0" + (i + 1), 13, colors[i], Ui.NUM);
            num.setLetterSpacing(0.2f);
            FrameLayout.LayoutParams nl = new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.RIGHT);
            nl.setMargins(0, Ui.dp(14), Ui.dp(16), 0);
            tile.addView(num, nl);
            IconView ic = new IconView(a, icons[i], Ui.alpha(0xFFFFFFFF, 0.85f));
            FrameLayout.LayoutParams il = new FrameLayout.LayoutParams(Ui.dp(22), Ui.dp(22), Gravity.TOP | Gravity.LEFT);
            il.setMargins(Ui.dp(14), Ui.dp(12), 0, 0);
            tile.addView(ic, il);
            LinearLayout tx = Ui.vbox(a);
            tx.setPadding(Ui.dp(16), 0, Ui.dp(16), Ui.dp(16));
            tx.addView(Ui.text(a, titles[i], 24, 0xFFFFFFFF, Ui.BOLD));
            TextView st = Ui.text(a, subs[i], 12, 0xCCEDE6DA, Ui.REG);
            st.setPadding(0, Ui.dp(4), 0, 0);
            tx.addView(st);
            View line = new View(a);
            line.setBackground(Ui.round(colors[i], 2));
            LinearLayout.LayoutParams ll = Ui.lpm(Ui.dp(28), Ui.dp(3), 0, 10, 0, 0);
            tx.addView(line, ll);
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
            LinearLayout.LayoutParams tl = Ui.weight(1);
            tl.height = Ui.MATCH;
            if (i % 2 == 0) tl.setMargins(0, 0, 0, 0);
            else tl.setMargins(0, 0, Ui.dp(12), 0);
            // in RTL the first child sits on the right; margin on the second keeps a gutter
            tl.setMarginStart(i % 2 == 1 ? Ui.dp(12) : 0);
            tl.setMarginEnd(0);
            row.addView(tile, tl);
        }
        return grid;
    }

    private View quoteCard() {
        String[] q = Content.QUOTES[(int) (Stats.today() % Content.QUOTES.length)];
        FrameLayout f = new FrameLayout(a);
        f.setBackground(Ui.round(Ui.CARD, 22, 0x1AFFFFFF, 1));
        TextView mark = Ui.text(a, "״", 110, Ui.alpha(Ui.BRASS, 0.18f), Ui.SERIF);
        FrameLayout.LayoutParams ml = new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.LEFT);
        ml.setMargins(Ui.dp(14), -Ui.dp(6), 0, 0);
        f.addView(mark, ml);
        LinearLayout c = Ui.vbox(a);
        c.setPadding(Ui.dp(22), Ui.dp(22), Ui.dp(22), Ui.dp(22));
        c.addView(Ui.tag(a, "מחשבה ליום", Ui.BRASS));
        TextView tq = Ui.text(a, q[0], 21, Ui.TEXT, Ui.SERIF);
        tq.setLineSpacing(0, 1.25f);
        tq.setPadding(0, Ui.dp(12), 0, Ui.dp(14));
        c.addView(tq);
        c.addView(Ui.text(a, "— " + q[1], 14, Ui.BRASS, Ui.BOLD));
        f.addView(c);
        return f;
    }

    @Override
    void onShow() {
        if (greetV == null) return;
        greetV.setText(greeting());
        count(streakV, a.stats.streak(), "");
        count(minV, a.stats.minutes(), "");
        int hold = a.stats.bestHold();
        holdV.setText(hold == 0 ? "—" : Ui.mmss(hold));
        if (first) {
            first = false;
            Ui.stagger(col, 120);
        }
    }

    private void count(final TextView tv, final int to, final String suffix) {
        ValueAnimator va = ValueAnimator.ofInt(0, to);
        va.setDuration(Math.min(1400, 400 + to * 20L));
        va.setInterpolator(new DecelerateInterpolator(2f));
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator an) {
                tv.setText(an.getAnimatedValue() + suffix);
            }
        });
        va.start();
    }
}
