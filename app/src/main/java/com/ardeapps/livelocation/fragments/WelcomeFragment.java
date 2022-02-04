package com.ardeapps.livelocation.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import com.ardeapps.livelocation.AppRes;
import com.ardeapps.livelocation.R;
import com.ardeapps.livelocation.handlers.facebook.GetProfileInfoHandler;
import com.ardeapps.livelocation.handlers.facebook.GetProfilePictureHandler;
import com.ardeapps.livelocation.handlers.firebase.AuthenticateHandler;
import com.ardeapps.livelocation.handlers.firebase.UploadProfilePictureHandler;
import com.ardeapps.livelocation.objects.LiveLatLng;
import com.ardeapps.livelocation.objects.Profile;
import com.ardeapps.livelocation.services.FacebookService;
import com.ardeapps.livelocation.services.FirebaseService;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import java.util.Arrays;

public class WelcomeFragment extends Fragment implements EditProfileDialogFragment.EditProfileDialogCloseListener, UploadProfilePictureHandler {

    public interface Listener {
        void onProfileCreated();
    }

    Listener mListener = null;

    public void setListener(Listener l) {
        mListener = l;
    }

    EditProfileDialogFragment dialog;
    LoginButton fb_login_button;
    CallbackManager callbackManager;
    Button anonymous_login;
    Profile profileToSave;
    AppRes appRes = (AppRes) AppRes.getContext();
    boolean loginWithFacebook;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_welcome, container, false);

        callbackManager = CallbackManager.Factory.create();

        fb_login_button = (LoginButton) v.findViewById(R.id.fb_login_button);
        anonymous_login = (Button) v.findViewById(R.id.anonymous_login);

        fb_login_button.setFragment(this);
        fb_login_button.setReadPermissions(Arrays.asList("user_friends"));

        fb_login_button.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                loginWithFacebook = true;
                FacebookService.getInstance().getProfileInfo(new GetProfileInfoHandler() {
                    @Override
                    public void onGetProfileInfoSuccess(final String firstName, final String lastName) {
                        FacebookService.getInstance().getProfilePicture(AccessToken.getCurrentAccessToken().getUserId(), new GetProfilePictureHandler() {
                            @Override
                            public void onGetProfilePictureSuccess(String url) {
                                dialog = new EditProfileDialogFragment();
                                dialog.refreshData(firstName, lastName, url, true);
                                dialog.show(getActivity().getSupportFragmentManager(), "Muokkaa profiilia");
                                dialog.setListener(WelcomeFragment.this);
                            }
                        });
                    }
                });
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException e) {
            }
        });

        anonymous_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginWithFacebook = false;
                dialog = new EditProfileDialogFragment();
                dialog.refreshData("", "", null, true);
                dialog.show(getActivity().getSupportFragmentManager(), "Luo profiili");
                dialog.setListener(WelcomeFragment.this);
            }
        });
        return v;
    }

    @Override
    public void onEditProfileDialogSave(final String firstName, final String lastName, final Bitmap profilePicture) {
        FirebaseService.getInstance().authenticate(loginWithFacebook, new AuthenticateHandler() {
            @Override
            public void onAuthenticateSuccess(String userId) {
                profileToSave = new Profile();
                profileToSave.userId = userId;
                profileToSave.firstName = firstName;
                profileToSave.lastName = lastName;
                if(profilePicture != null) {
                    FirebaseService.getInstance().uploadProfilePicture(profileToSave.userId, profilePicture, WelcomeFragment.this);
                } else {
                    profileToSave.isImageUploaded = false;
                    setProfile(profileToSave);
                }
            }
        });
    }

    @Override
    public void onEditProfileDialogCancel() {
        if(loginWithFacebook)
            LoginManager.getInstance().logOut();
    }

    @Override
    public void onUploadImageSuccess() {
        profileToSave.isImageUploaded = true;
        setProfile(profileToSave);
    }

    private void setProfile(Profile profileToSave) {
        profileToSave.creationTime = System.currentTimeMillis();
        profileToSave.facebookId = AppRes.getIsLoggedInWithFacebook() ? AccessToken.getCurrentAccessToken().getUserId() : "";

        FirebaseService.getInstance().setProfile(profileToSave);
        FirebaseService.getInstance().setUser(profileToSave.asUser());
        appRes.setProfile(profileToSave);

        SharedPreferences.Editor editor;
        SharedPreferences pref;

        pref = AppRes.getContext().getSharedPreferences("profile", 0);
        editor = pref.edit();
        editor.putString("userId", profileToSave.userId);
        editor.apply();

        mListener.onProfileCreated();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
