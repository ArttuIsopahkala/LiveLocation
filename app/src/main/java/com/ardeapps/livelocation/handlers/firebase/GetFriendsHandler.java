package com.ardeapps.livelocation.handlers.firebase;

import com.ardeapps.livelocation.objects.User;

import java.util.ArrayList;

/**
 * Created by Arttu on 18.6.2017.
 */

public interface GetFriendsHandler {
    void onGetFriendsSuccess(ArrayList<User> friends);
}
