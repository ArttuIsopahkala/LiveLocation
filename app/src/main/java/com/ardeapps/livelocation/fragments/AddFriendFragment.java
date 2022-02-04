package com.ardeapps.livelocation.fragments;


import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.ardeapps.livelocation.AppRes;
import com.ardeapps.livelocation.ImageUtil;
import com.ardeapps.livelocation.Logger;
import com.ardeapps.livelocation.R;
import com.ardeapps.livelocation.StringUtil;
import com.ardeapps.livelocation.handlers.firebase.DownloadProfilePictureHandler;
import com.ardeapps.livelocation.objects.User;
import com.ardeapps.livelocation.services.FirebaseService;
import com.ardeapps.livelocation.services.FragmentListeners;

import static com.facebook.FacebookSdk.getApplicationContext;

public class AddFriendFragment extends Fragment {

    public interface Listener {
        void onSendFriendRequest(User recipient);
        void onRemoveSentFriendRequest(User recipient);
    }

    Listener mListener = null;

    public void setListener(Listener l) {
        mListener = l;
    }

    ImageView profile_picture;
    TextView name_text;
    Button add_friend_button;
    TextView menu_title;
    User friend;
    AppRes appRes = (AppRes) getApplicationContext();
    boolean friendRequestSent;

    private void refreshData() {
        // Lähetä kaveripyyntö <-> Poista kaveripyyntö
        friendRequestSent = appRes.getProfile().sentFriendRequests.contains(friend.userId);
        add_friend_button.setText(friendRequestSent ? R.string.friend_remove_sent_request : R.string.friend_send_request);
    }

    public void setFriend(User friend) {
        this.friend = friend;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentListeners.getInstance().setAddFriendListener(new FragmentListeners.AddFriendListener() {
            @Override
            public void update() {
                refreshData();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_add_friend, container, false);
        profile_picture = (ImageView) v.findViewById(R.id.profile_picture);
        name_text = (TextView) v.findViewById(R.id.name_text);
        add_friend_button = (Button) v.findViewById(R.id.add_friend_button);
        menu_title = (TextView) v.findViewById(R.id.menu_title);

        menu_title.setText(R.string.search_friends_title);
        refreshData();

        if(friend.isImageUploaded) {
            FirebaseService.getInstance().downloadProfilePicture(friend.userId, new DownloadProfilePictureHandler() {
                @Override
                public void onDownloadProfilePictureSuccess(Bitmap bitmap) {
                    ImageUtil.fadeImageIn(profile_picture);
                    profile_picture.setImageDrawable(ImageUtil.getRoundedDrawable(bitmap));
                }
            });
        } else {
            profile_picture.setImageDrawable(ContextCompat.getDrawable(AppRes.getContext(), R.drawable.default_profile_picture));
        }

        name_text.setText(StringUtil.getFullName(friend));

        add_friend_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(friendRequestSent)
                    mListener.onRemoveSentFriendRequest(friend);
                else
                    mListener.onSendFriendRequest(friend);
            }
        });

        return v;
    }


}
