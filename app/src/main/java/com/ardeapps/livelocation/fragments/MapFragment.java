package com.ardeapps.livelocation.fragments;


import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ardeapps.livelocation.AppRes;
import com.ardeapps.livelocation.ImageUtil;
import com.ardeapps.livelocation.Logger;
import com.ardeapps.livelocation.R;
import com.ardeapps.livelocation.StringUtil;
import com.ardeapps.livelocation.TimeUtil;
import com.ardeapps.livelocation.objects.LiveLatLng;
import com.ardeapps.livelocation.objects.LocationShare;
import com.ardeapps.livelocation.objects.Profile;
import com.ardeapps.livelocation.services.FragmentListeners;
import com.ardeapps.livelocation.services.SingleLocationProvider;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.R.transition.move;
import static com.ardeapps.livelocation.objects.LocationShare.ShareType.FOREVER;
import static com.ardeapps.livelocation.objects.LocationShare.ShareType.ONCE;
import static com.ardeapps.livelocation.objects.LocationShare.ShareType.ONGOING;
import static com.ardeapps.livelocation.services.FragmentListeners.MY_PERMISSION_ACCESS_COARSE_LOCATION;

public class MapFragment extends Fragment {

    private String TAG = MapFragment.class.getSimpleName();

    public interface Listener {
        void onStartShareLocation();
        void onStopShareLocation();
        void onRemoveOnceLocation(String friendId);
    }

    Listener mListener = null;

    public void setListener(Listener l) {
        mListener = l;
    }

    MapView mMapView;
    private GoogleMap googleMap;
    LinearLayout share_button;
    LinearLayout friendsLocationContainer;
    TextView button_title;
    TextView button_time;
    TextView sharing_title;
    AppRes appRes = (AppRes) AppRes.getContext();
    Context context = AppRes.getContext();
    Map<String, Bitmap> markerPictures;
    Profile me;
    Map<String, Marker> markers;
    Map<String, DetailsHolder> detailsHolders;
    LatLng myLocation;
    ArrayList<LocationShare> sharedLocations;
    CountDownTimer countDownTime;
    Bitmap myCurrentIcon;

    // Request permission container
    RelativeLayout mapPermissionLayout;
    Button allowMapButton;

    private class DetailsHolder {
        RelativeLayout detailsContainer;
        TextView distance_text;
        TextView name_text;
        TextView time_text;
        ImageView profile_picture;
        ImageView remove_icon;
    }

    public void update() {
        if(myLocation != null) {
            Marker marker = markers.get(me.userId);
            if (marker.getPosition() != myLocation)
                animateMarker(marker, marker.getPosition(), myLocation);
        }
        refreshShareLocations();
        setShareButton();
    }

    public void updateMyMarker() {
        BitmapDescriptor icon;
        if(markerPictures.get(me.userId) != null) {
            myCurrentIcon = markerPictures.get(me.userId);
            icon = ImageUtil.getMarkerIcon(myCurrentIcon, null, null);
        } else {
            myCurrentIcon = ImageUtil.getDrawableAsBitmap(ContextCompat.getDrawable(context, R.drawable.default_profile_picture));
            icon = ImageUtil.getMarkerIcon(myCurrentIcon, me.firstName, me.lastName);
        }
        markers.get(me.userId).setIcon(icon);
    }

    public void setShareButton() {
        if(me.shareResource != null) {
            button_title.setText(R.string.friends_stop_share);
            if(me.shareResource.shareType == ONGOING) {
                long timeLeft = me.shareResource.endTime - System.currentTimeMillis();
                startTimer(timeLeft, 60000);
                button_time.setVisibility(View.VISIBLE);
                button_time.setText(StringUtil.getTimeLeftText(timeLeft));
            } else {
                button_time.setVisibility(View.GONE);
                button_time.setText("");
            }
        } else {
            button_title.setText(R.string.map_share_to_all);
            button_time.setVisibility(View.GONE);
            button_time.setText("");
            if(appRes.getFriends().size() > 0) {
                share_button.setBackgroundColor(Color.WHITE);
                share_button.setEnabled(true);
            } else {
                share_button.setBackgroundColor(ContextCompat.getColor(context, R.color.color_button_disabled));
                share_button.setEnabled(false);
            }

            if(countDownTime != null)
                countDownTime.cancel();
        }
    }

    public void refreshData() {
        sharedLocations = appRes.getSharedLocations();
        if(appRes.getLocation() != null)
            myLocation = appRes.getLocation().asLatLng();
        markerPictures = appRes.getProfilePictures();
        me = appRes.getProfile();
    }

    public void initializeLocations() {
        markers = new HashMap<>();
        detailsHolders = new HashMap<>();

        // Set marker and icon of current user
        if(myLocation != null) {
            BitmapDescriptor icon;
            if (markerPictures.get(me.userId) == null) {
                myCurrentIcon = ImageUtil.getDrawableAsBitmap(ContextCompat.getDrawable(context, R.drawable.default_profile_picture));
                icon = ImageUtil.getMarkerIcon(myCurrentIcon, me.firstName, me.lastName);
            } else {
                myCurrentIcon = markerPictures.get(me.userId);
                icon = ImageUtil.getMarkerIcon(myCurrentIcon, null, null);
            }

            Marker myMarker = googleMap.addMarker(new MarkerOptions()
                    .position(myLocation)
                    .icon(icon));

            markers.put(me.userId, myMarker);
        }

        refreshShareLocations();

        refreshMapCamera();

        setShareButton();
    }

    private void addLocationShareToDetailsContainer(final LocationShare locationShare) {
        if(sharedLocations.size() > 0) {
            sharing_title.setVisibility(View.VISIBLE);
            BitmapDescriptor icon;
            Bitmap picture;
            if (markerPictures.get(locationShare.userId) == null) {
                picture = ImageUtil.getDrawableAsBitmap(ContextCompat.getDrawable(context, R.drawable.default_profile_picture));
                icon = ImageUtil.getMarkerIcon(picture, locationShare.firstName, locationShare.lastName);
            } else {
                picture = markerPictures.get(locationShare.userId);
                icon = ImageUtil.getMarkerIcon(picture, null, null);
            }

            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(locationShare.location.asLatLng())
                    .icon(icon));
            markers.put(locationShare.userId, marker);

            final DetailsHolder holder = new DetailsHolder();
            LayoutInflater inflater = LayoutInflater.from(appRes.getActivity());
            View cv = inflater.inflate(R.layout.friend_location_item, friendsLocationContainer, false);
            holder.detailsContainer = (RelativeLayout) cv.findViewById(R.id.detailsContainer);
            holder.distance_text = (TextView) cv.findViewById(R.id.distance_text);
            holder.profile_picture = (ImageView) cv.findViewById(R.id.profile_picture);
            holder.name_text = (TextView) cv.findViewById(R.id.name_text);
            holder.time_text = (TextView) cv.findViewById(R.id.time_text);
            holder.remove_icon = (ImageView) cv.findViewById(R.id.remove_icon);

            holder.name_text.setText(StringUtil.getFullName(locationShare.firstName, locationShare.lastName));
            holder.profile_picture.setImageDrawable(ImageUtil.getRoundedDrawable(picture));

            holder.remove_icon.setVisibility(locationShare.shareType == ONCE ? View.VISIBLE : View.GONE);
            holder.distance_text.setVisibility(locationShare.shareType == ONCE ? View.GONE : View.VISIBLE);
            holder.distance_text.setText(locationShare.shareType == ONCE ? "" : StringUtil.getDistanceText(locationShare.location));

            if (locationShare.shareType == ONCE) {
                holder.time_text.setText(StringUtil.getDateTimeText(locationShare.startTime));
                holder.remove_icon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mListener.onRemoveOnceLocation(locationShare.userId);
                    }
                });
            } else if (locationShare.shareType == ONGOING) {
                holder.time_text.setText(StringUtil.getShareUntilText(locationShare.endTime - locationShare.startTime));
            } else if (locationShare.shareType == FOREVER) {
                holder.time_text.setText(R.string.map_sharing);
            }

            holder.detailsContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                            .target(locationShare.location.asLatLng())
                            .zoom(googleMap.getCameraPosition().zoom)
                            .build()));
                }
            });

            detailsHolders.put(locationShare.userId, holder);
            friendsLocationContainer.addView(cv);
        } else {
            sharing_title.setVisibility(View.GONE);
        }
    }

    public void onSharedLocationAdded(LocationShare locationShare) {
        // Refresh data to get also new locationShare for showing "Jaettu sinulle" container
        refreshData();
        addLocationShareToDetailsContainer(locationShare);
        refreshMapCamera();
    }

    public void onSharedLocationChange(LocationShare locationShare) {
        Marker marker = markers.get(locationShare.userId);
        animateMarker(marker, marker.getPosition(), locationShare.location.asLatLng());
        detailsHolders.get(locationShare.userId).distance_text.setText(StringUtil.getDistanceText(locationShare.location));
    }

    private void refreshShareLocations() {
        // Set markers and icons of friends
        friendsLocationContainer.removeAllViews();
        for (final LocationShare locationShare : sharedLocations) {
            addLocationShareToDetailsContainer(locationShare);
        }
    }

    public void refreshMapCamera() {
        if(markers.size() > 1) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Map.Entry<String, Marker> entry : markers.entrySet()) {
                builder.include(entry.getValue().getPosition());
            }
            LatLngBounds bounds = builder.build();
            int pixelsFromEdges = ImageUtil.dipToPixels(100);
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, pixelsFromEdges);
            googleMap.moveCamera(cu);
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(myLocation).zoom(16).build()));
        }
    }

    public void animateMarker(final Marker marker, final LatLng startPosition, final LatLng toPosition) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();

        final long duration = 1000;
        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                double lng = t * toPosition.longitude + (1 - t) * startPosition.longitude;
                double lat = t * toPosition.latitude + (1 - t) * startPosition.latitude;

                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentListeners.getInstance().setPermissionHandledListener(new FragmentListeners.PermissionHandledListener() {
            @Override
            public void onPermissionGranted(int MY_PERMISSION) {
                if(MY_PERMISSION == MY_PERMISSION_ACCESS_COARSE_LOCATION) {
                    SingleLocationProvider.getLocationOnce(new SingleLocationProvider.GetLocationOnceHandler() {
                        @Override
                        public void onGetLocationOnceSuccess(LiveLatLng location) {
                            appRes.setLocation(location);
                            myLocation = location.asLatLng();
                            mapPermissionLayout.setVisibility(View.GONE);
                            initializeLocations();
                        }
                    });
                }
            }
        });
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_map, container, false);
        mMapView = (MapView) v.findViewById(R.id.mapView);
        share_button = (LinearLayout) v.findViewById(R.id.share_button);
        friendsLocationContainer = (LinearLayout) v.findViewById(R.id.friendsLocationContainer);
        button_title = (TextView) v.findViewById(R.id.button_title);
        button_time = (TextView) v.findViewById(R.id.button_time);
        sharing_title = (TextView) v.findViewById(R.id.sharing_title);
        mapPermissionLayout = (RelativeLayout) v.findViewById(R.id.mapPermissionLayout);
        allowMapButton = (Button) v.findViewById(R.id.allowMapButton);

        mMapView.onCreate(savedInstanceState);

        final boolean locationPermissionNeeded = ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;

        mapPermissionLayout.setVisibility(locationPermissionNeeded ? View.VISIBLE : View.GONE);

        try {
            MapsInitializer.initialize(AppRes.getContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;
                googleMap.clear();

                if(!locationPermissionNeeded) {
                    // Feikki sijainti VAIN TESTIN AJAN koska nox
                    /*if(!appRes.getProfile().userId.equals("BMidF9dUT6NMzlnRKqWXCvt3jeF2")) {
                        LiveLatLng location = new LiveLatLng(62.240631, 25.755300);
                        appRes.setLocation(location);
                        myLocation = location.asLatLng();
                        initializeLocations();
                    }*/

                    SingleLocationProvider.getLocationOnce(new SingleLocationProvider.GetLocationOnceHandler() {
                        @Override
                        public void onGetLocationOnceSuccess(LiveLatLng location) {
                            appRes.setLocation(location);
                            myLocation = location.asLatLng();
                            initializeLocations();
                        }
                    });
                }
            }
        });

        share_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(me.shareResource != null)
                    mListener.onStopShareLocation();
                else
                    mListener.onStartShareLocation();
            }
        });

        allowMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (locationPermissionNeeded) {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                            MY_PERMISSION_ACCESS_COARSE_LOCATION);
                } else {
                    mapPermissionLayout.setVisibility(View.GONE);
                }
            }
        });
        return v;
    }

    private void startTimer(long timeToFuture, long interval) {
        countDownTime = new CountDownTimer(timeToFuture, interval) {

            public void onTick(long millisUntilFinished) {
                button_time.setText(StringUtil.getTimeLeftText(millisUntilFinished));
            }

            public void onFinish() {
                if(appRes.getIsAppVisible()) {
                    mListener.onStopShareLocation();
                }
            }
        }.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }
}
