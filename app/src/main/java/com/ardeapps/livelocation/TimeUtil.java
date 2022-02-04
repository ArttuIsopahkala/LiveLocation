package com.ardeapps.livelocation;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by Arttu on 20.9.2017.
 */

public class TimeUtil {
    /** returns current time in milliseconds */
    public static long getLoactionSharedOnceTime() {
        return 0;
        //return System.currentTimeMillis();
    }

    /** returns one month in milliseconds */
    public static long getLocationSharedForeverTime() {
        return 2629746000L;
    }

    /** returns share time options to share */
    public static ArrayList<Long> getShareTimes() {
        ArrayList<Long> shareTimes = new ArrayList<>();
        shareTimes.add(TimeUnit.MINUTES.toMillis(15));
        shareTimes.add(TimeUnit.MINUTES.toMillis(30));
        shareTimes.add(TimeUnit.MINUTES.toMillis(45));
        shareTimes.add(TimeUnit.HOURS.toMillis(1));
        shareTimes.add(TimeUnit.HOURS.toMillis(2));
        shareTimes.add(TimeUnit.HOURS.toMillis(3));
        shareTimes.add(TimeUnit.HOURS.toMillis(4));
        shareTimes.add(TimeUnit.HOURS.toMillis(5));
        shareTimes.add(TimeUnit.HOURS.toMillis(6));
        shareTimes.add(TimeUnit.HOURS.toMillis(7));
        shareTimes.add(TimeUnit.HOURS.toMillis(8));
        shareTimes.add(TimeUnit.HOURS.toMillis(9));
        shareTimes.add(TimeUnit.HOURS.toMillis(10));
        shareTimes.add(TimeUnit.HOURS.toMillis(11));
        shareTimes.add(TimeUnit.HOURS.toMillis(12));
        shareTimes.add(TimeUnit.DAYS.toMillis(1));
        shareTimes.add(TimeUnit.DAYS.toMillis(2));
        shareTimes.add(TimeUnit.DAYS.toMillis(3));
        return shareTimes;
    }
}
