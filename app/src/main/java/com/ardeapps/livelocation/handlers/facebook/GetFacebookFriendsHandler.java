package com.ardeapps.livelocation.handlers.facebook;

import com.ardeapps.livelocation.objects.User;

import java.util.ArrayList;

/**
 * Created by Arttu on 18.6.2017.
 */

public interface GetFacebookFriendsHandler {
    void onGetFacebookFriendsSuccess(ArrayList<String> friendsFacebookIds);
}
