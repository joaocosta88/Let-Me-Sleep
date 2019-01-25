package com.laila.letmesleep;


import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import com.laila.letmesleep.R;

public class AlarmInitReceiver extends BroadcastReceiver {

    /**
     * Sets alarm on ACTION_BOOT_COMPLETED.  Resets alarm on
     * TIME_SET, TIMEZONE_CHANGED
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Alarms.saveSnoozeAlert(context, -1, -1);
            Alarms.disableExpiredAlarms(context);
        }
        Alarms.setNextAlert(context);
    }
}