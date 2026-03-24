package com.prym.backend.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for ZIP/postal code validation and coordinate lookup.
 * Uses Zippopotam.us (US/CA) and Nominatim (fallback) to resolve postal codes worldwide.
 */
public class ZipCodeUtil {

    private static final String ZIPPOPOTAM_URL = "https://api.zippopotam.us";
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";

    /**
     * Result object holding resolved location data.
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
     * Looks up a postal code and returns location data.
     * Tries Zippopotam.us first (US/CA), then Nominatim as fallback.
     */
    public static LocationResult lookupPostalCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String trimmed = code.trim();
        String normalized = normalizePostalCode(trimmed);
        String countryCode = detectCountryCode(trimmed, normalized);

        //Try Zippopotam.us for US and Canadian codes
        if ("us".equals(countryCode) || "ca".equals(countryCode)) {
            LocationResult result = lookupViaZippopotam(normalized, countryCode);
            if (result != null) return result;
        }

        //Fallback: try Nominatim for any code
        return lookupViaNominatim(trimmed, normalized, countryCode);
    }

    /**
     *Lookup via Zippopotam.us API.
     *US: /us/{zip}  —  CA: /ca/{FSA} (first 3 chars of postal code)
     */
    private static LocationResult lookupViaZippopotam(String normalized, String countryCode) {
        try {
            String queryCode = normalized;
            //Canadian postal codes: Zippopotam uses the FSA (first 3 chars)
            if ("ca".equals(countryCode) && normalized.length() == 6) {
                queryCode = normalized.substring(0, 3);
            }

            String requestUrl = ZIPPOPOTAM_URL + "/" + countryCode + "/" + queryCode;

            HttpURLConnection conn = (HttpURLConnection) new URL(requestUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "PRYM-App/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                return null;
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }

            String json = response.toString().trim();
            if (json.isEmpty()) return null;

            //Parse Zippopotam response
            String country = extractJsonString(json, "\"country\":");
            String places = extractJsonArray(json, "\"places\":");
            if (places == null) return null;

            String firstPlace = places;
            String city = extractJsonString(firstPlace, "\"place name\":");
            String state = extractJsonString(firstPlace, "\"state\":");
            double lat = parseJsonDoubleFromString(firstPlace, "\"latitude\":");
            double lon = parseJsonDoubleFromString(firstPlace, "\"longitude\":");

            return new LocationResult(lat, lon, city, state, country);

        } catch (Exception e) {
            System.err.println("Zippopotam lookup failed: " + e.getMessage());
            return null;
        }
    }

    /**
     *Fallback lookup via Nominatim (OpenStreetMap) API.
     *Works for international postal codes.
     */
    private static LocationResult lookupViaNominatim(String trimmed, String normalized, String countryCode) {
        try {
            String encodedCode = URLEncoder.encode(trimmed, StandardCharsets.UTF_8);
            String requestUrl = NOMINATIM_URL
                    + "?postalcode=" + encodedCode
                    + "&format=json"
                    + "&addressdetails=1"
                    + "&limit=1"
                    + (countryCode != null ? "&countrycodes=" + countryCode : "");

            String json = httpGet(requestUrl);
            if (json == null) return null;

            //If postalcode search returned empty, try free-text search
            if (json.equals("[]")) {
                String fallbackUrl = NOMINATIM_URL
                        + "?q=" + encodedCode
                        + "&format=json"
                        + "&addressdetails=1"
                        + "&limit=1"
                        + (countryCode != null ? "&countrycodes=" + countryCode : "");
                json = httpGet(fallbackUrl);
            }

            if (json == null || json.equals("[]")) return null;

            //Parse Nominatim response (array of results)
            String firstResult = json.substring(1); //remove leading [

            double lat = parseJsonDouble(firstResult, "\"lat\":");
            double lon = parseJsonDouble(firstResult, "\"lon\":");

            String address = extractJsonObject(firstResult, "\"address\":");
            String city = extractJsonString(address, "\"city\":");
            if (city == null) city = extractJsonString(address, "\"town\":");
            if (city == null) city = extractJsonString(address, "\"village\":");
            if (city == null) city = extractJsonString(address, "\"municipality\":");
            String state = extractJsonString(address, "\"state\":");
            String country = extractJsonString(address, "\"country\":");

            return new LocationResult(lat, lon, city, state, country);

        } catch (Exception e) {
            System.err.println("Nominatim lookup failed: " + e.getMessage());
            return null;
        }
    }

    private static String httpGet(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "PRYM-App/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) return null;

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }
            return response.toString().trim();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     *Gets the latitude and longitude for a given ZIP/postal code.
     */
    public static double[] getCoordinates(String code) {
        LocationResult result = lookupPostalCode(code);
        if (result == null) return null;
        return new double[]{result.latitude, result.longitude};
    }

    /**
     *Validates if a ZIP/postal code format is acceptable.
     *Checks format only — actual existence is verified by the API lookup.
     */
    public static boolean isValidZip(String code) {
        if (code == null || code.trim().isEmpty()) return false;

        String trimmed = code.trim();

        //US ZIP: exactly 5 digits
        if (trimmed.matches("^\\d{5}$")) return true;

        //Canadian postal code: A1A 1A1 or A1A1A1
        if (trimmed.matches("^[A-Za-z]\\d[A-Za-z]\\s?\\d[A-Za-z]\\d$")) return true;

        //UK postal codes (e.g., SW1A 1AA, EC1A 1BB)
        if (trimmed.matches("^[A-Za-z]{1,2}\\d[A-Za-z\\d]?\\s?\\d[A-Za-z]{2}$")) return true;

        //Generic: 3-10 alphanumeric characters (covers most international postal codes)
        if (trimmed.matches("^[A-Za-z0-9\\s\\-]{3,10}$")) return true;

        return false;
    }

    /**
     *Normalizes a postal code for lookup.
     *Removes extra spaces and converts to uppercase.
     */
    public static String normalizePostalCode(String code) {
        if (code == null) return null;
        return code.trim().replace(" ", "").toUpperCase();
    }

    /**
     *Formats a postal code for display.
     *Canadian: adds space in middle (A1A1A1 -> A1A 1A1)
     */
    public static String formatPostalCode(String code) {
        if (code == null || code.trim().isEmpty()) return code;

        String normalized = normalizePostalCode(code);

        //If it's a Canadian postal code (6 alphanumeric), add space
        if (normalized.matches("^[A-Z]\\d[A-Z]\\d[A-Z]\\d$")) {
            return normalized.substring(0, 3) + " " + normalized.substring(3);
        }
        return code.trim();
    }

    /**
     *Detects likely country code from postal code format.
     */
    private static String detectCountryCode(String trimmed, String normalized) {
        if (trimmed.matches("^\\d{5}$")) return "us";
        if (trimmed.matches("^[A-Za-z]\\d[A-Za-z]\\s?\\d[A-Za-z]\\d$")) return "ca";
        if (trimmed.matches("^[A-Za-z]{1,2}\\d[A-Za-z\\d]?\\s?\\d[A-Za-z]{2}$")) return "gb";
        return null;
    }

    //JSON parsing helpers
    private static double parseJsonDouble(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return 0;
        int start = idx + key.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"')) start++;
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '"' && json.charAt(end) != '}') end++;
        return Double.parseDouble(json.substring(start, end).trim());
    }

    /** Parses a double that's stored as a JSON string value (e.g. "latitude": "43.71") */
    private static double parseJsonDoubleFromString(String json, String key) {
        String val = extractJsonString(json, key);
        if (val == null) return 0;
        return Double.parseDouble(val);
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

    private static String extractJsonArray(String json, String key) {
        if (json == null) return null;
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int start = json.indexOf('[', idx);
        if (start == -1) return null;
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == '[') depth++;
            else if (json.charAt(i) == ']') depth--;
            if (depth == 0) return json.substring(start, i + 1);
        }
        return null;
    }
}
