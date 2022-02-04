package com.ardeapps.livelocation.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Arttu on 18.6.2017.
 */

public class Notification implements Serializable {

    public enum NotificationType {
        SHARE_LOCATION_STARTED,
        FRIEND_REQUEST_ACCEPTED,
        FRIEND_REQUEST_SENT,
        LOCATION_REQUESTED,
        LOCATION_SHARED,
        FRIEND_STARTED_MOVING
    }

    public String notificationId;
    public User sender;
    public NotificationType type;
    public long sendTime;

    public Notification() {
    }

    public Notification clone() {
        Notification clone = new Notification();
        clone.notificationId = this.notificationId;
        clone.sender = this.sender;
        clone.type = this.type;
        clone.sendTime = this.sendTime;

        return clone;
    }
}
