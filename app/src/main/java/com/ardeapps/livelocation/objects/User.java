package com.ardeapps.livelocation.objects;

import java.io.Serializable;

/**
 * Created by Arttu on 18.6.2017.
 */

public class User implements Serializable {
    public String userId;
    public String firstName;
    public String lastName;
    public boolean isImageUploaded;
    public String facebookId;

    public User() {
    }
}
