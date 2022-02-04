package com.ardeapps.livelocation.handlers.firebase;

import com.ardeapps.livelocation.objects.LocationShare;
import com.ardeapps.livelocation.objects.User;

import java.util.ArrayList;

/**
 * Created by Arttu on 18.6.2017.
 */

public interface GetSharedLocationsHandler {
    void onGetSharedLocationsSuccess(ArrayList<LocationShare> locationShares);
}
