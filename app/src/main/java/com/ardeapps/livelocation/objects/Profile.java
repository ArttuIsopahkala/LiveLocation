package com.ardeapps.livelocation.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Arttu on 18.6.2017.
 */

public class Profile implements Serializable {
    public String userId;
    public String firstName;
    public String lastName;
    public List<String> friends;
    public long lastLoginTime;
    public long creationTime;
    public ShareResource shareResource;
    public boolean isImageUploaded;
    public List<String> sentFriendRequests;
    public String facebookId;

    public Profile() {
    }

    public Profile clone() {
        Profile clone = new Profile();
        clone.userId = this.userId;
        clone.firstName = this.firstName;
        clone.lastName = this.lastName;
        clone.lastLoginTime = this.lastLoginTime;
        clone.creationTime = this.creationTime;
        clone.isImageUploaded = this.isImageUploaded;
        clone.shareResource = this.shareResource;
        clone.facebookId = this.facebookId;

        if(this.friends != null) {
            clone.friends = new ArrayList<>();
            for(String userId : this.friends)
                clone.friends.add(userId);
        } else {
            clone.friends = new ArrayList<>();
        }
        if(this.sentFriendRequests != null) {
            clone.sentFriendRequests = new ArrayList<>();
            for(String userId : this.sentFriendRequests)
                clone.sentFriendRequests.add(userId);
        } else {
            clone.sentFriendRequests = new ArrayList<>();
        }

        return clone;
    }

    public User asUser() {
        User user = new User();
        user.userId = this.userId;
        user.firstName = this.firstName;
        user.lastName = this.lastName;
        user.isImageUploaded = this.isImageUploaded;
        user.facebookId = this.facebookId;
        return user;
    }
}
