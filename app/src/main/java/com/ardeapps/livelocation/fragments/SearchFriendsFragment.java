package com.ardeapps.livelocation.fragments;


import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.ardeapps.livelocation.AppRes;
import com.ardeapps.livelocation.R;
import com.ardeapps.livelocation.StringUtil;
import com.ardeapps.livelocation.adapters.UserListAdapter;
import com.ardeapps.livelocation.objects.User;
import com.ardeapps.livelocation.services.FragmentListeners;
import com.ardeapps.livelocation.services.AppInviteService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.facebook.FacebookSdk.getApplicationContext;

public class SearchFriendsFragment extends Fragment {
    UserListAdapter userAdapter;
    ListView users_list;
    EditText search_text;
    TextView facebook_friends_info;
    Button invite_friends;
    TextView menu_title;

    ArrayList<User> users = new ArrayList<>();
    ArrayList<String> friendsFacebookIds = new ArrayList<>();
    ArrayList<User> friends = new ArrayList<>();
    AppRes appRes = (AppRes) getApplicationContext();

    private final int TRIGGER_SEARCH = 1;
    private final long SEARCH_TRIGGER_DELAY_IN_MS = 700;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == TRIGGER_SEARCH) {
                setAdapter();
            }
        }
    };

    public void refreshData() {
        users = appRes.getUsers();
        friendsFacebookIds = appRes.getFriendsFacebookIds();
        friends = appRes.getFriends();

        // Poistetaan kaverit ja oma tili listalta
        Iterator<User> itr = users.iterator();
        while(itr.hasNext()) {
            User user = itr.next();
            if(user.userId.equals(appRes.getProfile().userId))
                itr.remove();
            for(User friend : friends) {
                if(user.userId.equals(friend.userId))
                    itr.remove();
            }
        }
    }

    private void setAdapter() {
        facebook_friends_info.setVisibility(View.GONE);
        String keyword = getKeyword();
        ArrayList<User> searchResult = new ArrayList<>();
        if(StringUtil.isEmptyString(keyword)) {
            if(friendsFacebookIds.size() > 0) {
                for(User user : users) {
                    if(!StringUtil.isEmptyString(user.facebookId) && friendsFacebookIds.contains(user.facebookId)) {
                        boolean facebookFriendIsAlreadyFriend = false;
                        for(User friend : friends) {
                            if(friend.userId.equals(user.facebookId))
                                facebookFriendIsAlreadyFriend = true;
                        }
                        if(!facebookFriendIsAlreadyFriend)
                            searchResult.add(user);
                    }
                }
                if(searchResult.size() > 0)
                    facebook_friends_info.setVisibility(View.VISIBLE);
            }
            userAdapter.setUsers(searchResult);
            userAdapter.notifyDataSetChanged();
        } else {
            for(User user : users) {
                if(StringUtil.getFullName(user).toLowerCase().contains(keyword))
                    searchResult.add(user);
            }
            userAdapter.setUsers(searchResult);
            userAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentListeners.getInstance().setSearchFriendsListener(new FragmentListeners.SearchFriendsListener() {
            @Override
            public void update() {
                refreshData();
                setAdapter();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_search_friends, container, false);
        users_list = (ListView) v.findViewById(R.id.users_list);
        search_text = (EditText) v.findViewById(R.id.search_text);
        facebook_friends_info = (TextView) v.findViewById(R.id.facebook_friends_info);
        invite_friends = (Button) v.findViewById(R.id.invite_friends);
        menu_title = (TextView) v.findViewById(R.id.menu_title);

        menu_title.setText(R.string.search_friends_title);

        invite_friends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AppInviteService.openChooser();
            }
        });

        search_text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                handler.removeMessages(TRIGGER_SEARCH);
                handler.sendEmptyMessageDelayed(TRIGGER_SEARCH, SEARCH_TRIGGER_DELAY_IN_MS);
            }
        });

        userAdapter = new UserListAdapter();
        users_list.setAdapter(userAdapter);

        refreshData();
        setAdapter();
        return v;
    }

    private String getKeyword() {
        return search_text.getText().toString();
    }
}
