package com.yair.ironbreath;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class BreathScreen extends Screen {
    private LinearLayout list;
    private final TextView[] chips = new TextView[Content.CAT_NAMES.length];
    private int filter = 0;

    BreathScreen(MainActivity a) {
        super(a);
    }

    /** Shared header used by the four section tabs: image, scrim, stencil tag, big title. */
    static FrameLayout header(MainActivity a, int img, String tag, String title, String sub, int color) {
        FrameLayout f = new FrameLayout(a);
        ImageView iv = Ui.image(a, img, 2);
        f.addView(iv, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        Ui.kenBurns(iv, 30000);
        View scrim = new View(a);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x990C1214, 0x200C1214, 0xCC0C1214, 0xFF0C1214}));
        f.addView(scrim, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        LinearLayout t = Ui.vbox(a);
        t.setPadding(Ui.dp(22), 0, Ui.dp(22), Ui.dp(18));
        if (tag != null) t.addView(Ui.tag(a, tag, color));
        TextView h = Ui.text(a, title, 44, 0xFFFFFFFF, Ui.BOLD);
        h.setPadding(0, Ui.dp(6), 0, Ui.dp(6));
        h.setShadowLayer(Ui.dp(10), 0, 0, 0x99000000);
        t.addView(h);
        TextView s = Ui.text(a, sub, 14, 0xCCEDE6DA, Ui.REG);
        t.addView(s);
        f.addView(t, new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP, Gravity.BOTTOM));
        return f;
    }

    static ScrollView scroller(MainActivity a, LinearLayout content) {
        ScrollView sv = new ScrollView(a);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        sv.addView(content);
        return sv;
    }

    @Override
    View create() {
        LinearLayout col = Ui.vbox(a);
        col.addView(header(a, R.drawable.img_ocean, null, "נשימה",
                "הכלי הכי חזק שלך – תמיד זמין.", Ui.STEEL), Ui.lp(Ui.MATCH, Ui.dp(240)));

        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chipRow = Ui.hbox(a);
        chipRow.setPadding(Ui.dp(16), Ui.dp(4), Ui.dp(16), Ui.dp(4));
        for (int i = 0; i < Content.CAT_NAMES.length; i++) {
            final int idx = i;
            TextView c = Ui.text(a, Content.CAT_NAMES[i], 14, Ui.TEXT, Ui.BOLD);
            c.setPadding(Ui.dp(18), Ui.dp(9), Ui.dp(18), Ui.dp(9));
            c.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setFilter(idx);
                }
            });
            chips[i] = c;
            chipRow.addView(c, Ui.lpm(Ui.WRAP, Ui.WRAP, 0, 0, 8, 0));
        }
        hs.addView(chipRow);
        col.addView(hs, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 6, 0, 8));

        list = Ui.vbox(a);
        list.setPadding(Ui.dp(16), 0, Ui.dp(16), Ui.dp(110));
        col.addView(list);
        setFilter(0);
        return scroller(a, col);
    }

    private void setFilter(int f) {
        filter = f;
        for (int i = 0; i < chips.length; i++) {
            boolean on = i == f;
            chips[i].setBackground(on ? Ui.round(Ui.EMBER, 20) : Ui.round(Ui.CARD, 20, 0x22FFFFFF, 1));
            chips[i].setTextColor(on ? 0xFF0B1A16 : Ui.TEXT);
        }
        list.removeAllViews();
        for (Content.Technique t : Content.TECHNIQUES) {
            if (f != 0 && t.cat != f) continue;
            list.addView(card(t), Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 12, 0, 0));
        }
        Ui.stagger(list, 0);
    }

    private View card(final Content.Technique t) {
        LinearLayout r = Ui.hbox(a);
        r.setPadding(Ui.dp(12), Ui.dp(12), Ui.dp(16), Ui.dp(12));
        r.setBackground(Ui.round(Ui.CARD, 22));

        FrameLayout orbBox = new FrameLayout(a);
        orbBox.setBackground(Ui.round(Ui.alpha(t.color, 0.08f), 18));
        BreathView orb = new BreathView(a);
        orb.compact = true;
        orb.color = t.color;
        orb.driver = new BreathView.Driver() {
            @Override
            public void frame(BreathView v, long now) {
                v.scale = BreathView.previewScale(t, now / 1000f);
            }
        };
        orbBox.addView(orb, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        r.addView(orbBox, Ui.lp(Ui.dp(84), Ui.dp(84)));

        LinearLayout tx = Ui.vbox(a);
        tx.setPadding(Ui.dp(14), 0, Ui.dp(10), 0);
        TextView n = Ui.text(a, t.name, 19, Ui.TEXT, Ui.BOLD);
        n.setPadding(0, Ui.dp(5), 0, Ui.dp(3));
        tx.addView(n);
        tx.addView(Ui.text(a, t.tagline, 13, Ui.MUTED, Ui.REG));
        TextView mins = Ui.text(a, t.minutes(t.rounds) + " דקות", 12, t.color, Ui.BOLD);
        mins.setPadding(0, Ui.dp(6), 0, 0);
        tx.addView(mins);
        r.addView(tx, Ui.weight(1));

        r.setClickable(true);
        Ui.pressable(r);
        r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                a.push(new BreathSession(a, t));
            }
        });
        return r;
    }
}
