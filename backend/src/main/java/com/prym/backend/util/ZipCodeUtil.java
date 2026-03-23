package com.prym.backend.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for ZIP/postal code validation and coordinate lookup.
 * Uses the Nominatim (OpenStreetMap) geocoding API to resolve ANY postal code worldwide.
 */
public class ZipCodeUtil {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";

    /**
     * Result object holding resolved location data from Nominatim.
     */
    public static class LocationResult {
        public final double latitude;
        public final double longitude;
        public final String city;
        public final String state;
        public final String country;

        public LocationResult(double latitude, double longitude, String city, String state, String country) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.city = city;
            this.state = state;
            this.country = country;
        }
    }

    /**
     * Looks up a postal code via the Nominatim API and returns location data.
     *
     * @param code ZIP or postal code (any country)
     * @return LocationResult with lat/lng/city/state/country, or null if not found
     */
    public static LocationResult lookupPostalCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String normalized = normalizePostalCode(code);

        try {
            String encodedCode = URLEncoder.encode(normalized, StandardCharsets.UTF_8);
            String requestUrl = NOMINATIM_URL
                    + "?postalcode=" + encodedCode
                    + "&format=json"
                    + "&addressdetails=1"
                    + "&limit=1";

            HttpURLConnection conn = (HttpURLConnection) new URL(requestUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "PRYM-App/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status != 200) {
                return null;
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            String json = response.toString().trim();

            // Empty array means no results
            if (json.equals("[]")) {
                return null;
            }

            // Minimal JSON parsing without external library
            // Extract first result from the array
            String firstResult = json.substring(1); // remove leading [

            double lat = parseJsonDouble(firstResult, "\"lat\":");
            double lon = parseJsonDouble(firstResult, "\"lon\":");

            // Extract address details
            String address = extractJsonObject(firstResult, "\"address\":");
            String city = extractJsonString(address, "\"city\":");
            if (city == null) city = extractJsonString(address, "\"town\":");
            if (city == null) city = extractJsonString(address, "\"village\":");
            if (city == null) city = extractJsonString(address, "\"municipality\":");
            String state = extractJsonString(address, "\"state\":");
            String country = extractJsonString(address, "\"country\":");

            return new LocationResult(lat, lon, city, state, country);

        } catch (Exception e) {
            System.err.println("Nominatim lookup failed for code: " + normalized + " — " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets the latitude and longitude for a given ZIP/postal code.
     * Delegates to Nominatim API.
     *
     * @param code ZIP or postal code
     * @return double array {latitude, longitude} or null if not found
     */
    public static double[] getCoordinates(String code) {
        LocationResult result = lookupPostalCode(code);
        if (result == null) {
            return null;
        }
        return new double[]{result.latitude, result.longitude};
    }

    /**
     * Validates if a ZIP/postal code format is acceptable.
     * Checks format only — actual existence is verified by the Nominatim lookup.
     *
     * @param code ZIP or postal code to validate
     * @return true if format looks like a valid postal code
     */
    public static boolean isValidZip(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }

        String trimmed = code.trim();

        // US ZIP: exactly 5 digits
        if (trimmed.matches("^\\d{5}$")) {
            return true;
        }

        // Canadian postal code: A1A 1A1 or A1A1A1
        if (trimmed.matches("^[A-Za-z]\\d[A-Za-z]\\s?\\d[A-Za-z]\\d$")) {
            return true;
        }

        // UK postal codes (e.g., SW1A 1AA, EC1A 1BB)
        if (trimmed.matches("^[A-Za-z]{1,2}\\d[A-Za-z\\d]?\\s?\\d[A-Za-z]{2}$")) {
            return true;
        }

        // Generic: 3-10 alphanumeric characters (covers most international postal codes)
        if (trimmed.matches("^[A-Za-z0-9\\s\\-]{3,10}$")) {
            return true;
        }

        return false;
    }

    /**
     * Normalizes a postal code for lookup.
     * Removes extra spaces and converts to uppercase.
     */
    public static String normalizePostalCode(String code) {
        if (code == null) {
            return null;
        }
        return code.trim().replace(" ", "").toUpperCase();
    }

    /**
     * Formats a postal code for display.
     * Canadian: adds space in middle (A1A1A1 -> A1A 1A1)
     */
    public static String formatPostalCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return code;
        }

        String normalized = normalizePostalCode(code);

        // If it's a Canadian postal code (6 alphanumeric), add space
        if (normalized.matches("^[A-Z]\\d[A-Z]\\d[A-Z]\\d$")) {
            return normalized.substring(0, 3) + " " + normalized.substring(3);
        }
        return code.trim();
    }

    // --- Minimal JSON parsing helpers (no external dependency) ---

    private static double parseJsonDouble(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return 0;
        int start = idx + key.length();
        // skip whitespace and opening quote
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"')) start++;
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '"' && json.charAt(end) != '}') end++;
        return Double.parseDouble(json.substring(start, end).trim());
    }

    private static String extractJsonString(String json, String key) {
        if (json == null) return null;
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int start = json.indexOf('"', idx + key.length());
        if (start == -1) return null;
        int end = json.indexOf('"', start + 1);
        if (end == -1) return null;
        return json.substring(start + 1, end);
    }

    private static String extractJsonObject(String json, String key) {
        if (json == null) return null;
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int start = json.indexOf('{', idx);
        if (start == -1) return null;
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == '{') depth++;
            else if (json.charAt(i) == '}') depth--;
            if (depth == 0) return json.substring(start, i + 1);
        }
        return null;
    }
}
