package com.squib.buddy;

import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import java.util.List;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public final class SquibService extends Service {
    static final String ACTION_FEED = "com.squib.buddy.FEED";
    static final String ACTION_HIDE = "com.squib.buddy.HIDE";
    static final String ACTION_NAG = "com.squib.buddy.NAG";
    static final String ACTION_PAUSE = "com.squib.buddy.PAUSE";
    static final String ACTION_REFRESH = "com.squib.buddy.REFRESH";
    static final String ACTION_STOP = "com.squib.buddy.STOP";
    static final String ACTION_TALK = "com.squib.buddy.TALK";
    static final String ACTION_TRICK = "com.squib.buddy.TRICK";
    static final String CHANNEL = "squib";
    static volatile boolean running;
    View handle;
    WindowManager.LayoutParams handleLp;
    boolean handleShown;
    long lastUsageQuery;
    long screenOffAt;
    BroadcastReceiver screenReceiver;
    SquibView stage;
    WindowManager.LayoutParams stageLp;
    UsageStatsManager usage;
    AudioManager audio;
    int usageTick;
    WindowManager wm;
    final Handler handler = new Handler(Looper.getMainLooper());
    final Rect handleRect = new Rect();
    final Rect shownRect = new Rect();
    boolean screenOn = true;
    final Runnable frame = new Runnable() {
        @Override
        public void run() {
            if (!SquibService.this.screenOn || SquibService.this.stage == null) {
                return;
            }
            SquibView squibView = SquibService.this.stage;
            squibView.step();
            SquibService.this.updateHandle();
            SquibService squibService = SquibService.this;
            int i = squibService.usageTick + 1;
            squibService.usageTick = i;
            if (i >= 30) {
                SquibService.this.usageTick = 0;
                SquibService.this.pollForegroundApp();
                SquibService.this.pollMusic();
            }
            SquibService.this.handler.postDelayed(this, 33L);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Trace.init(getFilesDir());
        final Thread.UncaughtExceptionHandler crash = Thread.getDefaultUncaughtExceptionHandler();
        if (!(crash instanceof CrashLogger)) {
            Thread.setDefaultUncaughtExceptionHandler(new CrashLogger(crash));
        }
        Trace.i("service", "starting. android " + Build.VERSION.SDK_INT + ", " + Build.MANUFACTURER + " " + Build.MODEL);
        running = true;
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL, CHANNEL, 2);
        notificationChannel.setDescription("squib's controls: hide, feed, pause");
        ((NotificationManager) getSystemService(NotificationManager.class)).createNotificationChannel(notificationChannel);
        startInForeground();
        Pause.clear(this);
        if (!Settings.canDrawOverlays(this)) {
            Trace.w("service", "no permission to show over other apps: stopping");
            stopSelf();
            return;
        }
        Trace.i("service", "usage access: " + hasUsageAccess(this));
        this.wm = (WindowManager) getSystemService(WindowManager.class);
        this.usage = (UsageStatsManager) getSystemService(UsageStatsManager.class);
        this.audio = (AudioManager) getSystemService(AudioManager.class);
        // A fresh start: no headphones carried over from before he was stopped or paused.
        Fun.listening = false;
        Fun.listenOff = -1;
        Fun.musicFor = 0;
        addWindows();
        Remote.start(this);
        registerScreenReceiver();
        pollForegroundApp();
        pollMusic();
        // Started with the screen off (by the pause alarm or at boot): wait for the screen to come on.
        if (!((PowerManager) getSystemService(PowerManager.class)).isInteractive()) {
            Trace.i("service", "started with the screen off: waiting for it to come on");
            this.screenOn = false;
            this.screenOffAt = SystemClock.elapsedRealtime();
            return;
        }
        this.handler.post(this.frame);
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        startInForeground();
        if (this.stage == null) {
            return 2;
        }
        String action = intent != null ? intent.getAction() : null;
        Trace.i("service", "command: " + action + (intent != null && intent.getStringExtra("act") != null
                ? " act=" + intent.getStringExtra("act") + " variant=" + intent.getIntExtra("variant", -1) : ""));
        if (ACTION_STOP.equals(action)) {
            stopSelf();
            return 2;
        }
        if (ACTION_PAUSE.equals(action)) {
            Pause.begin(this);
            stopSelf();
            return 2;
        }
        if (ACTION_HIDE.equals(action)) {
            this.stage.userHidden = !this.stage.userHidden;
            startInForeground();
        } else if (ACTION_FEED.equals(action)) {
            this.stage.userHidden = false;
            this.stage.feed();
        } else if (ACTION_NAG.equals(action)) {
            this.stage.userHidden = false;
            this.stage.lateNag(Math.max(0, Fun.nagPhase()));
        } else if (ACTION_TALK.equals(action)) {
            this.stage.userHidden = false;
            this.stage.talk();
        } else if (ACTION_TRICK.equals(action)) {
            this.stage.userHidden = false;
            String act = intent.getStringExtra("act");
            if (act != null) {
                this.stage.trick(act, intent.getIntExtra("variant", -1));
            } else {
                this.stage.trick();
            }
        } else if (ACTION_REFRESH.equals(action)) {
            this.stage.reloadPrefs();
            measureScreen();
        }
        return 1;
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (this.stage != null) {
            measureScreen();
        }
    }

    @Override
    public void onDestroy() {
        Trace.i("service", "stopping");
        Trace.flush();
        running = false;
        this.handler.removeCallbacks(this.frame);
        BroadcastReceiver broadcastReceiver = this.screenReceiver;
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
        try {
            SquibView squibView = this.stage;
            if (squibView != null) {
                this.wm.removeView(squibView);
            }
            View view = this.handle;
            if (view != null) {
                this.wm.removeView(view);
            }
        } catch (RuntimeException e) {
        }
        super.onDestroy();
    }

    void startInForeground() {
        Notification buildNotification = buildNotification();
        if (Build.VERSION.SDK_INT < 34) {
            startForeground(1, buildNotification);
        } else {
            startForeground(1, buildNotification, 1073741824);
        }
    }

    Notification buildNotification() {
        SquibView squibView = this.stage;
        boolean z = squibView != null && squibView.userHidden;
        return new Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_notif).setContentTitle(z ? "squib is hiding" : "squib is hanging out").setContentText("tap for settings").setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, (Class<?>) MainActivity.class), 201326592)).setOngoing(true).setShowWhen(false).addAction(action(z ? "show" : "hide", ACTION_HIDE, 1)).addAction(action("feed", ACTION_FEED, 2)).addAction(action("pause", ACTION_PAUSE, 3)).build();
    }

    Notification.Action action(String str, String str2, int i) {
        return new Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_notif), str, PendingIntent.getForegroundService(this, i, new Intent(this, (Class<?>) SquibService.class).setAction(str2), 201326592)).build();
    }

    void addWindows() {
        this.stage = new SquibView(this);
        this.stage.onLateNag = new Runnable() {
            @Override
            public void run() {
                Haptics.tick(SquibService.this);
            }
        };
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(1, 1, 2038, 16778008, -3);
        this.stageLp = layoutParams;
        layoutParams.gravity = 8388659;
        if (Build.VERSION.SDK_INT >= 31) {
            this.stageLp.alpha = ((InputManager) getSystemService(InputManager.class)).getMaximumObscuringOpacityForTouch();
        }
        if (Build.VERSION.SDK_INT >= 28) {
            this.stageLp.layoutInDisplayCutoutMode = 1;
        }
        measureScreen();
        this.wm.addView(this.stage, this.stageLp);
        View view = new View(this);
        this.handle = view;
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view2, MotionEvent motionEvent) {
                return SquibService.this.stage.onHandleTouch(motionEvent);
            }
        });
        WindowManager.LayoutParams layoutParams2 = new WindowManager.LayoutParams(1, 1, 2038, 776, -3);
        this.handleLp = layoutParams2;
        layoutParams2.gravity = 8388659;
        if (Build.VERSION.SDK_INT >= 28) {
            this.handleLp.layoutInDisplayCutoutMode = 1;
        }
        this.handle.setVisibility(8);
        this.wm.addView(this.handle, this.handleLp);
    }

    void measureScreen() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.wm.getDefaultDisplay().getRealMetrics(displayMetrics);
        this.stageLp.width = displayMetrics.widthPixels;
        this.stageLp.height = displayMetrics.heightPixels;
        if (this.stage.isAttachedToWindow()) {
            this.wm.updateViewLayout(this.stage, this.stageLp);
        }
        this.stage.onScreen(displayMetrics.widthPixels, displayMetrics.heightPixels, dimen("status_bar_height"), dimen("navigation_bar_height"));
    }

    int dimen(String str) {
        int identifier = getResources().getIdentifier(str, "dimen", "android");
        if (identifier > 0) {
            return getResources().getDimensionPixelSize(identifier);
        }
        return 0;
    }

    void updateHandle() {
        boolean handleRect = this.stage.handleRect(this.handleRect);
        if (handleRect != this.handleShown) {
            this.handleShown = handleRect;
            this.handle.setVisibility(handleRect ? 0 : 8);
        }
        if (handleRect && !this.handleRect.equals(this.shownRect)) {
            this.shownRect.set(this.handleRect);
            this.handleLp.x = this.handleRect.left;
            this.handleLp.y = this.handleRect.top;
            this.handleLp.width = this.handleRect.width();
            this.handleLp.height = this.handleRect.height();
            this.wm.updateViewLayout(this.handle, this.handleLp);
        }
    }

    void pollForegroundApp() {
        if (hasUsageAccess(this)) {
            long currentTimeMillis = System.currentTimeMillis();
            long j = this.lastUsageQuery;
            UsageEvents queryEvents = this.usage.queryEvents(j == 0 ? currentTimeMillis - 600000 : j - 2000, currentTimeMillis);
            UsageEvents.Event event = new UsageEvents.Event();
            String str = null;
            int n = 0;
            while (queryEvents.hasNextEvent()) {
                queryEvents.getNextEvent(event);
                if (event.getEventType() == 1) {
                    str = event.getPackageName();
                    n++;
                    Trace.t("apps", "to front: " + str);
                }
            }
            this.lastUsageQuery = currentTimeMillis;
            if (str == null || str.equals("com.android.systemui")) {
                return;
            }
            boolean own = str.equals(getPackageName());
            if (own != this.stage.ownAppOpen) {
                Trace.d("apps", own ? "his own settings opened" : "left his own settings");
            }
            this.stage.ownAppOpen = own;
            if (this.stage.ownAppOpen) {
                return; // his own settings: stays in the mode of the app you came from
            }
            this.stage.setApp(str);
        }
    }

    /**
     * Whether anything is playing as music. Android says so to any app without a permission, but not which app
     * it is, so squib only puts headphones on while a music app is open (see Fun.listen).
     */
    void pollMusic() {
        if (this.audio != null && this.stage != null) {
            boolean was = this.stage.musicOn;
            boolean active = false;
            try {
                active = this.audio.isMusicActive();
            } catch (RuntimeException e) {
                Trace.e("music", "music check failed", e);
            }
            // isMusicActive alone isn't enough: on this phone it says "playing" for about 3 seconds after the screen
            // comes on, from a system sound (usage unknown) or with nothing listed at all. Music and game sound
            // is listed as a player with usage MEDIA or GAME, so both have to agree.
            int media = mediaPlayers();
            this.stage.musicOn = active && media > 0;
            if (active && media == 0) {
                Trace.t("music", "android says playing, but no music or game player is listed: ignoring it");
            }
            String players = players();
            boolean playersChanged = !players.equals(this.lastPlayers);
            this.lastPlayers = players;
            Trace.t("music", "check: " + (this.stage.musicOn ? "PLAYING" : "quiet") + ", app " + this.stage.pkg + " (" + this.stage.kind
                    + "), audio mode " + this.audio.getMode() + ", headphones " + (Fun.listening ? "on" : "off")
                    + ", music for " + String.format(java.util.Locale.US, "%.1f", Fun.musicFor) + "s"
                    + (playersChanged ? ", players now: " + players : ""));
            if (was != this.stage.musicOn) {
                Trace.d("music", "music " + (this.stage.musicOn ? "STARTED" : "STOPPED") + " (android says), app " + this.stage.pkg);
            }
        }
    }

    String lastPlayers = "";

    /** How many listed players are music, video or game sound (usage MEDIA or GAME) and not paused. */
    int mediaPlayers() {
        int n = 0;
        try {
            for (AudioPlaybackConfiguration c : this.audio.getActivePlaybackConfigurations()) {
                int usage = c.getAudioAttributes().getUsage();
                if (usage != android.media.AudioAttributes.USAGE_MEDIA && usage != android.media.AudioAttributes.USAGE_GAME) {
                    continue;
                }
                String s = c.toString(); // the state isn't readable directly; it's in the description
                if (s.contains("state:") && !s.contains("state:started")) {
                    continue;
                }
                n++;
            }
        } catch (RuntimeException e) {
            Trace.e("music", "player list failed", e);
        }
        return n;
    }

    /** What Android lists as playing sound right now: the kind of sound (usage) and its state. No app names (Android hides them). */
    String players() {
        try {
            List<AudioPlaybackConfiguration> list = this.audio.getActivePlaybackConfigurations();
            if (list.isEmpty()) {
                return "none";
            }
            StringBuilder sb = new StringBuilder();
            for (AudioPlaybackConfiguration c : list) {
                if (sb.length() > 0) {
                    sb.append(" | ");
                }
                String s = c.toString().replaceAll("\\s+", " ");
                sb.append("usage=").append(c.getAudioAttributes().getUsage()).append(" content=")
                        .append(c.getAudioAttributes().getContentType()).append(" [").append(s).append("]");
            }
            return sb.toString();
        } catch (RuntimeException e) {
            return "unknown (" + e.getClass().getSimpleName() + ")";
        }
    }

    /** Writes crashes to the log before Android handles them. */
    static final class CrashLogger implements Thread.UncaughtExceptionHandler {
        final Thread.UncaughtExceptionHandler next;

        CrashLogger(Thread.UncaughtExceptionHandler next) {
            this.next = next;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable err) {
            Trace.e("crash", "crashed on " + thread.getName(), err);
            if (next != null) {
                next.uncaughtException(thread, err);
            }
        }
    }

    static boolean hasUsageAccess(Context context) {
        int checkOpNoThrow;
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        int myUid = Process.myUid();
        if (Build.VERSION.SDK_INT >= 29) {
            checkOpNoThrow = appOpsManager.unsafeCheckOpNoThrow("android:get_usage_stats", myUid, context.getPackageName());
        } else {
            checkOpNoThrow = appOpsManager.checkOpNoThrow("android:get_usage_stats", myUid, context.getPackageName());
        }
        return checkOpNoThrow == 0;
    }

    void registerScreenReceiver() {
        this.screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Trace.i("screen", intent.getAction().replace("android.intent.action.", "").toLowerCase(java.util.Locale.US)
                        + " (headphones " + (Fun.listening ? "on" : "off") + ", music " + SquibService.this.stage.musicOn + " from before)");
                if ("android.intent.action.USER_PRESENT".equals(intent.getAction())) {
                    SquibService.this.pollMusic(); // unlocked: just for the log
                    return;
                }
                if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                    Trace.flush();
                    SquibService.this.screenOn = false;
                    SquibService.this.screenOffAt = SystemClock.elapsedRealtime();
                    SquibService.this.handler.removeCallbacks(SquibService.this.frame);
                    return;
                }
                if ("android.intent.action.SCREEN_ON".equals(intent.getAction()) && !SquibService.this.screenOn) {
                    SquibService.this.screenOn = true;
                    // The music check stops while the screen is off, so check again before he moves: if it was paused
                    // while the phone was locked, the headphones come straight off instead of after the gap-between-songs wait.
                    SquibService.this.pollMusic();
                    if (!SquibService.this.stage.musicOn) {
                        if (Fun.listening) {
                            Trace.i("headphones", "OFF at once: screen came on and nothing is playing");
                        }
                        Fun.listening = false;
                        Fun.listenOff = -1;
                        Fun.musicFor = 0;
                    }
                    SquibService.this.stage.onScreenOn((SystemClock.elapsedRealtime() - SquibService.this.screenOffAt) / 1000.0d);
                    SquibService.this.handler.post(SquibService.this.frame);
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        if (Build.VERSION.SDK_INT < 33) {
            registerReceiver(this.screenReceiver, intentFilter);
        } else {
            registerReceiver(this.screenReceiver, intentFilter, 4);
        }
    }
}
