package com.ardeapps.livelocation;

import android.util.Log;
import android.widget.Toast;

import static android.R.id.message;

/**
 * Created by Arttu on 21.8.2017.
 */

public class Logger {
    public static void log(Object message) {
        String className = new Exception().getStackTrace()[1].getClassName();
        Log.e(className, message+"");
    }
    public static void toast(Object message) {
        Toast.makeText(AppRes.getContext(), message+"", Toast.LENGTH_LONG).show();
    }
    public static void toast(int resourceId) {
        Toast.makeText(AppRes.getContext(), resourceId, Toast.LENGTH_LONG).show();
    }
}
