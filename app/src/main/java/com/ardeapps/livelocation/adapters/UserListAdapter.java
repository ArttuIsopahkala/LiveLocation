package com.ardeapps.livelocation.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ardeapps.livelocation.AppRes;
import com.ardeapps.livelocation.ImageUtil;
import com.ardeapps.livelocation.StringUtil;
import com.ardeapps.livelocation.handlers.firebase.DownloadProfilePictureHandler;
import com.ardeapps.livelocation.R;
import com.ardeapps.livelocation.objects.User;
import com.ardeapps.livelocation.services.FirebaseService;

import java.util.ArrayList;

import static com.ardeapps.livelocation.R.id.full_name_text;
import static com.ardeapps.livelocation.R.id.profile_picture;

/**
 * Created by Arttu on 15.9.2016.
 */
public class UserListAdapter extends BaseAdapter {

    public interface Listener {
        void onUserClicked(User user);
    }

    static Listener mListener = null;

    public static void setListener(Listener l) {
        mListener = l;
    }

    ArrayList<User> users = new ArrayList<>();
    Context context;
    private static LayoutInflater inflater = null;

    public UserListAdapter() {
        this.context = AppRes.getContext();

        inflater = (LayoutInflater) context.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setUsers(ArrayList<User> users) {
        this.users = users;
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public static class UserHolder {
        TextView full_name_text;
        ImageView user_profile_picture;
    }

    @Override
    public View getView(final int position, View cv, ViewGroup parent) {
        final UserHolder holder = new UserHolder();
        if (cv == null) {
            cv = inflater.inflate(R.layout.user_list_item, null);
        }
        holder.full_name_text = (TextView) cv.findViewById(R.id.full_name_text);
        holder.user_profile_picture = (ImageView) cv.findViewById(R.id.user_profile_picture);

        holder.full_name_text.setText(StringUtil.getFullName(users.get(position)));

        cv.setBackgroundColor(ContextCompat.getColor(context, position % 2 == 0 ? R.color.color_main : R.color.color_main_secondary));

        if(users.get(position).isImageUploaded) {
            FirebaseService.getInstance().downloadProfilePicture(users.get(position).userId, new DownloadProfilePictureHandler() {
                @Override
                public void onDownloadProfilePictureSuccess(Bitmap bitmap) {
                    ImageUtil.fadeImageIn(holder.user_profile_picture);
                    holder.user_profile_picture.setImageDrawable(ImageUtil.getRoundedDrawable(bitmap));
                }
            });
        } else {
            holder.user_profile_picture.setImageResource(R.drawable.default_profile_picture);
        }

        cv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AppRes.hideKeyBoard(holder.full_name_text);
                mListener.onUserClicked(users.get(position));
            }
        });

        return cv;
    }
}
