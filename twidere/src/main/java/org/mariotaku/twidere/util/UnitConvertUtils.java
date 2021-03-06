package org.mariotaku.twidere.util;

/**
 * Created by mariotaku on 16/2/4.
 */
public class UnitConvertUtils {

    public static final String[] fileSizeUnits = {"bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB",
            "YB"};

    public static final String[] countUnits = {null, "K", "M", "B"};

    public static String calculateProperSize(double bytes) {
        double value = bytes;
        int index;
        for (index = 0; index < fileSizeUnits.length; index++) {
            if (value < 1024) {
                break;
            }
            value = value / 1024;
        }
        return String.format("%.2f %s", value, fileSizeUnits[index]);
    }

    public static String calculateProperCount(long count) {
        if (count < 1000) {
            return String.valueOf(count);
        }
        double value = count;
        int index;
        for (index = 0; index < countUnits.length; index++) {
            if (value < 1000) {
                break;
            }
            value = value / 1000.0;
        }
        return String.format("%.2f %s", value, countUnits[index]);
    }
}
