/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.core;

import static java.util.Collections.emptyMap;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.LONG_LAT_LAYER;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.LONG_LAT_NO_GEOM_ON_THE_FLY_LAYER;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.LONG_LAT_QNAME;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.enableGeometryGenerationStrategy;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.filenameOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.IOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

public class GeometryGenerationFeatureSourceTest extends GeoServerSystemTestSupport {

    private FeatureTypeInfo featureTypeInfo = mock(FeatureTypeInfo.class);
    private SimpleFeatureSource delegate = mock(SimpleFeatureSource.class);
    private GeometryGenerationStrategy strategy = mock(GeometryGenerationStrategy.class);

    private GeometryGenerationFeatureSource featureSource =
            new GeometryGenerationFeatureSource(featureTypeInfo, delegate, strategy);

    @Before
    public void before() throws Exception {
        getGeoServer().reload();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        setupBasicLayer(testData);
        setupComplexLayer(testData);
    }

    private void setupBasicLayer(SystemTestData testData) throws IOException {
        testData.addVectorLayer(
                LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME,
                emptyMap(),
                filenameOf(LONG_LAT_NO_GEOM_ON_THE_FLY_LAYER),
                getClass(),
                getCatalog());
    }

    private void setupComplexLayer(SystemTestData testData) throws IOException {
        Catalog catalog = getCatalog();
        testData.addVectorLayer(
                LONG_LAT_QNAME, emptyMap(), filenameOf(LONG_LAT_LAYER), getClass(), catalog);
        FeatureTypeInfo featureTypeInfo = getFeatureTypeInfo(LONG_LAT_QNAME);
        enableGeometryGenerationStrategy(catalog, featureTypeInfo);
    }

    @Test
    public void testThatDefinesSchemaAtFirstUse() {
        // given
        SimpleFeatureType src = mock(SimpleFeatureType.class);
        given(delegate.getSchema()).willReturn(src);
        SimpleFeatureType defined = mock(SimpleFeatureType.class);
        given(strategy.defineGeometryAttributeFor(featureTypeInfo, src)).willReturn(defined);

        // when
        SimpleFeatureType schema = featureSource.getSchema();

        // then
        assertSame(schema, defined);
    }

    @Test
    public void testThatReturnsCachedSchemaForSubsequesntCalls() {
        // given
        SimpleFeatureType src = mock(SimpleFeatureType.class);
        given(delegate.getSchema()).willReturn(src);
        SimpleFeatureType defined = mock(SimpleFeatureType.class);
        given(strategy.defineGeometryAttributeFor(featureTypeInfo, src)).willReturn(defined);
        featureSource.getSchema();

        // when
        SimpleFeatureType schema = featureSource.getSchema();

        // then
        assertSame(schema, defined);
        verify(strategy, times(1)).defineGeometryAttributeFor(featureTypeInfo, src);
    }

    @Test
    public void testThatWrapsFeatureCollection() throws IOException {
        // given
        SimpleFeatureCollection collection = mock(SimpleFeatureCollection.class);
        given(delegate.getFeatures()).willReturn(collection);

        // when
        SimpleFeatureCollection features = featureSource.getFeatures();

        // then
        assertThat(features, CoreMatchers.instanceOf(GeometryGenerationFeatureCollection.class));
    }

    @Test
    public void testThatWrapsFeatureCollectionForQuery() throws IOException {
        // given
        Query srcQuery = mock(Query.class);
        Query query = mock(Query.class);
        given(strategy.convertQuery(featureTypeInfo, srcQuery)).willReturn(query);
        SimpleFeatureCollection collection = mock(SimpleFeatureCollection.class);
        given(delegate.getFeatures(query)).willReturn(collection);

        // when
        SimpleFeatureCollection features = featureSource.getFeatures(srcQuery);

        // then
        assertThat(features, CoreMatchers.instanceOf(GeometryGenerationFeatureCollection.class));
    }

    @Test
    public void testThatWrapsFeatureCollectionForFilter() throws IOException {
        // given
        Filter srcFilter = mock(Filter.class);
        Filter filter = mock(Filter.class);
        given(strategy.convertFilter(featureTypeInfo, srcFilter)).willReturn(filter);
        SimpleFeatureCollection collection = mock(SimpleFeatureCollection.class);
        given(delegate.getFeatures(filter)).willReturn(collection);

        // when
        SimpleFeatureCollection features = featureSource.getFeatures(srcFilter);

        // then
        assertThat(features, CoreMatchers.instanceOf(GeometryGenerationFeatureCollection.class));
    }

    @Test
    public void testThatCountsQueryResults() throws IOException {
        // given
        Query srcQuery = mock(Query.class);
        Query query = mock(Query.class);
        given(strategy.convertQuery(featureTypeInfo, srcQuery)).willReturn(query);
        given(delegate.getCount(query)).willReturn(17);

        // when
        int count = featureSource.getCount(srcQuery);

        // then
        assertThat(count, is(17));
    }

    @Test
    public void testThatBoundingBoxisCalculatedFromData() throws IOException {

        // when
        SimpleFeatureSource featureSource = getFeatureSource(LONG_LAT_QNAME);

        ReferencedEnvelope bounds = featureSource.getBounds();

        // then
        assertNotNull(bounds);

        assertTrue(bounds.getMaxX() == 1.0);
        assertTrue(bounds.getMaxY() == 1.0);
        assertTrue(bounds.getMinX() == -1.0);
        assertTrue(bounds.getMinY() == -1.0);

        ReferencedEnvelope boundsQuery = featureSource.getBounds(Query.ALL);

        // then
        assertNotNull(boundsQuery);

        assertTrue(boundsQuery.getMaxX() == 1.0);
        assertTrue(boundsQuery.getMaxY() == 1.0);
        assertTrue(boundsQuery.getMinX() == -1.0);
        assertTrue(boundsQuery.getMinY() == -1.0);
    }
}
