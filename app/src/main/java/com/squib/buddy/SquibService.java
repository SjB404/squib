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
        running = true;
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL, CHANNEL, 2);
        notificationChannel.setDescription("squib's controls: hide, feed, pause");
        ((NotificationManager) getSystemService(NotificationManager.class)).createNotificationChannel(notificationChannel);
        startInForeground();
        Pause.clear(this);
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        this.wm = (WindowManager) getSystemService(WindowManager.class);
        this.usage = (UsageStatsManager) getSystemService(UsageStatsManager.class);
        addWindows();
        Remote.start(this);
        registerScreenReceiver();
        pollForegroundApp();
        // Started with the screen off (by the pause alarm or at boot): wait for the screen to come on.
        if (!((PowerManager) getSystemService(PowerManager.class)).isInteractive()) {
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
        } else if (ACTION_TALK.equals(action)) {
            this.stage.userHidden = false;
            this.stage.talk();
        } else if (ACTION_TRICK.equals(action)) {
            this.stage.userHidden = false;
            this.stage.trick();
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
            while (queryEvents.hasNextEvent()) {
                queryEvents.getNextEvent(event);
                if (event.getEventType() == 1) {
                    str = event.getPackageName();
                }
            }
            this.lastUsageQuery = currentTimeMillis;
            if (str == null || str.equals(getPackageName()) || str.equals("com.android.systemui")) {
                return;
            }
            this.stage.setApp(str);
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
                if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                    SquibService.this.screenOn = false;
                    SquibService.this.screenOffAt = SystemClock.elapsedRealtime();
                    SquibService.this.handler.removeCallbacks(SquibService.this.frame);
                    return;
                }
                if ("android.intent.action.SCREEN_ON".equals(intent.getAction()) && !SquibService.this.screenOn) {
                    SquibService.this.screenOn = true;
                    SquibService.this.stage.onScreenOn((SystemClock.elapsedRealtime() - SquibService.this.screenOffAt) / 1000.0d);
                    SquibService.this.handler.post(SquibService.this.frame);
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        if (Build.VERSION.SDK_INT < 33) {
            registerReceiver(this.screenReceiver, intentFilter);
        } else {
            registerReceiver(this.screenReceiver, intentFilter, 4);
        }
    }
}
