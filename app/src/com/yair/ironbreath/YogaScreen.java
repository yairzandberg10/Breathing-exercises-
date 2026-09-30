package com.yair.ironbreath;

import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

final class YogaScreen extends Screen {
    private final ArrayList<PoseView> thumbs = new ArrayList<>();
    private final ArrayList<Content.Routine> thumbRoutines = new ArrayList<>();
    private final Handler h = new Handler(Looper.getMainLooper());
    private int beat;
    private boolean visible, first = true;
    private LinearLayout list;

    YogaScreen(MainActivity a) {
        super(a);
    }

    @Override
    View create() {
        LinearLayout col = Ui.vbox(a);
        col.addView(BreathScreen.header(a, R.drawable.img_dunes, "YOGA FOR MEN · " + Content.POSES.size() + " תנוחות", "יוגה",
                "גמישות, כוח ויציבה. בלי גמישות של בלרינה\nובלי תירוצים – רק גוף שעובד טוב יותר.", Ui.MOSS), Ui.lp(Ui.MATCH, Ui.dp(290)));

        list = Ui.vbox(a);
        list.setPadding(Ui.dp(16), 0, Ui.dp(16), 0);
        for (Content.Routine r : Content.ROUTINES) list.addView(card(r), Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 12, 0, 0));
        col.addView(list);

        TextView lt = Ui.text(a, "ספריית תנוחות", 19, Ui.TEXT, Ui.BOLD);
        col.addView(lt, Ui.lpm(Ui.MATCH, Ui.WRAP, 20, 28, 20, 4));
        TextView ls = Ui.text(a, "הקש על תנוחה כדי להחזיק אותה דקה עם טיימר", 13, Ui.MUTED, Ui.REG);
        col.addView(ls, Ui.lpm(Ui.MATCH, Ui.WRAP, 20, 0, 20, 10));
        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout lib = Ui.hbox(a);
        lib.setPadding(Ui.dp(16), 0, Ui.dp(16), 0);
        for (final Content.Pose p : Content.POSES) {
            LinearLayout c = Ui.vbox(a);
            c.setGravity(Gravity.CENTER_HORIZONTAL);
            c.setBackground(Ui.round(Ui.CARD, 18, 0x14FFFFFF, 1));
            c.setPadding(Ui.dp(6), Ui.dp(8), Ui.dp(6), Ui.dp(10));
            PoseView pv = new PoseView(a);
            pv.breathe = false;
            pv.showMat = false;
            pv.color = Ui.MOSS;
            pv.setPose(p, false);
            c.addView(pv, Ui.lp(Ui.dp(104), Ui.dp(96)));
            TextView n = Ui.text(a, p.name, 13, Ui.TEXT, Ui.BOLD);
            n.setGravity(Gravity.CENTER);
            c.addView(n);
            TextView s = Ui.text(a, p.sanskrit, 10, Ui.DIM, Ui.NUM);
            s.setGravity(Gravity.CENTER);
            s.setPadding(0, Ui.dp(2), 0, 0);
            c.addView(s);
            c.setClickable(true);
            Ui.pressable(c);
            c.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Content.Routine r = new Content.Routine();
                    r.name = p.name;
                    r.en = p.sanskrit.toUpperCase();
                    r.sub = p.cue;
                    r.color = Ui.MOSS;
                    r.poses = new String[]{p.key};
                    r.secs = new int[]{60};
                    a.push(new YogaSession(a, r));
                }
            });
            lib.addView(c, Ui.lpm(Ui.dp(118), Ui.WRAP, 0, 0, 10, 0));
        }
        hs.addView(lib);
        col.addView(hs, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 0, 0, 110));
        return BreathScreen.scroller(a, col);
    }

    private View card(final Content.Routine r) {
        LinearLayout c = Ui.hbox(a);
        c.setPadding(Ui.dp(10), Ui.dp(10), Ui.dp(16), Ui.dp(10));
        c.setBackground(Ui.round(Ui.CARD, 22, 0x17FFFFFF, 1));
        FrameLayout box = new FrameLayout(a);
        box.setBackground(Ui.round(Ui.alpha(r.color, 0.09f), 18));
        PoseView pv = new PoseView(a);
        pv.color = r.color;
        pv.setPose(Content.pose(r.poses[0]), false);
        box.addView(pv, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        thumbs.add(pv);
        thumbRoutines.add(r);
        c.addView(box, Ui.lp(Ui.dp(104), Ui.dp(104)));
        LinearLayout t = Ui.vbox(a);
        t.setPadding(Ui.dp(14), 0, Ui.dp(8), 0);
        t.addView(Ui.tag(a, r.en, r.color));
        TextView n = Ui.text(a, r.name, 19, Ui.TEXT, Ui.BOLD);
        n.setPadding(0, Ui.dp(5), 0, Ui.dp(3));
        t.addView(n);
        t.addView(Ui.text(a, r.sub, 13, Ui.MUTED, Ui.REG));
        TextView meta = Ui.text(a, r.poses.length + " תנוחות · " + Math.round(r.total() / 60f) + " דק׳", 12, r.color, Ui.NUM);
        meta.setPadding(0, Ui.dp(8), 0, 0);
        t.addView(meta);
        c.addView(t, Ui.weight(1));
        c.addView(new IconView(a, IconView.LEFT, Ui.DIM), Ui.lp(Ui.dp(18), Ui.dp(18)));
        c.setClickable(true);
        Ui.pressable(c);
        c.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                a.push(new YogaSession(a, r));
            }
        });
        return c;
    }

    /** Thumbnails flow through their routine's poses so the list feels alive. */
    private final Runnable cycle = new Runnable() {
        @Override
        public void run() {
            if (!visible) return;
            beat++;
            for (int i = 0; i < thumbs.size(); i++) {
                Content.Routine r = thumbRoutines.get(i);
                thumbs.get(i).setPose(Content.pose(r.poses[(beat + i) % r.poses.length]), true);
            }
            h.postDelayed(this, 2600);
        }
    };

    @Override
    void onShow() {
        visible = true;
        h.removeCallbacks(cycle);
        h.postDelayed(cycle, 1500);
        if (first && list != null) {
            first = false;
            Ui.stagger(list, 100);
        }
    }

    @Override
    void onHide() {
        visible = false;
        h.removeCallbacks(cycle);
    }
}
