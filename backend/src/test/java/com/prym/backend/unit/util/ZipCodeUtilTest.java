package com.prym.backend.unit.util;

import com.prym.backend.util.ZipCodeUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for ZipCodeUtil static helper methods.
 * No network calls are made — only format detection, normalization, and validation are tested here.
 * API-calling methods (lookupPostalCode, getCoordinates) are covered by integration tests.
 */
public class ZipCodeUtilTest {

    // ─────────────────────────────────────────────────────────────────
    // isValidZip — format validation
    // ─────────────────────────────────────────────────────────────────

    // Test 1: US 5-digit ZIP is valid
    @Test
    void isValidZip_UsZip_FiveDigits_ReturnsTrue() {
        assertTrue(ZipCodeUtil.isValidZip("10001"));
        assertTrue(ZipCodeUtil.isValidZip("90210"));
        assertTrue(ZipCodeUtil.isValidZip("00501")); // lowest valid US ZIP
    }

    // Test 2: US ZIP+4 extension is NOT matched by the 5-digit rule — but matches the generic rule
    @Test
    void isValidZip_UsZipPlusFour_ReturnsTrue() {
        // "10001-1234" = 10 chars, alphanumeric+dash → matches generic 3-10 rule
        assertTrue(ZipCodeUtil.isValidZip("10001-1234"));
    }

    // Test 3: Canadian A1A 1A1 format (with space) is valid
    @Test
    void isValidZip_CanadianWithSpace_ReturnsTrue() {
        assertTrue(ZipCodeUtil.isValidZip("M5V 2T6")); // Toronto
        assertTrue(ZipCodeUtil.isValidZip("K1A 0A9")); // Ottawa
        assertTrue(ZipCodeUtil.isValidZip("V6B 4N7")); // Vancouver
    }

    // Test 4: Canadian A1A1A1 format (no space) is also valid
    @Test
    void isValidZip_CanadianNoSpace_ReturnsTrue() {
        assertTrue(ZipCodeUtil.isValidZip("M5V2T6"));
        assertTrue(ZipCodeUtil.isValidZip("K1A0A9"));
    }

    // Test 5: UK postal code formats are valid
    @Test
    void isValidZip_UkPostalCode_ReturnsTrue() {
        assertTrue(ZipCodeUtil.isValidZip("SW1A 1AA")); // Westminster
        assertTrue(ZipCodeUtil.isValidZip("EC1A 1BB")); // City of London
        assertTrue(ZipCodeUtil.isValidZip("W1A 0AX")); // London W1
    }

    // Test 6: Generic 3-10 alphanumeric codes are accepted
    @Test
    void isValidZip_GenericAlphanumeric_ReturnsTrue() {
        assertTrue(ZipCodeUtil.isValidZip("12345"));   // 5 digits
        assertTrue(ZipCodeUtil.isValidZip("ABC"));     // 3 chars
        assertTrue(ZipCodeUtil.isValidZip("ABC12345")); // 8 alphanumeric
    }

    // Test 7: null input returns false
    @Test
    void isValidZip_Null_ReturnsFalse() {
        assertFalse(ZipCodeUtil.isValidZip(null));
    }

    // Test 8: empty string returns false
    @Test
    void isValidZip_EmptyString_ReturnsFalse() {
        assertFalse(ZipCodeUtil.isValidZip(""));
    }

    // Test 9: blank (spaces only) returns false
    @Test
    void isValidZip_BlankString_ReturnsFalse() {
        assertFalse(ZipCodeUtil.isValidZip("   "));
    }

    // Test 10: special characters not in the generic pattern are rejected
    @Test
    void isValidZip_SpecialCharacters_ReturnsFalse() {
        assertFalse(ZipCodeUtil.isValidZip("!@#$%"));
        assertFalse(ZipCodeUtil.isValidZip("ZIP!CODE"));
    }

    // Test 11: a 4-digit code like "1234" still matches the generic 3-10 rule
    @Test
    void isValidZip_FourDigits_ReturnsTrue() {
        // Australia uses 4-digit postal codes — should be accepted as generic
        assertTrue(ZipCodeUtil.isValidZip("2000")); // Sydney
    }

    // Test 12: 11-character code (too long) is rejected by generic rule
    @Test
    void isValidZip_TooLong_ReturnsFalse() {
        assertFalse(ZipCodeUtil.isValidZip("12345678901")); // 11 chars
    }

    // Test 13: 2-character code (too short for generic) is rejected
    @Test
    void isValidZip_TooShort_ReturnsFalse() {
        assertFalse(ZipCodeUtil.isValidZip("AB")); // only 2 chars, below minimum of 3
    }

    // ─────────────────────────────────────────────────────────────────
    // normalizePostalCode — strips spaces & uppercases
    // ─────────────────────────────────────────────────────────────────

    // Test 14: Canadian code with space → normalized to no-space uppercase
    @Test
    void normalizePostalCode_CanadianWithSpace_RemovesSpaceAndUppercases() {
        assertEquals("M5V2T6", ZipCodeUtil.normalizePostalCode("m5v 2t6"));
    }

    // Test 15: Lowercase input is uppercased
    @Test
    void normalizePostalCode_LowercaseInput_Uppercased() {
        assertEquals("SW1A1AA", ZipCodeUtil.normalizePostalCode("sw1a 1aa"));
    }

    // Test 16: Leading and trailing whitespace is trimmed
    @Test
    void normalizePostalCode_LeadingTrailingSpaces_Trimmed() {
        assertEquals("10001", ZipCodeUtil.normalizePostalCode("  10001  "));
    }

    // Test 17: Input with no spaces is returned as-is (uppercased)
    @Test
    void normalizePostalCode_NoSpaces_ReturnsUppercased() {
        assertEquals("M5V2T6", ZipCodeUtil.normalizePostalCode("M5V2T6"));
    }

    // Test 18: Multiple internal spaces all removed
    @Test
    void normalizePostalCode_MultipleSpaces_AllRemoved() {
        assertEquals("A1B2C3", ZipCodeUtil.normalizePostalCode("A 1 B 2 C 3"));
    }

    // Test 19: null input returns null
    @Test
    void normalizePostalCode_Null_ReturnsNull() {
        assertNull(ZipCodeUtil.normalizePostalCode(null));
    }

    // Test 20: empty string returns empty string (trimmed)
    @Test
    void normalizePostalCode_EmptyString_ReturnsEmpty() {
        assertEquals("", ZipCodeUtil.normalizePostalCode(""));
    }

    // ─────────────────────────────────────────────────────────────────
    // formatPostalCode — adds space to Canadian codes
    // ─────────────────────────────────────────────────────────────────

    // Test 21: 6-char Canadian code without space gets a space inserted at position 3
    @Test
    void formatPostalCode_CanadianNoSpace_AddsSpace() {
        assertEquals("M5V 2T6", ZipCodeUtil.formatPostalCode("M5V2T6"));
        assertEquals("K1A 0A9", ZipCodeUtil.formatPostalCode("K1A0A9"));
    }

    // Test 22: Canadian code already with space is trimmed and reformatted
    @Test
    void formatPostalCode_CanadianWithSpace_FormattedCorrectly() {
        // "M5V 2T6" → normalizes to "M5V2T6" → reformatted to "M5V 2T6"
        assertEquals("M5V 2T6", ZipCodeUtil.formatPostalCode("M5V 2T6"));
    }

    // Test 23: US 5-digit ZIP is returned unchanged (trimmed)
    @Test
    void formatPostalCode_UsZip_ReturnedUnchanged() {
        assertEquals("10001", ZipCodeUtil.formatPostalCode("10001"));
        assertEquals("90210", ZipCodeUtil.formatPostalCode("  90210  "));
    }

    // Test 24: UK postal code is returned as-is (no space inserted)
    @Test
    void formatPostalCode_UkCode_ReturnedAsIs() {
        assertEquals("SW1A 1AA", ZipCodeUtil.formatPostalCode("SW1A 1AA"));
    }

    // Test 25: null input returns null
    @Test
    void formatPostalCode_Null_ReturnsNull() {
        assertNull(ZipCodeUtil.formatPostalCode(null));
    }

    // Test 26: empty string returns empty string
    @Test
    void formatPostalCode_EmptyString_ReturnsEmpty() {
        assertEquals("", ZipCodeUtil.formatPostalCode(""));
    }

    // Test 27: blank string (only spaces) returns empty string
    @Test
    void formatPostalCode_BlankSpaces_ReturnsEmpty() {
        // Implementation does: if code.trim().isEmpty() return code — but code is " "
        // The blank guard returns the code as-is, which is "   ", trimmed is ""
        assertTrue(ZipCodeUtil.formatPostalCode("   ").isBlank());
    }

    // ─────────────────────────────────────────────────────────────────
    // lookupPostalCode — null/empty guard (no network calls)
    // ─────────────────────────────────────────────────────────────────

    // Test 28: null code returns null without any network call
    @Test
    void lookupPostalCode_Null_ReturnsNull() {
        ZipCodeUtil.LocationResult result = ZipCodeUtil.lookupPostalCode(null);
        assertNull(result, "null postal code must return null without a network call");
    }

    // Test 29: empty string returns null without any network call
    @Test
    void lookupPostalCode_EmptyString_ReturnsNull() {
        ZipCodeUtil.LocationResult result = ZipCodeUtil.lookupPostalCode("");
        assertNull(result, "empty postal code must return null");
    }

    // Test 30: whitespace-only string returns null without any network call
    @Test
    void lookupPostalCode_BlankString_ReturnsNull() {
        ZipCodeUtil.LocationResult result = ZipCodeUtil.lookupPostalCode("   ");
        assertNull(result, "blank postal code must return null");
    }

    // ─────────────────────────────────────────────────────────────────
    // getCoordinates — delegates to lookupPostalCode
    // ─────────────────────────────────────────────────────────────────

    // Test 31: null code → getCoordinates returns null
    @Test
    void getCoordinates_NullCode_ReturnsNull() {
        double[] coords = ZipCodeUtil.getCoordinates(null);
        assertNull(coords, "null code must return null coordinates");
    }

    // Test 32: empty code → getCoordinates returns null
    @Test
    void getCoordinates_EmptyCode_ReturnsNull() {
        double[] coords = ZipCodeUtil.getCoordinates("");
        assertNull(coords, "empty code must return null coordinates");
    }

    // ─────────────────────────────────────────────────────────────────
    // LocationResult — field access sanity check
    // ─────────────────────────────────────────────────────────────────

    // Test 33: LocationResult stores all fields correctly
    @Test
    void locationResult_StoresAllFields() {
        ZipCodeUtil.LocationResult loc =
                new ZipCodeUtil.LocationResult(43.7, -79.4, "Toronto", "Ontario", "Canada");

        assertEquals(43.7,     loc.latitude,  0.001);
        assertEquals(-79.4,    loc.longitude, 0.001);
        assertEquals("Toronto", loc.city);
        assertEquals("Ontario", loc.state);
        assertEquals("Canada",  loc.country);
    }

    // Test 34: LocationResult with null city/state is still valid
    @Test
    void locationResult_NullCityAndState_AllowedByConstructor() {
        ZipCodeUtil.LocationResult loc =
                new ZipCodeUtil.LocationResult(0.0, 0.0, null, null, "Unknown");

        assertNull(loc.city);
        assertNull(loc.state);
        assertEquals("Unknown", loc.country);
    }

    // ─────────────────────────────────────────────────────────────────
    // Edge cases for normalizePostalCode
    // ─────────────────────────────────────────────────────────────────

    // Test 35: already-normalized uppercase no-space code is returned unchanged
    @Test
    void normalizePostalCode_AlreadyNormalized_Unchanged() {
        assertEquals("10001",   ZipCodeUtil.normalizePostalCode("10001"));
        assertEquals("M5V2T6",  ZipCodeUtil.normalizePostalCode("M5V2T6"));
        assertEquals("SW1A1AA", ZipCodeUtil.normalizePostalCode("SW1A1AA"));
    }

    // Test 36: mixed-case US ZIP (edge case) is uppercased (digits stay digits)
    @Test
    void normalizePostalCode_DigitsOnlyNotAffectedByUppercase() {
        assertEquals("10001", ZipCodeUtil.normalizePostalCode("10001"));
    }

    // ─────────────────────────────────────────────────────────────────
    // isValidZip — boundary checks
    // ─────────────────────────────────────────────────────────────────

    // Test 37: exactly 3 alphanumeric characters (lower bound of generic rule) is valid
    @Test
    void isValidZip_ThreeChars_LowerBoundValid() {
        assertTrue(ZipCodeUtil.isValidZip("ABC"));
        assertTrue(ZipCodeUtil.isValidZip("123"));
    }

    // Test 38: exactly 10 alphanumeric characters (upper bound) is valid
    @Test
    void isValidZip_TenChars_UpperBoundValid() {
        assertTrue(ZipCodeUtil.isValidZip("ABCDE12345")); // exactly 10
    }

    // Test 39: digits-only 6-char code (like Australian) is accepted as generic
    @Test
    void isValidZip_SixDigitNumeric_AcceptedAsGeneric() {
        assertTrue(ZipCodeUtil.isValidZip("123456"));
    }

    // Test 40: Canadian code without leading letter (invalid pattern) falls through to generic
    @Test
    void isValidZip_InvalidCanadianPattern_FallsToGeneric() {
        // "1A11A1" does not match Canadian pattern (must start with letter)
        // but matches generic 3-10 alphanumeric
        assertTrue(ZipCodeUtil.isValidZip("1A11A1"));
    }
}