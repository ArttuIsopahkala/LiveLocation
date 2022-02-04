package com.ardeapps.livelocation.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import com.ardeapps.livelocation.AppRes;
import com.ardeapps.livelocation.BuildConfig;
import com.ardeapps.livelocation.ImageUtil;
import com.ardeapps.livelocation.R;
import com.ardeapps.livelocation.services.FragmentListeners;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.app.Activity.RESULT_OK;
import static com.ardeapps.livelocation.services.FragmentListeners.MY_PERMISSION_ACCESS_READ_EXTERNAL_STORAGE;
import static com.ardeapps.livelocation.services.FragmentListeners.MY_PERMISSION_ACCESS_TAKING_PICTURE;

/**
 * Created by Arttu on 29.11.2015.
 */
public class SelectPictureDialogFragment extends DialogFragment {

    public interface SelectPictureDialogCloseListener
    {
        void onPictureSelected(Bitmap selectedPicture);
        void onRemovePicture();
        void onCancelClick();
    }

    SelectPictureDialogCloseListener mListener = null;

    public void setListener(SelectPictureDialogCloseListener l) {
        mListener = l;
    }

    Button gallery_button;
    Button camera_button;
    Button remove_button;
    Button cancel_button;

    private String mCurrentPhotoPath;
    private static final int TAKE_PICTURE = 0;
    private static final int SELECT_PICTURE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentListeners.getInstance().setPermissionHandledListener(new FragmentListeners.PermissionHandledListener() {
            @Override
            public void onPermissionGranted(int MY_PERMISSION) {
                switch (MY_PERMISSION) {
                    case MY_PERMISSION_ACCESS_TAKING_PICTURE:
                        startCameraWithPermissionChecks();
                        break;
                    case MY_PERMISSION_ACCESS_READ_EXTERNAL_STORAGE:
                        openGallery();
                        break;
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_select_picture, container);

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        gallery_button = (Button) v.findViewById(R.id.gallery_button);
        camera_button = (Button) v.findViewById(R.id.camera_button);
        remove_button = (Button) v.findViewById(R.id.remove_button);
        cancel_button = (Button) v.findViewById(R.id.cancel_button);

        gallery_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        camera_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCameraWithPermissionChecks();
            }
        });

        remove_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onRemovePicture();
            }
        });

        cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onCancelClick();
            }
        });

        return v;
    }

    private void openGallery() {
        if (ContextCompat.checkSelfPermission(AppRes.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSION_ACCESS_READ_EXTERNAL_STORAGE);
        } else {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickIntent.setType("image/*");
            startActivityForResult(pickIntent, SELECT_PICTURE);
        }
    }

    private void startCameraWithPermissionChecks() {
        if (ContextCompat.checkSelfPermission(AppRes.getContext(), Manifest.permission.CAMERA)
                + ContextCompat.checkSelfPermission(AppRes.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                + ContextCompat.checkSelfPermission(AppRes.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSION_ACCESS_TAKING_PICTURE);
        } else {
            Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePicture.resolveActivity(AppRes.getContext().getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    return;
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(AppRes.getContext(),
                            BuildConfig.APPLICATION_ID + ".provider",
                            photoFile);
                    takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePicture, TAKE_PICTURE);
                }
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        File image = File.createTempFile(
                imageFileName,
                ".png",
                storageDir
        );

        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case SELECT_PICTURE:
                if(resultCode == RESULT_OK){
                    Uri uri = data.getData();
                    try {
                        Bitmap selectedPicture = ImageUtil.scaleImageForUpload(uri);
                        mListener.onPictureSelected(selectedPicture);
                    } catch(IOException e) {
                        mListener.onPictureSelected(null);
                    }
                }
                break;
            case TAKE_PICTURE:
                if(resultCode == RESULT_OK){
                    Uri imageUri = Uri.parse(mCurrentPhotoPath);
                    try {
                        Bitmap selectedPicture = ImageUtil.scaleImageForUpload(imageUri);
                        mListener.onPictureSelected(selectedPicture);
                    } catch(IOException e) {
                        mListener.onPictureSelected(null);
                    }
                }
                break;
            default:
                break;
        }
    }
}
