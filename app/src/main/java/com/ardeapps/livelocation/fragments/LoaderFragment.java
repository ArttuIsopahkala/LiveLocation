package com.ardeapps.livelocation.fragments;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import com.ardeapps.livelocation.AppRes;
import com.ardeapps.livelocation.Logger;
import com.ardeapps.livelocation.R;
import com.ardeapps.livelocation.StringUtil;
import com.ardeapps.livelocation.handlers.firebase.ProfilePicturesLoadedHandler;
import com.ardeapps.livelocation.handlers.facebook.GetFacebookFriendsHandler;
import com.ardeapps.livelocation.handlers.firebase.AuthenticateHandler;
import com.ardeapps.livelocation.handlers.firebase.GetFriendRequestsHandler;
import com.ardeapps.livelocation.handlers.firebase.GetFriendsHandler;
import com.ardeapps.livelocation.handlers.firebase.GetProfileHandler;
import com.ardeapps.livelocation.handlers.firebase.GetSharedLocationsHandler;
import com.ardeapps.livelocation.handlers.firebase.GetUsersHandler;
import com.ardeapps.livelocation.objects.LocationShare;
import com.ardeapps.livelocation.objects.Profile;
import com.ardeapps.livelocation.objects.User;
import com.ardeapps.livelocation.services.FacebookService;
import com.ardeapps.livelocation.services.FirebaseService;
import com.facebook.AccessToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Map;

public class LoaderFragment extends Fragment implements GetFriendsHandler, GetProfileHandler, ProfilePicturesLoadedHandler, GetSharedLocationsHandler,
        GetFacebookFriendsHandler, GetUsersHandler, GetFriendRequestsHandler, AuthenticateHandler {

    FirebaseAuth mAuth;
    AppRes appRes = (AppRes) AppRes.getContext();
    Context context = AppRes.getContext();
    ImageView loader_icon;
    private int ANIMATION_LENGTH = 500;

    public interface Listener {
        void onMainDataLoaded();
        void onProfileNotFound();
    }

    Listener mListener = null;

    public void setListener(Listener l) {
        mListener = l;
    }

    private void showLoader() {
        Animation scaleIn = new ScaleAnimation(0f, 1f, 0f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleIn.setDuration(ANIMATION_LENGTH);

        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(ANIMATION_LENGTH);

        AnimationSet animation = new AnimationSet(true);
        animation.addAnimation(scaleIn);
        animation.addAnimation(fadeIn);

        loader_icon.startAnimation(animation);
    }

    private interface AnimationEndHandler {
        void onLoaderHide();
    }

    private void hideLoader(final AnimationEndHandler handler) {
        Animation scaleOut = new ScaleAnimation(1f, 0f, 1f, 0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleOut.setDuration(ANIMATION_LENGTH);

        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setDuration(ANIMATION_LENGTH);

        AnimationSet animation = new AnimationSet(true);
        animation.addAnimation(scaleOut);
        animation.addAnimation(fadeOut);
        loader_icon.startAnimation(animation);

        animation.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation arg0) {
            }
            @Override
            public void onAnimationRepeat(Animation arg0) {
            }
            @Override
            public void onAnimationEnd(Animation arg0) {
                handler.onLoaderHide();
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_loader, container, false);
        loader_icon = (ImageView) v.findViewById(R.id.loader_icon);

        showLoader();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null)
            FirebaseService.getInstance().getProfile(user.getUid(), this);
        else
            FirebaseService.getInstance().logInToFirebase(this);

        return v;
    }

    @Override
    public void onAuthenticateSuccess(final String userId) {
        FirebaseService.getInstance().getProfile(userId, this);
    }

    @Override
    public void onGetProfileSuccess(Profile profile) {
        if(profile == null) {
            mListener.onProfileNotFound();
        } else {
            long now = System.currentTimeMillis();

            profile.lastLoginTime = now;
            if(profile.creationTime == 0)
                profile.creationTime = now;

            if(AppRes.getIsLoggedInWithFacebook() && StringUtil.isEmptyString(profile.facebookId)) {
                profile.facebookId = AccessToken.getCurrentAccessToken().getUserId();
                FirebaseService.getInstance().setUser(profile.asUser());
            }

            if(profile.shareResource == null || (profile.shareResource.endTime != null && profile.shareResource.endTime < now)) {
                appRes.setShareResourceForService(null);
            } else {
                appRes.setShareResourceForService(profile.shareResource);
            }

            FirebaseService.getInstance().setProfile(profile);
            appRes.setProfile(profile);

            /** GET MAIN DATA */
            FirebaseService.getInstance().setUserId(appRes.getProfile().userId);
            FirebaseService.getInstance().getFriends(LoaderFragment.this);
        }
    }

    @Override
    public void onGetFriendsSuccess(ArrayList<User> friends) {
        appRes.setFriends(friends);
        ArrayList<String> userIds = new ArrayList<>();
        Profile me = appRes.getProfile();
        if(me.isImageUploaded)
            userIds.add(me.userId);

        for(User friend : friends) {
            if(friend.isImageUploaded)
                userIds.add(friend.userId);
        }

        FirebaseService.getInstance().downloadProfilePictures(userIds, this);
    }

    @Override
    public void onProfilePicturesLoaded(Map<String, Bitmap> profilePictures) {
        appRes.setProfilePictures(profilePictures);
        FirebaseService.getInstance().getSharedLocations(this);
    }

    @Override
    public void onGetSharedLocationsSuccess(ArrayList<LocationShare> locationShares) {
        appRes.setSharedLocations(locationShares);
        FirebaseService.getInstance().getUsers(this);
    }

    @Override
    public void onGetUsersSuccess(ArrayList<User> users) {
        appRes.setUsers(users);
        FirebaseService.getInstance().getFriendRequests(this);
    }

    @Override
    public void onGetFriendRequestsSuccess(ArrayList<User> friendRequests) {
        appRes.setFriendRequests(friendRequests);

        if(AppRes.getIsLoggedInWithFacebook()) {
            FacebookService.getInstance().getFriends(this);
        } else {
            appRes.setFriendsFacebookIds(new ArrayList<String>());
            hideLoader(new AnimationEndHandler() {
                @Override
                public void onLoaderHide() {
                    mListener.onMainDataLoaded();
                }
            });
        }
    }

    @Override
    public void onGetFacebookFriendsSuccess(ArrayList<String> friendsFacebookIds) {
        appRes.setFriendsFacebookIds(friendsFacebookIds);
        hideLoader(new AnimationEndHandler() {
            @Override
            public void onLoaderHide() {
                mListener.onMainDataLoaded();
            }
        });
    }
}
