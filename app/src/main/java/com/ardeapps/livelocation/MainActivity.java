package com.ardeapps.livelocation;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.ardeapps.livelocation.adapters.FriendListAdapter;
import com.ardeapps.livelocation.adapters.UserListAdapter;
import com.ardeapps.livelocation.adapters.ViewPagerAdapter;
import com.ardeapps.livelocation.fragments.AddFriendFragment;
import com.ardeapps.livelocation.fragments.CommonDialog;
import com.ardeapps.livelocation.fragments.FriendOptionsDialogFragment;
import com.ardeapps.livelocation.fragments.FriendsFragment;
import com.ardeapps.livelocation.fragments.LoaderFragment;
import com.ardeapps.livelocation.fragments.MapFragment;
import com.ardeapps.livelocation.fragments.ProfileFragment;
import com.ardeapps.livelocation.fragments.SearchFriendsFragment;
import com.ardeapps.livelocation.fragments.SelectShareTimeDialogFragment;
import com.ardeapps.livelocation.fragments.WelcomeFragment;
import com.ardeapps.livelocation.handlers.firebase.SharedLocationChangeHandler;
import com.ardeapps.livelocation.objects.LiveLatLng;
import com.ardeapps.livelocation.objects.LocationShare;
import com.ardeapps.livelocation.objects.Profile;
import com.ardeapps.livelocation.objects.ShareResource;
import com.ardeapps.livelocation.objects.User;
import com.ardeapps.livelocation.services.FirebaseService;
import com.ardeapps.livelocation.services.FragmentListeners;
import com.ardeapps.livelocation.services.LocationService;
import com.ardeapps.livelocation.services.NotificationService;
import com.ardeapps.livelocation.services.SingleLocationProvider;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static android.R.attr.filter;
import static com.ardeapps.livelocation.objects.LocationShare.ShareType.FOREVER;
import static com.ardeapps.livelocation.objects.LocationShare.ShareType.ONCE;
import static com.ardeapps.livelocation.objects.Notification.NotificationType.FRIEND_REQUEST_ACCEPTED;
import static com.ardeapps.livelocation.objects.Notification.NotificationType.FRIEND_REQUEST_SENT;
import static com.ardeapps.livelocation.objects.Notification.NotificationType.LOCATION_REQUESTED;
import static com.ardeapps.livelocation.objects.Notification.NotificationType.LOCATION_SHARED;
import static com.ardeapps.livelocation.objects.Notification.NotificationType.SHARE_LOCATION_STARTED;

public class MainActivity extends AppCompatActivity implements LocationService.Listener, SharedLocationChangeHandler {

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    TabLayout tabLayout;
    ViewPager viewPager;
    ViewPagerAdapter pagerAdapter;
    FrameLayout container;
    LoaderFragment loaderFragment;
    WelcomeFragment welcomeFragment;
    ProfileFragment profileFragment;
    FriendsFragment friendsFragment;
    MapFragment mapFragment;
    SearchFriendsFragment searchFriendsFragment;
    AddFriendFragment addFriendFragment;

    AppRes appRes;
    SharedPreferences appPref;
    SharedPreferences profilePref;
    SharedPreferences.Editor editor;
    boolean isFirstTime = true;
    boolean appStartedFirstTime = true;

    Intent notificationServiceIntent;
    Intent locationServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = (ViewPager) findViewById(R.id.viewPager);
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        container = (FrameLayout) findViewById(R.id.fragment_container);

        // Create fragments and services
        notificationServiceIntent = new Intent(this, NotificationService.class);
        locationServiceIntent = new Intent(this, LocationService.class);

        loaderFragment = new LoaderFragment();
        friendsFragment = new FriendsFragment();
        mapFragment = new MapFragment();
        welcomeFragment = new WelcomeFragment();
        profileFragment = new ProfileFragment();
        searchFriendsFragment = new SearchFriendsFragment();
        addFriendFragment = new AddFriendFragment();
        setListeners();

        // Initialize local variables
        profilePref = getSharedPreferences("profile", 0);
        appRes = (AppRes) getApplicationContext();
        appRes.setActivity(this);
        appPref = getSharedPreferences("app", 0);
        isFirstTime = appPref.getBoolean("isFirstTime", true);
        appStartedFirstTime = appPref.getBoolean("appStartedFirstTime", true);

        if(appStartedFirstTime) {
            Intent shortcutIntent = new Intent(getApplicationContext(),
                    MainActivity.class);
            shortcutIntent.setAction(Intent.ACTION_MAIN);
            Intent intent = new Intent();

            // Create Implicit intent and assign Shortcut Application Name, Icon
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, R.string.app_name);
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(
                            getApplicationContext(), R.mipmap.ic_launcher));
            intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            getApplicationContext().sendBroadcast(intent);

            editor = appPref.edit();
            editor.putBoolean("appStartedFirstTime", false);
            editor.apply();
        }

        if(!StringUtil.isEmptyString(profilePref.getString("userId", null))) {
            openLoaderFragment();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, welcomeFragment)
                    .commitAllowingStateLoss();
        }
    }

    private void openLoaderFragment() {
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, loaderFragment)
                .commitAllowingStateLoss();
        container.setVisibility(View.VISIBLE);
    }

    private boolean isGooglePlayServicesAvailable() {
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(AppRes.getContext());
        if(resultCode != ConnectionResult.SUCCESS) {
            if(GoogleApiAvailability.getInstance().isUserResolvableError(resultCode)) {
                GoogleApiAvailability.getInstance().getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Logger.toast(R.string.google_play_not_supported);
            }
            return false;
        }
        return true;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            FragmentListeners.getInstance().getPermissionHandledListener().onPermissionGranted(requestCode);
        } else {
            Logger.toast(R.string.permission_not_granted);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        appRes.setIsAppVisible(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        appRes.setIsAppVisible(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(notificationServiceIntent);
    }

    public void onBackIconPressed(View view) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        AppRes.hideKeyBoard(getWindow().getDecorView());

        //go to previous fragment or close app if user is on mainscreen
        int backStack = getSupportFragmentManager().getBackStackEntryCount();

        if (backStack > 0) {
            if(backStack == 1) {
                container.setVisibility(View.INVISIBLE);
            }
            getSupportFragmentManager().popBackStack();
        } else super.onBackPressed();
    }

    /** SET FRAGMENT LISTENERS */
    private void setListeners() {
        loaderFragment.setListener(new LoaderFragment.Listener() {
            @Override
            public void onMainDataLoaded() {
                openMainApp();
            }
            @Override
            public void onProfileNotFound() {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if(fragment != null) {
                    getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
                }
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, welcomeFragment)
                        .commitAllowingStateLoss();
            }
        });
        welcomeFragment.setListener(new WelcomeFragment.Listener() {
            @Override
            public void onProfileCreated() {
                openLoaderFragment();
            }
        });
        friendsFragment.setListener(new FriendsFragment.Listener() {
            @Override
            public void onSearchFriendsClick() {
                switchToFragment(searchFriendsFragment);
            }
            @Override
            public void onStartShareLocation(ArrayList<String> selectedFriendIds) {
                openStartSharingDialog(selectedFriendIds);
            }
            @Override
            public void onAddFriendsToShare(ArrayList<String> friendIdsToAdd) {
                addFriendsToShare(friendIdsToAdd);
            }
            @Override
            public void onStopShareLocation() {
                onStopSharing();
            }
            @Override
            public void onDeclineFriendRequest(User request) {
                removeFriendRequest(request);
            }
            @Override
            public void onAcceptFriendRequest(User request) {
                addMeToFriendList(request);
                FirebaseService.getInstance().sendNotification(FRIEND_REQUEST_ACCEPTED, request.userId);
                removeFriendRequest(request);
            }
        });
        mapFragment.setListener(new MapFragment.Listener() {
            @Override
            public void onStartShareLocation() {
                ArrayList<String> allFriendIds = new ArrayList<>();
                for(User friend : appRes.getFriends()) {
                    allFriendIds.add(friend.userId);
                }
                openStartSharingDialog(allFriendIds);
            }
            @Override
            public void onStopShareLocation() {
                onStopSharing();
            }
            @Override
            public void onRemoveOnceLocation(String friendId) {
                FirebaseService.getInstance().removeSharedOnceLocation(friendId);
                Iterator<LocationShare> itr = appRes.getSharedLocations().iterator();
                while(itr.hasNext()) {
                    LocationShare locationShare = itr.next();
                    if(locationShare.userId.equals(friendId))
                        itr.remove();
                }
                pagerAdapter.updateMapFragment();
            }
        });
        addFriendFragment.setListener(new AddFriendFragment.Listener() {
            @Override
            public void onSendFriendRequest(User recipient) {
                FirebaseService.getInstance().sendFriendRequest(recipient);
                Profile me = appRes.getProfile();
                me.sentFriendRequests.add(recipient.userId);
                setProfile(me);

                FirebaseService.getInstance().sendNotification(FRIEND_REQUEST_SENT, recipient.userId);
                Logger.toast(getString(R.string.friend_request_sent));
                if(FragmentListeners.getInstance().getAddFriendListener() != null) {
                    FragmentListeners.getInstance().getAddFriendListener().update();
                }
            }
            @Override
            public void onRemoveSentFriendRequest(User friend) {
                // Remove friend from profile's sentFriendRequests
                Profile profileToSave = appRes.getProfile();
                Iterator<String> itr = profileToSave.sentFriendRequests.iterator();
                while(itr.hasNext())  {
                    String userId = itr.next();
                    if(userId.equals(friend.userId)) {
                        itr.remove();
                        break;
                    }
                }
                setProfile(profileToSave);

                FirebaseService.getInstance().removeSentFriendRequest(friend);
                Logger.toast(getString(R.string.friend_request_removed));
                if(FragmentListeners.getInstance().getAddFriendListener() != null) {
                    FragmentListeners.getInstance().getAddFriendListener().update();
                }
            }
        });
        profileFragment.setListener(new ProfileFragment.Listener() {
            @Override
            public void onMarkerMustUpdate() {
                mapFragment.refreshData();
                mapFragment.updateMyMarker();
            }
        });

        UserListAdapter.setListener(new UserListAdapter.Listener() {
            @Override
            public void onUserClicked(User user) {
                addFriendFragment.setFriend(user);
                switchToFragment(addFriendFragment);
            }
        });

        FriendListAdapter.setListener(new FriendListAdapter.Listener() {
            @Override
            public void onSelectedFriendsChanged() {
                friendsFragment.setShareButton();
            }

            @Override
            public void onMoreButtonClick(User friend) {
                final FriendOptionsDialogFragment optionsDialog = new FriendOptionsDialogFragment();
                optionsDialog.setFriend(friend);
                optionsDialog.setListener(new FriendOptionsDialogFragment.Listener() {
                    @Override
                    public void onRemoveFriendClicked(final User selectedFriend) {
                        CommonDialog dialog = new CommonDialog();
                        dialog.refreshData(null, getString(R.string.friends_remove_confirmation), CommonDialog.CommonDialogType.YES_NO_DIALOG);
                        dialog.setListener(new CommonDialog.Listener() {
                            @Override
                            public void onDialogYesClick() {
                                optionsDialog.dismiss();
                                removeFriend(selectedFriend);
                            }
                        });
                        dialog.show(getSupportFragmentManager(), "poista kaveri");
                    }

                    @Override
                    public void onLocationRequested(final User selectedFriend) {
                        FirebaseService.getInstance().sendNotification(LOCATION_REQUESTED, selectedFriend.userId);
                        Logger.toast(getString(R.string.friends_location_requested));
                    }

                    @Override
                    public void onLocationSent(final User selectedFriend) {
                        shareLocationOnce(Arrays.asList(selectedFriend.userId));
                    }
                });
                optionsDialog.show(getSupportFragmentManager(), "kaverin lisävalikko");
            }
        });
    }

    public void openMainApp() {
        // Remove loader fragment
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if(fragment != null) {
            getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
        }
        container.setVisibility(View.INVISIBLE);

        tabLayout.setVisibility(View.VISIBLE);
        friendsFragment.refreshData();
        mapFragment.refreshData();
        profileFragment.refreshData();
        ArrayList<Fragment> fragments = new ArrayList<>();
        fragments.add(friendsFragment);
        fragments.add(mapFragment);
        fragments.add(profileFragment);
        pagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), fragments);
        viewPager.setOffscreenPageLimit(2);
        viewPager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        setTabIcons();

        FirebaseService.getInstance().setSharedLocationsListener(this);

        // käynnistä servicet
        if (!isMyServiceRunning(NotificationService.class)) {
            startService(notificationServiceIntent);
        }

        if(isGooglePlayServicesAvailable() && appRes.getProfile().shareResource != null && !isMyServiceRunning(LocationService.class)) {
            startService(locationServiceIntent);
        }
    }

    private void openStartSharingDialog(final ArrayList<String> selectedFriendIds) {
        final SelectShareTimeDialogFragment dialog = new SelectShareTimeDialogFragment();
        dialog.setCancelable(false);
        dialog.show(getSupportFragmentManager(), "Valitse aika");
        dialog.setListener(new SelectShareTimeDialogFragment.SelectShareTimeDialogCloseListener() {
            @Override
            public void onStartShareYesClick(LocationShare.ShareType shareType, long timeToShare) {
                dialog.dismiss();

                if(shareType == ONCE) {
                    shareLocationOnce(selectedFriendIds);
                } else {
                    Profile profile = appRes.getProfile();
                    long currentTime = System.currentTimeMillis();
                    //long endTime = shareType == FOREVER ? 0 : currentTime + timeToShare;
                    long endTime = currentTime + timeToShare;
                    LocationShare locationShare = new LocationShare();
                    locationShare.userId = profile.userId;
                    locationShare.firstName = profile.firstName;
                    locationShare.lastName = profile.lastName;
                    locationShare.startTime = currentTime;
                    locationShare.endTime = endTime;
                    locationShare.location = appRes.getLocation();
                    locationShare.shareType = shareType;
                    FirebaseService.getInstance().setSharedLocation(selectedFriendIds, locationShare);

                    // Save sharing end time to profile
                    Profile profileToSave = appRes.getProfile();
                    ShareResource shareResource = new ShareResource();
                    shareResource.endTime = endTime;
                    shareResource.friendIdsToShare = selectedFriendIds;
                    shareResource.shareType = shareType;
                    appRes.setShareResourceForService(shareResource);

                    profileToSave.shareResource = shareResource;
                    setProfile(profileToSave);

                    // Send notification to selected users
                    for(String recipientId : selectedFriendIds) {
                        FirebaseService.getInstance().sendNotification(SHARE_LOCATION_STARTED, recipientId);
                    }

                    if(isGooglePlayServicesAvailable()) {
                        LocationService.setListener(MainActivity.this);
                        startService(locationServiceIntent);
                    }
                }
                // Update fragments
                pagerAdapter.updateFriendsFragment();
                pagerAdapter.updateMapFragment();
            }

            @Override
            public void onStartShareDialogCancel() {
                dialog.dismiss();
            }
        });
    }

    public void shareLocationOnce(final List<String> selectedFriendIds) {
        SingleLocationProvider.getLocationOnce(new SingleLocationProvider.GetLocationOnceHandler() {
            @Override
            public void onGetLocationOnceSuccess(LiveLatLng location) {
                Profile profile = appRes.getProfile();
                LocationShare singleShare = new LocationShare();
                singleShare.userId = profile.userId;
                singleShare.location = location;
                singleShare.startTime = System.currentTimeMillis();
                singleShare.firstName = profile.firstName;
                singleShare.lastName = profile.lastName;
                singleShare.endTime = 0;
                singleShare.shareType = ONCE;
                for(String friendId : selectedFriendIds) {
                    FirebaseService.getInstance().setSharedLocation(Collections.singletonList(friendId), singleShare);
                    FirebaseService.getInstance().sendNotification(LOCATION_SHARED, friendId);
                }
                Logger.toast(getString(R.string.friends_location_shared));
            }
        });
    }

    private void addFriendsToShare(ArrayList<String> friendIdsToAdd) {
        Profile profileToSave = appRes.getProfile();
        ShareResource shareResource = profileToSave.shareResource.clone();

        LocationShare locationShare = new LocationShare();
        locationShare.userId = profileToSave.userId;
        locationShare.firstName = profileToSave.firstName;
        locationShare.lastName = profileToSave.lastName;
        locationShare.startTime = System.currentTimeMillis();
        locationShare.endTime = shareResource.endTime;
        locationShare.location = appRes.getLocation();
        FirebaseService.getInstance().setSharedLocation(friendIdsToAdd, locationShare);

        shareResource.friendIdsToShare.addAll(friendIdsToAdd);
        appRes.setShareResourceForService(shareResource);

        profileToSave.shareResource = shareResource;
        setProfile(profileToSave);

        // Send notification to selected users
        for(String friendId : friendIdsToAdd) {
            FirebaseService.getInstance().sendNotification(SHARE_LOCATION_STARTED, friendId);
        }

        if(isGooglePlayServicesAvailable()) {
            LocationService.setListener(MainActivity.this);
            startService(locationServiceIntent);
        }

        // Update fragments
        pagerAdapter.updateFriendsFragment();
        pagerAdapter.updateMapFragment();
    }

    private void onStopSharing() {
        // Joko painetaan "lopeta sijainnin jakaminen" tai aika kuluu loppuun timeristä
        Profile profileToSave = appRes.getProfile();
        FirebaseService.getInstance().removeSharedLocation(profileToSave.shareResource.friendIdsToShare);

        profileToSave.shareResource = null;
        setProfile(profileToSave);

        appRes.setShareResourceForService(null);
        stopService(locationServiceIntent);

        pagerAdapter.updateFriendsFragment();
        pagerAdapter.updateMapFragment();
    }

    @Override
    public void onNewLocationForShareFound(LiveLatLng location) {
        appRes.setLocation(location);
        pagerAdapter.updateMapFragment();
    }

    private void switchToFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
        container.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSharedLocationAdded(LocationShare locationShare) {
        ArrayList<LocationShare> locationShares = appRes.getSharedLocations();
        locationShares.add(locationShare);
        appRes.setSharedLocations(locationShares);
        mapFragment.onSharedLocationAdded(locationShare);
        pagerAdapter.updateFriendsFragment();
    }

    @Override
    public void onSharedLocationChange(LocationShare locationShare) {
        mapFragment.onSharedLocationChange(locationShare);
    }

    private void setProfile(Profile profileToSave) {
        FirebaseService.getInstance().setProfile(profileToSave);
        appRes.setProfile(profileToSave);
    }

    private void addMeToFriendList(User user) {
        // Add friend to my friend list
        Profile profileToSave = appRes.getProfile();
        profileToSave.friends.add(user.userId);
        setProfile(profileToSave);
        // Add me to friend's list
        FirebaseService.getInstance().addMeToFriendList(user);
        appRes.getFriends().add(user);
    }

    private void removeFriendRequest(User request) {
        FirebaseService.getInstance().removeFriendRequest(request);
        Iterator<User> itr = appRes.getFriendRequests().iterator();
        while(itr.hasNext()) {
            User user = itr.next();
            if(user.userId.equals(request.userId))
                itr.remove();
        }
        pagerAdapter.updateFriendsFragment();
        if(FragmentListeners.getInstance().getSearchFriendsListener() != null) {
            FragmentListeners.getInstance().getSearchFriendsListener().update();
        }

        Logger.toast(R.string.friend_added);
    }

    private void removeFriend(final User selectedFriend) {
        // Remove friend from my profile's friend list
        Profile profileToSave = appRes.getProfile();
        Iterator<String> itr = profileToSave.friends.iterator();
        while(itr.hasNext())  {
            String userId = itr.next();
            if(userId.equals(selectedFriend.userId)) {
                itr.remove();
                break;
            }
        }
        setProfile(profileToSave);

        // Remove friend from local friend list
        Iterator<User> itr2 = appRes.getFriends().iterator();
        while(itr2.hasNext()) {
            User friend = itr2.next();
            if(friend.userId.equals(selectedFriend.userId))
                itr2.remove();
        }

        // Refresh friendsFragment
        pagerAdapter.updateFriendsFragment();
        if(FragmentListeners.getInstance().getSearchFriendsListener() != null) {
            FragmentListeners.getInstance().getSearchFriendsListener().update();
        }
        FirebaseService.getInstance().removeMeFromFriendList(selectedFriend);
    }

    private void setTabIcons() {
        setTabIcon(tabLayout.getTabAt(0), R.drawable.group_icon, null);
        setTabIcon(tabLayout.getTabAt(1), R.drawable.marker_icon, null);
        setTabIcon(tabLayout.getTabAt(2), R.drawable.profile_icon, null);

        final int colorUnSelected = ContextCompat.getColor(MainActivity.this, R.color.color_secondary);
        final int colorSelected = ContextCompat.getColor(MainActivity.this, R.color.color_text_light);
        setTabIcon(tabLayout.getTabAt(0), null, colorSelected);
        setTabIcon(tabLayout.getTabAt(1), null, colorUnSelected);
        setTabIcon(tabLayout.getTabAt(2), null, colorUnSelected);

        tabLayout.addOnTabSelectedListener(
                new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {

                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        super.onTabSelected(tab);
                        setTabIcon(tab, null, colorSelected);
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                        super.onTabUnselected(tab);
                        setTabIcon(tab, null, colorUnSelected);
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                        super.onTabReselected(tab);
                    }
                }
        );
    }

    private void setTabIcon(TabLayout.Tab tab, Integer icon, Integer color) {
        if(icon != null && color == null) {
            tab.setIcon(icon);
            return;
        }

        Drawable tabIcon = tab.getIcon();
        if(tabIcon != null)
            tabIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }
}
