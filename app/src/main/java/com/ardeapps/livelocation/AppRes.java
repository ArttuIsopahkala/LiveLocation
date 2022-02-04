package com.ardeapps.livelocation;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.LocationProvider;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.ardeapps.livelocation.objects.LiveLatLng;
import com.ardeapps.livelocation.objects.LocationShare;
import com.ardeapps.livelocation.objects.Profile;
import com.ardeapps.livelocation.objects.ShareResource;
import com.ardeapps.livelocation.objects.User;
import com.facebook.AccessToken;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Created by Arttu on 4.5.2017.
 */
public class AppRes extends Application {

    private static Context mContext;
    private Activity activity;
    private Profile profile;
    private ArrayList<User> users;
    private ArrayList<User> friends;
    private ArrayList<String> friendsFacebookIds;
    private ArrayList<User> friendRequests;
    private ArrayList<LocationShare> locationShares;
    private Map<String, Bitmap> profilePictures;
    private LiveLatLng location;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    public static void hideKeyBoard(View tokenView) {
        InputMethodManager inputMethodManager = (InputMethodManager) mContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(tokenView.getWindowToken(), 0);
    }

    public static Context getContext(){
        return mContext;
    }

    public static boolean getIsLoggedInWithFacebook() {
        return AccessToken.getCurrentAccessToken() != null;
    }

    public boolean getIsAppVisible() {
        SharedPreferences appPref = getSharedPreferences("app", 0);
        return appPref.getBoolean("isAppVisible", false);
    }

    public void setIsAppVisible(boolean isAppVisible) {
        SharedPreferences appPref = getSharedPreferences("app", 0);
        SharedPreferences.Editor editor = appPref.edit();
        editor.putBoolean("isAppVisible", isAppVisible);
        editor.apply();
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public Profile getProfile() {
        return profile.clone();
    }

    public void setUsers(ArrayList<User> users) {
        this.users = users;
    }

    public ArrayList<User> getUsers() {
        return users;
    }

    public void setFriendsFacebookIds(ArrayList<String> friendsFacebookIds) {
        this.friendsFacebookIds = friendsFacebookIds;
    }

    public ArrayList<String> getFriendsFacebookIds() {
        return friendsFacebookIds;
    }

    public void setFriends(ArrayList<User> friends) {
        this.friends = friends;
    }

    public ArrayList<User> getFriends() {
        return friends;
    }

    public void setFriendRequests(ArrayList<User> friendRequests) {
        this.friendRequests = friendRequests;
    }

    public ArrayList<User> getFriendRequests() {
        return friendRequests;
    }

    public void setLocation(LiveLatLng location) {
        this.location = location;
    }

    public LiveLatLng getLocation() {
        return location;
    }

    public void setSharedLocations(ArrayList<LocationShare> locationShares) {
        this.locationShares = locationShares;
    }

    public ArrayList<LocationShare> getSharedLocations() {
        return locationShares;
    }

    public void setProfilePictures(Map<String, Bitmap> profilePictures) {
        this.profilePictures = profilePictures;
    }

    public Map<String, Bitmap> getProfilePictures() {
        return profilePictures;
    }

    public void setShareResourceForService(ShareResource shareResource) {
        SharedPreferences profilePref = getSharedPreferences("profile", 0);
        SharedPreferences.Editor editor = profilePref.edit();
        if(shareResource != null) {
            //editor.putBoolean("isSharingLocation", shareResource.isSharingLocation);
            editor.putStringSet("friendIds", new HashSet<>(shareResource.friendIdsToShare));
            editor.putLong("shareEndTime", shareResource.endTime);
            editor.putString("shareType", shareResource.shareType.name());
        } else {
            Set<String> emptySet = new HashSet<>();
            editor.putStringSet("friendIds", emptySet);
            editor.putLong("shareEndTime", 0);
            editor.putString("shareType", "");
        }
        editor.apply();
    }

    public ShareResource getShareResourceForService() {
        Set<String> emptySet = new HashSet<>();
        SharedPreferences profilePref = getSharedPreferences("profile", 0);
        ShareResource shareResource = new ShareResource();
        //shareResource.isSharingLocation = profilePref.getBoolean("isSharingLocation", false);
        shareResource.friendIdsToShare = new ArrayList<>(profilePref.getStringSet("friendIds", emptySet));
        shareResource.endTime = profilePref.getLong("shareEndTime", 0);
        if(StringUtil.isEmptyString(profilePref.getString("shareType", ""))) {
            shareResource.shareType = LocationShare.ShareType.ONCE;
        } else {
            shareResource.shareType = LocationShare.ShareType.valueOf(profilePref.getString("shareType", ""));
        }
        return shareResource;
    }
}
