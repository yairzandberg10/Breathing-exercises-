package com.yair.ironbreath;

import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** Soundscape mixer: layer any of the synthesised sounds, save-free presets and a sleep timer. */
final class SoundScreen extends Screen {
    private final SoundIconView[] icons = new SoundIconView[Content.S_COUNT];
    private final LinearLayout[] tiles = new LinearLayout[Content.S_COUNT];
    private final SeekBar[] bars = new SeekBar[Content.S_COUNT];
    private WaveView wave;
    private TextView nowV, timerV;
    private final TextView[] timerChips = new TextView[4];
    private static final int[] TIMERS = {0, 15, 30, 60};
    private int timerSel;
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean visible;
    private SoundEngine eng;

    SoundScreen(MainActivity a) {
        super(a);
    }

    @Override
    View create() {
        eng = SoundEngine.get(a);
        LinearLayout col = Ui.vbox(a);
        col.addView(BreathScreen.header(a, R.drawable.img_forest, "SOUNDSCAPES · סינתזה חיה", "סאונד",
                "כל צליל נוצר בזמן אמת – בלי לופים ובלי חזרות.\nערבב שכבות עד שזה מרגיש כמו בית.", Ui.EMBER), Ui.lp(Ui.MATCH, Ui.dp(290)));

        col.addView(playerCard(), Ui.lpm(Ui.MATCH, Ui.WRAP, 16, 4, 16, 0));

        TextView pt = Ui.text(a, "מיקסים מוכנים", 19, Ui.TEXT, Ui.BOLD);
        col.addView(pt, Ui.lpm(Ui.MATCH, Ui.WRAP, 20, 24, 20, 10));
        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout pr = Ui.hbox(a);
        pr.setPadding(Ui.dp(16), 0, Ui.dp(16), 0);
        for (int i = 0; i < Content.PRESET_NAMES.length; i++) pr.addView(preset(i), Ui.lpm(Ui.dp(150), Ui.dp(96), 0, 0, 10, 0));
        hs.addView(pr);
        col.addView(hs);

        TextView mt = Ui.text(a, "מיקסר", 19, Ui.TEXT, Ui.BOLD);
        col.addView(mt, Ui.lpm(Ui.MATCH, Ui.WRAP, 20, 24, 20, 10));
        LinearLayout grid = Ui.vbox(a);
        grid.setPadding(Ui.dp(16), 0, Ui.dp(16), Ui.dp(110));
        LinearLayout row = null;
        for (int i = 0; i < Content.S_COUNT; i++) {
            if (i % 2 == 0) {
                row = Ui.hbox(a);
                row.setGravity(Gravity.TOP);
                grid.addView(row, Ui.lpm(Ui.MATCH, Ui.WRAP, 0, i == 0 ? 0 : 10, 0, 0));
            }
            LinearLayout.LayoutParams lp = Ui.weight(1);
            lp.setMarginStart(i % 2 == 1 ? Ui.dp(10) : 0);
            row.addView(tile(i), lp);
        }
        col.addView(grid);
        eng.onChange = new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        };
        refresh();
        return BreathScreen.scroller(a, col);
    }

    private View playerCard() {
        LinearLayout c = Ui.vbox(a);
        c.setBackground(Ui.round(Ui.CARD, 22, 0x17FFFFFF, 1));
        c.setPadding(Ui.dp(16), Ui.dp(14), Ui.dp(16), Ui.dp(14));
        LinearLayout top = Ui.hbox(a);
        LinearLayout tt = Ui.vbox(a);
        tt.addView(Ui.tag(a, "NOW PLAYING", Ui.EMBER));
        nowV = Ui.text(a, "", 16, Ui.TEXT, Ui.BOLD);
        nowV.setPadding(0, Ui.dp(4), 0, 0);
        tt.addView(nowV);
        top.addView(tt, Ui.weight(1));
        FrameLayout stop = new FrameLayout(a);
        stop.setBackground(Ui.round(0x1FFFFFFF, 22));
        stop.addView(new IconView(a, IconView.STOP, Ui.TEXT), new FrameLayout.LayoutParams(Ui.dp(18), Ui.dp(18), Gravity.CENTER));
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eng.stopAll();
                setTimer(0);
                refresh();
            }
        });
        Ui.pressable(stop);
        top.addView(stop, Ui.lp(Ui.dp(44), Ui.dp(44)));
        c.addView(top);
        wave = new WaveView(a);
        c.addView(wave, Ui.lpm(Ui.MATCH, Ui.dp(80), 0, 6, 0, 6));

        LinearLayout tr = Ui.hbox(a);
        tr.addView(new IconView(a, IconView.TIMER, Ui.MUTED), Ui.lp(Ui.dp(18), Ui.dp(18)));
        timerV = Ui.text(a, "  טיימר שינה", 13, Ui.MUTED, Ui.REG);
        tr.addView(timerV, Ui.weight(1));
        String[] names = {"∞", "15", "30", "60"};
        for (int i = 0; i < 4; i++) {
            final int k = i;
            TextView ch = Ui.text(a, names[i], 13, Ui.TEXT, Ui.NUM);
            ch.setGravity(Gravity.CENTER);
            ch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setTimer(k);
                }
            });
            timerChips[i] = ch;
            tr.addView(ch, Ui.lpm(Ui.dp(40), Ui.dp(30), 6, 0, 0, 0));
        }
        c.addView(tr);
        setTimer(0);
        return c;
    }

    private void setTimer(int k) {
        timerSel = k;
        eng.sleepTimer(TIMERS[k]);
        for (int i = 0; i < 4; i++) {
            boolean on = i == k;
            timerChips[i].setBackground(on ? Ui.round(Ui.EMBER, 15) : Ui.round(0x14FFFFFF, 15));
            timerChips[i].setTextColor(on ? 0xFF140A05 : Ui.TEXT);
        }
        updateTimer();
    }

    private void updateTimer() {
        long ms = eng.sleepRemainingMs();
        if (ms < 0 && timerSel != 0) {
            setTimer(0);
            return;
        }
        if (ms < 0) timerV.setText("  טיימר שינה");
        else timerV.setText("  כיבוי בעוד " + Ui.mmss(ms / 1000));
    }

    private View preset(final int i) {
        float[] p = Content.PRESETS[i];
        int c0 = Content.SOUND_COLORS[(int) p[0]], c1 = Content.SOUND_COLORS[(int) p[2]];
        FrameLayout f = new FrameLayout(a);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Ui.mix(c0, Ui.BG, 0.55f), Ui.mix(c1, Ui.BG, 0.8f)});
        g.setCornerRadius(Ui.dp(20));
        g.setStroke(1, 0x22FFFFFF);
        f.setBackground(g);
        LinearLayout ic = Ui.hbox(a);
        for (int k = 0; k < p.length; k += 2) {
            SoundIconView s = new SoundIconView(a, (int) p[k]);
            ic.addView(s, Ui.lp(Ui.dp(26), Ui.dp(26)));
        }
        FrameLayout.LayoutParams il = new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.RIGHT);
        il.setMargins(0, Ui.dp(12), Ui.dp(12), 0);
        f.addView(ic, il);
        TextView n = Ui.text(a, Content.PRESET_NAMES[i], 16, 0xFFFFFFFF, Ui.BOLD);
        FrameLayout.LayoutParams nl = new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP, Gravity.BOTTOM | Gravity.RIGHT);
        nl.setMargins(0, 0, Ui.dp(14), Ui.dp(12));
        f.addView(n, nl);
        f.setClickable(true);
        Ui.pressable(f);
        f.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float[] p = Content.PRESETS[i];
                for (int k = 0; k < Content.S_COUNT; k++) {
                    float vol = 0;
                    for (int j = 0; j < p.length; j += 2) if ((int) p[j] == k) vol = p[j + 1];
                    eng.set(k, vol);
                }
                refresh();
            }
        });
        return f;
    }

    private View tile(final int id) {
        final LinearLayout t = Ui.vbox(a);
        t.setPadding(Ui.dp(14), Ui.dp(14), Ui.dp(14), Ui.dp(12));
        LinearLayout top = Ui.hbox(a);
        SoundIconView ic = new SoundIconView(a, id);
        icons[id] = ic;
        top.addView(ic, Ui.lp(Ui.dp(48), Ui.dp(48)));
        t.addView(top);
        TextView n = Ui.text(a, Content.SOUND_NAMES[id], 16, Ui.TEXT, Ui.BOLD);
        n.setPadding(0, Ui.dp(10), 0, Ui.dp(2));
        t.addView(n);
        t.addView(Ui.text(a, Content.SOUND_SUBS[id], 12, Ui.MUTED, Ui.REG));
        SeekBar sb = new SeekBar(a);
        sb.setMax(100);
        sb.setPadding(Ui.dp(4), Ui.dp(10), Ui.dp(4), Ui.dp(4));
        int col = Content.SOUND_COLORS[id];
        sb.setProgressTintList(Ui.csl(col));
        sb.setThumbTintList(Ui.csl(Ui.mix(col, 0xFFFFFFFF, 0.3f)));
        sb.setProgressBackgroundTintList(Ui.csl(0x44FFFFFF));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (user) {
                    eng.set(id, Math.max(0.02f, p / 100f));
                    updateWave();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        bars[id] = sb;
        t.addView(sb, Ui.lp(Ui.MATCH, Ui.WRAP));
        tiles[id] = t;
        t.setClickable(true);
        Ui.pressable(t);
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eng.set(id, eng.on(id) ? 0 : 0.7f);
                refresh();
                Ui.pop(icons[id]);
            }
        });
        return t;
    }

    void refresh() {
        if (nowV == null) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Content.S_COUNT; i++) {
            boolean on = eng.on(i);
            int col = Content.SOUND_COLORS[i];
            tiles[i].setBackground(on ? Ui.round(Ui.mix(col, Ui.CARD, 0.82f), 20, Ui.alpha(col, 0.7f), 1.2f)
                    : Ui.round(Ui.CARD, 20, 0x14FFFFFF, 1));
            icons[i].setActive(on);
            bars[i].setVisibility(on ? View.VISIBLE : View.INVISIBLE);
            bars[i].setProgress((int) (eng.volume(i) * 100));
            if (on) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(Content.SOUND_NAMES[i]);
            }
        }
        nowV.setText(sb.length() == 0 ? "שקט. בחר צליל כדי להתחיל." : sb.toString());
        updateWave();
        updateTimer();
    }

    private void updateWave() {
        int n = 0;
        for (int i = 0; i < Content.S_COUNT; i++) if (eng.on(i)) n++;
        int[] cols = new int[n];
        float[] lv = new float[n];
        int k = 0;
        for (int i = 0; i < Content.S_COUNT; i++) {
            if (eng.on(i)) {
                cols[k] = Content.SOUND_COLORS[i];
                lv[k++] = eng.volume(i) * 0.6f;
            }
        }
        wave.colors = cols;
        wave.levels = lv;
    }

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!visible) return;
            updateTimer();
            h.postDelayed(this, 1000);
        }
    };

    @Override
    void onShow() {
        visible = true;
        refresh();
        h.removeCallbacks(ticker);
        h.post(ticker);
    }

    @Override
    void onHide() {
        visible = false;
    }
}
