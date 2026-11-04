package com.squib.buddy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

/** Settings screen: permissions, start/stop, toggles, and a little preview of squib. */
public final class MainActivity extends Activity {
    static final int BG = 0xFF1F1E1D;
    static final int CORAL = 0xFFD87656;
    static final int CREAM = 0xFFFAF9F5;
    static final int MUTED = 0xFFB8B3A8;
    static final String[] SIZE_NAMES = {"small", "medium", "large"};

    float d;
    SharedPreferences prefs;
    Button hideButton, notifyButton, overlayButton, sizeButton, startStop, textButton, usageButton;
    TextView notifyStatus, overlayStatus, pauseLength, stats, usageStatus;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        prefs = Prefs.get(this);
        d = getResources().getDisplayMetrics().density;

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(40));
        scroll.addView(root);
        setContentView(scroll);

        root.addView(new Preview(this), new LinearLayout.LayoutParams(-1, dp(110)));
        TextView title = label("squib", 30, CORAL, true);
        title.setGravity(17);
        root.addView(title);
        TextView sub = label("your little pixel buddy, now in your phone", 14, MUTED, false);
        sub.setGravity(17);
        root.addView(sub);
        TextView ver = label("version " + version(), 12, MUTED, false);
        ver.setGravity(17);
        root.addView(ver);

        heading(root, "setup");
        overlayStatus = label("", 14, CREAM, false);
        overlayButton = button("allow", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            }
        });
        row(root, overlayStatus, overlayButton);

        usageStatus = label("", 14, CREAM, false);
        usageButton = button("allow", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            }
        });
        row(root, usageStatus, usageButton);

        notifyStatus = label("", 14, CREAM, false);
        notifyButton = button("allow", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
            }
        });
        if (Build.VERSION.SDK_INT >= 33) {
            row(root, notifyStatus, notifyButton);
        }
        root.addView(label("if a switch is greyed out in android's settings: open app info for squib, tap ⋮, then \"allow restricted settings\".", 12, MUTED, false));

        startStop = button("", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SquibService.running) {
                    stopService(new Intent(MainActivity.this, SquibService.class));
                } else if (Settings.canDrawOverlays(MainActivity.this)) {
                    startForegroundService(new Intent(MainActivity.this, SquibService.class));
                } else {
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
                }
                startStop.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                }, 300);
            }
        });
        LinearLayout.LayoutParams big = new LinearLayout.LayoutParams(-1, dp(56));
        big.topMargin = dp(18);
        root.addView(startStop, big);

        LinearLayout actions = new LinearLayout(this);
        actions.addView(sendButton("feed", SquibService.ACTION_FEED), weight());
        actions.addView(sendButton("talk", SquibService.ACTION_TALK), weight());
        actions.addView(sendButton("trick", SquibService.ACTION_TRICK), weight());
        hideButton = sendButton("hide", SquibService.ACTION_HIDE);
        actions.addView(hideButton, weight());
        root.addView(actions);
        LinearLayout picks = new LinearLayout(this);
        picks.addView(button("pick a trick", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pick(false);
            }
        }), weight());
        picks.addView(button("pick a fidget", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pick(true);
            }
        }), weight());
        picks.addView(sendButton("nag", SquibService.ACTION_NAG), weight());
        root.addView(picks);

        heading(root, "settings");
        sizeButton = button("", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int size = prefs.getInt("size", 2);
                prefs.edit().putInt("size", size < 4 ? size + 1 : 2).apply();
                changed();
            }
        });
        root.addView(sizeButton);
        textButton = button("", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.edit().putInt("textSize", (prefs.getInt("textSize", 1) + 1) % SIZE_NAMES.length).apply();
                changed();
            }
        });
        root.addView(textButton);
        toggle(root, "chatty", "talks on his own now and then", "chatty", true);
        toggle(root, "break reminders", "after 50 minutes on your phone, and long scrolls", "reminders", true);
        toggle(root, "app modes", "reacts to the app you're in (needs usage access)", "appModes", true);
        toggle(root, "start with phone", "comes back when your phone restarts", "boot", true);
        toggle(root, "come back after a pause", "\"pause\" in his notification stops him completely, for apps that won't work with him on screen. on: he comes back after the time below. off: he waits until you press start", "pauseAuto", true);
        pauseLength = label("", 14, CREAM, false);
        LinearLayout pauseRow = new LinearLayout(this);
        pauseRow.setGravity(16);
        pauseRow.addView(pauseLength, new LinearLayout.LayoutParams(0, -2, 1));
        pauseRow.addView(button("-", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stepPause(-1);
            }
        }));
        pauseRow.addView(button("+", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stepPause(1);
            }
        }));
        root.addView(pauseRow);
        toggle(root, "click-through", "taps go straight through him. you can't boop, pet or drag him while this is on", "clickThrough", false);
        Remote.addRow(this, root);

        heading(root, "how to play");
        root.addView(label("tap him to boop. double-tap for ink. drag him around and let go to throw him. hold your finger on him to pet him. \"trick\" makes him do something right now. \"pick a trick\" / \"pick a fidget\" let you choose which one, and which version. \"nag\" makes him tell you it's late right now (with the phone tick), to test it. his notification has hide, feed and pause. when he's paused, it has start and stop.", 14, CREAM, false));
        heading(root, "app modes");
        root.addView(label(Apps.SUMMARY, 13, CREAM, false));

        stats = label("", 12, MUTED, false);
        stats.setGravity(17);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(24);
        root.addView(stats, lp);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        refresh();
    }

    void refresh() {
        boolean overlay = Settings.canDrawOverlays(this);
        boolean usage = SquibService.hasUsageAccess(this);
        boolean notify = Build.VERSION.SDK_INT < 33 || checkSelfPermission("android.permission.POST_NOTIFICATIONS") == 0;
        overlayStatus.setText(mark(overlay) + "show over other apps (needed)");
        overlayButton.setVisibility(overlay ? View.GONE : View.VISIBLE);
        usageStatus.setText(mark(usage) + "usage access, so he knows which app you're in");
        usageButton.setVisibility(usage ? View.GONE : View.VISIBLE);
        notifyStatus.setText(mark(notify) + "notifications, where his controls live");
        notifyButton.setVisibility(notify ? View.GONE : View.VISIBLE);
        String start = "start squib";
        if (Pause.paused(this)) {
            long until = Pause.until(this);
            start = until > System.currentTimeMillis() ? "start squib (paused till " + Pause.clock(this, until) + ")" : "start squib (paused)";
        }
        startStop.setText(SquibService.running ? "send squib to bed" : overlay ? start : "allow squib to appear first");
        int mins = Pause.minutes(this);
        pauseLength.setText("pause for: " + (mins < 60 || mins % 60 != 0 ? mins + " min" : (mins / 60) + (mins == 60 ? " hour" : " hours")));
        hideButton.setEnabled(SquibService.running);
        sizeButton.setText("size: " + SIZE_NAMES[prefs.getInt("size", 2) - 2] + "   (tap to change)");
        textButton.setText("text size: " + SIZE_NAMES[prefs.getInt("textSize", 1)] + "   (tap to change)");
        long today = System.currentTimeMillis() / 86400000L;
        stats.setText("squib " + version() + "  ·  day " + ((today - prefs.getLong("born", today)) + 1) + "  ·  " + prefs.getInt("fed", 0) + " shrimp eaten  ·  "
                + prefs.getInt("boops", 0) + " boops  ·  " + prefs.getInt("pets", 0) + " pets");
        Remote.refreshRow(this);
    }

    /** Moves the pause length one step along Pause.MINUTES. */
    void stepPause(int dir) {
        int mins = Pause.minutes(this);
        int i = 0;
        while (i < Pause.MINUTES.length - 1 && Pause.MINUTES[i] < mins) {
            i++;
        }
        i = Math.max(0, Math.min(Pause.MINUTES.length - 1, i + dir));
        prefs.edit().putInt("pauseMinutes", Pause.MINUTES[i]).apply();
        refresh();
    }

    String version() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    static String mark(boolean ok) {
        return ok ? "✓ " : "✗ ";
    }

    void changed() {
        refresh();
        send(SquibService.ACTION_REFRESH);
    }

    void send(String action) {
        if (SquibService.running) {
            startService(new Intent(this, SquibService.class).setAction(action));
        }
    }

    /** Lists every activity (or fidget), then its versions if it has any, and makes him do it. */
    void pick(boolean fidgets) {
        final ArrayList<Integer> ids = new ArrayList<Integer>();
        ArrayList<String> names = new ArrayList<String>();
        int from = fidgets ? Fun.FIRST_FIDGET : 1, to = fidgets ? Fun.NAMES.length - 1 : Fun.LAST_ACTIVITY;
        for (int a = from; a <= to; a++) {
            ids.add(a);
            names.add(Fun.label(a) + (Fun.variantNames(a) != null ? "  ..." : ""));
        }
        new AlertDialog.Builder(this)
                .setTitle(fidgets ? "pick a fidget" : "pick a trick")
                .setItems(names.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final int a = ids.get(which);
                        String[] versions = Fun.variantNames(a);
                        if (versions == null) {
                            doTrick(a, -1);
                            return;
                        }
                        String[] items = new String[versions.length + 1];
                        items[0] = "random";
                        System.arraycopy(versions, 0, items, 1, versions.length);
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(Fun.label(a))
                                .setItems(items, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface d2, int w) {
                                        doTrick(a, w - 1);
                                    }
                                }).show();
                    }
                }).show();
    }

    void doTrick(int a, int variant) {
        if (!SquibService.running) {
            Toast.makeText(this, "start squib first", Toast.LENGTH_SHORT).show();
            return;
        }
        startService(new Intent(this, SquibService.class).setAction(SquibService.ACTION_TRICK)
                .putExtra("act", Fun.NAMES[a]).putExtra("variant", variant));
    }

    Button sendButton(String text, final String action) {
        return button(text, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send(action);
            }
        });
    }

    int dp(float f) {
        return Math.round(f * d);
    }

    TextView label(String text, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.MONOSPACE, bold ? Typeface.BOLD : Typeface.NORMAL);
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    Button button(String text, View.OnClickListener onClick) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTypeface(Typeface.MONOSPACE);
        b.setOnClickListener(onClick);
        return b;
    }

    void heading(LinearLayout root, String text) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(26);
        lp.bottomMargin = dp(8);
        root.addView(label(text, 16, CORAL, true), lp);
    }

    void row(LinearLayout root, TextView text, Button button) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(16);
        r.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(button);
        root.addView(r);
    }

    void toggle(LinearLayout root, String name, String help, final String key, boolean def) {
        Switch s = new Switch(this);
        s.setText(name + "\n" + help);
        s.setTextColor(CREAM);
        s.setTypeface(Typeface.MONOSPACE);
        s.setTextSize(14);
        s.setChecked(prefs.getBoolean(key, def));
        s.setPadding(0, dp(8), 0, dp(8));
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean on) {
                prefs.edit().putBoolean(key, on).apply();
                changed();
            }
        });
        root.addView(s);
    }

    LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, -2, 1);
    }

    /** Draws squib's sprite at the top of the settings screen. */
    static final class Preview extends View {
        final Rect dst = new Rect();
        final Paint paint = new Paint();
        final SquibView squib;

        Preview(Context context) {
            super(context);
            squib = new SquibView(context);
            squib.drawSprite();
            paint.setFilterBitmap(false);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int px = Math.max(1, getHeight() / 20);
            int w = px * 28;
            dst.set((getWidth() - w) / 2, getHeight() - (px * 20), (getWidth() + w) / 2, getHeight());
            canvas.drawBitmap(squib.sprite, null, dst, paint);
        }
    }
}
