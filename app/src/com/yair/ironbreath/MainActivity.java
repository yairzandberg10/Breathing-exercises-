package com.yair.ironbreath;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends Activity {
    static final int TAB_HOME = 0, TAB_BREATH = 1, TAB_SOUND = 2, TAB_MEDITATE = 3, TAB_YOGA = 4;
    private static final String[] TAB_NAMES = {"בית", "נשימה", "סאונד", "מדיטציה", "יוגה"};
    private static final int[] TAB_ICONS = {IconView.HOME, IconView.BREATH, IconView.SOUND, IconView.MEDITATE, IconView.YOGA};

    FrameLayout root;
    private FrameLayout tabHost;
    private FrameLayout navBar;
    private LinearLayout navItems;
    private final Screen[] tabs = new Screen[5];
    private final IconView[] tabIcons = new IconView[5];
    private final TextView[] tabLabels = new TextView[5];
    private final FrameLayout[] medals = new FrameLayout[5];
    private int tab = -1;
    private final ArrayList<Screen> stack = new ArrayList<>();
    Stats stats;
    int statusBar;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.init(this);
        stats = new Stats(this);
        Window w = getWindow();
        w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(0x00000000);
        w.setNavigationBarColor(Ui.BG);
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        statusBar = id > 0 ? getResources().getDimensionPixelSize(id) : Ui.dp(24);

        root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        tabHost = new FrameLayout(this);
        root.addView(tabHost, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        buildNav();
        setContentView(root);

        tabs[TAB_HOME] = new HomeScreen(this);
        tabs[TAB_BREATH] = new BreathScreen(this);
        tabs[TAB_SOUND] = new SoundScreen(this);
        tabs[TAB_MEDITATE] = new MeditateScreen(this);
        tabs[TAB_YOGA] = new YogaScreen(this);
        showTab(TAB_HOME);
    }

    private void buildNav() {
        // Floating dock; the active section rises out of it as a glowing ember medallion.
        navBar = new FrameLayout(this);
        View dock = new View(this);
        dock.setBackground(Ui.round(0xFF162124, 26, 0x14FFFFFF, 1));
        dock.setElevation(Ui.dp(10));
        FrameLayout.LayoutParams dl = new FrameLayout.LayoutParams(Ui.MATCH, Ui.dp(62), Gravity.BOTTOM);
        dl.setMargins(Ui.dp(12), 0, Ui.dp(12), Ui.dp(10));
        navBar.addView(dock, dl);

        navItems = Ui.hbox(this);
        navItems.setElevation(Ui.dp(11));
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            FrameLayout item = new FrameLayout(this);
            FrameLayout medal = new FrameLayout(this);
            IconView ic = new IconView(this, TAB_ICONS[i], Ui.MUTED);
            ic.stroke = 1.9f;
            medal.addView(ic, new FrameLayout.LayoutParams(Ui.dp(22), Ui.dp(22), Gravity.CENTER));
            FrameLayout.LayoutParams ml = new FrameLayout.LayoutParams(Ui.dp(40), Ui.dp(40), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            ml.topMargin = Ui.dp(14);
            item.addView(medal, ml);
            TextView tl = Ui.centered(this, TAB_NAMES[i], 11, Ui.MUTED, Ui.REG);
            FrameLayout.LayoutParams ll = new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP, Gravity.BOTTOM);
            ll.bottomMargin = Ui.dp(18);
            item.addView(tl, ll);
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showTab(idx);
                }
            });
            tabIcons[i] = ic;
            tabLabels[i] = tl;
            medals[i] = medal;
            navItems.addView(item, new LinearLayout.LayoutParams(0, Ui.MATCH, 1));
        }
        FrameLayout.LayoutParams il = new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH);
        il.setMargins(Ui.dp(14), 0, Ui.dp(14), 0);
        navBar.addView(navItems, il);
        root.addView(navBar, new FrameLayout.LayoutParams(Ui.MATCH, Ui.dp(84), Gravity.BOTTOM));
    }

    void showTab(int i) {
        if (i == tab) {
            return;
        }
        final Screen old = tab >= 0 ? tabs[tab] : null;
        tab = i;
        Screen s = tabs[i];
        View v = s.view();
        if (v.getParent() == null) tabHost.addView(v, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        v.setVisibility(View.VISIBLE);
        v.bringToFront();
        v.setAlpha(0f);
        v.setTranslationY(Ui.dp(14));
        v.animate().alpha(1f).translationY(0).setDuration(320).setInterpolator(new DecelerateInterpolator(2f)).start();
        if (old != null) {
            old.onHide();
            final View ov = old.view();
            ov.animate().alpha(0f).setDuration(200).withEndAction(new Runnable() {
                @Override
                public void run() {
                    if (tabs[tab] != null && tabs[tab].view() != ov) ov.setVisibility(View.GONE);
                }
            }).start();
        }
        s.onShow();
        for (int k = 0; k < 5; k++) {
            boolean on = k == i;
            tabIcons[k].set(TAB_ICONS[k], on ? 0xFF0B1A16 : Ui.MUTED);
            tabLabels[k].setTextColor(on ? Ui.TEXT : Ui.MUTED);
            tabLabels[k].setTypeface(on ? Ui.BOLD : Ui.REG);
            medals[k].setBackground(on ? Ui.round(Ui.EMBER, 22, Ui.BG, 3) : null);
            medals[k].setElevation(on ? Ui.dp(6) : 0);
            medals[k].animate().translationY(on ? -Ui.dp(15) : 0).scaleX(on ? 1.1f : 1f).scaleY(on ? 1.1f : 1f)
                    .setDuration(380).setInterpolator(new OvershootInterpolator(on ? 2.2f : 1f)).start();
        }
    }

    /** Opens a full-screen session on top of everything. */
    void push(Screen s) {
        View v = s.view();
        if (v.getParent() != null) ((ViewGroup) v.getParent()).removeView(v);
        root.addView(v, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        v.setElevation(Ui.dp(16));
        v.setAlpha(0f);
        v.setTranslationY(Ui.dp(60));
        v.animate().alpha(1f).translationY(0).setDuration(380).setInterpolator(new DecelerateInterpolator(2.2f)).start();
        stack.add(s);
        s.onShow();
    }

    void pop() {
        if (stack.isEmpty()) return;
        final Screen s = stack.remove(stack.size() - 1);
        s.onHide();
        final View v = s.view();
        v.animate().alpha(0f).translationY(Ui.dp(60)).setDuration(260).withEndAction(new Runnable() {
            @Override
            public void run() {
                root.removeView(v);
            }
        }).start();
        if (stack.isEmpty() && tab >= 0) tabs[tab].onShow();
    }

    @Override
    public void onBackPressed() {
        if (!stack.isEmpty()) {
            if (!stack.get(stack.size() - 1).onBack()) pop();
        } else if (tab != TAB_HOME) {
            showTab(TAB_HOME);
        } else {
            super.onBackPressed();
        }
    }

    void keepScreenOn(boolean on) {
        if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    void buzz(long ms) {
        if (!stats.flag("vib", true)) return;
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) v.vibrate(ms);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        Voice.get(this).stop();
        super.onDestroy();
    }
}
