package com.yair.ironbreath;

import android.animation.ValueAnimator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

/** Full-screen guided breathing session: setup → countdown → live guidance → debrief. */
final class BreathSession extends Screen implements BreathView.Driver {
    private static final int SETUP = 0, COUNTDOWN = 1, RUN = 2, DONE = 3;

    private final Content.Technique t;
    private int rounds;
    private int state = SETUP;
    private final ArrayList<Content.Phase> seq = new ArrayList<>();
    private int idx;
    private long phaseStart, sessionStart, pausedAt, pausedTotal, countStart;
    private boolean paused;
    private float sFrom = 0.55f, sTo = 0.55f;
    private int bestThisSession;
    private boolean newRecord;
    private long lastShown = -1;

    private BreathView bv;
    private FrameLayout panel;
    private View setupPanel, runPanel;
    private LinearLayout donePanel;
    private TextView roundsV, minutesV, roundLabel, timeLabel, continueBtn;
    private IconView pauseIcon;
    private EmberView embers;
    private int smallH, bigH;

    BreathSession(MainActivity a, Content.Technique t) {
        super(a);
        this.t = t;
        this.rounds = t.rounds;
    }

    @Override
    View create() {
        FrameLayout root = new FrameLayout(a);
        root.setBackgroundColor(Ui.BG);
        root.setClickable(true);
        int img = t.cat == Content.CAT_SLEEP ? R.drawable.img_lake : t.cat == Content.CAT_ENERGY ? R.drawable.img_hero
                : t.cat == Content.CAT_FOCUS ? R.drawable.img_ocean : R.drawable.img_cliff;
        ImageView bg = Ui.image(a, img, 2);
        bg.setAlpha(0.22f);
        root.addView(bg, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        Ui.kenBurns(bg, 40000);
        View scrim = new View(a);
        scrim.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xCC0B0C10, 0x660B0C10, 0xF20B0C10}));
        root.addView(scrim, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        embers = new EmberView(a);
        embers.ambient = 0;
        embers.color = t.color;

        LinearLayout col = Ui.vbox(a);
        col.setPadding(0, a.statusBar, 0, 0);
        root.addView(col, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        col.addView(topBar(a, t.name, t.en, t.color, new Runnable() {
            @Override
            public void run() {
                a.pop();
            }
        }));

        int sw = a.getResources().getDisplayMetrics().widthPixels;
        int sh = a.getResources().getDisplayMetrics().heightPixels;
        smallH = (int) Math.min(sw * 0.8f, sh * 0.36f);
        bigH = (int) Math.min(sw * 1.0f, sh * 0.58f);
        bv = new BreathView(a);
        bv.color = t.color;
        bv.mode = t.mode;
        bv.driver = this;
        col.addView(bv, Ui.lp(Ui.MATCH, smallH));

        panel = new FrameLayout(a);
        col.addView(panel, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1));
        setupPanel = buildSetup();
        runPanel = buildRun();
        donePanel = Ui.vbox(a);
        panel.addView(setupPanel, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        panel.addView(runPanel, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        panel.addView(donePanel, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        runPanel.setVisibility(View.GONE);
        donePanel.setVisibility(View.GONE);
        root.addView(embers, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        return root;
    }

    static LinearLayout topBar(final MainActivity a, String title, String en, int color, final Runnable onClose) {
        LinearLayout bar = Ui.hbox(a);
        bar.setPadding(Ui.dp(12), Ui.dp(8), Ui.dp(12), Ui.dp(4));
        FrameLayout close = new FrameLayout(a);
        close.setBackground(Ui.round(0x22FFFFFF, 22));
        close.addView(new IconView(a, IconView.CLOSE, Ui.TEXT), new FrameLayout.LayoutParams(Ui.dp(18), Ui.dp(18), Gravity.CENTER));
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClose.run();
            }
        });
        Ui.pressable(close);
        bar.addView(close, Ui.lp(Ui.dp(42), Ui.dp(42)));
        LinearLayout tt = Ui.vbox(a);
        tt.setGravity(Gravity.CENTER);
        tt.addView(Ui.tag(a, en, color));
        TextView tv = Ui.text(a, title, 18, Ui.TEXT, Ui.BOLD);
        tv.setPadding(0, Ui.dp(3), 0, 0);
        tt.addView(tv);
        bar.addView(tt, Ui.weight(1));
        View sp = new View(a);
        bar.addView(sp, Ui.lp(Ui.dp(42), Ui.dp(42)));
        return bar;
    }

    // ------------------------------------------------------------------ setup
    private View buildSetup() {
        ScrollView sv = new ScrollView(a);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout c = Ui.vbox(a);
        c.setPadding(Ui.dp(20), Ui.dp(6), Ui.dp(20), Ui.dp(28));
        sv.addView(c);

        TextView about = Ui.text(a, t.about, 15, 0xE6EDE6DA, Ui.REG);
        about.setLineSpacing(0, 1.3f);
        c.addView(about);

        TextView ben = Ui.text(a, t.benefits, 13, t.color, Ui.BOLD);
        ben.setPadding(0, Ui.dp(12), 0, Ui.dp(4));
        c.addView(ben);

        LinearLayout how = Ui.vbox(a);
        how.setPadding(Ui.dp(16), Ui.dp(14), Ui.dp(16), Ui.dp(14));
        how.setBackground(Ui.round(0xCC15171D, 18, 0x17FFFFFF, 1));
        how.addView(Ui.tag(a, "איך מבצעים", Ui.BRASS));
        for (int i = 0; i < t.how.length; i++) {
            LinearLayout r = Ui.hbox(a);
            r.setGravity(Gravity.TOP);
            TextView n = Ui.text(a, String.valueOf(i + 1), 13, t.color, Ui.NUM);
            n.setGravity(Gravity.CENTER);
            n.setBackground(Ui.round(Ui.alpha(t.color, 0.14f), 12));
            r.addView(n, Ui.lp(Ui.dp(24), Ui.dp(24)));
            TextView s = Ui.text(a, t.how[i], 14, Ui.TEXT, Ui.REG);
            s.setPadding(Ui.dp(10), Ui.dp(3), 0, 0);
            r.addView(s, Ui.weight(1));
            how.addView(r, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 10, 0, 0));
        }
        c.addView(how, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 10, 0, 0));

        if (t.warning != null) {
            TextView w = Ui.text(a, "⚠  " + t.warning, 13, 0xFFFFC27A, Ui.REG);
            w.setLineSpacing(0, 1.25f);
            w.setPadding(Ui.dp(14), Ui.dp(12), Ui.dp(14), Ui.dp(12));
            w.setBackground(Ui.round(0x22FFA23A, 14, 0x55FFA23A, 1));
            c.addView(w, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 12, 0, 0));
        }

        // rounds stepper
        LinearLayout st = Ui.hbox(a);
        st.setPadding(Ui.dp(16), Ui.dp(10), Ui.dp(16), Ui.dp(10));
        st.setBackground(Ui.round(0xCC15171D, 18, 0x17FFFFFF, 1));
        LinearLayout lab = Ui.vbox(a);
        lab.addView(Ui.text(a, "סבבים", 15, Ui.TEXT, Ui.BOLD));
        minutesV = Ui.text(a, "", 12, Ui.MUTED, Ui.REG);
        lab.addView(minutesV);
        st.addView(lab, Ui.weight(1));
        st.addView(stepBtn(IconView.MINUS, -1));
        roundsV = Ui.text(a, "", 26, Ui.TEXT, Ui.NUM);
        roundsV.setGravity(Gravity.CENTER);
        st.addView(roundsV, Ui.lp(Ui.dp(56), Ui.WRAP));
        st.addView(stepBtn(IconView.PLUS, 1));
        c.addView(st, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 12, 0, 0));
        updateRounds();

        LinearLayout toggles = Ui.hbox(a);
        toggles.addView(toggle(IconView.NOTE, "צלילי הנחיה", "snd"), Ui.weight(1));
        View gap = new View(a);
        toggles.addView(gap, Ui.lp(Ui.dp(10), 1));
        toggles.addView(toggle(IconView.VIBRATE, "רטט", "vib"), Ui.weight(1));
        c.addView(toggles, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 12, 0, 0));

        TextView start = Ui.button(a, "התחל", t.color, 0xFF140A05);
        start.setTextSize(18);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                begin();
            }
        });
        c.addView(start, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 18, 0, 0));
        Ui.stagger(c, 150);
        return sv;
    }

    private View stepBtn(int icon, final int d) {
        FrameLayout b = new FrameLayout(a);
        b.setBackground(Ui.round(0x1FFFFFFF, 20));
        b.addView(new IconView(a, icon, Ui.TEXT), new FrameLayout.LayoutParams(Ui.dp(16), Ui.dp(16), Gravity.CENTER));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rounds = Math.max(1, Math.min(40, rounds + d));
                updateRounds();
                Ui.pop(roundsV);
            }
        });
        Ui.pressable(b);
        LinearLayout.LayoutParams lp = Ui.lp(Ui.dp(40), Ui.dp(40));
        b.setLayoutParams(lp);
        return b;
    }

    private void updateRounds() {
        roundsV.setText(String.valueOf(rounds));
        minutesV.setText("כ־" + t.minutes(rounds) + " דקות");
    }

    View toggle(int icon, String label, final String key) {
        final LinearLayout b = Ui.hbox(a);
        b.setGravity(Gravity.CENTER);
        b.setPadding(Ui.dp(12), Ui.dp(12), Ui.dp(12), Ui.dp(12));
        final IconView ic = new IconView(a, icon, Ui.TEXT);
        b.addView(ic, Ui.lp(Ui.dp(18), Ui.dp(18)));
        final TextView tv = Ui.text(a, "  " + label, 14, Ui.TEXT, Ui.BOLD);
        b.addView(tv);
        final Runnable paint = new Runnable() {
            @Override
            public void run() {
                boolean on = a.stats.flag(key, true);
                b.setBackground(on ? Ui.round(Ui.alpha(t.color, 0.16f), 16, Ui.alpha(t.color, 0.6f), 1) : Ui.round(0x0DFFFFFF, 16, 0x17FFFFFF, 1));
                ic.set(ic.type, on ? t.color : Ui.DIM);
                tv.setTextColor(on ? Ui.TEXT : Ui.DIM);
            }
        };
        paint.run();
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                a.stats.setFlag(key, !a.stats.flag(key, true));
                paint.run();
            }
        });
        Ui.pressable(b);
        return b;
    }

    // ------------------------------------------------------------------ run
    private View buildRun() {
        LinearLayout c = Ui.vbox(a);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(Ui.dp(20), Ui.dp(4), Ui.dp(20), Ui.dp(20));
        LinearLayout info = Ui.hbox(a);
        roundLabel = Ui.text(a, "", 15, Ui.TEXT, Ui.BOLD);
        info.addView(roundLabel, Ui.weight(1));
        timeLabel = Ui.text(a, "", 15, Ui.MUTED, Ui.NUM);
        info.addView(timeLabel);
        c.addView(info, Ui.lp(Ui.MATCH, Ui.WRAP));

        continueBtn = Ui.button(a, "נשמתי – המשך", t.color, 0xFF140A05);
        continueBtn.setVisibility(View.GONE);
        continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                releaseHold();
            }
        });
        c.addView(continueBtn, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 14, 0, 0));

        View sp = new View(a);
        c.addView(sp, new LinearLayout.LayoutParams(1, 0, 1));

        LinearLayout ctr = Ui.hbox(a);
        ctr.setGravity(Gravity.CENTER);
        FrameLayout stop = new FrameLayout(a);
        stop.setBackground(Ui.round(0x1FFFFFFF, 28));
        stop.addView(new IconView(a, IconView.STOP, Ui.TEXT), new FrameLayout.LayoutParams(Ui.dp(20), Ui.dp(20), Gravity.CENTER));
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(true);
            }
        });
        Ui.pressable(stop);
        ctr.addView(stop, Ui.lp(Ui.dp(56), Ui.dp(56)));
        FrameLayout pause = new FrameLayout(a);
        pause.setBackground(Ui.round(t.color, 40));
        pause.setElevation(Ui.dp(8));
        pauseIcon = new IconView(a, IconView.PAUSE, 0xFF140A05);
        pause.addView(pauseIcon, new FrameLayout.LayoutParams(Ui.dp(28), Ui.dp(28), Gravity.CENTER));
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePause();
            }
        });
        Ui.pressable(pause);
        ctr.addView(pause, Ui.lpm(Ui.dp(76), Ui.dp(76), 22, 0, 22, 0));
        FrameLayout snd = new FrameLayout(a);
        final IconView sndIc = new IconView(a, IconView.NOTE, a.stats.flag("snd", true) ? t.color : Ui.DIM);
        snd.setBackground(Ui.round(0x1FFFFFFF, 28));
        snd.addView(sndIc, new FrameLayout.LayoutParams(Ui.dp(20), Ui.dp(20), Gravity.CENTER));
        snd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean on = !a.stats.flag("snd", true);
                a.stats.setFlag("snd", on);
                sndIc.set(IconView.NOTE, on ? t.color : Ui.DIM);
            }
        });
        Ui.pressable(snd);
        ctr.addView(snd, Ui.lp(Ui.dp(56), Ui.dp(56)));
        c.addView(ctr);
        return c;
    }

    private void resizeOrb(int to) {
        final ViewGroup.LayoutParams lp = bv.getLayoutParams();
        ValueAnimator va = ValueAnimator.ofInt(lp.height, to);
        va.setDuration(600);
        va.setInterpolator(new DecelerateInterpolator(2f));
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator an) {
                lp.height = (Integer) an.getAnimatedValue();
                bv.setLayoutParams(lp);
            }
        });
        va.start();
    }

    private void crossfade(final View from, View to) {
        from.animate().alpha(0f).setDuration(220).withEndAction(new Runnable() {
            @Override
            public void run() {
                from.setVisibility(View.GONE);
            }
        }).start();
        to.setVisibility(View.VISIBLE);
        to.setAlpha(0f);
        to.animate().alpha(1f).setStartDelay(150).setDuration(350).start();
    }

    private void begin() {
        seq.clear();
        for (int r = 0; r < rounds; r++) seq.addAll(t.round);
        state = COUNTDOWN;
        countStart = System.currentTimeMillis();
        paused = false;
        pausedTotal = 0;
        bestThisSession = 0;
        newRecord = false;
        pauseIcon.set(IconView.PAUSE, 0xFF140A05);
        crossfade(setupPanel, runPanel);
        resizeOrb(bigH);
        roundLabel.setText("מתכוננים");
        a.keepScreenOn(true);
    }

    private void startPhase(int i, long at) {
        idx = i;
        if (i >= seq.size()) {
            finish(false);
            return;
        }
        Content.Phase ph = seq.get(i);
        phaseStart = at;
        sFrom = bv.scale;
        sTo = BreathView.target(ph.type, sFrom);
        bv.phaseType = ph.type;
        bv.label = ph.label;
        bv.sub = ph.sub == null ? "" : ph.sub;
        bv.boxSide = i % 4;
        int per = t.round.size();
        roundLabel.setText("סבב " + (i / per + 1) + " מתוך " + rounds);
        continueBtn.setVisibility(ph.type == Content.OPEN ? View.VISIBLE : View.GONE);
        if (ph.type == Content.OPEN) Ui.pop(continueBtn);

        boolean snd = a.stats.flag("snd", true);
        SoundEngine e = SoundEngine.get(a);
        if (ph.type == Content.OPEN) {
            if (snd) e.bell(220f, 0.3f);
            a.buzz(60);
            return;
        }
        if (ph.sec >= 0.9f) {
            if (snd) {
                switch (ph.type) {
                    case Content.IN: e.tone(174f, 262f, ph.sec, 0.13f); break;
                    case Content.IN2: e.tone(262f, 311f, ph.sec, 0.12f); break;
                    case Content.OUT: e.tone(262f, 165f, ph.sec, 0.12f); break;
                    default: e.tick(0.12f);
                }
            }
            if (ph.sec >= 2f) a.buzz(28);
        }
    }

    @Override
    public void frame(BreathView v, long now) {
        if (state == SETUP || state == DONE) {
            float x = (float) Math.sin(now / 1000.0 * 2 * Math.PI / 6.0);
            v.scale = 0.58f + 0.08f * x;
            v.phaseProgress = 0;
            v.phaseType = -1;
            v.label = state == DONE ? "" : "";
            v.big = "";
            v.sub = "";
            return;
        }
        if (state == COUNTDOWN) {
            float el = (now - countStart) / 1000f;
            int n = 3 - (int) el;
            v.label = "התכונן";
            v.sub = "שב זקוף · כתפיים למטה";
            v.big = String.valueOf(Math.max(1, n));
            v.phaseProgress = el - (int) el;
            v.scale = 0.5f;
            if (el >= 3f) {
                state = RUN;
                sessionStart = now;
                startPhase(0, now);
            }
            return;
        }
        // RUN
        long ref = paused ? pausedAt : now;
        Content.Phase ph = seq.get(idx);
        float el = (ref - phaseStart) / 1000f;
        if (ph.type == Content.OPEN) {
            v.big = Ui.mmss((long) el);
            v.phaseProgress = (el % 60f) / 60f;
            v.scale = sFrom;
        } else {
            if (!paused && el >= ph.sec) {
                long next = phaseStart + (long) (ph.sec * 1000);
                v.scale = sTo;
                startPhase(idx + 1, next);
                if (state != RUN) return;
                ph = seq.get(idx);
                el = (now - phaseStart) / 1000f;
            }
            float prog = ph.sec <= 0 ? 1 : Math.min(1f, el / ph.sec);
            v.phaseProgress = prog;
            v.scale = sFrom + (sTo - sFrom) * BreathView.ease(prog);
            if (ph.sub != null && ph.sub.contains("/")) {
                v.big = ph.sub.substring(0, ph.sub.indexOf('/'));
                v.sub = "מתוך " + ph.sub.substring(ph.sub.indexOf('/') + 1);
            } else {
                v.big = String.valueOf((int) Math.ceil(Math.max(0.01f, ph.sec - el)));
            }
        }
        if (paused) {
            v.label = "מושהה";
        }
        v.totalProgress = (idx + Math.min(1f, v.phaseProgress)) / seq.size();
        long total = (ref - sessionStart - pausedTotal) / 1000;
        if (total != lastShown) {
            lastShown = total;
            timeLabel.setText(Ui.mmss(total));
        }
    }

    private void releaseHold() {
        if (state != RUN || seq.get(idx).type != Content.OPEN) return;
        long now = System.currentTimeMillis();
        int held = (int) ((now - phaseStart) / 1000);
        bestThisSession = Math.max(bestThisSession, held);
        if (a.stats.recordHold(held)) newRecord = true;
        startPhase(idx + 1, now);
    }

    private void togglePause() {
        if (state != RUN) return;
        long now = System.currentTimeMillis();
        if (!paused) {
            paused = true;
            pausedAt = now;
            pauseIcon.set(IconView.PLAY, 0xFF140A05);
        } else {
            paused = false;
            long d = now - pausedAt;
            phaseStart += d;
            pausedTotal += d;
            pauseIcon.set(IconView.PAUSE, 0xFF140A05);
            bv.label = seq.get(idx).label;
        }
    }

    private void finish(boolean early) {
        if (state == DONE) return;
        long now = System.currentTimeMillis();
        long secs = state == RUN ? ((paused ? pausedAt : now) - sessionStart - pausedTotal) / 1000 : 0;
        state = DONE;
        a.keepScreenOn(false);
        continueBtn.setVisibility(View.GONE);
        if (early && secs < 20) {
            a.pop();
            return;
        }
        a.stats.addSession(secs);
        SoundEngine.get(a).bell(196f, 0.35f);
        a.buzz(120);
        resizeOrb(smallH);
        buildDone(secs, early);
        crossfade(runPanel, donePanel);
        bv.post(new Runnable() {
            @Override
            public void run() {
                embers.burst(embers.getWidth() / 2f, bv.getTop() + smallH / 2f);
            }
        });
    }

    private void buildDone(long secs, boolean early) {
        donePanel.removeAllViews();
        donePanel.setGravity(Gravity.CENTER_HORIZONTAL);
        donePanel.setPadding(Ui.dp(20), Ui.dp(4), Ui.dp(20), Ui.dp(20));
        FrameLayout ok = new FrameLayout(a);
        ok.setBackground(Ui.round(Ui.alpha(t.color, 0.18f), 34, Ui.alpha(t.color, 0.7f), 1.5f));
        ok.addView(new IconView(a, IconView.CHECK, t.color), new FrameLayout.LayoutParams(Ui.dp(30), Ui.dp(30), Gravity.CENTER));
        donePanel.addView(ok, Ui.lp(Ui.dp(64), Ui.dp(64)));
        Ui.pop(ok);
        TextView h = Ui.text(a, early ? "עצרת – וזה גם בסדר." : "משימה הושלמה.", 26, Ui.TEXT, Ui.BOLD);
        h.setGravity(Gravity.CENTER);
        h.setPadding(0, Ui.dp(12), 0, Ui.dp(4));
        donePanel.addView(h);
        TextView s = Ui.text(a, "כל נשימה מודעת היא חזרה באימון של הראש.", 14, Ui.MUTED, Ui.REG);
        s.setGravity(Gravity.CENTER);
        donePanel.addView(s);

        LinearLayout row = Ui.hbox(a);
        row.setBackground(Ui.round(0xCC15171D, 18, 0x17FFFFFF, 1));
        row.setPadding(0, Ui.dp(14), 0, Ui.dp(14));
        cell(row, Ui.mmss(secs), "זמן");
        cell(row, String.valueOf(a.stats.streak()), "ימי רצף");
        if (t.id.equals("power")) cell(row, bestThisSession == 0 ? "—" : Ui.mmss(bestThisSession), newRecord ? "שיא חדש!" : "עצירה הכי ארוכה");
        else cell(row, String.valueOf(a.stats.minutes()), "דקות סה״כ");
        donePanel.addView(row, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, 18, 0, 0));

        View sp = new View(a);
        donePanel.addView(sp, new LinearLayout.LayoutParams(1, 0, 1));
        LinearLayout btns = Ui.hbox(a);
        TextView again = Ui.ghostButton(a, "עוד סבב", t.color);
        again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                state = SETUP;
                begin();
                crossfade(donePanel, runPanel);
            }
        });
        btns.addView(again, Ui.weight(1));
        View g = new View(a);
        btns.addView(g, Ui.lp(Ui.dp(12), 1));
        TextView close = Ui.button(a, "סיום", t.color, 0xFF140A05);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                a.pop();
            }
        });
        btns.addView(close, Ui.weight(1));
        donePanel.addView(btns, Ui.lp(Ui.MATCH, Ui.WRAP));
    }

    private void cell(LinearLayout row, String v, String l) {
        LinearLayout c = Ui.vbox(a);
        c.setGravity(Gravity.CENTER);
        c.addView(Ui.text(a, v, 24, t.color, Ui.NUM));
        TextView lt = Ui.text(a, l, 12, Ui.MUTED, Ui.REG);
        lt.setPadding(0, Ui.dp(4), 0, 0);
        c.addView(lt);
        row.addView(c, Ui.weight(1));
    }

    @Override
    boolean onBack() {
        if (state == RUN || state == COUNTDOWN) {
            finish(true);
            return true;
        }
        return false;
    }

    @Override
    void onHide() {
        state = DONE;
        a.keepScreenOn(false);
    }
}
