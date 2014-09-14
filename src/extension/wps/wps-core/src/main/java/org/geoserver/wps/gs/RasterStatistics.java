package org.geoserver.wps.gs;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.config.GeoServer;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.GeometryBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.raster.RasterZonalStatistics;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * The process wraps JaiTools zonal-stats, providing statistics
 * (min,max,mean,std_dev) on the selected raster.
 **/

@DescribeProcess(title = "Raster Statisticss", description = "Provides raster statistics on the selected area.")
public class RasterStatistics implements GeoServerProcess {

        protected static final Logger LOGGER = Logging
                        .getLogger(RasterStatistics.class);

        protected GeoServer geoServer;

        protected Catalog catalog;

        protected FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

        protected GeometryBuilder geomBuilder = new GeometryBuilder();

        public static final String DEFAULT_TYPE_NAME = "RasterStatistics";

        public RasterStatistics(GeoServer geoServer) {
                this.geoServer = geoServer;
                this.catalog = geoServer.getCatalog();
        }

        @DescribeResult(name = "result", description = "List of attributes to be converted to a FeatureType")
        public SimpleFeatureCollection execute(
                        @DescribeParameter(name = "layerName", min = 0, description = "RasterAlgebra attribute layerName") String layerName,
                        @DescribeParameter(name = "areaOfInterest", min = 0, description = "RasterAlgebra attribute ROI") Geometry areaOfInterest,
                        @DescribeParameter(name = "aoiCRS", min = 0, description = "RasterAlgebra attribute ROI CRS") CoordinateReferenceSystem aoiCRS,
                        ProgressListener progressListener) throws ProcessException {

                Throwable cause = null;

                if (layerName != null) {
                        LayerInfo layerInfo = catalog.getLayerByName(layerName);
                        ResourceInfo resourceInfo = layerInfo.getResource();
                        StoreInfo storeInfo = resourceInfo.getStore();

                        if (storeInfo == null) {
                                cause = new IllegalArgumentException("Unable to locate coverage:" + resourceInfo.getName());
                                if (progressListener != null) {
                                        progressListener.exceptionOccurred(new ProcessException("Could not complete the Process", cause));
                                }
                                throw new ProcessException("Could not complete the Process", cause);
                        }

                        if (storeInfo instanceof CoverageStoreInfo) {
                                final CoverageStoreInfo coverageStore = (CoverageStoreInfo) storeInfo;
                                final CoverageInfo coverage = catalog.getCoverageByName(resourceInfo.getName());

                                if (coverageStore == null || coverage == null) {
                                        cause = new IllegalArgumentException("Unable to locate coverage:" + resourceInfo.getName());
                                        if (progressListener != null) {
                                                progressListener.exceptionOccurred(new ProcessException("Could not complete the Process", cause));
                                        }
                                        throw new ProcessException("Could not complete the Process", cause);
                                } else {
                                        RasterZonalStatistics statProcess = new RasterZonalStatistics();

                                        SimpleFeatureCollection features;
                                        try {
                                                GridCoverage2D gc = (GridCoverage2D) coverage.getGridCoverage(progressListener, null);
                                                CoordinateReferenceSystem crs = gc.getCoordinateReferenceSystem();

                                                SimpleFeatureCollection zones = null;

                                                if (areaOfInterest != null) {
                                                        if (areaOfInterest instanceof Polygon || areaOfInterest instanceof MultiPolygon) {
                                                                // build the feature type
                                                                List<Object> values = new LinkedList<Object>();
                                                                SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
                                                                tb.setName(DEFAULT_TYPE_NAME);
                                                                tb.add("the_geom", areaOfInterest.getClass(), crs);

                                                                if (aoiCRS == null) {
                                                                        if (areaOfInterest.getUserData() instanceof CoordinateReferenceSystem) {
                                                                                aoiCRS = (CoordinateReferenceSystem) areaOfInterest.getUserData();
                                                                        } else {
                                                                                // assume the geometry is in the same crs
                                                                                aoiCRS = crs;
                                                                        }
                                                                }

                                                                if (!CRS.equalsIgnoreMetadata(aoiCRS, crs)) {
                                                                        areaOfInterest = JTS.transform(areaOfInterest, CRS.findMathTransform(aoiCRS, crs, true));
                                                                }
                                                                values.add(areaOfInterest);

                                                                SimpleFeatureType schema = tb.buildFeatureType();

                                                                // build the feature
                                                                SimpleFeature sf = SimpleFeatureBuilder.build(schema, values, null);
                                                                zones = new ListFeatureCollection(schema);
                                                                ((ListFeatureCollection) zones).add(sf);
                                                        }
                                                }

                                                features = statProcess.execute(gc, null, zones, null);
                                                return features;
                                        } catch (Exception e) {
                                                cause = e;
                                                if (progressListener != null) {
                                                        progressListener.exceptionOccurred(new ProcessException("Could not complete the Process", cause));
                                                }
                                                throw new ProcessException("Could not complete the Process", cause);
                                        }
                                }
                        } else {
                                cause = new IllegalArgumentException("The layer is not a coverage");
                                if (progressListener != null) {
                                        progressListener.exceptionOccurred(new ProcessException("Could not complete the Process", cause));
                                }
                                throw new ProcessException("Could not complete the Process", cause);
                        }
                } else {
                        cause = new IllegalArgumentException("Layer name is null");
                        if (progressListener != null) {
                                progressListener.exceptionOccurred(new ProcessException("Could not complete the Process", cause));
                        }
                        throw new ProcessException("Could not complete the Process", cause);
                }
        }

}