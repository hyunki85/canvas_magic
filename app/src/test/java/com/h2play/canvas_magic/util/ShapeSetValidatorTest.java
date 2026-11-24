package com.h2play.canvas_magic.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShapeSetValidatorTest {
    private static final String VALID = "{\"shapes\":[" +
            "[{\"action\":\"down\",\"x\":0.1,\"y\":0.2},{\"action\":\"up\",\"x\":0.2,\"y\":0.3}]," +
            "[],[],[],[],[]]}";

    @Test public void acceptsNormalizedAllowedSet() {
        assertTrue(ShapeSetValidator.isValidCommunitySet(VALID, 6));
    }

    @Test public void rejectsDeclaredCountMismatch() {
        assertFalse(ShapeSetValidator.isValidCommunitySet(VALID, 9));
    }

    @Test public void rejectsOutOfBoundsOrNonFiniteCoordinates() {
        assertFalse(ShapeSetValidator.isValidCommunitySet(
                VALID.replace("0.1", "1.1"), 6));
        assertFalse(ShapeSetValidator.isValidCommunitySet(
                VALID.replace("0.1", "NaN"), 6));
    }

    @Test public void rejectsUnknownAction() {
        assertFalse(ShapeSetValidator.isValidCommunitySet(
                VALID.replace("down", "script"), 6));
    }
}
