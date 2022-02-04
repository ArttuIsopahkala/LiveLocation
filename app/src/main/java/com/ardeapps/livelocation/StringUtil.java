package com.ardeapps.livelocation;

import android.content.Context;
import android.location.Location;
import android.text.Html;
import android.text.Spanned;

import com.ardeapps.livelocation.objects.LiveLatLng;
import com.ardeapps.livelocation.objects.Profile;
import com.ardeapps.livelocation.objects.User;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * Created by Arttu on 6.7.2017.
 */

public class StringUtil {

    public static String getFullName(Profile profile) {
        return getFullName(profile.firstName, profile.lastName);
    }

    public static String getFullName(User user) {
        return getFullName(user.firstName, user.lastName);
    }

    public static boolean isEmptyString(String text) {
        return text == null || text.trim().equals("");
    }

    public static String getFullName(String firstName, String lastName) {
        String fullName = "";
        if(!isEmptyString(firstName))
            fullName += firstName + " ";
        if(!isEmptyString(lastName))
            fullName += lastName;

        return fullName;
    }

    public static String getDateTimeText(long milliseconds) {
        SimpleDateFormat formatter = new SimpleDateFormat("d.M.yyyy HH:mm", Locale.ENGLISH);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds);
        return formatter.format(calendar.getTime());
    }

    public static String getTimeLeftText(long millisUntilFinished) {
        String timeText;
        long time = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
        long hours = time / 60;
        long minutes = time % 60;
        if(TimeUnit.MILLISECONDS.toHours(millisUntilFinished) > 0) {
            timeText = hours + "t "
                    + minutes + "min "
                    + AppRes.getContext().getString(R.string.map_time_left);
        } else {
            timeText = time + "min "
                    + AppRes.getContext().getString(R.string.map_time_left);
        }

        return timeText;
    }

    public static String getShareOptionText(long millisToShare) {
        Context ctx = AppRes.getContext();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millisToShare);
        long hours = TimeUnit.MILLISECONDS.toHours(millisToShare);
        long days = TimeUnit.MILLISECONDS.toDays(millisToShare);

        if(days > 0) {
           return days + " " + ctx.getString(days == 1 ? R.string.share_time_day : R.string.share_time_days);
        } else if(hours > 0){
            return hours + " " + ctx.getString(hours == 1 ? R.string.share_time_hour : R.string.share_time_hours);
        } else if(minutes > 0) {
            return minutes + " " + ctx.getString(minutes == 1 ? R.string.share_time_minute : R.string.share_time_minutes);
        }
        return "";
    }

    public static String getShareUntilText(long millisToShare) {
        Context ctx = AppRes.getContext();
        long now = System.currentTimeMillis();
        long endTime = now + millisToShare;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        int todayDate = cal.get(Calendar.DAY_OF_WEEK);
        cal.setTimeInMillis(endTime);
        int endDate = cal.get(Calendar.DAY_OF_WEEK);

        int minutes = cal.get(Calendar.MINUTE);
        int hours = cal.get(Calendar.HOUR_OF_DAY);

        String timeText = "";
        timeText += hours < 10 ? "0" + hours : hours;
        timeText += ":";
        timeText += minutes < 10 ? "0" + minutes : minutes;

        if(todayDate == endDate) {
            return ctx.getString(R.string.share_time_until) + " " + timeText;
        } else {
            String dateText = "";
            int date = cal.get(Calendar.DAY_OF_WEEK);
            if(date == 1)
                dateText = ctx.getString(R.string.sunday);
            else if(date == 2)
                dateText = ctx.getString(R.string.monday);
            else if(date == 3)
                dateText = ctx.getString(R.string.tuesday);
            else if(date == 4)
                dateText = ctx.getString(R.string.wednesday);
            else if(date == 5)
                dateText = ctx.getString(R.string.thursday);
            else if(date == 6)
                dateText = ctx.getString(R.string.friday);
            else if(date == 7)
                dateText = ctx.getString(R.string.saturday);
            return ctx.getString(R.string.share_time_until) + " " + dateText + " " + timeText;
        }

    }

    public static String getDistanceText(LiveLatLng targetLatLng) {
        AppRes appRes = (AppRes) AppRes.getContext();
        LiveLatLng location = appRes.getLocation();
        String distanceText;
        float[] results = new float[1];
        Location.distanceBetween(location.latitude, location.longitude, targetLatLng.latitude, targetLatLng.longitude, results);
        long meters = 10 * Math.round(results[0] / 10);
        if (meters < 1000) {
            if(meters < 10)
                distanceText = ">10m";
            else
                distanceText = meters + "m";
        } else {
            distanceText = format(Locale.ENGLISH, "%.1f", (float) meters / 1000) + "km";
        }
        return distanceText;
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String html){
        Spanned result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(html);
        }
        return result;
    }
}
