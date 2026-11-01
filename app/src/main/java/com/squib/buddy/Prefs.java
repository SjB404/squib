package com.squib.buddy;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static SharedPreferences get(Context context) {
        return context.getSharedPreferences("squib", 0);
    }

    private Prefs() {
    }
}
