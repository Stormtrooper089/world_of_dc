package org.dcoffice.cachar.util;


import com.uber.h3core.H3Core;
import java.io.IOException;

public class H3Util {

    private static final H3Core h3;

    static {
        try {
            h3 = H3Core.newInstance();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize H3", e);
        }
    }

    public static String getH3Index(double lat, double lon, int resolution) {
        long h3Index = h3.latLngToCell(lat, lon, resolution);
        return h3.h3ToString(h3Index);
    }
}
