package kr.ac.ssu.infocom.opencv_contrib_test;

/**
 * Created by park on 2018-09-23.
 */

public class Utils {
    public static void addLineToSB(StringBuffer sb, String name, Object value) {
        if (sb == null) return;
        sb.
                append((name == null || "".equals(name)) ? "" : name + ": ").
                append(value == null ? "" : value + "").
                append("\n");
    }
}
