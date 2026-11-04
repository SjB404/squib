package com.squib.buddy;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    /** Default bedtime, in minutes after midnight: midnight to 8am. Changed in his settings ("bedStart" / "bedEnd"). */
    static final int BED_START = 0, BED_END = 480;

    static SharedPreferences get(Context context) {
        return context.getSharedPreferences("squib", 0);
    }

    private Prefs() {
    }
}
