package com.yair.ironbreath;

import android.view.View;

/** A lightweight screen: tabs and full-screen sessions are both Screens hosted by MainActivity. */
abstract class Screen {
    final MainActivity a;
    private View view;

    Screen(MainActivity a) {
        this.a = a;
    }

    abstract View create();

    final View view() {
        if (view == null) view = create();
        return view;
    }

    void onShow() {
    }

    void onHide() {
    }

    /** @return true if the back press was consumed. */
    boolean onBack() {
        return false;
    }
}
