package com.squib.buddy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction()) && Prefs.get(context).getBoolean("boot", true) && Settings.canDrawOverlays(context)) {
            context.startForegroundService(new Intent(context, (Class<?>) SquibService.class));
        }
    }
}
