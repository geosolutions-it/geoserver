/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.logging.Logger;
import org.geoserver.mapml.TaggedPolygon.TaggedCoordinateSequence;
import org.geoserver.mapml.TaggedPolygon.TaggedLineString;
import org.geotools.util.logging.Logging;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

/**
 * Checks the polygon tagger is working as expected. For the moment it's not testing the coordinate
 * values as there is no written spec of how they should appear, interactive tests have so far
 * proven inconclusive.
 */
public class MapMLGeometryClipperTest {

    static final Boolean QUIET_TESTS = Boolean.getBoolean("quietTests");

    static final Logger LOGGER = Logging.getLogger(MapMLGeometryClipperTest.class);

    private Polygon world;

    @Before
    public void setupTestGeometries() throws Exception {
        world = getPolygon("POLYGON((-180 -90, -180 90, 180 90, 180 -90, -180 -90))");
    }

    private static Polygon getPolygon(String wkt) throws ParseException {
        return (Polygon) new WKTReader().read(wkt);
    }

    @Test
    public void testWorldHalfWest() throws Exception {
        TaggedPolygon tagged = getTaggedPolygon(new Envelope(-180, 0, -90, 90));

        // no holes
        assertEquals(0, tagged.getHoles().size());
        // the invisible segment is fully inside the invisible CS
        TaggedLineString ls = tagged.getBoundary();
        List<TaggedCoordinateSequence> coordinates = ls.getCoordinates();
        assertEquals(3, coordinates.size());
        //        assertCoordinates(coordinates.get(0), true, -180, -90);
        //        assertCoordinates(coordinates.get(1), false, 0, -90, 0, 90);
        //        assertCoordinates(coordinates.get(2), true, -180, 90, -180, -90);
    }

    @Test
    public void testWorldHalfEast() throws Exception {
        TaggedPolygon tagged = getTaggedPolygon(new Envelope(0, 180, -90, 90));

        // no holes
        assertEquals(0, tagged.getHoles().size());
        // the invisible segment is fully inside the invisible CS
        TaggedLineString ls = tagged.getBoundary();
        List<TaggedCoordinateSequence> coordinates = ls.getCoordinates();
        assertEquals(2, coordinates.size());
        //        assertCoordinates(coordinates.get(0), true, 0, -90, 180, -90, 180, 90);
        //        assertCoordinates(coordinates.get(1), false, 0, 90, 0, -90);
    }

    @Test
    public void testWorldAllInvisible() throws Exception {
        TaggedPolygon tagged = getTaggedPolygon(new Envelope(-45, 45, -45, 45));

        // no holes
        assertEquals(0, tagged.getHoles().size());
        // everything is invisible
        TaggedLineString ls = tagged.getBoundary();
        List<TaggedCoordinateSequence> coordinates = ls.getCoordinates();
        assertEquals(1, coordinates.size());
        //        assertCoordinates(coordinates.get(0), false, -45, -45, 45, -45, 45, 45, -45, 45,
        // -45, -45);
    }

    @Test
    public void testWorldTopLeftCorner() throws Exception {
        TaggedPolygon tagged = getTaggedPolygon(new Envelope(-180, 0, 0, 90));

        // no holes
        assertEquals(0, tagged.getHoles().size());
        // two and two
        TaggedLineString ls = tagged.getBoundary();
        List<TaggedCoordinateSequence> coordinates = ls.getCoordinates();
        assertEquals(2, coordinates.size());
        //        assertCoordinates(coordinates.get(0), false, -180, 0, 0, 0, 0, 90);
        //        assertCoordinates(coordinates.get(1), true, -180, 90, -180, 0);
    }

    protected void assertCoordinates(
            TaggedCoordinateSequence cs, boolean visible, double... expected) {
        assertEquals(visible, cs.isVisible());
        List<Coordinate> coordinates = cs.getCoordinates();
        assertEquals(expected.length / 2, coordinates.size());
        for (int i = 0; i < expected.length; i += 2) {
            assertEquals(expected[i], coordinates.get(i / 2).getX(), 1e-6);
            assertEquals(expected[i + 1], coordinates.get(i / 2).getY(), 1e-6);
        }
    }

    private TaggedPolygon getTaggedPolygon(Envelope clipEnvelope) {
        MapMLGeometryClipper tagger = new MapMLGeometryClipper(world, clipEnvelope);
        Geometry geometry = tagger.clipAndTag();
        if (!(geometry.getUserData() instanceof TaggedPolygon)) return null;
        TaggedPolygon tagged = (TaggedPolygon) geometry.getUserData();
        if (!QUIET_TESTS) {
            LOGGER.info("Tagged polygon: " + tagged);
        }
        return tagged;
    }
}
