package com.prym.backend.util;

/**
 * Utility class for calculating distances between geographic coordinates.
 * Uses the Haversine formula to calculate great-circle distances.
 */
public class DistanceUtil {

    /**
     * Earth's radius in miles.
     * Used for Haversine formula to calculate distances in miles.
     */
    private static final double EARTH_RADIUS_MILES = 3958.8;

    /**
     * Calculates the distance in miles between two geographic points
     * using the Haversine formula.
     *
     * The Haversine formula determines the great-circle distance between
     * two points on a sphere given their longitudes and latitudes.
     *
     * @param lat1 Latitude of first point in decimal degrees
     * @param lon1 Longitude of first point in decimal degrees
     * @param lat2 Latitude of second point in decimal degrees
     * @param lon2 Longitude of second point in decimal degrees
     * @return Distance in miles
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert latitude and longitude from degrees to radians
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);

        // Haversine formula
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Distance in miles
        return EARTH_RADIUS_MILES * c;
    }

    /**
     * Formats a distance value as a human-readable string.
     * Distances less than 1 mile are shown as "< 1 mi".
     * Other distances are rounded to 1 decimal place.
     *
     * @param miles Distance in miles
     * @return Formatted string (e.g., "15.3 mi" or "< 1 mi")
     */
    public static String formatDistance(double miles) {
        if (miles < 1.0) {
            return "< 1 mi";
        }
        return String.format("%.1f mi", miles);
    }

    /**
     * Formats a distance value as a human-readable string with a maximum.
     * Useful for displaying "500+ mi" for very distant locations.
     *
     * @param miles Distance in miles
     * @param maxDisplay Maximum distance to show before displaying "X+ mi"
     * @return Formatted string (e.g., "15.3 mi", "< 1 mi", or "500+ mi")
     */
    public static String formatDistanceWithMax(double miles, double maxDisplay) {
        if (miles < 1.0) {
            return "< 1 mi";
        }
        if (miles > maxDisplay) {
            return String.format("%.0f+ mi", maxDisplay);
        }
        return String.format("%.1f mi", miles);
    }
}
