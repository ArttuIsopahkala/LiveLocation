package com.ardeapps.livelocation.services;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.ardeapps.livelocation.AppRes;
import com.ardeapps.livelocation.MainActivity;
import com.ardeapps.livelocation.R;
import com.ardeapps.livelocation.StringUtil;
import com.ardeapps.livelocation.objects.LiveLatLng;
import com.ardeapps.livelocation.objects.LocationShare;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static com.ardeapps.livelocation.R.id.mapPermissionLayout;
import static com.ardeapps.livelocation.objects.LocationShare.ShareType.FOREVER;
import static com.ardeapps.livelocation.objects.LocationShare.ShareType.ONGOING;
import static com.ardeapps.livelocation.services.FragmentListeners.MY_PERMISSION_ACCESS_COARSE_LOCATION;

/**
 * Created by Arttu on 26.6.2017.
 */

public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private String TAG = LocationService.class.getSimpleName();

    public Location mLastLocation;

    private GoogleApiClient mGoogleApiClient;

    private LocationRequest mLocationRequest;

    private static int UPDATE_INTERVAL = 10000;
    private static int FASTEST_INTERVAL = 5000;
    private static int DISPLACEMENT = 20;

    Set<String> emptySet = new HashSet<>();
    private ArrayList<String> friendIds = new ArrayList<>();
    private long shareEndTime;
    private LocationShare.ShareType shareType;
    SharedPreferences profilePref;
    SharedPreferences appPref;

    NotificationManager mNotifyMgr;
    NotificationCompat.Builder mBuilder;
    public static int TIME_NOTIFICATION_ID = 99;
    private boolean firstTime = true;
    MyTimerTask myTask = new MyTimerTask();
    Timer myTimer = new Timer();

    public interface Listener {
        void onNewLocationForShareFound(LiveLatLng location);
    }

    static Listener mListener = null;

    public static void setListener(Listener l) {
        mListener = l;
    }

    @Override
    public void onCreate() {
        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);
        buildGoogleApiClient();
        FragmentListeners.getInstance().setPermissionHandledListener(new FragmentListeners.PermissionHandledListener() {
            @Override
            public void onPermissionGranted(int MY_PERMISSION) {
                if(MY_PERMISSION == MY_PERMISSION_ACCESS_COARSE_LOCATION) {
                    buildGoogleApiClient();
                }
            }
        });
    }

    /**
     * Sets up location service after permissions is granted
     */
    private void buildGoogleApiClient() {
        Context context = AppRes.getContext();

        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
        if(mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        super.onStartCommand(intent, flags, startId);
        profilePref = getSharedPreferences("profile", 0);
        appPref = getSharedPreferences("app", 0);

        friendIds.clear();
        friendIds.addAll(profilePref.getStringSet("friendIds", emptySet));
        shareEndTime = profilePref.getLong("shareEndTime", 0);
        if(StringUtil.isEmptyString(profilePref.getString("shareType", ""))) {
            shareType = LocationShare.ShareType.ONCE;
        } else {
            shareType = LocationShare.ShareType.valueOf(profilePref.getString("shareType", ""));
        }

        if(mGoogleApiClient != null) {
            if(!mGoogleApiClient.isConnected()) {
                mGoogleApiClient.connect();
            } else {
                startLocationUpdates();
            }
        } else {
            buildGoogleApiClient();
            mGoogleApiClient.connect();
        }

        return START_STICKY;
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        newLocationFound();
    }

    @Override
    public void onConnected(Bundle bundle) {
        try {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            newLocationFound();
        } catch (SecurityException ex) {
            Log.e(TAG, "SecurityException!");
        }

        startLocationUpdates();
    }

    private class MyTimerTask extends TimerTask {
        public void run() {
            if(shareType == ONGOING) {
                if (shareEndTime > System.currentTimeMillis()) {
                    showNotification();
                } else {
                    // Aika kulunut loppuun
                    onDestroy();
                }
            }
        }
    }

    protected void startLocationUpdates() {
        Log.e(TAG, "startLocationUpdates");
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            if(shareType == ONGOING)
                myTimer.schedule(myTask, 0, 60000);
            else
                showNotification();
        } catch (SecurityException ex) {
            Log.e(TAG, "SecurityException!");
        }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        myTimer.cancel();
        hideNotification();
    }

    private void newLocationFound() {
        if(shareType == ONGOING && System.currentTimeMillis() > shareEndTime) {
            onDestroy();
        } else {
            if(mLastLocation != null) {
                LiveLatLng loc = new LiveLatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                FirebaseService.getInstance().updateSharedLocation(friendIds, loc);
                if (mListener != null && appPref.getBoolean("isAppVisible", false)) {
                    mListener.onNewLocationForShareFound(loc);
                }
            }
        }
    }

    public void showNotification() {
        String contentTitle = getString(R.string.notification_sharing);
        String content;
        if(shareType == FOREVER) {
            content = AppRes.getContext().getString(R.string.share_time_until_stop);
        } else {
            long millisUntilFinish = shareEndTime - System.currentTimeMillis();
            content = StringUtil.getTimeLeftText(millisUntilFinish);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent mainPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        notificationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        if (firstTime) {
            mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(contentTitle)
                    .setContentText(content)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(mainPendingIntent);
            firstTime = false;
        }

        mBuilder.setContentText(content);

        Notification n = mBuilder.build();
        n.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

        mNotifyMgr.notify(TIME_NOTIFICATION_ID, n);
    }

    public void hideNotification() {
        mNotifyMgr.cancel(TIME_NOTIFICATION_ID);
        firstTime = true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: " + connectionResult.getErrorCode());
    }

}
