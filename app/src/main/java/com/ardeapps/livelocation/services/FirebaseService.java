package com.ardeapps.livelocation.services;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.ardeapps.livelocation.AppRes;
import com.ardeapps.livelocation.ImageUtil;
import com.ardeapps.livelocation.Logger;
import com.ardeapps.livelocation.R;
import com.ardeapps.livelocation.handlers.firebase.ProfilePicturesLoadedHandler;
import com.ardeapps.livelocation.handlers.firebase.AuthenticateHandler;
import com.ardeapps.livelocation.handlers.firebase.DeleteProfilePictureHandler;
import com.ardeapps.livelocation.handlers.firebase.DownloadProfilePictureHandler;
import com.ardeapps.livelocation.handlers.firebase.GetFriendRequestsHandler;
import com.ardeapps.livelocation.handlers.firebase.GetFriendsHandler;
import com.ardeapps.livelocation.handlers.firebase.GetProfileHandler;
import com.ardeapps.livelocation.handlers.firebase.GetSharedLocationsHandler;
import com.ardeapps.livelocation.handlers.firebase.GetUsersHandler;
import com.ardeapps.livelocation.handlers.firebase.SharedLocationChangeHandler;
import com.ardeapps.livelocation.handlers.firebase.UploadProfilePictureHandler;
import com.ardeapps.livelocation.objects.LiveLatLng;
import com.ardeapps.livelocation.objects.LocationShare;
import com.ardeapps.livelocation.objects.Notification;
import com.ardeapps.livelocation.objects.Profile;
import com.ardeapps.livelocation.objects.User;
import com.facebook.AccessToken;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.ardeapps.livelocation.objects.LocationShare.ShareType.ONCE;
import static com.facebook.FacebookSdk.getApplicationContext;

public class FirebaseService {

    public final static String USERS = "users";
    public final static String PROFILES = "profiles";
    public final static String FRIENDS = "friends";
    public final static String FRIEND_LISTS = "friendLists";
    public final static String SHARED_LOCATIONS = "sharedLocations";
    public final static String SHARING_END_TIME = "sharingEndTime";
    public final static String LOCATION = "location";
    public final static String FRIEND_REQUESTS = "friendRequests";
    public final static String NOTIFICATIONS = "notifications";

    private String TAG = "FirebaseService";
    private DatabaseReference database;
    private StorageReference storage;
    private String userId;
    private ProgressDialog progress;
    SharedPreferences appPref;
    SharedPreferences.Editor editor;
    AppRes appRes = (AppRes) getApplicationContext();

    private static FirebaseService instance;

    public static FirebaseService getInstance() {
        if(instance == null) {
            instance = new FirebaseService();
        }
        return instance;
    }

    private FirebaseService() {
        storage = FirebaseStorage.getInstance().getReference();
        database = FirebaseDatabase.getInstance().getReference();
        appPref = AppRes.getContext().getSharedPreferences("app", 0);
        editor = appPref.edit();
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    private void showLoader(String message) {
        progress = new ProgressDialog(appRes.getActivity(), R.style.LoadingDialog);
        progress.setMessage(message);
        progress.setCancelable(false);
        progress.show();
    }

    private void hideLoader() {
        progress.dismiss();
    }

    private void onNetworkError() {
        Logger.toast(R.string.error_network);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) AppRes.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public void setProfile(Profile profile) {
        Log.i(TAG, "setProfile");
        database.child(PROFILES).child(profile.userId).setValue(profile);
    }

    public void setUser(User me) {
        Log.i(TAG, "setUser");
        database.child(USERS).child(me.userId).setValue(me);
    }

    public void logInToFirebase(final AuthenticateHandler handler) {
        final FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signInWithCustomToken(appPref.getString("token", "")).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    final FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        handler.onAuthenticateSuccess(user.getUid());
                    } else
                        Logger.toast("No user ID");
                } else {
                    Logger.toast("Authentication error");
                }
            }
        });
    }

    public void authenticate(boolean withFacebook, final AuthenticateHandler handler) {
        showLoader(AppRes.getContext().getString(R.string.loading_user_create));
        final FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (withFacebook) {
            // Sign in with facebook
            AuthCredential credential = FacebookAuthProvider.getCredential(AccessToken.getCurrentAccessToken().getToken());
            mAuth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        final FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                                @Override
                                public void onComplete(@NonNull Task<GetTokenResult> task) {
                                    hideLoader();
                                    if (task.isSuccessful()) {
                                        String token = task.getResult().getToken();
                                        editor = appPref.edit();
                                        editor.putString("token", token);
                                        editor.apply();

                                        handler.onAuthenticateSuccess(user.getUid());
                                    } else {
                                        Logger.toast("Token not got");
                                    }
                                }
                            });
                        } else
                            Logger.toast("No user ID");
                    } else {
                        Logger.toast("Authentication error");
                    }
                }
            });
        } else {
            // Sing in anonymous first to get userId
            mAuth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        final FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            user.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                                @Override
                                public void onComplete(@NonNull Task<GetTokenResult> task) {
                                    if (task.isSuccessful()) {
                                        String token = task.getResult().getToken();
                                        editor = appPref.edit();
                                        editor.putString("token", token);
                                        editor.apply();

                                        String email = user.getUid() + "@livelocation.com";
                                        String password = user.getUid();
                                        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
                                        user.linkWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<AuthResult> task) {
                                                hideLoader();
                                                if (task.isSuccessful()) {
                                                    handler.onAuthenticateSuccess(user.getUid());
                                                } else {
                                                    Logger.toast("Authentication error");
                                                }
                                            }
                                        });
                                    } else {
                                        Logger.toast("Token not got");
                                    }
                                }
                            });
                        } else
                            Logger.toast("No user ID");
                    } else {
                        Logger.toast("Authentication error");
                    }
                }
            });
        }
    }

    public void getProfile(String userId, final GetProfileHandler handler) {
        Log.i(TAG, "getProfile");
        if(isNetworkAvailable()) {
            // Read from the database
            database.child(PROFILES).child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists())
                        handler.onGetProfileSuccess(dataSnapshot.getValue(Profile.class));
                    else
                        handler.onGetProfileSuccess(null);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        } else onNetworkError();
    }

    public void getUsers(final GetUsersHandler handler) {
        Log.i(TAG, "getUsers");
        if(isNetworkAvailable()) {
            database.child(USERS).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    ArrayList<User> users = new ArrayList<>();
                    for(DataSnapshot object : dataSnapshot.getChildren()) {
                        User user = object.getValue(User.class);
                        users.add(user);
                    }
                    handler.onGetUsersSuccess(users);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        } else onNetworkError();
    }

    public void addMeToFriendList(final User friend) {
        Log.i(TAG, "addMeToFriendList");
        database.child(PROFILES).child(friend.userId).child(FRIENDS).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> friends = new ArrayList<>();
                for(DataSnapshot object : dataSnapshot.getChildren()) {
                    String friendId = object.getValue(String.class);
                    friends.add(friendId);
                }
                friends.add(userId);
                database.child(PROFILES).child(friend.userId).child(FRIENDS).setValue(friends);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    public void removeMeFromFriendList(final User friend) {
        Log.i(TAG, "removeMeFromFriendList");
        database.child(PROFILES).child(friend.userId).child(FRIENDS).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> friends = new ArrayList<>();
                for(DataSnapshot object : dataSnapshot.getChildren()) {
                    String friendId = object.getValue(String.class);
                    friends.add(friendId);
                }
                Iterator<String> itr = friends.iterator();
                while(itr.hasNext())  {
                    String id = itr.next();
                    if(userId.equals(id)) {
                        itr.remove();
                        break;
                    }
                }
                database.child(PROFILES).child(friend.userId).child(FRIENDS).setValue(friends);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    public void removeFriendRequest(final User request) {
        Log.i(TAG, "removeFriendRequest");
        database.child(FRIEND_REQUESTS).child(userId).child(request.userId).setValue(null);
        database.child(PROFILES).child(request.userId).child(FRIENDS).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Poistetaan kaveripyyntö kysyjän profiilista
                for(DataSnapshot object : dataSnapshot.getChildren()) {
                    String friendId = object.getValue(String.class);
                    if(friendId != null && friendId.equals(userId)) {
                        database.child(PROFILES).child(request.userId).child(FRIENDS).child(object.getKey()).setValue(null);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    public void sendNotification(Notification.NotificationType type, String recipientId) {
        Log.i(TAG, "sendNotification");
        Notification notification = new Notification();
        notification.notificationId = database.child(NOTIFICATIONS).child(recipientId).push().getKey();
        notification.type = type;
        notification.sender = appRes.getProfile().asUser();
        notification.sendTime = System.currentTimeMillis();
        database.child(NOTIFICATIONS).child(recipientId).child(notification.notificationId).setValue(notification);
    }

    public void removeSentFriendRequest(User recipient) {
        Log.i(TAG, "removeSentFriendRequest");
        database.child(FRIEND_REQUESTS).child(recipient.userId).child(userId).setValue(null);
    }

    public void sendFriendRequest(User recipient) {
        Log.i(TAG, "sendFriendRequest");
        database.child(FRIEND_REQUESTS).child(recipient.userId).child(userId).setValue(appRes.getProfile().asUser());
    }

    public void getFriendRequests(final GetFriendRequestsHandler handler) {
        Log.i(TAG, "getFriendRequests");
        if(isNetworkAvailable()) {
            database.child(FRIEND_REQUESTS).child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    ArrayList<User> friendRequests = new ArrayList<>();
                    for(DataSnapshot object : dataSnapshot.getChildren()) {
                        User user = object.getValue(User.class);
                        friendRequests.add(user);
                    }

                    handler.onGetFriendRequestsSuccess(friendRequests);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        } else onNetworkError();
    }

    public void getFriends(final GetFriendsHandler handler) {
        Log.i(TAG, "getFriends");
        if(isNetworkAvailable()) {
            final ArrayList<User> friends = new ArrayList<>();
            final List<String> friendIds = appRes.getProfile().friends;
            if(friendIds != null && friendIds.size() > 0) {
                for (String friendId : friendIds) {
                    database.child(USERS).child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            User friend = dataSnapshot.getValue(User.class);
                            friends.add(friend);

                            if (friends.size() == friendIds.size()) {
                                handler.onGetFriendsSuccess(friends);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
                }
            } else {
                handler.onGetFriendsSuccess(friends);
            }
        } else onNetworkError();
    }

    public void setSharedLocation(List<String> selectedFriendIds, LocationShare locationShare) {
        Log.i(TAG, "setSharedLocation");
        for(String friendId : selectedFriendIds) {
            database.child(SHARED_LOCATIONS).child(friendId).child(userId).setValue(locationShare);
        }
    }

    public void updateSharedLocation(List<String> friendIds, LiveLatLng location) {
        Log.i(TAG, "updateSharedLocation");
        for(String friendId : friendIds) {
            if(friendId != null && userId != null)
               database.child(SHARED_LOCATIONS).child(friendId).child(userId).child(LOCATION).setValue(location);
        }
    }

    public void removeSharedLocation(List<String> friendIds) {
        Log.i(TAG, "removeSharedLocation");
        for(String friendId : friendIds) {
            if(friendId != null && userId != null)
                database.child(SHARED_LOCATIONS).child(friendId).child(userId).setValue(null);
        }
    }

    public void removeSharedOnceLocation(String friendId) {
        Log.i(TAG, "removeSharedLocation");
        database.child(SHARED_LOCATIONS).child(userId).child(friendId).setValue(null);
    }

    public void getSharedLocations(final GetSharedLocationsHandler handler) {
        Log.i(TAG, "getSharedLocations");
        if(isNetworkAvailable()) {
            database.child(SHARED_LOCATIONS).child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    ArrayList<LocationShare> locationShares = new ArrayList<>();
                    for(DataSnapshot object : dataSnapshot.getChildren()) {
                        LocationShare locationShare = object.getValue(LocationShare.class);
                        if (locationShare != null) {
                            if (locationShare.shareType == ONCE || locationShare.endTime > System.currentTimeMillis()) {
                                locationShares.add(locationShare);
                            } else {
                                database.child(SHARED_LOCATIONS).child(userId).child(locationShare.userId).setValue(null);
                            }
                        }
                    }
                    handler.onGetSharedLocationsSuccess(locationShares);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        } else onNetworkError();
    }

    public void setSharedLocationsListener(final SharedLocationChangeHandler handler) {
        Log.i(TAG, "setSharedLocationsListener");
        database.child(SHARED_LOCATIONS).child(userId).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                // Get userIds of already shared locations
                ArrayList<String> friendIds = new ArrayList<>();
                for(LocationShare sharedLocations : appRes.getSharedLocations()) {
                    friendIds.add(sharedLocations.userId);
                }
                // Check if there is new shared location
                LocationShare locationShare = dataSnapshot.getValue(LocationShare.class);
                if(locationShare != null && !friendIds.contains(locationShare.userId)) {
                    handler.onSharedLocationAdded(locationShare);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                LocationShare locationShare = dataSnapshot.getValue(LocationShare.class);
                // Käsitellään myös se kun poistettu locationShare
                if (locationShare != null && locationShare.location != null && locationShare.endTime > System.currentTimeMillis())
                    handler.onSharedLocationChange(locationShare);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    public void deleteProfilePicture(String userId, final DeleteProfilePictureHandler handler) {
        storage.child("images/" + userId).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    handler.onDeletePictureSuccess();
                } else {
                    Logger.toast(AppRes.getContext().getString(R.string.profile_picture_remove_error));
                }
            }
        });
    }

    public void uploadProfilePicture(String userId, Bitmap bitmap, final UploadProfilePictureHandler handler) {
        Log.i(TAG, "uploadImage");
        showLoader(AppRes.getContext().getString(R.string.loading_picture));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = storage.child("images/" + userId).putBytes(data);
        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                hideLoader();
                if(task.isSuccessful()) {
                    handler.onUploadImageSuccess();
                } else {
                    Logger.toast(AppRes.getContext().getString(R.string.profile_picture_upload_error));
                }
            }
        });
    }

    public void downloadProfilePicture(String userId, final DownloadProfilePictureHandler handler) {
        Log.i(TAG, "downloadProfilePicture");
        final long SIZE480PX = 480 * 480;
        final long ONE_MEGABYTE = 1024 * 1024;
        storage.child("images/" + userId).getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                handler.onDownloadProfilePictureSuccess(bitmap);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Palauta default kuva
                handler.onDownloadProfilePictureSuccess(ImageUtil.getDrawableAsBitmap(ContextCompat.getDrawable(AppRes.getContext(), R.drawable.default_profile_picture)));
            }
        });
    }

    public void downloadProfilePictures(final List<String> userIds, final ProfilePicturesLoadedHandler handler) {
        Log.i(TAG, "downloadProfilePictures");
        final long SIZE480PX = 480 * 480;
        final long ONE_MEGABYTE = 1024 * 1024;
        final Map<String, Bitmap> profilePictures = new HashMap<>();
        if(userIds.size() > 0) {
            for (final String userId : userIds) {
                storage.child("images/" + userId).getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        profilePictures.put(userId, bitmap);
                        if (profilePictures.size() == userIds.size()) {
                            handler.onProfilePicturesLoaded(profilePictures);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        profilePictures.put(userId, ImageUtil.getDrawableAsBitmap(ContextCompat.getDrawable(AppRes.getContext(), R.drawable.default_profile_picture)));
                        if (profilePictures.size() == userIds.size()) {
                            handler.onProfilePicturesLoaded(profilePictures);
                        }
                    }
                });
            }
        } else {
            handler.onProfilePicturesLoaded(profilePictures);
        }
    }
}
