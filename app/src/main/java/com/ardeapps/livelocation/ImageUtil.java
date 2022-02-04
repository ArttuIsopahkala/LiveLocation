package com.ardeapps.livelocation;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static android.R.attr.bitmap;
import static android.R.attr.name;
import static android.R.attr.x;
import static android.R.attr.y;
import static com.ardeapps.livelocation.R.id.loader_icon;

public class ImageUtil {
    private static String TAG = "ImageUtil";

    public static RoundedBitmapDrawable getRoundedDrawable(Bitmap bitmap) {
        int dimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);

        RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(AppRes.getContext().getResources(), output);
        dr.setCornerRadius(Math.max(output.getWidth(), output.getHeight()) / 2.0f);
        return dr;
    }

    public static Bitmap getSquarePicture(Bitmap bitmap) {
        int dimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
        return ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);
    }

    public static int dipToPixels(float dipValue) {
        DisplayMetrics metrics = AppRes.getContext().getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }

    public static Bitmap getCircleBitmap(Bitmap bitmap) {
        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);

        final int color = Color.RED;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    public static BitmapDescriptor getMarkerIcon(Bitmap sourceBitmap, String firstName, String lastName){
        int border = 6;
        int size = 120;
        Bitmap circleBitmap = getScaledBitmap(getCircleBitmap(sourceBitmap), size, size);

        Bitmap dstBitmap = Bitmap.createBitmap(
                circleBitmap.getWidth() + border*2,
                circleBitmap.getHeight() + border*2,
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(dstBitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xFFFFFFFF);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(border);
        paint.setAntiAlias(true);

        Rect rect = new Rect(
                border / 2,
                border / 2,
                canvas.getWidth() - border / 2,
                canvas.getHeight() - border / 2
        );

        final RectF rectF = new RectF(rect);
        final int cornerSizePx = dstBitmap.getWidth() / 2;
        canvas.drawRoundRect(rectF, cornerSizePx, cornerSizePx, paint);

        canvas.drawBitmap(circleBitmap, border, border, null);

        if(!StringUtil.isEmptyString(firstName) || !StringUtil.isEmptyString(lastName)) {
            float scale = AppRes.getContext().getResources().getDisplayMetrics().density;
            // new antialised Paint
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            // text color
            textPaint.setColor(ContextCompat.getColor(AppRes.getContext(), R.color.color_main));
            // text size in pixels
            int textSize = (int) (8 * scale);
            textPaint.setTextSize(textSize);
            //textPaint.setTextSize(30);

            textPaint.setTypeface(Typeface.DEFAULT_BOLD);

            // draw text to the Canvas center
            Rect bounds = new Rect();
            int y;
            int x;
            if(!StringUtil.isEmptyString(firstName) && StringUtil.isEmptyString(lastName)) {
                textPaint.getTextBounds(firstName, 0, firstName.length(), bounds);
                x = (dstBitmap.getWidth() - bounds.width()) / 2;
                y = (dstBitmap.getHeight() + bounds.height()) / 2;
                canvas.drawText(firstName, x, y, textPaint);
            } else if(StringUtil.isEmptyString(firstName) && !StringUtil.isEmptyString(lastName)) {
                textPaint.getTextBounds(lastName, 0, lastName.length(), bounds);
                x = (dstBitmap.getWidth() - bounds.width()) / 2;
                y = (dstBitmap.getHeight() + bounds.height()) / 2;
                canvas.drawText(lastName, x, y, textPaint);
            } else {
                textPaint.getTextBounds(firstName, 0, firstName.length(), bounds);
                x = (dstBitmap.getWidth() - bounds.width()) / 2;
                y = (dstBitmap.getHeight() + bounds.height()) / 2 - (textSize / 2);
                canvas.drawText(firstName, x, y, textPaint);

                textPaint.getTextBounds(lastName, 0, lastName.length(), bounds);
                x = (dstBitmap.getWidth() - bounds.width()) / 2;
                y = (dstBitmap.getHeight() + bounds.height()) / 2 + (textSize / 2);
                canvas.drawText(lastName, x, y, textPaint);
            }
        }

        return BitmapDescriptorFactory.fromBitmap(dstBitmap);
    }

    public static Bitmap getScaledBitmap(Bitmap bitmap, int newWidth, int newHeight) {
        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

        float ratioX = newWidth / (float) bitmap.getWidth();
        float ratioY = newHeight / (float) bitmap.getHeight();
        float middleX = newWidth / 2.0f;
        float middleY = newHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap, middleX - bitmap.getWidth() / 2, middleY - bitmap.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;
    }

    public static Bitmap scaleImageForUpload(Uri photoUri) throws IOException {
        Context context = AppRes.getContext();
        int MAX_IMAGE_DIMENSION = 480;
        InputStream is = context.getContentResolver().openInputStream(photoUri);
        BitmapFactory.Options dbo = new BitmapFactory.Options();
        dbo.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, dbo);
        is.close();

        ExifInterface ei = new ExifInterface(photoUri.getPath());
        int orientation;
        int orientation_tag = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        switch(orientation_tag) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                orientation = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                orientation = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                orientation = 270;
                break;
            case ExifInterface.ORIENTATION_NORMAL:
            default:
                orientation = 0;
        }

        int rotatedWidth, rotatedHeight;
        if (orientation_tag == ExifInterface.ORIENTATION_ROTATE_90 || orientation_tag == ExifInterface.ORIENTATION_ROTATE_270) {
            rotatedWidth = dbo.outWidth;
            rotatedHeight = dbo.outHeight;
        } else {
            rotatedWidth = dbo.outWidth;
            rotatedHeight = dbo.outHeight;
        }

        Bitmap srcBitmap;
        is = context.getContentResolver().openInputStream(photoUri);
        if (rotatedWidth > MAX_IMAGE_DIMENSION || rotatedHeight > MAX_IMAGE_DIMENSION) {
            float widthRatio = ((float) rotatedWidth) / ((float) MAX_IMAGE_DIMENSION);
            float heightRatio = ((float) rotatedHeight) / ((float) MAX_IMAGE_DIMENSION);
            float maxRatio = Math.max(widthRatio, heightRatio);

            // Create the bitmap from file
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = (int) maxRatio;
            srcBitmap = BitmapFactory.decodeStream(is, null, options);
        } else {
            srcBitmap = BitmapFactory.decodeStream(is);
        }
        is.close();

        if (orientation > 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);

            srcBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(),
                    srcBitmap.getHeight(), matrix, true);
        }

        srcBitmap = getSquarePicture(srcBitmap);

        String type = context.getContentResolver().getType(photoUri);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if(type == null) {
            srcBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        } else if (type.equals("image/png")) {
            srcBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        } else if (type.equals("image/jpg") || type.equals("image/jpeg")) {
            srcBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        }
        byte[] bMapArray = baos.toByteArray();
        baos.close();
        return BitmapFactory.decodeByteArray(bMapArray, 0, bMapArray.length);
    }

    public static Bitmap getDrawableAsBitmap (Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static void fadeImageIn(ImageView view) {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(300);
        view.startAnimation(fadeIn);
    }
}
