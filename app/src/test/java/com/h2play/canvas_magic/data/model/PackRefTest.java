package com.h2play.canvas_magic.data.model;

import com.h2play.canvas_magic.data.model.response.ShapeInfo;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class PackRefTest {
    @Test public void customPackSurvivesListReordering() {
        PackRef ref = PackRef.custom("file7.txt");
        assertEquals(1, ref.findCustomIndex(Arrays.asList(
                new ShapeInfo("file2.txt", "First", 9),
                new ShapeInfo("file7.txt", "Chosen", 9))));
        assertEquals(0, ref.findCustomIndex(Arrays.asList(
                new ShapeInfo("file7.txt", "Chosen", 9),
                new ShapeInfo("file2.txt", "First", 9))));
    }

    @Test public void deletedCustomPackDoesNotPointAtAnotherIndex() {
        assertEquals(-1, PackRef.custom("deleted.txt").findCustomIndex(Arrays.asList(
                new ShapeInfo("other.txt", "Other", 9))));
    }
}
