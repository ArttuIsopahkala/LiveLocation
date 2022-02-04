package com.ardeapps.livelocation.fragments;


import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.ardeapps.livelocation.AppRes;
import com.ardeapps.livelocation.ImageUtil;
import com.ardeapps.livelocation.Logger;
import com.ardeapps.livelocation.R;
import com.ardeapps.livelocation.StringUtil;
import com.ardeapps.livelocation.adapters.FriendListAdapter;
import com.ardeapps.livelocation.handlers.firebase.DownloadProfilePictureHandler;
import com.ardeapps.livelocation.objects.Profile;
import com.ardeapps.livelocation.objects.User;
import com.ardeapps.livelocation.services.FirebaseService;
import com.ardeapps.livelocation.services.FragmentListeners;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;

import static com.ardeapps.livelocation.ImageUtil.dipToPixels;
import static com.ardeapps.livelocation.services.FragmentListeners.MY_PERMISSION_ACCESS_COARSE_LOCATION;
import static com.facebook.FacebookSdk.getApplicationContext;

public class FriendsFragment extends Fragment {

    public interface Listener {
        void onSearchFriendsClick();
        void onStartShareLocation(ArrayList<String> selectedFriends);
        void onAddFriendsToShare(ArrayList<String> friendsToAdd);
        void onStopShareLocation();
        void onDeclineFriendRequest(User request);
        void onAcceptFriendRequest(User request);
    }

    Listener mListener = null;

    public void setListener(Listener l) {
        mListener = l;
    }

    AdView mAdView;
    Button add_friends_button;
    Button start_share_button;
    ListView friend_list;
    LinearLayout friend_requests;
    TextView friend_requests_text;
    TextView friends_info;
    ImageView sharing_ball;

    Context context = AppRes.getContext();
    AppRes appRes = (AppRes) getApplicationContext();

    FriendListAdapter friendAdapter;
    ArrayList<User> friends;
    ArrayList<User> friendRequests;
    Profile profile;

    private class FriendRequestHolder {
        TextView request_full_name_text;
        ImageView request_profile_picture;
        Button request_decline_button;
        Button request_accept_button;
    }

    public void update() {
        friend_requests.removeAllViews();
        if(friendRequests.size() == 0) {
            friend_requests_text.setVisibility(View.GONE);
        } else {
            friend_requests_text.setVisibility(View.VISIBLE);
            int position = 0;
            for (final User request : friendRequests) {
                final FriendRequestHolder holder = new FriendRequestHolder();
                LayoutInflater inflater = LayoutInflater.from(context);
                View cv = inflater.inflate(R.layout.friend_request_list_item, friend_requests, false);
                holder.request_full_name_text = (TextView) cv.findViewById(R.id.request_full_name_text);
                holder.request_profile_picture = (ImageView) cv.findViewById(R.id.request_profile_picture);
                holder.request_decline_button = (Button) cv.findViewById(R.id.request_decline_button);
                holder.request_accept_button = (Button) cv.findViewById(R.id.request_accept_button);

                holder.request_full_name_text.setText(StringUtil.getFullName(request));
                cv.setBackgroundColor(ContextCompat.getColor(context, position % 2 == 0 ? R.color.color_main : R.color.color_main_secondary));

                if (request.isImageUploaded) {
                    FirebaseService.getInstance().downloadProfilePicture(request.userId, new DownloadProfilePictureHandler() {
                        @Override
                        public void onDownloadProfilePictureSuccess(Bitmap bitmap) {
                            ImageUtil.fadeImageIn(holder.request_profile_picture);
                            holder.request_profile_picture.setImageDrawable(ImageUtil.getRoundedDrawable(bitmap));
                        }
                    });
                }

                holder.request_decline_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mListener.onDeclineFriendRequest(request);
                    }
                });

                holder.request_accept_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mListener.onAcceptFriendRequest(request);
                    }
                });

                position++;
                friend_requests.addView(cv);
            }
        }

        friendAdapter = new FriendListAdapter();
        sharing_ball.setVisibility(profile.shareResource != null ? View.VISIBLE : View.GONE);

        if(friends.size() == 0) {
            friends_info.setText(getString(R.string.friends_no_friends));
            start_share_button.setEnabled(false);
        } else {
            friends_info.setText(getString(profile.shareResource != null ? R.string.friends_sharing : R.string.friends_select_friends));

            LayoutParams list = friend_list.getLayoutParams();
            int heightInDp = friends.size() * 70; // Yhden friend_list_item korkeus on 70dp
            list.height = ImageUtil.dipToPixels(heightInDp);
            friend_list.setLayoutParams(list);

            friendAdapter.refreshData();
            friend_list.setAdapter(friendAdapter);
        }

        setShareButton();
    }

    public void refreshData() {
        friends = appRes.getFriends();
        friendRequests = appRes.getFriendRequests();
        profile = appRes.getProfile();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentListeners.getInstance().setPermissionHandledListener(new FragmentListeners.PermissionHandledListener() {
            @Override
            public void onPermissionGranted(int MY_PERMISSION) {
                if(MY_PERMISSION == MY_PERMISSION_ACCESS_COARSE_LOCATION) {
                    mListener.onStartShareLocation(friendAdapter.getSelectedFriendIds());
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_friends, container, false);

        friend_list = (ListView) v.findViewById(R.id.friend_list);
        add_friends_button = (Button) v.findViewById(R.id.add_friends_button);
        start_share_button = (Button) v.findViewById(R.id.start_share_button);
        friends_info = (TextView) v.findViewById(R.id.friends_info);
        friend_requests_text = (TextView) v.findViewById(R.id.friend_requests_text);
        friend_requests = (LinearLayout) v.findViewById(R.id.friend_requests);
        sharing_ball = (ImageView) v.findViewById(R.id.sharing_ball);
        mAdView = (AdView) v.findViewById(R.id.adView);

        MobileAds.initialize(getApplicationContext(), getString(R.string.admob_app_id));
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        refreshData();
        update();

        add_friends_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onSearchFriendsClick();
            }
        });

        start_share_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(profile.shareResource != null) {
                    start_share_button.setEnabled(true);
                    if(friendAdapter.getSelectedFriendIds().size() > 0) {
                        mListener.onAddFriendsToShare(friendAdapter.getSelectedFriendIds());
                    } else {
                        mListener.onStopShareLocation();
                    }
                } else {
                    if(friendAdapter.getSelectedFriendIds().size() > 0) {
                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                                    MY_PERMISSION_ACCESS_COARSE_LOCATION);
                        } else {
                            mListener.onStartShareLocation(friendAdapter.getSelectedFriendIds());
                        }
                    }
                }
            }
        });

        return v;
    }

    public void setShareButton() {
        ArrayList<String> selectedFriends = friendAdapter.getSelectedFriendIds();
        if(profile.shareResource != null) {
            start_share_button.setEnabled(true);
            if(selectedFriends.size() > 0) {
                start_share_button.setText(getString(R.string.friends_share_also));
            } else {
                start_share_button.setText(getString(R.string.friends_stop_share));
            }
        } else {
            start_share_button.setText(getString(R.string.friends_start_share));
            if(selectedFriends.size() > 0) {
                start_share_button.setEnabled(true);
            } else {
                start_share_button.setEnabled(false);
            }
        }
    }
}
