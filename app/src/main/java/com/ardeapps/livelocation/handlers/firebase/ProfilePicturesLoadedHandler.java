package com.ardeapps.livelocation.handlers.firebase;

import android.graphics.Bitmap;

import java.util.Map;

/**
 * Created by Arttu on 29.6.2017.
 */

public interface ProfilePicturesLoadedHandler {
    void onProfilePicturesLoaded(Map<String, Bitmap> profilePictures);
}
