package com.prym.backend.unit.util;

import com.prym.backend.util.DistanceUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DistanceUtilTest {

    private static final double DELTA = 5.0; // ±5 miles tolerance for real-world coordinates

    // ── calculateDistance ──────────────────────────────────────────────────

    @Test
    void calculateDistance_SamePoint_ReturnsZero() {
        double dist = DistanceUtil.calculateDistance(43.6532, -79.3832, 43.6532, -79.3832);
        assertEquals(0.0, dist, 0.0001);
    }

    @Test
    void calculateDistance_NewYorkToLosAngeles_IsApproximatelyCorrect() {
        // NYC (40.7128, -74.0060) → LA (34.0522, -118.2437) ≈ 2445 miles
        double dist = DistanceUtil.calculateDistance(40.7128, -74.0060, 34.0522, -118.2437);
        assertEquals(2445.0, dist, DELTA);
    }

    @Test
    void calculateDistance_TorontoToMontreal_IsApproximatelyCorrect() {
        // Toronto (43.6532, -79.3832) → Montreal (45.5017, -73.5673) ≈ 313 miles
        double dist = DistanceUtil.calculateDistance(43.6532, -79.3832, 45.5017, -73.5673);
        assertEquals(313.0, dist, DELTA);
    }

    @Test
    void calculateDistance_IsSymmetric() {
        double aToB = DistanceUtil.calculateDistance(40.7128, -74.0060, 34.0522, -118.2437);
        double bToA = DistanceUtil.calculateDistance(34.0522, -118.2437, 40.7128, -74.0060);
        assertEquals(aToB, bToA, 0.0001);
    }

    @Test
    void calculateDistance_ReturnsNonNegative() {
        double dist = DistanceUtil.calculateDistance(0, 0, 1, 1);
        assertTrue(dist >= 0.0);
    }

    @Test
    void calculateDistance_AcrossEquator_IsPositive() {
        // Point in northern hemisphere to point in southern hemisphere
        double dist = DistanceUtil.calculateDistance(10.0, 0.0, -10.0, 0.0);
        assertTrue(dist > 0.0);
        assertEquals(1381.0, dist, DELTA); // ~1381 miles for 20 degrees latitude
    }

    // ── formatDistance ─────────────────────────────────────────────────────

    @Test
    void formatDistance_LessThanOneMile_ReturnsLessThanString() {
        assertEquals("< 1 mi", DistanceUtil.formatDistance(0.0));
        assertEquals("< 1 mi", DistanceUtil.formatDistance(0.5));
        assertEquals("< 1 mi", DistanceUtil.formatDistance(0.99));
    }

    @Test
    void formatDistance_ExactlyOneMile_FormatsWithOneDecimal() {
        assertEquals("1.0 mi", DistanceUtil.formatDistance(1.0));
    }

    @Test
    void formatDistance_LargeDistance_FormatsWithOneDecimal() {
        assertEquals("15.3 mi", DistanceUtil.formatDistance(15.3));
        assertEquals("100.0 mi", DistanceUtil.formatDistance(100.0));
        assertEquals("2445.0 mi", DistanceUtil.formatDistance(2445.0));
    }

    @Test
    void formatDistance_JustBelowOneMile_StillReturnsLessThanString() {
        assertEquals("< 1 mi", DistanceUtil.formatDistance(0.999));
    }

    // ── formatDistanceWithMax ──────────────────────────────────────────────

    @Test
    void formatDistanceWithMax_LessThanOneMile_ReturnsLessThanString() {
        assertEquals("< 1 mi", DistanceUtil.formatDistanceWithMax(0.5, 500.0));
    }

    @Test
    void formatDistanceWithMax_BelowMax_FormatsNormally() {
        assertEquals("50.0 mi", DistanceUtil.formatDistanceWithMax(50.0, 500.0));
        assertEquals("1.5 mi", DistanceUtil.formatDistanceWithMax(1.5, 100.0));
    }

    @Test
    void formatDistanceWithMax_ExceedsMax_ShowsPlusSuffix() {
        assertEquals("500+ mi", DistanceUtil.formatDistanceWithMax(600.0, 500.0));
        assertEquals("100+ mi", DistanceUtil.formatDistanceWithMax(150.0, 100.0));
    }

    @Test
    void formatDistanceWithMax_ExactlyAtMax_FormatsNormally() {
        // Exactly at the max should NOT show the "+" suffix
        assertEquals("500.0 mi", DistanceUtil.formatDistanceWithMax(500.0, 500.0));
    }

    @Test
    void formatDistanceWithMax_SmallMax_LessThanOneMileStillUsesLessThan() {
        assertEquals("< 1 mi", DistanceUtil.formatDistanceWithMax(0.3, 0.5));
    }
}
