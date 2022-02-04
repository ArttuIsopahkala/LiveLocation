package com.ardeapps.livelocation.fragments;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ardeapps.livelocation.AppRes;
import com.ardeapps.livelocation.ImageUtil;
import com.ardeapps.livelocation.Logger;
import com.ardeapps.livelocation.R;
import com.ardeapps.livelocation.StringUtil;
import com.ardeapps.livelocation.handlers.facebook.GetFacebookFriendsHandler;
import com.ardeapps.livelocation.handlers.firebase.DeleteProfilePictureHandler;
import com.ardeapps.livelocation.handlers.firebase.DownloadProfilePictureHandler;
import com.ardeapps.livelocation.handlers.firebase.UploadProfilePictureHandler;
import com.ardeapps.livelocation.objects.Profile;
import com.ardeapps.livelocation.objects.User;
import com.ardeapps.livelocation.services.AppInviteService;
import com.ardeapps.livelocation.services.FacebookService;
import com.ardeapps.livelocation.services.FirebaseService;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import java.util.ArrayList;
import java.util.Arrays;

import static com.ardeapps.livelocation.R.id.container;
import static com.facebook.FacebookSdk.getApplicationContext;

public class ProfileFragment extends Fragment implements UploadProfilePictureHandler, DownloadProfilePictureHandler, DeleteProfilePictureHandler {

    public interface Listener {
        void onMarkerMustUpdate();
    }

    Listener mListener = null;

    public void setListener(Listener l) {
        mListener = l;
    }

    CallbackManager callbackManager;
    ImageView profile_picture;
    EditText first_name_text;
    EditText last_name_text;
    TextView full_name_text;
    TextView facebook_info;
    LoginButton fb_login_button;
    RelativeLayout profile_picture_container;
    RelativeLayout edit_name_container;
    AppRes appRes = (AppRes) getApplicationContext();
    Context context = AppRes.getContext();
    Profile profile;
    Bitmap picture;
    Button save_button;
    Button rate_button;
    boolean imageChanged = false;

    public void refreshData() {
        profile = appRes.getProfile();
        picture = appRes.getProfilePictures().get(profile.userId);
    }

    public void update() {
        if(!profile.firstName.isEmpty() && !profile.lastName.isEmpty()) {
            edit_name_container.setVisibility(View.GONE);
            full_name_text.setVisibility(View.VISIBLE);
            full_name_text.setText(StringUtil.getFullName(profile));
        } else {
            edit_name_container.setVisibility(View.VISIBLE);
            full_name_text.setVisibility(View.GONE);
            first_name_text.setText(profile.firstName);
            last_name_text.setText(profile.lastName);
        }

        if(picture != null) {
            profile_picture.setImageDrawable(ImageUtil.getRoundedDrawable(picture));
        } else {
            profile_picture.setImageResource(R.drawable.default_profile_picture);
        }
        if(imageChanged) {
            ImageUtil.fadeImageIn(profile_picture);
            imageChanged = false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);
        profile_picture = (ImageView) v.findViewById(R.id.profile_picture);
        first_name_text = (EditText) v.findViewById(R.id.first_name_text);
        last_name_text = (EditText) v.findViewById(R.id.last_name_text);
        full_name_text = (TextView) v.findViewById(R.id.full_name_text);
        profile_picture_container = (RelativeLayout) v.findViewById(R.id.profile_picture_container);
        edit_name_container = (RelativeLayout) v.findViewById(R.id.edit_name_container);
        save_button = (Button) v.findViewById(R.id.save_button);
        facebook_info = (TextView) v.findViewById(R.id.facebook_info);
        fb_login_button = (LoginButton) v.findViewById(R.id.fb_login_button);
        rate_button = (Button) v.findViewById(R.id.rate_button);

        callbackManager = CallbackManager.Factory.create();
        update();

        profile_picture_container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final SelectPictureDialogFragment dialog = new SelectPictureDialogFragment();
                dialog.show(getActivity().getSupportFragmentManager(), "Vaihda profiilikuva");
                dialog.setListener(new SelectPictureDialogFragment.SelectPictureDialogCloseListener() {
                    @Override
                    public void onPictureSelected(Bitmap selectedPicture) {
                        if(selectedPicture != null) {
                            FirebaseService.getInstance().uploadProfilePicture(profile.userId, selectedPicture, ProfileFragment.this);
                            dialog.dismiss();
                        } else {
                            Logger.toast(getString(R.string.profile_picture_error));
                        }
                    }
                    @Override
                    public void onRemovePicture() {
                        dialog.dismiss();
                        if(picture != null)
                            FirebaseService.getInstance().deleteProfilePicture(profile.userId, ProfileFragment.this);
                    }

                    @Override
                    public void onCancelClick() {
                        dialog.dismiss();
                    }
                });
            }
        });

        full_name_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditProfileDialogFragment dialog = new EditProfileDialogFragment();
                dialog.refreshData(profile.firstName, profile.lastName, null, false);
                dialog.show(getActivity().getSupportFragmentManager(), "Muokkaa nimeä");
                dialog.setListener(new EditProfileDialogFragment.EditProfileDialogCloseListener() {
                    @Override
                    public void onEditProfileDialogSave(String firstName, String lastName, Bitmap profilePicture) {
                        saveProfile(firstName, lastName);
                        update();
                    }
                    @Override
                    public void onEditProfileDialogCancel() {}
                });
            }
        });

        save_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String firstName = first_name_text.getText().toString();
                String lastName = last_name_text.getText().toString();
                if(!StringUtil.isEmptyString(firstName) || !StringUtil.isEmptyString(lastName)) {
                    AppRes.hideKeyBoard(last_name_text);
                    saveProfile(firstName, lastName);
                    Logger.toast(getString(R.string.profile_saved));
                }
                update();
            }
        });

        fb_login_button.setFragment(this);
        fb_login_button.setReadPermissions(Arrays.asList("user_friends"));

        fb_login_button.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                if(StringUtil.isEmptyString(profile.facebookId)) {
                    profile.facebookId = AccessToken.getCurrentAccessToken().getUserId();
                    appRes.setProfile(profile);

                    FirebaseService.getInstance().setProfile(profile);
                    FirebaseService.getInstance().setUser(profile.asUser());
                }

                FacebookService.getInstance().getFriends(new GetFacebookFriendsHandler() {
                    @Override
                    public void onGetFacebookFriendsSuccess(ArrayList<String> friendsFacebookIds) {
                        CommonDialog dialog = new CommonDialog();
                        if(!friendsFacebookIds.isEmpty()) {
                            appRes.setFriendsFacebookIds(friendsFacebookIds);
                            dialog.refreshData(null, getString(R.string.profile_friends_using_app), CommonDialog.CommonDialogType.OK_DIALOG);
                        } else {
                            dialog.refreshData(null, getString(R.string.profile_no_facebook_friends), CommonDialog.CommonDialogType.YES_NO_DIALOG);
                            dialog.setListener(new CommonDialog.Listener() {
                                @Override
                                public void onDialogYesClick() {
                                    AppInviteService.openChooser();
                                }
                            });
                        }
                        dialog.show(getFragmentManager(), "facebook kaverisi");
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
        rate_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("http://play.google.com/store/apps/details?id=com.ardeapps.livelocation"));
                startActivity(i);
            }
        });
        return v;
    }

    private void saveProfile(String firstName, String lastName) {
        profile.firstName = firstName;
        profile.lastName = lastName;

        appRes.setProfile(profile);

        FirebaseService.getInstance().setProfile(profile);
        FirebaseService.getInstance().setUser(profile.asUser());

        // Update marker because default picture and name has changed
        if(picture == null)
            mListener.onMarkerMustUpdate();
    }

    @Override
    public void onDeletePictureSuccess() {
        profile.isImageUploaded = false;
        FirebaseService.getInstance().setProfile(profile);
        FirebaseService.getInstance().setUser(profile.asUser());
        appRes.setProfile(profile);

        appRes.getProfilePictures().put(profile.userId, null);
        picture = null;
        mListener.onMarkerMustUpdate();
        imageChanged = true;
        update();
    }

    @Override
    public void onUploadImageSuccess() {
        profile.isImageUploaded = true;
        FirebaseService.getInstance().setProfile(profile);
        FirebaseService.getInstance().setUser(profile.asUser());
        appRes.setProfile(profile);

        FirebaseService.getInstance().downloadProfilePicture(profile.userId, this);
    }

    @Override
    public void onDownloadProfilePictureSuccess(Bitmap bitmap) {
        if(bitmap != null) {
            appRes.getProfilePictures().put(profile.userId, bitmap);
            picture = bitmap;
            mListener.onMarkerMustUpdate();
            imageChanged = true;
            update();
        } else {
            Log.e("asd", "picture download is NULL!!!");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
