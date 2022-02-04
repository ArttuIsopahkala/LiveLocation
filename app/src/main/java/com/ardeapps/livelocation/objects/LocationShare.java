package com.ardeapps.livelocation.objects;

/**
 * Created by Arttu on 2.7.2017.
 */

public class LocationShare {

    public enum ShareType {
        ONGOING,
        FOREVER,
        ONCE
    }

    public String userId;
    public String firstName;
    public String lastName;
    public long startTime;
    public long endTime;
    public LiveLatLng location;
    public ShareType shareType;

    public LocationShare() {
    }

    public LocationShare clone() {
        LocationShare clone = new LocationShare();
        clone.userId = this.userId;
        clone.firstName = this.firstName;
        clone.lastName = this.lastName;
        clone.startTime = this.startTime;
        clone.endTime = this.endTime;
        clone.location = this.location;
        clone.shareType = this.shareType;
        return clone;
    }
}
