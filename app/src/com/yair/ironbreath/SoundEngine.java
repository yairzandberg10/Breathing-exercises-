package com.yair.ironbreath;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import java.util.ArrayList;

/**
 * Real-time procedural audio engine. Every soundscape (rain, sea, fire, wind, thunder, crickets,
 * singing bowls, drones, noise and binaural beats) is synthesised on the fly, so the app ships
 * without audio files and nothing ever loops audibly. One-shot cues (bells, breath tones, ticks)
 * are mixed on top.
 */
final class SoundEngine implements Runnable {
    static final int SR = 44100;
    private static final int N = 1024;
    private static final float[] SIN = new float[4096];

    static {
        for (int i = 0; i < SIN.length; i++) SIN[i] = (float) Math.sin(i * 2 * Math.PI / SIN.length);
    }

    static float sin(float cycles) {
        return SIN[((int) (cycles * 4096f)) & 4095];
    }

    private static SoundEngine inst;

    static synchronized SoundEngine get(Context c) {
        if (inst == null) inst = new SoundEngine(c.getApplicationContext());
        return inst;
    }

    private final Context ctx;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final float[] target = new float[Content.S_COUNT];
    private final float[] cur = new float[Content.S_COUNT];
    private final Gen[] gens = new Gen[Content.S_COUNT];
    private final ArrayList<Cue> cues = new ArrayList<>();
    private final float[] L = new float[N], R = new float[N];
    private final short[] out = new short[N * 2];
    private boolean running;
    private float master = 1f;
    private volatile long sleepAt;
    private boolean fading;
    Runnable onChange;

    private SoundEngine(Context c) {
        ctx = c;
        gens[Content.S_RAIN] = new Rain();
        gens[Content.S_OCEAN] = new Ocean();
        gens[Content.S_FIRE] = new Fire();
        gens[Content.S_WIND] = new Wind();
        gens[Content.S_STORM] = new Storm();
        gens[Content.S_NIGHT] = new Night();
        gens[Content.S_BOWLS] = new Bowls();
        gens[Content.S_DRONE] = new Drone();
        gens[Content.S_BROWN] = new Brown();
        gens[Content.S_ALPHA] = new Binaural(200f, 10f);
        gens[Content.S_THETA] = new Binaural(170f, 6f);
        gens[Content.S_DELTA] = new Binaural(130f, 2.5f);
    }

    // ---------------------------------------------------------------- public API
    synchronized void set(int id, float vol) {
        target[id] = Math.max(0, Math.min(1, vol));
        if (vol > 0) {
            fading = false;
            master = Math.max(master, 0.001f);
        }
        ensure();
        syncService();
    }

    synchronized float volume(int id) {
        return target[id];
    }

    synchronized boolean on(int id) {
        return target[id] > 0;
    }

    synchronized boolean anyOn() {
        for (float v : target) if (v > 0) return true;
        return false;
    }

    synchronized void stopAll() {
        for (int i = 0; i < target.length; i++) target[i] = 0;
        sleepAt = 0;
        syncService();
        changed();
    }

    /** Fade everything out after the given number of minutes (0 = never). */
    void sleepTimer(int minutes) {
        sleepAt = minutes <= 0 ? 0 : System.currentTimeMillis() + minutes * 60000L;
    }

    long sleepRemainingMs() {
        long s = sleepAt;
        return s == 0 ? -1 : Math.max(0, s - System.currentTimeMillis());
    }

    void bell(float freq, float amp) {
        cue(new BowlVoice(freq, amp, 0f, 0.35f));
    }

    void tone(float f0, float f1, float dur, float amp) {
        cue(new Tone(f0, f1, dur, amp));
    }

    void tick(float amp) {
        cue(new Tick(amp));
    }

    private void cue(Cue c) {
        synchronized (cues) {
            if (cues.size() < 24) cues.add(c);
        }
        synchronized (this) {
            ensure();
        }
    }

    private void ensure() {
        if (!running) {
            running = true;
            Thread t = new Thread(this, "iron-audio");
            t.setPriority(Thread.MAX_PRIORITY);
            t.start();
        }
    }

    private void syncService() {
        final boolean any = anyOn();
        main.post(new Runnable() {
            @Override
            public void run() {
                if (any) SoundService.start(ctx);
                else SoundService.stop(ctx);
            }
        });
    }

    private void changed() {
        final Runnable r = onChange;
        if (r != null) main.post(r);
    }

    // ---------------------------------------------------------------- audio thread
    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
        AudioTrack track = null;
        try {
            int min = AudioTrack.getMinBufferSize(SR, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            track = new AudioTrack(AudioManager.STREAM_MUSIC, SR, AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, Math.max(min, N * 8), AudioTrack.MODE_STREAM);
            track.play();
            int idle = 0;
            while (true) {
                boolean active = render();
                track.write(out, 0, out.length);
                idle = active ? 0 : idle + 1;
                if (idle > SR / N * 2) {
                    synchronized (this) {
                        boolean hasCues;
                        synchronized (cues) {
                            hasCues = !cues.isEmpty();
                        }
                        if (!anyOn() && !hasCues) {
                            running = false;
                            break;
                        }
                    }
                    idle = 0;
                }
            }
        } catch (Throwable e) {
            synchronized (this) {
                running = false;
            }
        } finally {
            if (track != null) {
                try {
                    track.stop();
                } catch (Throwable ignored) {
                }
                track.release();
            }
        }
    }

    private boolean render() {
        java.util.Arrays.fill(L, 0);
        java.util.Arrays.fill(R, 0);
        boolean active = false;

        long s = sleepAt;
        if (s != 0 && System.currentTimeMillis() >= s) {
            fading = true;
            sleepAt = 0;
        }
        float m0 = master;
        if (fading) {
            master = Math.max(0, master - (float) N / (SR * 10f));
            if (master <= 0) {
                fading = false;
                synchronized (this) {
                    for (int i = 0; i < target.length; i++) {
                        target[i] = 0;
                        cur[i] = 0;
                    }
                }
                master = 1f;
                m0 = 0;
                syncService();
                changed();
            }
        } else if (master < 1f) {
            master = Math.min(1f, master + (float) N / (SR * 0.5f));
        }
        float m1 = fading ? master : master;

        for (int i = 0; i < gens.length; i++) {
            float t;
            synchronized (this) {
                t = target[i];
            }
            float c0 = cur[i];
            float c1 = c0 + (t - c0) * 0.12f;
            if (t == 0 && c1 < 0.002f) c1 = 0;
            cur[i] = c1;
            if (c0 > 0 || c1 > 0) {
                float g = gens[i].gain;
                gens[i].render(L, R, N, c0 * c0 * g * m0, c1 * c1 * g * m1);
                active = true;
            }
        }
        synchronized (cues) {
            for (int i = cues.size() - 1; i >= 0; i--) {
                if (!cues.get(i).render(L, R, N)) cues.remove(i);
            }
            if (!cues.isEmpty()) active = true;
        }
        for (int i = 0; i < N; i++) {
            out[i * 2] = (short) (clip(L[i]) * 30000f);
            out[i * 2 + 1] = (short) (clip(R[i]) * 30000f);
        }
        return active;
    }

    private static float clip(float x) {
        if (x > 3f) return 1f;
        if (x < -3f) return -1f;
        return x * (27f + x * x) / (27f + 9f * x * x);
    }

    // ---------------------------------------------------------------- DSP building blocks
    static final class Rng {
        int s;

        Rng(int seed) {
            s = seed * 0x9E3779B1 + 0x7F4A7C15;
            if (s == 0) s = 1;
        }

        float nf() {
            s ^= s << 13;
            s ^= s >>> 17;
            s ^= s << 5;
            return s * 4.656613e-10f;
        }

        float uf() {
            return (nf() + 1f) * 0.5f;
        }
    }

    abstract static class Gen {
        float gain = 1f;

        /** Adds n samples into l/r with a linear gain ramp g0 -> g1. */
        abstract void render(float[] l, float[] r, int n, float g0, float g1);
    }

    interface Cue {
        boolean render(float[] l, float[] r, int n);
    }

    static float decay(float seconds) {
        return (float) Math.exp(-1.0 / (SR * seconds));
    }

    // ---------------------------------------------------------------- generators
    static final class Brown extends Gen {
        final Rng r = new Rng(3);
        float bl, br;

        Brown() {
            gain = 0.85f;
        }

        void render(float[] l, float[] rr, int n, float g0, float g1) {
            float dg = (g1 - g0) / n, g = g0;
            for (int i = 0; i < n; i++, g += dg) {
                bl = (bl + r.nf() * 0.02f) * 0.998f;
                br = (br + r.nf() * 0.02f) * 0.998f;
                l[i] += bl * g;
                rr[i] += br * g;
            }
        }
    }

    static final class Rain extends Gen {
        final Rng r = new Rng(11);
        float lpL, lpR, hpL, hpR, rum;
        final float[] amp = new float[40], ph = new float[40], inc = new float[40], dec = new float[40], pan = new float[40];

        void render(float[] l, float[] rr, int n, float g0, float g1) {
            float dg = (g1 - g0) / n, g = g0;
            for (int i = 0; i < n; i++, g += dg) {
                lpL += 0.4f * (r.nf() - lpL);
                lpR += 0.4f * (r.nf() - lpR);
                hpL += 0.015f * (lpL - hpL);
                hpR += 0.015f * (lpR - hpR);
                rum = (rum + r.nf() * 0.02f) * 0.997f;
                if (r.uf() < 0.0022f) spawn();
                float dl = 0, dr = 0;
                for (int k = 0; k < 40; k++) {
                    if (amp[k] > 0.0005f) {
                        float v = sin(ph[k]) * amp[k];
                        ph[k] += inc[k];
                        if (ph[k] > 1f) ph[k] -= 1f;
                        amp[k] *= dec[k];
                        dl += v * pan[k];
                        dr += v * (1f - pan[k]);
                    }
                }
                l[i] += g * ((lpL - hpL) * 0.28f + rum * 0.45f + dl);
                rr[i] += g * ((lpR - hpR) * 0.28f + rum * 0.45f + dr);
            }
        }

        void spawn() {
            for (int k = 0; k < 40; k++) {
                if (amp[k] <= 0.0005f) {
                    float u = r.uf();
                    boolean tin = r.uf() < 0.25f;
                    amp[k] = (0.03f + u * u * 0.25f) * (tin ? 0.7f : 1f);
                    float f = tin ? 2400 + r.uf() * 2600 : 900 + r.uf() * 2800;
                    inc[k] = f / SR;
                    ph[k] = 0;
                    dec[k] = decay(tin ? 0.035f + r.uf() * 0.05f : 0.004f + r.uf() * 0.01f);
                    pan[k] = 0.15f + r.uf() * 0.7f;
                    return;
                }
            }
        }
    }

    static final class Ocean extends Gen {
        final Rng r = new Rng(21);
        float t, lpL, lpR, bL, bR;

        Ocean() {
            gain = 1.25f;
        }

        void render(float[] l, float[] rr, int n, float g0, float g1) {
            float dg = (g1 - g0) / n, g = g0;
            for (int i = 0; i < n; i++, g += dg) {
                t += 1f / SR;
                if (t > 1000f) t -= 1000f;
                float eL = env(t), eR = env(t + 0.6f);
                lpL += (0.02f + 0.33f * eL) * (r.nf() - lpL);
                lpR += (0.02f + 0.33f * eR) * (r.nf() - lpR);
                bL = (bL + r.nf() * 0.02f) * 0.998f;
                bR = (bR + r.nf() * 0.02f) * 0.998f;
                l[i] += g * (lpL * eL * 1.1f + bL * (0.12f + 0.3f * eL));
                rr[i] += g * (lpR * eR * 1.1f + bR * (0.12f + 0.3f * eR));
            }
        }

        static float env(float t) {
            float a = 0.5f + 0.5f * sin(t / 10.5f);
            float b = 0.5f + 0.5f * sin(t / 7.3f + 0.3f);
            return a * a * a * 0.72f + b * b * b * b * 0.28f + 0.05f;
        }
    }

    static final class Fire extends Gen {
        final Rng r = new Rng(31);
        float bL, bR, prev, flick = 0.5f, flickT = 0.5f;
        int cnt;
        final float[] amp = new float[16], dec = new float[16], pan = new float[16];

        void render(float[] l, float[] rr, int n, float g0, float g1) {
            float dg = (g1 - g0) / n, g = g0;
            for (int i = 0; i < n; i++, g += dg) {
                if (++cnt >= 2000) {
                    cnt = 0;
                    flickT = 0.3f + r.uf() * 0.7f;
                }
                flick += (flickT - flick) * 0.0005f;
                bL = (bL + r.nf() * 0.02f) * 0.998f;
                bR = (bR + r.nf() * 0.02f) * 0.998f;
                float w = r.nf();
                float hp = w - prev;
                prev = w;
                if (r.uf() < 0.00032f + flick * 0.0003f) spawn();
                float cl = 0, cr = 0;
                for (int k = 0; k < 16; k++) {
                    if (amp[k] > 0.001f) {
                        float v = hp * amp[k];
                        amp[k] *= dec[k];
                        cl += v * pan[k];
                        cr += v * (1 - pan[k]);
                    }
                }
                float hiss = hp * 0.018f * flick;
                l[i] += g * (bL * 0.55f * (0.6f + flick * 0.5f) + cl + hiss);
                rr[i] += g * (bR * 0.55f * (0.6f + flick * 0.5f) + cr + hiss);
            }
        }

        void spawn() {
            for (int k = 0; k < 16; k++) {
                if (amp[k] <= 0.001f) {
                    float u = r.uf();
                    amp[k] = 0.08f + u * u * u * 0.9f;
                    dec[k] = decay(0.0015f + r.uf() * 0.006f);
                    pan[k] = 0.2f + r.uf() * 0.6f;
                    return;
                }
            }
        }
    }

    static final class Wind extends Gen {
        final Rng r = new Rng(41);
        float lowL, bandL, lowR, bandR, fcL = 400, fcR = 500, tL = 400, tR = 500, gust = 0.5f, gustT = 0.5f;
        int cnt;

        Wind() {
            gain = 3.2f;
        }

        void render(float[] l, float[] rr, int n, float g0, float g1) {
            float dg = (g1 - g0) / n, g = g0;
            for (int i = 0; i < n; i++, g += dg) {
                if (++cnt >= 4410) {
                    cnt = 0;
                    tL = 180 + r.uf() * 800;
                    tR = 180 + r.uf() * 800;
                    gustT = 0.15f + r.uf() * r.uf() * 1.0f;
                }
                fcL += (tL - fcL) * 0.00006f;
                fcR += (tR - fcR) * 0.00006f;
                gust += (gustT - gust) * 0.00004f;
                float fL = 2f * sin(fcL / SR * 0.5f), fR = 2f * sin(fcR / SR * 0.5f);
                float q = 0.35f;
                float inL = r.nf(), inR = r.nf();
                lowL += fL * bandL;
                float hL = inL - lowL - q * bandL;
                bandL += fL * hL;
                lowR += fR * bandR;
                float hR = inR - lowR - q * bandR;
                bandR += fR * hR;
                l[i] += g * bandL * gust * 0.35f;
                rr[i] += g * bandR * gust * 0.35f;
            }
        }
    }

    static final class Storm extends Gen {
        final Rng r = new Rng(51);
        float b, lp1, lp2, env, envT, crack, hissL, hissR, hpS;
        int next = SR * 2, cnt, phase;
        float rumbleMod = 1f, rumbleT = 1f;

        Storm() {
            gain = 0.9f;
        }

        void render(float[] l, float[] rr, int n, float g0, float g1) {
            float dg = (g1 - g0) / n, g = g0;
            for (int i = 0; i < n; i++, g += dg) {
                if (--next <= 0) {
                    next = (int) (SR * (10 + r.uf() * 22));
                    envT = 0.5f + r.uf() * 0.7f;
                    phase = 1;
                    if (r.uf() < 0.45f) crack = 0.5f + r.uf() * 0.4f;
                }
                if (phase == 1) {
                    env += (envT - env) * 0.00012f;
                    if (env > envT * 0.95f) phase = 2;
                } else {
                    env *= 0.999992f;
                }
                if (++cnt >= 2205) {
                    cnt = 0;
                    rumbleT = 0.4f + r.uf() * 0.9f;
                }
                rumbleMod += (rumbleT - rumbleMod) * 0.0008f;
                b = (b + r.nf() * 0.02f) * 0.999f;
                lp1 += 0.03f * (b - lp1);
                lp2 += 0.03f * (lp1 - lp2);
                float w = r.nf();
                float hp = w - hpS;
                hpS = w;
                float c = hp * crack;
                crack *= 0.99985f;
                hissL += 0.3f * (r.nf() - hissL);
                hissR += 0.3f * (r.nf() - hissR);
                float th = lp2 * env * rumbleMod * 1.6f;
                l[i] += g * (th + c + hissL * 0.05f);
                rr[i] += g * (th * 0.9f + c * 0.8f + hissR * 0.05f);
            }
        }
    }

    static final class Night extends Gen {
        final Rng r = new Rng(61);
        final float[] f = {4300, 4750, 3900}, per = {0.9f, 1.1f, 1.6f}, tc = {0, 0.37f, 0.8f}, ph = new float[3], pan = {0.25f, 0.75f, 0.5f}, lvl = {1f, 0.7f, 0.4f};
        float bl, br, air;

        void render(float[] l, float[] rr, int n, float g0, float g1) {
            float dg = (g1 - g0) / n, g = g0;
            float dt = 1f / SR;
            for (int i = 0; i < n; i++, g += dg) {
                float cl = 0, cr = 0;
                for (int k = 0; k < 3; k++) {
                    tc[k] += dt;
                    if (tc[k] > per[k]) {
                        tc[k] = 0;
                        per[k] = (k == 2 ? 1.4f : 0.7f) + r.uf() * 0.6f;
                    }
                    float x = tc[k];
                    int pulse = (int) (x / 0.042f);
                    float within = x - pulse * 0.042f;
                    if (pulse < 3 && within < 0.026f) {
                        float e = sin(within / 0.052f);
                        ph[k] += f[k] / SR;
                        if (ph[k] > 1) ph[k] -= 1;
                        float v = sin(ph[k]) * e * 0.17f * lvl[k];
                        cl += v * pan[k];
                        cr += v * (1 - pan[k]);
                    }
                }
                bl = (bl + r.nf() * 0.02f) * 0.998f;
                br = (br + r.nf() * 0.02f) * 0.998f;
                air += 0.1f * (r.nf() - air);
                l[i] += g * (cl + bl * 0.22f + air * 0.04f);
                rr[i] += g * (cr + br * 0.22f + air * 0.04f);
            }
        }
    }

    /** One struck singing bowl: detuned partial pairs give the slow "wah-wah" beating. */
    static final class BowlVoice implements Cue {
        static final float[] RATIO = {1f, 2.71f, 5.12f, 8.26f};
        static final float[] AMP = {1f, 0.42f, 0.2f, 0.09f};
        static final float[] TAU = {11f, 6.5f, 3.6f, 2.2f};
        final float[] ph = new float[8], inc = new float[8], a = new float[8], d = new float[8];
        final float pan;
        float attack;

        BowlVoice(float freq, float amp, float panOffset, float tauScale) {
            for (int p = 0; p < 4; p++) {
                for (int s = 0; s < 2; s++) {
                    int k = p * 2 + s;
                    float fr = freq * RATIO[p] + (s == 0 ? -0.35f : 0.35f) * (p + 1);
                    inc[k] = fr / SR;
                    ph[k] = s * 0.25f;
                    a[k] = amp * AMP[p] * 0.5f;
                    d[k] = decay(TAU[p] * (tauScale <= 0 ? 1f : tauScale * 2.5f));
                }
            }
            pan = 0.5f + panOffset;
        }

        public boolean render(float[] l, float[] r, int n) {
            for (int i = 0; i < n; i++) {
                if (attack < 1f) attack += 1f / (SR * 0.006f);
                float v = 0;
                for (int k = 0; k < 8; k++) {
                    v += sin(ph[k]) * a[k];
                    ph[k] += inc[k];
                    if (ph[k] > 1f) ph[k] -= 1f;
                    a[k] *= d[k];
                }
                v *= Math.min(1f, attack);
                l[i] += v * pan;
                r[i] += v * (1 - pan);
            }
            return a[0] > 0.0004f;
        }
    }

    static final class Bowls extends Gen {
        final Rng r = new Rng(71);
        final ArrayList<BowlVoice> voices = new ArrayList<>();
        int next = 1000;
        final float[] tl = new float[N], tr = new float[N];
        static final float[] NOTES = {146.8f, 164.8f, 196f, 220f, 246.9f, 293.7f};

        void render(float[] l, float[] rr, int n, float g0, float g1) {
            next -= n;
            if (next <= 0) {
                next = (int) (SR * (5 + r.uf() * 7));
                if (voices.size() < 6) voices.add(new BowlVoice(NOTES[(int) (r.uf() * NOTES.length) % NOTES.length], 0.42f, (r.uf() - 0.5f) * 0.6f, 0));
            }
            java.util.Arrays.fill(tl, 0, n, 0f);
            java.util.Arrays.fill(tr, 0, n, 0f);
            for (int i = voices.size() - 1; i >= 0; i--) {
                if (!voices.get(i).render(tl, tr, n)) voices.remove(i);
            }
            float dg = (g1 - g0) / n, g = g0;
            for (int i = 0; i < n; i++, g += dg) {
                l[i] += tl[i] * g;
                rr[i] += tr[i] * g;
            }
        }
    }

    static final class Drone extends Gen {
        final float[] fr = {55f, 55.18f, 82.41f, 110f, 110.27f, 164.81f, 220f, 329.63f, 440.4f};
        final float[] am = {.26f, .26f, .2f, .15f, .15f, .08f, .05f, .025f, .012f};
        final float[] lfo = {.031f, .043f, .057f, .071f, .037f, .089f, .061f, .11f, .05f};
        final float[] pn = {.3f, .7f, .5f, .2f, .8f, .4f, .6f, .35f, .65f};
        final float[] ph = new float[9], lph = new float[9];
        float t;

        void render(float[] l, float[] rr, int n, float g0, float g1) {
            float dg = (g1 - g0) / n, g = g0;
            for (int i = 0; i < n; i++, g += dg) {
                t += 1f / SR;
                if (t > 1000f) t -= 1000f;
                float breath = 0.78f + 0.22f * sin(t * 0.05f);
                float vl = 0, vr = 0;
                for (int k = 0; k < 9; k++) {
                    ph[k] += fr[k] / SR;
                    if (ph[k] > 1f) ph[k] -= 1f;
                    float amp = am[k] * (0.6f + 0.4f * sin(t * lfo[k] + k * 0.13f));
                    float s = sin(ph[k]) + 0.18f * sin(ph[k] * 2f);
                    float v = s * amp;
                    vl += v * pn[k];
                    vr += v * (1 - pn[k]);
                }
                l[i] += g * vl * breath;
                rr[i] += g * vr * breath;
            }
        }
    }

    static final class Binaural extends Gen {
        final float base, beat;
        float pl, pr;

        Binaural(float base, float beat) {
            this.base = base;
            this.beat = beat;
            gain = 0.5f;
        }

        void render(float[] l, float[] rr, int n, float g0, float g1) {
            float dg = (g1 - g0) / n, g = g0;
            float il = base / SR, ir = (base + beat) / SR;
            for (int i = 0; i < n; i++, g += dg) {
                pl += il;
                if (pl > 1f) pl -= 1f;
                pr += ir;
                if (pr > 1f) pr -= 1f;
                l[i] += g * sin(pl) * 0.45f;
                rr[i] += g * sin(pr) * 0.45f;
            }
        }
    }

    // ---------------------------------------------------------------- cues
    /** Soft gliding tone that follows the breath: rises on inhale, falls on exhale. */
    static final class Tone implements Cue {
        final float f0, f1, amp;
        final int len;
        int pos;
        float ph;

        Tone(float f0, float f1, float dur, float amp) {
            this.f0 = f0;
            this.f1 = f1;
            this.amp = amp;
            this.len = (int) (dur * SR);
        }

        public boolean render(float[] l, float[] r, int n) {
            float att = Math.min(0.35f * SR, len * 0.3f), rel = Math.min(0.5f * SR, len * 0.4f);
            for (int i = 0; i < n && pos < len; i++, pos++) {
                float x = (float) pos / len;
                float f = f0 + (f1 - f0) * (x * x * (3 - 2 * x));
                ph += f / SR;
                if (ph > 1f) ph -= 1f;
                float e = 1f;
                if (pos < att) e = pos / att;
                else if (pos > len - rel) e = (len - pos) / rel;
                e = e * e;
                float v = (sin(ph) + 0.25f * sin(ph * 2f) + 0.08f * sin(ph * 3f)) * e * amp;
                l[i] += v;
                r[i] += v;
            }
            return pos < len;
        }
    }

    static final class Tick implements Cue {
        final float amp;
        int pos;
        float ph, a;

        Tick(float amp) {
            this.amp = amp;
            a = amp;
        }

        public boolean render(float[] l, float[] r, int n) {
            float d = decay(0.018f);
            for (int i = 0; i < n; i++) {
                ph += 1320f / SR;
                if (ph > 1f) ph -= 1f;
                float v = sin(ph) * a;
                a *= d;
                l[i] += v;
                r[i] += v;
            }
            pos += n;
            return a > 0.0005f;
        }
    }
}
