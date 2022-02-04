package com.ardeapps.livelocation.objects;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;

/**
 * Created by Arttu on 1.7.2017.
 */

public class LiveLatLng implements Serializable {
    public double latitude;
    public double longitude;

    public LiveLatLng() {}

    public LiveLatLng(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public LatLng asLatLng() {
        return new LatLng(latitude, longitude);
    }
}
