package com.ardeapps.livelocation.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ardeapps.livelocation.AppRes;
import com.ardeapps.livelocation.ImageUtil;
import com.ardeapps.livelocation.R;
import com.ardeapps.livelocation.StringUtil;
import com.ardeapps.livelocation.TimeUtil;
import com.ardeapps.livelocation.handlers.firebase.DownloadProfilePictureHandler;
import com.ardeapps.livelocation.objects.Location;
import com.ardeapps.livelocation.objects.LocationShare;
import com.ardeapps.livelocation.objects.ShareResource;
import com.ardeapps.livelocation.objects.User;
import com.ardeapps.livelocation.services.FirebaseService;

import java.util.ArrayList;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by Arttu on 15.9.2016.
 */
public class FriendListAdapter extends BaseAdapter {

    public interface Listener {
        void onSelectedFriendsChanged();
        void onMoreButtonClick(User friend);
    }

    static Listener mListener = null;

    public static void setListener(Listener l) {
        mListener = l;
    }

    ArrayList<User> friends = new ArrayList<>();
    Context context;
    ArrayList<String> selectedFriendIds;
    ShareResource shareResource;
    ArrayList<LocationShare> locationShares;

    AppRes appRes = (AppRes) getApplicationContext();
    private static LayoutInflater inflater = null;

    public FriendListAdapter() {
        this.context = AppRes.getContext();

        selectedFriendIds = new ArrayList<>();

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void refreshData() {
        friends = appRes.getFriends();
        shareResource = appRes.getProfile().shareResource;
        locationShares = appRes.getSharedLocations();
    }

    @Override
    public int getCount() {
        return friends.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private class Holder {
        LinearLayout friendContainer;
        TextView full_name_text;
        ImageView user_profile_picture;
        CheckBox addToShareBox;
        ImageView moreButton;
        RelativeLayout disabled_overlay;
        ImageView sharing_ring;
    }

    @Override
    public View getView(final int position, View v, ViewGroup parent) {
        final Holder holder = new Holder();
        if (v == null) {
            v = inflater.inflate(R.layout.friend_list_item, null);
        }
        holder.friendContainer = (LinearLayout) v.findViewById(R.id.friendContainer);
        holder.full_name_text = (TextView) v.findViewById(R.id.full_name_text);
        holder.user_profile_picture = (ImageView) v.findViewById(R.id.user_profile_picture);
        holder.addToShareBox = (CheckBox) v.findViewById(R.id.addToShareBox);
        holder.moreButton = (ImageView) v.findViewById(R.id.moreButton);
        holder.disabled_overlay = (RelativeLayout) v.findViewById(R.id.disabled_overlay);
        holder.sharing_ring = (ImageView) v.findViewById(R.id.sharing_ring);

        final User friend = friends.get(position);

        boolean isSharingForMe = false;
        for(LocationShare locationShare : locationShares) {
            // Jos jakaa minulle ja ei ole kertasijainti
            if(locationShare.userId.equals(friend.userId) && locationShare.endTime != TimeUtil.getLoactionSharedOnceTime()) {
                isSharingForMe = true;
                break;
            }
        }

        if(isSharingForMe) {
            AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
            alphaAnimation.setDuration(1000);
            alphaAnimation.setRepeatCount(Animation.INFINITE);
            alphaAnimation.setRepeatMode(Animation.REVERSE);
            holder.sharing_ring.setAnimation(alphaAnimation);
        } else {
            holder.sharing_ring.setVisibility(View.GONE);
        }

        boolean inSharing = shareResource != null && shareResource.friendIdsToShare.contains(friend.userId);

        // If sharing, set disabled overlay
        holder.disabled_overlay.setVisibility(inSharing ? View.VISIBLE : View.INVISIBLE);
        holder.addToShareBox.setChecked(inSharing);
        holder.addToShareBox.setEnabled(!inSharing);
        v.setClickable(!inSharing);

        holder.friendContainer.setBackgroundColor(ContextCompat.getColor(context, position % 2 == 0 ? R.color.color_main : R.color.color_main_secondary));

        holder.full_name_text.setText(StringUtil.getFullName(friend));

        if (friend.isImageUploaded) {
            Bitmap bitmap = appRes.getProfilePictures().get(friend.userId);
            if(bitmap != null) {
                holder.user_profile_picture.setImageDrawable(ImageUtil.getRoundedDrawable(bitmap));
            } else {
                FirebaseService.getInstance().downloadProfilePicture(friend.userId, new DownloadProfilePictureHandler() {
                    @Override
                    public void onDownloadProfilePictureSuccess(Bitmap bitmap) {
                        ImageUtil.fadeImageIn(holder.user_profile_picture);
                        holder.user_profile_picture.setImageDrawable(ImageUtil.getRoundedDrawable(bitmap));
                    }
                });
            }
        }

        // Set listeners if not sharing
        if(!inSharing) {
            holder.friendContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    holder.addToShareBox.setChecked(!holder.addToShareBox.isChecked());
                }
            });

            holder.addToShareBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    if (checked) {
                        selectedFriendIds.add(friend.userId);
                    } else {
                        selectedFriendIds.remove(friend.userId);
                    }
                    mListener.onSelectedFriendsChanged();
                }
            });

            holder.moreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.onMoreButtonClick(friend);
                }
            });
        }
        return v;
    }

    // Return only selected users which are not in share
    public ArrayList<String> getSelectedFriendIds() {
        return selectedFriendIds;
    }
}
