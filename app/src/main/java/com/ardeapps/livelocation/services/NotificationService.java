package com.ardeapps.livelocation.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.ardeapps.livelocation.AppRes;
import com.ardeapps.livelocation.MainActivity;
import com.ardeapps.livelocation.R;
import com.ardeapps.livelocation.StringUtil;
import com.ardeapps.livelocation.objects.LocationShare;
import com.ardeapps.livelocation.objects.Notification;
import com.ardeapps.livelocation.objects.User;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static android.R.attr.name;
import static com.ardeapps.livelocation.objects.Notification.NotificationType.FRIEND_STARTED_MOVING;
import static com.ardeapps.livelocation.services.FirebaseService.FRIEND_REQUESTS;
import static com.ardeapps.livelocation.services.FirebaseService.NOTIFICATIONS;
import static com.ardeapps.livelocation.services.FirebaseService.SHARED_LOCATIONS;


public class NotificationService extends Service {
    public static final String RESTART_NOTIFICATION_SERVICE = "com.ardeapps.livelocation.RESTART_NOTIFICATION_SERVICE";

    DatabaseReference database;
    SharedPreferences userPref;
    String userId;
    String lastNotificationId;

    @Override
    public void onCreate() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sendBroadcast(new Intent(RESTART_NOTIFICATION_SERVICE));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        super.onStartCommand(intent, flags, startId);
        userPref = getSharedPreferences("profile", 0);
        userId = userPref.getString("userId", "");
        lastNotificationId = userPref.getString("lastNotificationId", "");

        database = FirebaseDatabase.getInstance().getReference();

        if(userId != null) {
            database.child(NOTIFICATIONS).child(userId).limitToLast(1).addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    final Notification notification = dataSnapshot.getValue(Notification.class);
                    if(notification != null) {
                        if(StringUtil.isEmptyString(lastNotificationId) || !notification.notificationId.equals(lastNotificationId)) {
                            String name = StringUtil.getFullName(notification.sender);
                            switch (notification.type) {
                                case FRIEND_REQUEST_SENT:
                                    showNotification(name + " " + getString(R.string.notification_request_sent), 0);
                                    break;
                                case FRIEND_REQUEST_ACCEPTED:
                                    showNotification(name + " " + getString(R.string.notification_request_accepted), 1);
                                    break;
                                case SHARE_LOCATION_STARTED:
                                    showNotification(name + " " + getString(R.string.notification_started_share), 2);
                                    break;
                                case LOCATION_REQUESTED:
                                    showNotification(name + " " + getString(R.string.notification_location_requested), 3);
                                    break;
                                case LOCATION_SHARED:
                                    showNotification(name + " " + getString(R.string.notification_location_sent), 4);
                                    break;
                                case FRIEND_STARTED_MOVING:
                                    showNotification(name + " " + getString(R.string.notification_started_moving), 5);
                                    break;
                            }

                            lastNotificationId = notification.notificationId;
                            SharedPreferences.Editor editor = userPref.edit();
                            editor.putString("lastNotificationId", notification.notificationId);
                            editor.apply();
                        }
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {}

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * int option
     * 0: henkilö alkoi jakaa sijainnin kanssasi
     * 1: kaveripyyntö käsiteltiin tai vastaanotettu -> avaa ja päivitä friendsFragment
     */
    public void showNotification(String content, int option) {
        AppRes appRes = (AppRes) getApplicationContext();
        if(!appRes.getIsAppVisible()) {
            String contentTitle = getString(R.string.app_name);

            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent mainPendingIntent =
                    PendingIntent.getActivity(
                            this,
                            0,
                            notificationIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(contentTitle)
                    .setContentText(content)
                    .setAutoCancel(true)
                    .setContentIntent(mainPendingIntent);


            mBuilder.setVibrate(new long[]{1000, 500});

            // Gets an instance of the NotificationManager service
            NotificationManager mNotifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            // Builds the notification and issues it.
            mNotifyMgr.notify(option, mBuilder.build());
        }
    }
}
