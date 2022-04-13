package com.cyberlight.perfect.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Log.d(TAG, "Boot completed");
            // 发送广播给RemindReceiver，启动事件提醒
            Intent startReminderIntent = new Intent(context, EventReminderReceiver.class);
            startReminderIntent.setAction(EventReminderReceiver.EVENT_REMINDER_ACTION);
            context.sendBroadcast(startReminderIntent);
        }
    }
}