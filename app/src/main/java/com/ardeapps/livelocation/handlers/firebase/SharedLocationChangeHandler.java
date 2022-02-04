package com.ardeapps.livelocation.handlers.firebase;

import com.ardeapps.livelocation.objects.LocationShare;

import java.util.ArrayList;

/**
 * Created by Arttu on 18.6.2017.
 */

public interface SharedLocationChangeHandler {
    void onSharedLocationAdded(LocationShare locationShare);
    void onSharedLocationChange(LocationShare locationShare);
}
