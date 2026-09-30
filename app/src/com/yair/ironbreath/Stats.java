package com.yair.ironbreath;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.TimeZone;

/** Tiny persistence layer for progress: streak, minutes, sessions, best breath hold. */
final class Stats {
    private final SharedPreferences p;

    Stats(Context c) {
        p = c.getSharedPreferences("iron_stats", Context.MODE_PRIVATE);
    }

    static long today() {
        long now = System.currentTimeMillis();
        return (now + TimeZone.getDefault().getOffset(now)) / 86400000L;
    }

    void addSession(long seconds) {
        if (seconds < 20) return;
        long day = today();
        long last = p.getLong("lastDay", -10);
        int streak = p.getInt("streak", 0);
        if (last == day) {
            // already counted today
        } else if (last == day - 1) {
            streak++;
        } else {
            streak = 1;
        }
        p.edit()
                .putLong("lastDay", day)
                .putInt("streak", streak)
                .putInt("bestStreak", Math.max(streak, p.getInt("bestStreak", 0)))
                .putLong("seconds", p.getLong("seconds", 0) + seconds)
                .putInt("sessions", p.getInt("sessions", 0) + 1)
                .apply();
    }

    int streak() {
        long last = p.getLong("lastDay", -10);
        long day = today();
        if (last == day || last == day - 1) return p.getInt("streak", 0);
        return 0;
    }

    int minutes() {
        return (int) (p.getLong("seconds", 0) / 60);
    }

    int sessions() {
        return p.getInt("sessions", 0);
    }

    int bestHold() {
        return p.getInt("bestHold", 0);
    }

    /** @return true when this is a new personal record. */
    boolean recordHold(int seconds) {
        if (seconds > bestHold()) {
            p.edit().putInt("bestHold", seconds).apply();
            return true;
        }
        return false;
    }

    boolean flag(String k, boolean def) {
        return p.getBoolean(k, def);
    }

    void setFlag(String k, boolean v) {
        p.edit().putBoolean(k, v).apply();
    }
}
