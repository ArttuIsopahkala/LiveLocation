package com.ardeapps.livelocation.fragments;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.ardeapps.livelocation.Logger;
import com.ardeapps.livelocation.R;
import com.ardeapps.livelocation.StringUtil;
import com.ardeapps.livelocation.TimeUtil;
import com.ardeapps.livelocation.objects.LocationShare;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.ardeapps.livelocation.R.id.time_group;
import static com.ardeapps.livelocation.R.string.share;
import static com.ardeapps.livelocation.objects.LocationShare.ShareType.FOREVER;
import static com.ardeapps.livelocation.objects.LocationShare.ShareType.ONCE;
import static com.ardeapps.livelocation.objects.LocationShare.ShareType.ONGOING;

/**
 * Created by Arttu on 29.11.2015.
 */
public class SelectShareTimeDialogFragment extends DialogFragment {

    public interface SelectShareTimeDialogCloseListener
    {
        void onStartShareYesClick(LocationShare.ShareType shareType, long timeToShare);
        void onStartShareDialogCancel();
    }

    SelectShareTimeDialogCloseListener mListener = null;

    public void setListener(SelectShareTimeDialogCloseListener l) {
        mListener = l;
    }

    Button ready_button;
    Button cancel_button;
    Button btn_add_time;
    Button btn_reduce_time;
    RadioButton radio_own;
    RadioButton radio_once;
    RadioButton radio_forever;
    TextView time_until;
    ArrayList<Long> shareTimes;
    int shareTimeIndex = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        shareTimes = TimeUtil.getShareTimes();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_select_share_time, container);

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        ready_button = (Button) v.findViewById(R.id.btn_ready);
        cancel_button = (Button) v.findViewById(R.id.btn_cancel);
        radio_own = (RadioButton) v.findViewById(R.id.radio_own);
        radio_forever = (RadioButton) v.findViewById(R.id.radio_forever);
        radio_once = (RadioButton) v.findViewById(R.id.radio_once);
        btn_reduce_time = (Button) v.findViewById(R.id.btn_reduce_time);
        btn_add_time = (Button) v.findViewById(R.id.btn_add_time);
        time_until = (TextView) v.findViewById(R.id.time_until);

        radio_own.setChecked(true);

        shareTimeIndex = 3;
        radio_own.setText(StringUtil.getShareOptionText(shareTimes.get(shareTimeIndex)));
        time_until.setText(StringUtil.getShareUntilText(shareTimes.get(shareTimeIndex)));

        btn_reduce_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareTimeIndex--;
                setSelectTimeButtons();
            }
        });

        btn_add_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareTimeIndex++;
                setSelectTimeButtons();
            }
        });

        ready_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocationShare.ShareType shareType = ONCE;
                long timeToShare = 0;
                if(radio_own.isChecked()) {
                    timeToShare = shareTimes.get(shareTimeIndex);
                    shareType = ONGOING;
                } else if(radio_forever.isChecked()) {
                    shareType = FOREVER;
                } else if(radio_once.isChecked()) {
                    shareType = ONCE;
                }
                mListener.onStartShareYesClick(shareType, timeToShare);
            }
        });

        cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onStartShareDialogCancel();
            }
        });

        return v;
    }

    private void setSelectTimeButtons() {
        radio_own.setText(StringUtil.getShareOptionText(shareTimes.get(shareTimeIndex)));
        time_until.setText(StringUtil.getShareUntilText(shareTimes.get(shareTimeIndex)));
        btn_add_time.setEnabled(shareTimeIndex < shareTimes.size() - 1);
        btn_reduce_time.setEnabled(shareTimeIndex > 0);
    }
}
