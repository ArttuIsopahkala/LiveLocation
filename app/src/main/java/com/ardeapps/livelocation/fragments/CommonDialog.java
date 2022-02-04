package com.ardeapps.livelocation.fragments;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.ardeapps.livelocation.AppRes;
import com.ardeapps.livelocation.R;
import com.ardeapps.livelocation.StringUtil;

import static com.ardeapps.livelocation.fragments.CommonDialog.CommonDialogType.OK_DIALOG;
import static com.ardeapps.livelocation.fragments.CommonDialog.CommonDialogType.YES_NO_DIALOG;

/**
 * Created by Arttu on 29.11.2015.
 */
public class CommonDialog extends DialogFragment {

    public enum CommonDialogType {
        OK_DIALOG,
        YES_NO_DIALOG
    }

    public interface Listener {
        void onDialogYesClick();
    }

    Listener mListener = null;

    public void setListener(Listener l) {
        mListener = l;
    }

    TextView info_text;
    TextView title;
    Button no_button;
    Button yes_button;
    String title_text;
    String desc_text;
    CommonDialogType type;

    public void refreshData(String title, String desc, CommonDialogType type) {
        this.title_text = title;
        this.desc_text = desc;
        this.type = type;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.common_dialog, container);

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        title = (TextView) v.findViewById(R.id.title);
        info_text = (TextView) v.findViewById(R.id.info_text);
        no_button = (Button) v.findViewById(R.id.btn_no);
        yes_button = (Button) v.findViewById(R.id.btn_yes);

        if(!StringUtil.isEmptyString(title_text)) {
            title.setText(title_text);
            title.setVisibility(View.VISIBLE);
        } else {
            title.setText("");
            title.setVisibility(View.GONE);
        }

        if(!StringUtil.isEmptyString(desc_text)) {
            info_text.setText(desc_text);
            info_text.setVisibility(View.VISIBLE);
        } else {
            info_text.setText("");
            info_text.setVisibility(View.GONE);
        }

        if(type == OK_DIALOG) {
            yes_button.setText(getString(R.string.ok));
            no_button.setVisibility(View.GONE);
        } else if(type == YES_NO_DIALOG) {
            yes_button.setText(getString(R.string.yes));
            no_button.setVisibility(View.VISIBLE);
        } else {
            yes_button.setText(getString(R.string.ok));
            no_button.setVisibility(View.GONE);
        }

        no_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        yes_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if(type == YES_NO_DIALOG) {
                    mListener.onDialogYesClick();
                }
            }
        });

        return v;
    }
}
