package com.yair.ironbreath;

import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Timed guided meditation with cross-fading guidance text, optional voice and ambient drone. */
final class MeditationSession extends Screen {
    private final Content.Meditation m;
    private final Handler h = new Handler(Looper.getMainLooper());
    private AuraView aura;
    private TextView guide, stepV, timeV;
    private IconView pauseIc, voiceIc, ambIc;
    private float elapsed;
    private int step = -1;
    private boolean paused, running, done, ownsAmbient;
    private long last;
    private LinearLayout controls;
    private TextView finishBtn;
    private EmberView embers;

    MeditationSession(MainActivity a, Content.Meditation m) {
        super(a);
        this.m = m;
    }

    @Override
    View create() {
        FrameLayout root = new FrameLayout(a);
        root.setBackgroundColor(Ui.BG);
        root.setClickable(true);
        ImageView bg = Ui.image(a, m.image, 2);
        root.addView(bg, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        Ui.kenBurns(bg, 45000);
        View scrim = new View(a);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xE60B0C10, 0x990B0C10, 0xF50B0C10}));
        root.addView(scrim, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        embers = new EmberView(a);
        embers.ambient = 14;
        embers.color = m.color;
        root.addView(embers, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        LinearLayout col = Ui.vbox(a);
        col.setPadding(0, a.statusBar, 0, Ui.dp(24));
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(col, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        col.addView(BreathSession.topBar(a, m.title, m.en, m.color, new Runnable() {
            @Override
            public void run() {
                a.pop();
            }
        }), Ui.lp(Ui.MATCH, Ui.WRAP));

        int sw = a.getResources().getDisplayMetrics().widthPixels;
        int sh = a.getResources().getDisplayMetrics().heightPixels;
        int size = (int) Math.min(sw * 0.78f, sh * 0.38f);
        FrameLayout auraBox = new FrameLayout(a);
        aura = new AuraView(a);
        aura.color = m.color;
        auraBox.addView(aura, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        LinearLayout center = Ui.vbox(a);
        center.setGravity(Gravity.CENTER);
        timeV = Ui.text(a, Ui.mmss(m.total()), 34, 0xFFFFFFFF, Ui.NUM);
        timeV.setShadowLayer(Ui.dp(8), 0, 0, 0xAA000000);
        center.addView(timeV);
        stepV = Ui.text(a, "", 12, 0xCCEDE6DA, Ui.REG);
        stepV.setPadding(0, Ui.dp(4), 0, 0);
        center.addView(stepV);
        auraBox.addView(center, new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP, Gravity.CENTER));
        col.addView(auraBox, Ui.lpm(Ui.MATCH, size, 0, 12, 0, 0));

        guide = Ui.text(a, "מצא תנוחה נוחה.\nכשתהיה מוכן – לחץ התחל.", 20, Ui.TEXT, Ui.REG);
        guide.setGravity(Gravity.CENTER);
        guide.setLineSpacing(0, 1.35f);
        guide.setPadding(Ui.dp(28), Ui.dp(16), Ui.dp(28), 0);
        col.addView(guide, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1));

        controls = Ui.hbox(a);
        controls.setGravity(Gravity.CENTER);
        FrameLayout voice = roundBtn(IconView.VOICE);
        voiceIc = (IconView) voice.getChildAt(0);
        voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean on = !a.stats.flag("voice", false);
                a.stats.setFlag("voice", on);
                paintToggles();
                if (on && step >= 0) Voice.get(a).say(m.steps[step]);
                else Voice.get(a).stop();
            }
        });
        controls.addView(voice, Ui.lp(Ui.dp(56), Ui.dp(56)));
        FrameLayout play = new FrameLayout(a);
        play.setBackground(Ui.round(m.color, 40));
        play.setElevation(Ui.dp(8));
        pauseIc = new IconView(a, IconView.PLAY, 0xFF140A05);
        play.addView(pauseIc, new FrameLayout.LayoutParams(Ui.dp(28), Ui.dp(28), Gravity.CENTER));
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });
        Ui.pressable(play);
        controls.addView(play, Ui.lpm(Ui.dp(78), Ui.dp(78), 24, 0, 24, 0));
        FrameLayout amb = roundBtn(IconView.NOTE);
        ambIc = (IconView) amb.getChildAt(0);
        amb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean on = !a.stats.flag("ambient", true);
                a.stats.setFlag("ambient", on);
                paintToggles();
                if (running) ambient(on);
            }
        });
        controls.addView(amb, Ui.lp(Ui.dp(56), Ui.dp(56)));
        col.addView(controls);

        finishBtn = Ui.button(a, "סיום", m.color, 0xFF140A05);
        finishBtn.setVisibility(View.GONE);
        finishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                a.pop();
            }
        });
        col.addView(finishBtn, Ui.lpm(Ui.MATCH, Ui.WRAP, 24, 0, 24, 0));
        paintToggles();
        aura.paused = true;
        return root;
    }

    private FrameLayout roundBtn(int icon) {
        FrameLayout b = new FrameLayout(a);
        b.setBackground(Ui.round(0x1FFFFFFF, 28));
        b.addView(new IconView(a, icon, Ui.TEXT), new FrameLayout.LayoutParams(Ui.dp(20), Ui.dp(20), Gravity.CENTER));
        Ui.pressable(b);
        return b;
    }

    private void paintToggles() {
        voiceIc.set(IconView.VOICE, a.stats.flag("voice", false) ? m.color : Ui.DIM);
        ambIc.set(IconView.NOTE, a.stats.flag("ambient", true) ? m.color : Ui.DIM);
    }

    private void ambient(boolean on) {
        SoundEngine e = SoundEngine.get(a);
        if (on && !e.on(Content.S_DRONE)) {
            e.set(Content.S_DRONE, 0.4f);
            ownsAmbient = true;
        } else if (!on && ownsAmbient) {
            e.set(Content.S_DRONE, 0);
            ownsAmbient = false;
        }
    }

    private void toggle() {
        if (done) return;
        if (!running) {
            running = true;
            paused = false;
            last = System.currentTimeMillis();
            SoundEngine.get(a).bell(174.6f, 0.4f);
            if (a.stats.flag("ambient", true)) ambient(true);
            a.keepScreenOn(true);
            h.post(tick);
        } else {
            paused = !paused;
            last = System.currentTimeMillis();
            if (paused) Voice.get(a).stop();
        }
        aura.paused = paused;
        pauseIc.set(paused ? IconView.PLAY : IconView.PAUSE, 0xFF140A05);
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running || done) return;
            long now = System.currentTimeMillis();
            if (!paused) elapsed += (now - last) / 1000f;
            last = now;
            int total = m.total();
            aura.progress = Math.min(1f, elapsed / total);
            timeV.setText(Ui.mmss((long) Math.ceil(total - elapsed)));
            int acc = 0, s = m.steps.length - 1;
            for (int i = 0; i < m.steps.length; i++) {
                acc += m.secs[i];
                if (elapsed < acc) {
                    s = i;
                    break;
                }
            }
            if (s != step) showStep(s);
            if (elapsed >= total) {
                complete();
                return;
            }
            h.postDelayed(this, 250);
        }
    };

    private void showStep(final int s) {
        step = s;
        stepV.setText("שלב " + (s + 1) + " מתוך " + m.steps.length);
        guide.animate().alpha(0f).translationY(-Ui.dp(8)).setDuration(450).withEndAction(new Runnable() {
            @Override
            public void run() {
                guide.setText(m.steps[s]);
                guide.setTranslationY(Ui.dp(10));
                guide.animate().alpha(1f).translationY(0).setDuration(700).start();
            }
        }).start();
        if (s > 0) SoundEngine.get(a).bell(261.6f, 0.12f);
        if (a.stats.flag("voice", false)) Voice.get(a).say(m.steps[s]);
    }

    private void complete() {
        done = true;
        a.stats.addSession((long) elapsed);
        SoundEngine e = SoundEngine.get(a);
        e.bell(174.6f, 0.4f);
        e.bell(261.6f, 0.25f);
        if (ownsAmbient) ambient(false);
        a.keepScreenOn(false);
        aura.progress = 1f;
        timeV.setText("✓");
        guide.animate().alpha(0f).setDuration(400).withEndAction(new Runnable() {
            @Override
            public void run() {
                guide.setText("סיימת.\nקח את השקט הזה איתך להמשך היום.");
                guide.animate().alpha(1f).setDuration(700).start();
            }
        }).start();
        if (a.stats.flag("voice", false)) Voice.get(a).say("סיימת. קח את השקט הזה איתך להמשך היום.");
        controls.setVisibility(View.GONE);
        finishBtn.setVisibility(View.VISIBLE);
        Ui.pop(finishBtn);
    }

    @Override
    void onHide() {
        running = false;
        h.removeCallbacks(tick);
        Voice.get(a).stop();
        if (ownsAmbient) ambient(false);
        if (!done && elapsed > 60) a.stats.addSession((long) elapsed);
        done = true;
        a.keepScreenOn(false);
    }
}
