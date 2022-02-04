package com.ardeapps.livelocation.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ardeapps.livelocation.AppRes;
import com.ardeapps.livelocation.ImageUtil;
import com.ardeapps.livelocation.Logger;
import com.ardeapps.livelocation.R;
import com.ardeapps.livelocation.StringUtil;
import com.ardeapps.livelocation.services.FirebaseService;

import java.io.InputStream;

/**
 * Created by Arttu on 29.11.2015.
 */
public class EditProfileDialogFragment extends DialogFragment {

    public interface EditProfileDialogCloseListener
    {
        void onEditProfileDialogSave(String firstName, String lastName, Bitmap profilePicture);
        void onEditProfileDialogCancel();
    }

    EditProfileDialogCloseListener mListener = null;

    public void setListener(EditProfileDialogCloseListener l) {
        mListener = l;
    }

    String firstName;
    String lastName;
    String url;
    boolean createUser;
    Bitmap profilePicture;

    ImageView profile_picture;
    RelativeLayout profile_picture_container;
    EditText first_name;
    EditText last_name;
    Button save_button;
    Button close_button;

    public void refreshData(String firstName, String lastName, String url, boolean createUser) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.url = url;
        this.createUser = createUser;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_edit_profile, container);

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        profile_picture = (ImageView) v.findViewById(R.id.profile_picture);
        profile_picture_container = (RelativeLayout) v.findViewById(R.id.profile_picture_container);
        first_name = (EditText) v.findViewById(R.id.first_name);
        last_name = (EditText) v.findViewById(R.id.last_name);
        save_button = (Button) v.findViewById(R.id.save_button);
        close_button = (Button) v.findViewById(R.id.close_button);

        if(createUser) {
            profile_picture_container.setVisibility(View.VISIBLE);
            if (url != null)
                new DownloadImageTask(profile_picture).execute(url);
        } else {
            profile_picture_container.setVisibility(View.GONE);
        }

        first_name.setText(firstName);
        last_name.setText(lastName);

        profile_picture_container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final SelectPictureDialogFragment dialog = new SelectPictureDialogFragment();
                dialog.show(getActivity().getSupportFragmentManager(), "Vaihda profiilikuva");
                dialog.setListener(new SelectPictureDialogFragment.SelectPictureDialogCloseListener() {
                    @Override
                    public void onPictureSelected(Bitmap selectedPicture) {
                        if(selectedPicture != null) {
                            profilePicture = selectedPicture;
                            profile_picture.setImageDrawable(ImageUtil.getRoundedDrawable(selectedPicture));
                            dialog.dismiss();
                        } else {
                            Logger.toast(getString(R.string.profile_picture_error));
                        }
                    }

                    @Override
                    public void onRemovePicture() {
                        profilePicture = null;
                        profile_picture.setImageResource(R.drawable.default_profile_picture);
                        dialog.dismiss();
                    }

                    @Override
                    public void onCancelClick() {
                        dialog.dismiss();
                    }
                });
            }
        });

        save_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String firstName = first_name.getText().toString();
                String lastName = last_name.getText().toString();
                if(!StringUtil.isEmptyString(firstName) || !StringUtil.isEmptyString(lastName)) {
                    AppRes.hideKeyBoard(last_name);
                    dismiss();
                    mListener.onEditProfileDialogSave(firstName, lastName, profilePicture);
                }
            }
        });

        close_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppRes.hideKeyBoard(last_name);
                dismiss();
                mListener.onEditProfileDialogCancel();
            }
        });

        return v;
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            profilePicture = result;
            ImageUtil.fadeImageIn(bmImage);
            bmImage.setImageDrawable(ImageUtil.getRoundedDrawable(result));
        }
    }
}
