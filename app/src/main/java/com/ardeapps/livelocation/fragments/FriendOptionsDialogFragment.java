package com.ardeapps.livelocation.fragments;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import com.ardeapps.livelocation.R;
import com.ardeapps.livelocation.objects.User;

/**
 * Created by Arttu on 29.11.2015.
 */
public class FriendOptionsDialogFragment extends DialogFragment {

    public interface Listener {
        void onRemoveFriendClicked(User selectedFriend);
        void onLocationRequested(User selectedFriend);
        void onLocationSent(User selectedFriend);
    }

    Listener mListener = null;

    public void setListener(Listener l) {
        mListener = l;
    }

    Button ask_location_button;
    Button send_location_button;
    Button remove_button;
    Button cancel_button;
    User friend;

    public void setFriend(User friend) {
        this.friend = friend;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_friend_options, container);

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        ask_location_button = (Button) v.findViewById(R.id.ask_location_button);
        send_location_button = (Button) v.findViewById(R.id.send_location_button);
        remove_button = (Button) v.findViewById(R.id.remove_button);
        cancel_button = (Button) v.findViewById(R.id.cancel_button);

        ask_location_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                mListener.onLocationRequested(friend);
            }
        });

        send_location_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                mListener.onLocationSent(friend);
            }
        });

        remove_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onRemoveFriendClicked(friend);
            }
        });

        cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return v;
    }
}
