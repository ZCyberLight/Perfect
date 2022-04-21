package com.cyberlight.perfect.service;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;

import com.cyberlight.perfect.R;
import com.cyberlight.perfect.ui.FocusActivity;
import com.cyberlight.perfect.util.DbUtil;
import com.cyberlight.perfect.util.FlashlightUtil;
import com.cyberlight.perfect.util.SettingManager;
import com.cyberlight.perfect.util.SharedPrefSettingManager;
import com.cyberlight.perfect.util.ToastUtil;

import java.text.NumberFormat;

@SuppressLint("UnspecifiedImmutableFlag")
public class FocusService extends Service {
    private static final boolean ENABLE_TEST_MODE = false;

    // 闪光、振动提醒的时间
    private static final long[] RELAX_VIBRATION_FLASHLIGHT_TIMINGS = {0, 100, 100, 100, 100, 100, 100, 100};
    private static final long[] FOCUS_VIBRATION_FLASHLIGHT_TIMINGS = {0, 800, 100, 800};

    // 该前台服务的通知相关常量
    private static final CharSequence CHANNEL_NAME = "Focus notifications";
    private static final String CHANNEL_ID = "focus_channel";
    private static final int CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_DEFAULT;
    private static final int NOTIFICATION_ID = 8;

    // 专注定时提醒任务相关的信息
    private long mCurStart;
    private long mNextStart;
    private long mCurDuration;
    private boolean mFocusing;
    private String mFocusStateStr;

    // 设置中的值，加载后不应改变
    private long mFocusDuration;
    private long mRelaxDuration;
    private boolean mVibration;
    private boolean mSound;
    private boolean mFlashlight;
    private boolean mStrictTime;

    // 服务相关
    private OnUpdateListener mOnUpdateListener;
    private BroadcastReceiver mFocusReminderReceiver;
    private final IBinder mBinder = new FocusServiceBinder();
    private final NumberFormat mNumberFormat = NumberFormat.getNumberInstance();
    private final Handler mHandler = new Handler();
    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            // 计算进度、剩余时间
            long curTimeMillis = System.currentTimeMillis();
            long remain = mNextStart - curTimeMillis;
            int remainSecs = (int) (remain / 1000);
            int min = remainSecs / 60;
            int sec = remainSecs % 60;
            float mProgress = (float) remain / mCurDuration;
            String mRemainTimeStr = mNumberFormat.format(min) + ":" + mNumberFormat.format(sec);
            // 更新通知
            Notification notification = buildNotification(mFocusStateStr, mRemainTimeStr);
            showNotification(notification);
            // 为保证秒数显示稳定、不会跳数，计算下次刷新的延迟，
            // 控制每次在一秒的中间刷新
            long delayMillis = 1000 + (500 - (curTimeMillis % 1000));
            // 通知Activity更新界面
            if (mOnUpdateListener != null)
                mOnUpdateListener.onUpdate(delayMillis, mProgress, mRemainTimeStr, mFocusStateStr);
            mHandler.postDelayed(mRunnable, delayMillis);
        }
    };

    @Override
    public void onCreate() {
        mNumberFormat.setMinimumIntegerDigits(2);
        // 注册专注广播接收器
        mFocusReminderReceiver = new FocusReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(FocusReceiver.REMIND_ACTION);
        filter.addAction(FocusReceiver.UPDATE_ACTION);
        registerReceiver(mFocusReminderReceiver, filter);
        // 加载设置
        loadSettings();
        // 初始化专注定时提醒任务数据
        initReminderData();
        // 启动专注提醒
        setAlarm(this, mNextStart);
        // 创建通知渠道
        createNotificationChannel();
        // 设置为前台服务
        startForeground(NOTIFICATION_ID, buildNotification("", ""));
        // 启动每秒更新任务
        mHandler.post(mRunnable);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        // 停止刷新任务
        mHandler.removeCallbacks(mRunnable);
        // 注销广播接收器
        unregisterReceiver(mFocusReminderReceiver);
        // 取消定时任务
        cancelAlarm(this);
        super.onDestroy();
    }

    public void setOnUpdateListener(OnUpdateListener onUpdateListener) {
        mOnUpdateListener = onUpdateListener;
    }

    public void removeOnUpdateListener() {
        mOnUpdateListener = null;
    }

    /**
     * 获取设置的值
     */
    private void loadSettings() {
        SettingManager settingManager = SharedPrefSettingManager.getInstance(this);
        mFocusDuration = settingManager.getFocusDuration();
        mRelaxDuration = mFocusDuration < 1800000 ? 1800000 - mFocusDuration : 3600000 - mFocusDuration;
        mVibration = settingManager.getVibration();
        mSound = settingManager.getSound();
        mFlashlight = settingManager.getFlashlight();
        mStrictTime = settingManager.getStrictTime();

        // TEST
        if (ENABLE_TEST_MODE) {
            mFocusDuration = 12987;
            mRelaxDuration = 5000;
            mStrictTime = false;
        }
    }

    /**
     * 初始化FocusReminderReceiver(定时任务)的数据，计算初始倒计时
     */
    private void initReminderData() {
        long curTime = System.currentTimeMillis();
        mCurStart = curTime;
        if (mStrictTime) {
            // 计算现在到下次提醒剩余毫秒
            long remainMillis = 3600000 - (curTime % 3600000);// 距下一小时剩余毫秒
            if (mFocusDuration < 1800000) {
                // mFocusDuration < 1800000说明半小时为一个周期
                remainMillis = remainMillis > 1800000 ? remainMillis - 1800000 : remainMillis;
            }
            if (remainMillis > mRelaxDuration) {
                // 当前在专注时段
                remainMillis = remainMillis - mRelaxDuration;
                mNextStart = curTime + remainMillis;
                mFocusing = true;
                mCurDuration = mFocusDuration;
                mFocusStateStr = getString(R.string.focus_focusing_state);
            } else {
                // 当前在休息时段
                mNextStart = curTime + remainMillis;
                mFocusing = false;
                mCurDuration = mRelaxDuration;
                mFocusStateStr = getString(R.string.focus_relaxing_state);
            }
        } else {
            // 不是严格时间模式
            mNextStart = curTime + mFocusDuration;
            mFocusing = true;
            mCurDuration = mFocusDuration;
            mFocusStateStr = getString(R.string.focus_focusing_state);
        }
    }

    /**
     * 通知专注服务设置有变化
     *
     * @param context 可用Context对象
     */
    public static void sendUpdateBroadcast(Context context) {
        Intent intent = new Intent();
        intent.setAction(FocusReceiver.UPDATE_ACTION);
        context.sendBroadcast(intent);
    }

    /**
     * 设置专注提醒定时任务
     *
     * @param context         可用Context对象
     * @param triggerAtMillis 任务触发时间(EpochMilli)
     */
    private static void setAlarm(Context context, long triggerAtMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent();
        intent.setAction(FocusReceiver.REMIND_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
    }

    /**
     * 取消专注提醒定时任务
     *
     * @param context 可用Context对象
     */
    private static void cancelAlarm(Context context) {
        Intent intent = new Intent();
        intent.setAction(FocusReceiver.REMIND_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, CHANNEL_IMPORTANCE);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private Notification buildNotification(String title, String text) {
        Intent intent = new Intent(this, FocusActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void showNotification(Notification notification) {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(NOTIFICATION_ID, notification);
    }

    private class FocusReceiver extends BroadcastReceiver {
        private static final String REMIND_ACTION = "remind_action";
        private static final String UPDATE_ACTION = "update_action";

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(REMIND_ACTION)) {
                if (mFocusing) {
                    // 专注结束，切换到休息
                    // 保存专注记录到数据库
                    DbUtil.addFocusRecord(context, mNextStart, mNextStart - mCurStart);
                    // 计算下一次提醒任务的信息
                    mCurStart = mNextStart;
                    mNextStart += mRelaxDuration;
                    mCurDuration = mRelaxDuration;
                    mFocusing = false;
                    mFocusStateStr = getString(R.string.focus_relaxing_state);
                } else {
                    // 休息结束，切换到专注
                    // 计算下一次提醒任务的信息
                    mCurStart = mNextStart;
                    mNextStart += mFocusDuration;
                    mCurDuration = mFocusDuration;
                    mFocusing = true;
                    mFocusStateStr = getString(R.string.focus_focusing_state);
                }
                // 发出提醒
                remind(context, mFocusing, mSound, mVibration, mFlashlight);
                // 设置下次专注提醒
                setAlarm(context, mNextStart);
            } else if (action.equals(UPDATE_ACTION)) {
                // 刷新设置与专注数据，重新设置专注任务
                ToastUtil.showToast(context, R.string.focus_new_settings_applied_toast, Toast.LENGTH_SHORT);
                loadSettings();
                initReminderData();
                setAlarm(context, mNextStart);
            }
        }

        private void remind(Context context, boolean focusing, boolean sound, boolean vibration, boolean flashlight) {
            final long[] timings = focusing ? FOCUS_VIBRATION_FLASHLIGHT_TIMINGS : RELAX_VIBRATION_FLASHLIGHT_TIMINGS;
            final int resId = R.raw.focus_reminder_sound;
            if (sound) {
                MediaPlayer mediaPlayer = MediaPlayer.create(context.getApplicationContext(), resId);
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.start();
            }
            if (vibration) {
                AudioAttributes vibrationAttrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build();
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                VibrationEffect vibe = VibrationEffect.createWaveform(timings, -1);
                vibrator.vibrate(vibe, vibrationAttrs);
            }
            if (flashlight) {
                FlashlightUtil.flash(context, timings);
            }
        }
    }

    public class FocusServiceBinder extends Binder {
        public FocusService getService() {
            return FocusService.this;
        }
    }

    /**
     * 该接口仅限用于通知FocusActivity更新界面
     */
    public interface OnUpdateListener {
        void onUpdate(long delayMillis, float progress, String remainTimeStr, String focusStateStr);
    }

}
