package com.ardeapps.livelocation.services;

/**
 * Created by Arttu on 18.6.2017.
 */

public class FragmentListeners {

    public final static int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    public static final int MY_PERMISSION_ACCESS_READ_EXTERNAL_STORAGE = 12;
    public static final int MY_PERMISSION_ACCESS_CAMERA = 13;
    public static final int MY_PERMISSION_ACCESS_WRITE_EXTERNAL_STORAGE = 14;
    public static final int MY_PERMISSION_ACCESS_TAKING_PICTURE = 15;

    private static FragmentListeners instance;

    public static FragmentListeners getInstance() {
        if(instance == null) {
            instance = new FragmentListeners();
        }
        return instance;
    }

    public interface PermissionHandledListener {
        void onPermissionGranted(int MY_PERMISSION);
    }

    private PermissionHandledListener permissionHandledListener;

    public PermissionHandledListener getPermissionHandledListener() {
        return permissionHandledListener;
    }

    public void setPermissionHandledListener(PermissionHandledListener permissionHandledListener) {
        this.permissionHandledListener = permissionHandledListener;
    }

    public interface SearchFriendsListener {
        void update();
    }

    private SearchFriendsListener searchFriendsListener;

    public SearchFriendsListener getSearchFriendsListener() {
        return searchFriendsListener;
    }

    public void setSearchFriendsListener(SearchFriendsListener searchFriendsListener) {
        this.searchFriendsListener = searchFriendsListener;
    }

    public interface AddFriendListener {
        void update();
    }

    private AddFriendListener addFriendListener;

    public AddFriendListener getAddFriendListener() {
        return addFriendListener;
    }

    public void setAddFriendListener(AddFriendListener addFriendListener) {
        this.addFriendListener = addFriendListener;
    }
}
