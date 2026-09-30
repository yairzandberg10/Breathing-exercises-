package com.yair.ironbreath;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

/** Optional Hebrew voice guidance through the system text-to-speech engine. */
final class Voice implements TextToSpeech.OnInitListener {
    private static Voice inst;
    private final Context ctx;
    private TextToSpeech tts;
    private boolean ready, failed, warned;
    private String pending;

    static Voice get(Context c) {
        if (inst == null) inst = new Voice(c.getApplicationContext());
        return inst;
    }

    private Voice(Context c) {
        ctx = c;
    }

    void warm() {
        if (tts == null && !failed) tts = new TextToSpeech(ctx, this);
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            failed = true;
            warn();
            return;
        }
        int r = tts.setLanguage(new Locale("he", "IL"));
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            r = tts.setLanguage(new Locale("iw", "IL"));
        }
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            failed = true;
            warn();
            return;
        }
        tts.setSpeechRate(0.88f);
        tts.setPitch(0.82f);
        ready = true;
        if (pending != null) {
            say(pending);
            pending = null;
        }
    }

    private void warn() {
        if (warned) return;
        warned = true;
        Toast.makeText(ctx, "לא נמצא קול עברי במכשיר. אפשר להתקין דרך הגדרות > המרת טקסט לדיבור.", Toast.LENGTH_LONG).show();
    }

    void say(String text) {
        if (failed) {
            warn();
            return;
        }
        if (!ready) {
            pending = text;
            warm();
            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "iron");
    }

    void stop() {
        pending = null;
        if (tts != null && ready) tts.stop();
    }
}
