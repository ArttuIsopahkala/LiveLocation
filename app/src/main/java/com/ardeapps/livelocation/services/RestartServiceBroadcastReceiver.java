package com.ardeapps.livelocation.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Arttu on 26.5.2017.
 */
public class RestartServiceBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case NotificationService.RESTART_NOTIFICATION_SERVICE:
                context.startService(new Intent(context, NotificationService.class));
                break;
        }
    }
}
