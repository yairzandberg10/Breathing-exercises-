package com.yair.ironbreath;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Guided yoga flow: animated figure, timer per pose, segmented progress, voice cues. */
final class YogaSession extends Screen {
    private final Content.Routine r;
    private final Handler h = new Handler(Looper.getMainLooper());
    private PoseView pose;
    private TextView nameV, sansV, cueV, timeV, nextV;
    private IconView playIc, voiceIc;
    private Segments seg;
    private int idx = -1;
    private float left;
    private boolean running, paused, done, prep;
    private long last, startedAt;
    private float activeSecs;
    private int lastBeep = -1;
    private LinearLayout controls;
    private TextView finishBtn;
    private EmberView embers;

    YogaSession(MainActivity a, Content.Routine r) {
        super(a);
        this.r = r;
    }

    @Override
    View create() {
        FrameLayout root = new FrameLayout(a);
        root.setBackgroundColor(Ui.BG);
        root.setClickable(true);
        LinearLayout col = Ui.vbox(a);
        col.setPadding(0, a.statusBar, 0, Ui.dp(22));
        root.addView(col, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        embers = new EmberView(a);
        embers.ambient = 0;
        embers.color = r.color;
        root.addView(embers, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        col.addView(BreathSession.topBar(a, r.name, r.en, r.color, new Runnable() {
            @Override
            public void run() {
                a.pop();
            }
        }), Ui.lp(Ui.MATCH, Ui.WRAP));

        seg = new Segments(a);
        col.addView(seg, Ui.lpm(Ui.MATCH, Ui.dp(6), 20, 10, 20, 0));

        int sh = a.getResources().getDisplayMetrics().heightPixels;
        FrameLayout stage = new FrameLayout(a);
        stage.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Ui.BG, Ui.mix(r.color, Ui.BG, 0.9f), Ui.BG}));
        pose = new PoseView(a);
        pose.color = r.color;
        pose.setPose(Content.pose(r.poses[0]), false);
        stage.addView(pose, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        timeV = Ui.text(a, "", 40, 0xFFFFFFFF, Ui.NUM);
        FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.LEFT);
        tl.setMargins(Ui.dp(22), Ui.dp(14), 0, 0);
        stage.addView(timeV, tl);
        col.addView(stage, Ui.lp(Ui.MATCH, (int) (sh * 0.4f)));

        LinearLayout info = Ui.vbox(a);
        info.setPadding(Ui.dp(22), Ui.dp(8), Ui.dp(22), 0);
        sansV = Ui.tag(a, "", r.color);
        info.addView(sansV);
        nameV = Ui.text(a, "", 30, Ui.TEXT, Ui.BOLD);
        nameV.setPadding(0, Ui.dp(4), 0, Ui.dp(6));
        info.addView(nameV);
        cueV = Ui.text(a, "", 15, 0xE6EDE6DA, Ui.REG);
        cueV.setLineSpacing(0, 1.3f);
        info.addView(cueV);
        nextV = Ui.text(a, "", 13, Ui.MUTED, Ui.REG);
        nextV.setPadding(0, Ui.dp(10), 0, 0);
        info.addView(nextV);
        col.addView(info, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1));

        controls = Ui.hbox(a);
        controls.setGravity(Gravity.CENTER);
        FrameLayout prev = btn(IconView.RIGHT);
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (running && idx > 0) go(idx - 1);
            }
        });
        controls.addView(prev, Ui.lp(Ui.dp(52), Ui.dp(52)));
        FrameLayout voice = btn(IconView.VOICE);
        voiceIc = (IconView) voice.getChildAt(0);
        voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean on = !a.stats.flag("voice", false);
                a.stats.setFlag("voice", on);
                if (on) Voice.get(a).warm();
                else Voice.get(a).stop();
                paintVoice();
            }
        });
        controls.addView(voice, Ui.lpm(Ui.dp(52), Ui.dp(52), 14, 0, 0, 0));
        FrameLayout play = new FrameLayout(a);
        play.setBackground(Ui.round(r.color, 40));
        play.setElevation(Ui.dp(8));
        playIc = new IconView(a, IconView.PLAY, 0xFF140A05);
        play.addView(playIc, new FrameLayout.LayoutParams(Ui.dp(28), Ui.dp(28), Gravity.CENTER));
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });
        Ui.pressable(play);
        controls.addView(play, Ui.lpm(Ui.dp(76), Ui.dp(76), 18, 0, 18, 0));
        FrameLayout snd = btn(IconView.NOTE);
        final IconView sndIc = (IconView) snd.getChildAt(0);
        sndIc.set(IconView.NOTE, a.stats.flag("snd", true) ? r.color : Ui.DIM);
        snd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean on = !a.stats.flag("snd", true);
                a.stats.setFlag("snd", on);
                sndIc.set(IconView.NOTE, on ? r.color : Ui.DIM);
            }
        });
        controls.addView(snd, Ui.lpm(Ui.dp(52), Ui.dp(52), 0, 0, 14, 0));
        FrameLayout next = btn(IconView.LEFT);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (running) go(idx + 1);
            }
        });
        controls.addView(next, Ui.lp(Ui.dp(52), Ui.dp(52)));
        col.addView(controls);

        finishBtn = Ui.button(a, "סיום", r.color, 0xFF140A05);
        finishBtn.setVisibility(View.GONE);
        finishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                a.pop();
            }
        });
        col.addView(finishBtn, Ui.lpm(Ui.MATCH, Ui.WRAP, 22, 0, 22, 0));

        paintVoice();
        showPoseInfo(0);
        timeV.setText(Ui.mmss(r.total()));
        nextV.setText(r.poses.length + " תנוחות · " + Math.round(r.total() / 60f) + " דקות. לחץ ▶ כשאתה על המזרן.");
        return root;
    }

    private FrameLayout btn(int icon) {
        FrameLayout b = new FrameLayout(a);
        b.setBackground(Ui.round(0x1FFFFFFF, 26));
        b.addView(new IconView(a, icon, Ui.TEXT), new FrameLayout.LayoutParams(Ui.dp(20), Ui.dp(20), Gravity.CENTER));
        Ui.pressable(b);
        return b;
    }

    private void paintVoice() {
        voiceIc.set(IconView.VOICE, a.stats.flag("voice", false) ? r.color : Ui.DIM);
    }

    private void showPoseInfo(int i) {
        Content.Pose p = Content.pose(r.poses[i]);
        sansV.setText(p.sanskrit);
        nameV.setText(p.name);
        cueV.setText(p.cue);
    }

    private void toggle() {
        if (done) return;
        if (!running) {
            running = true;
            startedAt = System.currentTimeMillis();
            last = startedAt;
            a.keepScreenOn(true);
            prep = true;
            left = 5;
            nameV.setText("התכונן");
            sansV.setText("GET READY");
            cueV.setText("עמוד על המזרן. נשימה עמוקה. אנחנו מתחילים עם " + Content.pose(r.poses[0]).name + ".");
            nextV.setText("");
            if (a.stats.flag("voice", false)) Voice.get(a).say("התכונן. מתחילים עם " + Content.pose(r.poses[0]).name);
            h.post(tick);
        } else {
            paused = !paused;
            last = System.currentTimeMillis();
            pose.breathe = !paused;
            if (paused) Voice.get(a).stop();
        }
        playIc.set(paused ? IconView.PLAY : IconView.PAUSE, 0xFF140A05);
    }

    private void go(int i) {
        prep = false;
        if (i >= r.poses.length) {
            complete();
            return;
        }
        idx = i;
        left = r.secs[i];
        lastBeep = -1;
        Content.Pose p = Content.pose(r.poses[i]);
        pose.setPose(p, true);
        showPoseInfo(i);
        nameV.setAlpha(0f);
        nameV.setTranslationY(Ui.dp(10));
        nameV.animate().alpha(1f).translationY(0).setDuration(400).start();
        cueV.setAlpha(0f);
        cueV.animate().alpha(1f).setStartDelay(150).setDuration(500).start();
        if (i + 1 < r.poses.length) nextV.setText("הבא: " + Content.pose(r.poses[i + 1]).name);
        else nextV.setText("אחרונה – תן הכל.");
        if (a.stats.flag("snd", true)) SoundEngine.get(a).bell(i == 0 ? 196f : 261.6f, 0.22f);
        a.buzz(40);
        if (a.stats.flag("voice", false)) Voice.get(a).say(p.name + ". " + p.cue);
        seg.invalidate();
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running || done) return;
            long now = System.currentTimeMillis();
            float dt = (now - last) / 1000f;
            last = now;
            if (!paused) {
                left -= dt;
                if (!prep) activeSecs += dt;
                int sec = (int) Math.ceil(left);
                if (sec <= 3 && sec >= 1 && sec != lastBeep) {
                    lastBeep = sec;
                    if (a.stats.flag("snd", true)) SoundEngine.get(a).tick(0.18f);
                }
                if (left <= 0) {
                    go(prep ? 0 : idx + 1);
                    if (done) return;
                }
            }
            timeV.setText(Ui.mmss((long) Math.ceil(Math.max(0, left))));
            seg.invalidate();
            h.postDelayed(this, 100);
        }
    };

    private void complete() {
        done = true;
        a.stats.addSession((long) activeSecs);
        a.keepScreenOn(false);
        pose.setPose(Content.pose("lotus"), true);
        nameV.setText("אימון הושלם");
        sansV.setText("NAMASTE, WARRIOR");
        cueV.setText("הגוף שלך יגיד תודה. שתה מים, ותן לשרירים להתאושש.");
        nextV.setText("זמן אימון: " + Ui.mmss((long) activeSecs) + " · ימי רצף: " + a.stats.streak());
        timeV.setText("✓");
        SoundEngine.get(a).bell(174.6f, 0.35f);
        if (a.stats.flag("voice", false)) Voice.get(a).say("אימון הושלם. כל הכבוד.");
        controls.setVisibility(View.GONE);
        finishBtn.setVisibility(View.VISIBLE);
        Ui.pop(finishBtn);
        embers.burst(embers.getWidth() / 2f, embers.getHeight() * 0.35f);
        seg.invalidate();
    }

    @Override
    void onHide() {
        running = false;
        h.removeCallbacks(tick);
        Voice.get(a).stop();
        if (!done && activeSecs > 60) a.stats.addSession((long) activeSecs);
        done = true;
        a.keepScreenOn(false);
    }

    /** One slim segment per pose; the active one fills up with its timer. */
    private final class Segments extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rf = new RectF();

        Segments(Context c) {
            super(c);
        }

        @Override
        protected void onDraw(Canvas c) {
            int n = r.poses.length;
            float gap = Ui.dp(4);
            float w = (getWidth() - gap * (n - 1)) / n;
            float hh = getHeight();
            for (int i = 0; i < n; i++) {
                // RTL: first segment on the right
                float x = getWidth() - (i + 1) * w - i * gap;
                rf.set(x, 0, x + w, hh);
                p.setColor(0x22FFFFFF);
                c.drawRoundRect(rf, hh / 2, hh / 2, p);
                float fill = done || i < idx ? 1f : i == idx && !prep ? 1f - Math.max(0, left) / r.secs[i] : 0f;
                if (fill > 0) {
                    rf.set(x + w * (1 - fill), 0, x + w, hh);
                    p.setColor(r.color);
                    c.drawRoundRect(rf, hh / 2, hh / 2, p);
                }
            }
        }
    }
}
