package com.jonlee.android.SJplayer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import androidx.core.content.ContextCompat;
import android.util.Log;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility helper to retrieve device location, embed geotags in recordings,
 * and reverse geocode coordinates into friendly place names for blind users.
 */
public class LocationHelper {
    private static final String TAG = "LocationHelper";
    private static final Pattern ISO_6709_PATTERN = Pattern.compile("([+-]\\d+(?:\\.\\d+)?)([+-]\\d+(?:\\.\\d+)?)");

    public static boolean isLocationPermissionGranted(Context context) {
        if (context == null) return false;
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static Location getLastKnownLocation(Context context) {
        if (!isLocationPermissionGranted(context)) {
            return null;
        }

        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) return null;

            List<String> providers = locationManager.getProviders(true);
            Location bestLocation = null;

            for (String provider : providers) {
                Location l = locationManager.getLastKnownLocation(provider);
                if (l == null) continue;
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy() || l.getTime() > bestLocation.getTime()) {
                    bestLocation = l;
                }
            }
            return bestLocation;
        } catch (SecurityException e) {
            Log.w(TAG, "Location permission missing: " + e.getMessage());
            return null;
        } catch (Exception e) {
            Log.w(TAG, "Error getting location: " + e.getMessage());
            return null;
        }
    }

    public static double[] parseIso6709Location(String iso6709) {
        if (iso6709 == null || iso6709.trim().isEmpty()) {
            return null;
        }
        try {
            Matcher matcher = ISO_6709_PATTERN.matcher(iso6709);
            if (matcher.find()) {
                double lat = Double.parseDouble(matcher.group(1));
                double lng = Double.parseDouble(matcher.group(2));
                return new double[]{lat, lng};
            }
        } catch (Exception e) {
            Log.w(TAG, "Error parsing ISO 6709 location: " + iso6709, e);
        }
        return null;
    }

    public static String getPlaceName(Context context, double lat, double lng) {
        if (context == null) return null;
        try {
            if (Geocoder.isPresent()) {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    if (addr.getLocality() != null) {
                        sb.append(addr.getLocality());
                    } else if (addr.getSubAdminArea() != null) {
                        sb.append(addr.getSubAdminArea());
                    }
                    if (addr.getAdminArea() != null) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(addr.getAdminArea());
                    }
                    if (sb.length() > 0) {
                        return sb.toString();
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Geocoder lookup failed: " + e.getMessage());
        }
        return String.format(Locale.US, "%.2f, %.2f", lat, lng);
    }
}
