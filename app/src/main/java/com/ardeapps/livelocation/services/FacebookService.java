package com.ardeapps.livelocation.services;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.ardeapps.livelocation.AppRes;
import com.ardeapps.livelocation.Logger;
import com.ardeapps.livelocation.R;
import com.ardeapps.livelocation.handlers.facebook.GetFacebookFriendsHandler;
import com.ardeapps.livelocation.handlers.facebook.GetProfileInfoHandler;
import com.ardeapps.livelocation.handlers.facebook.GetProfilePictureHandler;
import com.ardeapps.livelocation.objects.User;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Arttu on 18.6.2017.
 */

public class FacebookService {

    private String TAG = "FacebookService";
    public final static String PICTURE_LARGE = "large";
    public final static String PICTURE_SMALL = "small";
    public final static String PICTURE_SIZE = "480";

    private static FacebookService instance;

    public static FacebookService getInstance() {
        if(instance == null) {
            instance = new FacebookService();
        }
        return instance;
    }

    private String getNode(JSONObject object, String node) {
        String value = "";
        try {
            value = object.getString(node);
        } catch (JSONException e) {
            Log.e(TAG, "getNodeError - " + node + " not found from " + object.toString());
        }
        return value;
    }

    private JSONObject getJSONObject(JSONArray objects, int index) {
        JSONObject obj = new JSONObject();
        try {
            obj = objects.getJSONObject(index);
        } catch (JSONException e) {
            Log.e(TAG, "getJSONObjectError - index " + index + " not found from " + objects.toString());
        }
        return obj;
    }

    private JSONObject getJSONObject(JSONObject object, String node) {
        JSONObject obj = new JSONObject();
        try {
            obj = object.getJSONObject(node);
        } catch (JSONException e) {
            Log.e(TAG, "getJSONObjectError - " + node + " not found from " + object.toString());
        }
        return obj;
    }

    private JSONArray getJSONArray(JSONObject object, String node) {
        JSONArray arr = new JSONArray();
        try {
            arr = object.getJSONArray(node);
        } catch (JSONException e) {
            Log.e(TAG, "getJSONArrayError - " + node + " not found from " + object.toString());
        }
        return arr;
    }

    private void onLoadDataError(String error) {
        Log.e(TAG, error);
        Logger.toast(R.string.facebook_error);
    }

    public void getFriends(final GetFacebookFriendsHandler handler) {
        Log.i(TAG, "getFriends");
        Bundle params = new Bundle();
        //params.putString("fields", "id, first_name, last_name, email, picture.width(" + PICTURE_SIZE + ").height(" + PICTURE_SIZE + ")");
        params.putString("fields", "id");
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "me/friends",
                params,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        if(response.getError() != null) {
                            onLoadDataError(response.getError().getErrorMessage());
                        } else {
                            JSONObject data = response.getJSONObject();
                            ArrayList<String> friendsFacebookIds = new ArrayList<>();
                            JSONArray objects = getJSONArray(data, "data");

                            for (int i = 0; i < objects.length(); i++) {
                                JSONObject object = getJSONObject(objects, i);
                                friendsFacebookIds.add(getNode(object, "id"));
                            }

                            handler.onGetFacebookFriendsSuccess(friendsFacebookIds);
                        }
                    }
                }
        ).executeAsync();
    }

    public void getProfileInfo(final GetProfileInfoHandler handler) {
        Log.i(TAG, "getProfileInfo");
        GraphRequest request = GraphRequest.newMeRequest(
                AccessToken.getCurrentAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        if(response.getError() != null) {
                            onLoadDataError(response.getError().getErrorMessage());
                        } else {
                            String firstName = getNode(object, "first_name");
                            String lastName = getNode(object, "last_name");
                            handler.onGetProfileInfoSuccess(firstName, lastName);
                        }
                    }
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "first_name, last_name");

        request.setParameters(parameters);
        request.executeAsync();
    }

    public void getProfilePicture(String userId, final GetProfilePictureHandler handler) {
        Log.i(TAG, "getProfilePicture");
        Bundle params = new Bundle();
        params.putBoolean("redirect", false);
        params.putString("width", PICTURE_SIZE);
        params.putString("height", PICTURE_SIZE);
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                userId + "/picture",
                params,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        if(response.getError() != null) {
                            onLoadDataError(response.getError().getErrorMessage());
                        } else {
                            JSONObject object = response.getJSONObject();
                            JSONObject data = getJSONObject(object, "data");
                            String url = getNode(data, "url");
                            handler.onGetProfilePictureSuccess(url);
                        }
                    }
                }
        ).executeAsync();
    }
}
