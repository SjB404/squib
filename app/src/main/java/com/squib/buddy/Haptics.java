package com.squib.buddy;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

/** Phone vibration. Light, crisp ticks (the haptic engine's "tick"), not a long buzz. */
final class Haptics {
    private Haptics() {
    }

    /** Two quick ticks: tik-tik. Used when he tells you it's late. */
    static void tick(Context c) {
        try {
            Vibrator v;
            if (Build.VERSION.SDK_INT >= 31) {
                VibratorManager vm = (VibratorManager) c.getSystemService(VibratorManager.class);
                v = vm == null ? null : vm.getDefaultVibrator();
            } else {
                v = (Vibrator) c.getSystemService(Vibrator.class);
            }
            if (v == null || !v.hasVibrator()) {
                return;
            }
            VibrationEffect e;
            if (Build.VERSION.SDK_INT >= 30 && v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)) {
                // phones with a proper haptic motor: two short ticks, 90ms apart
                e = VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.8f, 90)
                        .compose();
            } else if (Build.VERSION.SDK_INT >= 29) {
                e = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK);
            } else {
                e = VibrationEffect.createOneShot(12, v.hasAmplitudeControl() ? 90 : VibrationEffect.DEFAULT_AMPLITUDE);
            }
            v.vibrate(e);
        } catch (Throwable ignored) {
            // no vibrator, or the phone refused: not worth crashing over
        }
    }
}
