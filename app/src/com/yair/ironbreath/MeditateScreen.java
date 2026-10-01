package com.yair.ironbreath;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

final class MeditateScreen extends Screen {
    private LinearLayout list;
    private boolean first = true;

    MeditateScreen(MainActivity a) {
        super(a);
    }

    @Override
    View create() {
        LinearLayout col = Ui.vbox(a);
        col.addView(BreathScreen.header(a, R.drawable.img_cliff, null, "מדיטציה",
                "אימון לראש – מיקוד, שליטה ושקט.", Ui.BRASS), Ui.lp(Ui.MATCH, Ui.dp(240)));

        LinearLayout toggles = Ui.hbox(a);
        toggles.setPadding(Ui.dp(16), Ui.dp(4), Ui.dp(16), 0);
        toggles.addView(flagChip(IconView.VOICE, "קול מנחה", "voice", false), Ui.weight(1));
        View g = new View(a);
        toggles.addView(g, Ui.lp(Ui.dp(10), 1));
        toggles.addView(flagChip(IconView.NOTE, "רקע מוזיקלי", "ambient", true), Ui.weight(1));
        col.addView(toggles);

        list = Ui.vbox(a);
        list.setPadding(Ui.dp(16), Ui.dp(4), Ui.dp(16), Ui.dp(110));
        for (Content.Meditation m : Content.MEDITATIONS) list.addView(card(m), Ui.lpm(Ui.MATCH, Ui.dp(168), 0, 14, 0, 0));
        col.addView(list);
        return BreathScreen.scroller(a, col);
    }

    private View flagChip(int icon, String label, final String key, final boolean def) {
        final LinearLayout b = Ui.hbox(a);
        b.setGravity(Gravity.CENTER);
        b.setPadding(Ui.dp(10), Ui.dp(11), Ui.dp(10), Ui.dp(11));
        final IconView ic = new IconView(a, icon, Ui.TEXT);
        b.addView(ic, Ui.lp(Ui.dp(18), Ui.dp(18)));
        final TextView tv = Ui.text(a, "  " + label, 14, Ui.TEXT, Ui.BOLD);
        b.addView(tv);
        final Runnable paint = new Runnable() {
            @Override
            public void run() {
                boolean on = a.stats.flag(key, def);
                b.setBackground(on ? Ui.round(Ui.alpha(Ui.BRASS, 0.16f), 16, Ui.alpha(Ui.BRASS, 0.6f), 1) : Ui.round(0x0DFFFFFF, 16, 0x17FFFFFF, 1));
                ic.set(ic.type, on ? Ui.BRASS : Ui.DIM);
                tv.setTextColor(on ? Ui.TEXT : Ui.DIM);
            }
        };
        paint.run();
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean on = !a.stats.flag(key, def);
                a.stats.setFlag(key, on);
                if (on && key.equals("voice")) Voice.get(a).warm();
                paint.run();
            }
        });
        Ui.pressable(b);
        return b;
    }

    private View card(final Content.Meditation m) {
        FrameLayout f = new FrameLayout(a);
        Ui.clip(f, 22);
        f.setBackgroundColor(Ui.CARD);
        ImageView iv = Ui.image(a, m.image, 2);
        f.addView(iv, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        View sc = new View(a);
        sc.setBackground(new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, new int[]{0xF50C1214, 0xB00C1214, 0x100C1214}));
        f.addView(sc, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        LinearLayout t = Ui.vbox(a);
        t.setPadding(Ui.dp(20), Ui.dp(16), Ui.dp(20), Ui.dp(16));
        TextView n = Ui.text(a, m.title, 23, 0xFFFFFFFF, Ui.BOLD);
        n.setPadding(0, Ui.dp(6), 0, Ui.dp(4));
        t.addView(n);
        t.addView(Ui.text(a, m.sub, 13, 0xCCEDE6DA, Ui.REG));
        View sp = new View(a);
        t.addView(sp, new LinearLayout.LayoutParams(1, 0, 1));
        TextView d = Ui.text(a, Math.round(m.total() / 60f) + " דק׳ · " + m.steps.length + " שלבים", 12, Ui.TEXT, Ui.NUM);
        d.setPadding(Ui.dp(10), Ui.dp(4), Ui.dp(10), Ui.dp(4));
        d.setBackground(Ui.round(0x66000000, 12, Ui.alpha(m.color, 0.5f), 1));
        LinearLayout dr = Ui.hbox(a);
        dr.addView(d);
        t.addView(dr);
        f.addView(t, new FrameLayout.LayoutParams(Ui.dp(260), Ui.MATCH, Gravity.RIGHT));

        FrameLayout play = new FrameLayout(a);
        play.setBackground(Ui.round(Ui.alpha(m.color, 0.9f), 26));
        play.addView(new IconView(a, IconView.PLAY, 0xFF0B1A16), new FrameLayout.LayoutParams(Ui.dp(20), Ui.dp(20), Gravity.CENTER));
        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams(Ui.dp(48), Ui.dp(48), Gravity.LEFT | Gravity.BOTTOM);
        pl.setMargins(Ui.dp(16), 0, 0, Ui.dp(16));
        f.addView(play, pl);

        f.setClickable(true);
        Ui.pressable(f);
        f.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                a.push(new MeditationSession(a, m));
            }
        });
        return f;
    }

    @Override
    void onShow() {
        if (first && list != null) {
            first = false;
            Ui.stagger(list, 100);
        }
    }
}
