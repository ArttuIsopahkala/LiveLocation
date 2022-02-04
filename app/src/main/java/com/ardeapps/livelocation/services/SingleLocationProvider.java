package com.ardeapps.livelocation.services;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.ardeapps.livelocation.AppRes;
import com.ardeapps.livelocation.objects.LiveLatLng;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import static com.ardeapps.livelocation.services.FragmentListeners.MY_PERMISSION_ACCESS_COARSE_LOCATION;
import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by Arttu on 21.11.2016.
 */
public class SingleLocationProvider {
    public interface GetLocationOnceHandler {
        void onGetLocationOnceSuccess(LiveLatLng location);
    }

    public static void getLocationOnce(final GetLocationOnceHandler handler) {
        AppRes appRes = (AppRes) getApplicationContext();
        Activity activity = appRes.getActivity();
        final boolean locationPermissionNeeded = ContextCompat.checkSelfPermission(activity,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        if (locationPermissionNeeded) {
            ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSION_ACCESS_COARSE_LOCATION);
        } else {
            LocationServices.getFusedLocationProviderClient(activity).getLastLocation().addOnSuccessListener(activity, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        LiveLatLng loc = new LiveLatLng(location.getLatitude(), location.getLongitude());
                        handler.onGetLocationOnceSuccess(loc);
                    }
                }
            });
        }
    }
}
