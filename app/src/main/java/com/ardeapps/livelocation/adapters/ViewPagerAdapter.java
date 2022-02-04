package com.ardeapps.livelocation.adapters;

import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.ardeapps.livelocation.fragments.FriendsFragment;
import com.ardeapps.livelocation.fragments.MapFragment;
import com.ardeapps.livelocation.fragments.ProfileFragment;

import java.util.ArrayList;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    @Override
    public Parcelable saveState() {
        return null;
    }

    private FriendsFragment friendsFragment;
    private MapFragment mapFragment;
    private ProfileFragment profileFragment;

    private ArrayList<Fragment> fragments;

    public ViewPagerAdapter(FragmentManager supportFragmentManager, ArrayList<Fragment> fragments) {
        super(supportFragmentManager);
        this.fragments = fragments;
        friendsFragment = (FriendsFragment)fragments.get(0);
        mapFragment = (MapFragment)fragments.get(1);
        profileFragment = (ProfileFragment)fragments.get(2);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:
                return friendsFragment;
            case 1:
                return mapFragment;
            case 2:
                return profileFragment;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    @Override
    public int getItemPosition(Object object) {
        //don't return POSITION_NONE, avoid fragment recreation.
        return super.getItemPosition(object);
    }

    public void updateEverything() {
        updateFriendsFragment();
        updateMapFragment();
        updateProfileFragment();
    }

    public void updateFriendsFragment() {
        friendsFragment.refreshData();
        friendsFragment.update();
    }

    public void updateMapFragment() {
        mapFragment.refreshData();
        mapFragment.update();
    }

    public void updateProfileFragment() {
        profileFragment.refreshData();
        profileFragment.update();
    }
}
