package com.ardeapps.livelocation.handlers.facebook;

import com.ardeapps.livelocation.objects.Profile;

/**
 * Created by Arttu on 18.6.2017.
 */

public interface GetProfileInfoHandler {
    void onGetProfileInfoSuccess(String firstName, String lastName);
}
