/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.GWC;
import org.geoserver.mapml.gwc.gridset.MapMLGridsets;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.api.geometry.MismatchedDimensionException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.referencing.CRS;

/** @author prushforth */
public class TiledCRSConstants {

    private static final HashMap<String, TiledCRSParams> BUILT_IN_TILED_CRS_DEFINITIONS =
            new HashMap<>();

    private static final HashMap<String, TiledCRSParams> BUILT_IN_TILED_CRS_BY_SRS_NAME =
            new HashMap<>();

    public static final HashMap<String, TiledCRSParams> tiledCRSDefinitions = new HashMap<>();

    public static final HashMap<String, TiledCRSParams> tiledCRSBySrsName = new HashMap<>();

    public static final HashMap<String, TiledCRS> BUILT_IN_TILED_CRS = new HashMap<>();

    /**
     * This map contains TiledCRS definitions in any variation (EPSG:CODE, MAPML:NAME, NAME,
     * URN:CODE) so that we can always retrieve the related TiledCRS no matter which
     * name/representation/alias is used for the lookup
     */
    public static final HashMap<String, TiledCRS> TILED_CRS = new HashMap<>();

    private static final Set<CRSMapper> CRS_MAPPERS = new HashSet<>();

    private static final Set<CRSMapper> BUILT_IN_CRS_MAPPERS = new HashSet<>();

    static class CRSMapper {

        Set<String> inputCRSs;

        String outputCRS;

        public CRSMapper(Set<String> inputCRSs, String outputCRS) {
            this.inputCRSs = inputCRSs;
            this.outputCRS = outputCRS;
        }

        boolean isSupporting(String inputCRS) {
            return inputCRSs.contains(inputCRS.toUpperCase());
        }

        String getOutputCRS() {
            return outputCRS;
        }
    }

    /** Look for an output CRS (EPSG:CODE) matching the input requested CRS. */
    public static String getSupportedOutputCRS(String requestedCRS) {
        String outputCRS = null;
        for (CRSMapper mapper : CRS_MAPPERS) {
            if (mapper.isSupporting(requestedCRS)) {
                outputCRS = mapper.getOutputCRS();
                break;
            }
        }
        return outputCRS;
    }

    public static final String TCRS_METADATA_KEY = "MapMLTCRSList.Key";

    static {
        createBuiltInDefinitions();
        reloadDefinitions();
    }

    private static void createBuiltInDefinitions() {
        final String WGS84_NAME = "WGS84";
        final String WGS84_CODE = "urn:ogc:def:crs:OGC:1.3:CRS84";
        final CoordinateReferenceSystem CRS_WGS84;
        String WGS84_SRSNAME = null;
        try {
            CRS_WGS84 = CRS.decode(WGS84_CODE);
            WGS84_SRSNAME = CRS.toSRS(CRS_WGS84);
        } catch (Exception e) {
        }
        final Bounds WGS84_BOUNDS =
                new Bounds(new Point(-180.0D, -90.0D), new Point(180.0D, 90.0D));
        final int WGS84_TILE_SIZE = 256;
        final double[] WGS84_SCALES = {
            /* "scale" is the reciprocal of "resolution", per Proj4Leaflet.js */
            1 / 0.703125D,
            1 / 0.3515625D,
            1 / 0.17578125D,
            1 / 0.087890625D,
            1 / 0.0439453125D,
            1 / 0.02197265625D,
            1 / 0.010986328125D,
            1 / 0.0054931640625D,
            1 / 0.00274658203125D,
            1 / 0.001373291015625D,
            1 / 0.0006866455078125D,
            1 / 0.0003433227539062D,
            1 / 0.0001716613769531D,
            1 / 0.0000858306884766D,
            1 / 0.0000429153442383D,
            1 / 0.0000214576721191D,
            1 / 0.0000107288360596D,
            1 / 0.0000053644180298D,
            1 / 0.0000026822090149D,
            1 / 0.0000013411045074D,
            1 / 0.0000006705522537D,
            1 / 0.0000003352761269D
        };
        final Point WGS84_TILE_ORIGIN = new Point(-180.0D, 90.0D);
        TiledCRSParams wgs84TiledCrsParams =
                new TiledCRSParams(
                        WGS84_NAME,
                        WGS84_CODE,
                        WGS84_BOUNDS,
                        WGS84_TILE_SIZE,
                        WGS84_TILE_ORIGIN,
                        WGS84_SCALES);
        BUILT_IN_TILED_CRS_DEFINITIONS.put(WGS84_NAME, wgs84TiledCrsParams);
        BUILT_IN_TILED_CRS_DEFINITIONS.put(WGS84_SRSNAME, wgs84TiledCrsParams);
        BUILT_IN_TILED_CRS_DEFINITIONS.put(WGS84_CODE, wgs84TiledCrsParams);
        BUILT_IN_TILED_CRS_BY_SRS_NAME.put(WGS84_SRSNAME, wgs84TiledCrsParams);
        BUILT_IN_TILED_CRS.put(WGS84_NAME, new TiledCRS(WGS84_NAME, wgs84TiledCrsParams));

        final String OSMTILE_NAME = "OSMTILE";
        final String OSMTILE_CODE = "urn:x-ogc:def:crs:EPSG:3857";
        final CoordinateReferenceSystem CRS_OSMTILE;
        String OSMTILE_SRSNAME = null;
        try {
            CRS_OSMTILE = CRS.decode(OSMTILE_CODE);
            OSMTILE_SRSNAME = CRS.toSRS(CRS_OSMTILE);
        } catch (Exception e) {
        }
        Projection proj = new Projection(OSMTILE_CODE);
        final Bounds OSMTILE_BOUNDS;
        try {
            OSMTILE_BOUNDS =
                    new Bounds(
                            proj.project(new LatLng(-85.0511287798, -180)),
                            proj.project(new LatLng(85.0511287798, 180)));
        } catch (MismatchedDimensionException | TransformException ex) {
            throw new RuntimeException(ex);
        }
        final int OSMTILE_TILE_SIZE = 256;
        final double[] OSMTILE_SCALES = {
            /* "scale" is the reciprocal of "resolution", per Proj4Leaflet.js */
            1 / 156543.0339D,
            1 / 78271.51695D,
            1 / 39135.758475D,
            1 / 19567.8792375D,
            1 / 9783.93961875D,
            1 / 4891.969809375D,
            1 / 2445.9849046875D,
            1 / 1222.9924523438D,
            1 / 611.49622617188D,
            1 / 305.74811308594D,
            1 / 152.87405654297D,
            1 / 76.437028271484D,
            1 / 38.218514135742D,
            1 / 19.109257067871D,
            1 / 9.5546285339355D,
            1 / 4.7773142669678D,
            1 / 2.3886571334839D,
            1 / 1.1943285667419D,
            1 / 0.59716428337097D
        };
        final Point OSMTILE_TILE_ORIGIN = new Point(-20037508.342787D, 20037508.342787D);
        TiledCRSParams osmTiledCrsParams =
                new TiledCRSParams(
                        OSMTILE_NAME,
                        OSMTILE_CODE,
                        OSMTILE_BOUNDS,
                        OSMTILE_TILE_SIZE,
                        OSMTILE_TILE_ORIGIN,
                        OSMTILE_SCALES);
        BUILT_IN_TILED_CRS_DEFINITIONS.put(OSMTILE_NAME, osmTiledCrsParams);
        BUILT_IN_TILED_CRS_DEFINITIONS.put(OSMTILE_SRSNAME, osmTiledCrsParams);
        BUILT_IN_TILED_CRS_DEFINITIONS.put(OSMTILE_CODE, osmTiledCrsParams);
        BUILT_IN_TILED_CRS_BY_SRS_NAME.put(OSMTILE_SRSNAME, osmTiledCrsParams);
        BUILT_IN_TILED_CRS.put(OSMTILE_NAME, new TiledCRS(OSMTILE_NAME, osmTiledCrsParams));

        final String CBMTILE_NAME = "CBMTILE";
        final String CBMTILE_CODE = "urn:x-ogc:def:crs:EPSG:3978";
        final CoordinateReferenceSystem CRS_CBMTILE;
        String CBMTILE_SRSNAME = null;
        try {
            CRS_CBMTILE = CRS.decode(CBMTILE_CODE);
            CBMTILE_SRSNAME = CRS.toSRS(CRS_CBMTILE);
        } catch (Exception e) {
        }
        final Bounds CBMTILE_BOUNDS =
                new Bounds(new Point(-3.46558E7D, -3.9E7D), new Point(1.0E7D, 3.931E7D));
        //                new Bounds(new Point(-4282638.06150141,-5153821.09213678),new
        // Point(4852210.1755664,4659267.000000001));
        final int CBMTILE_TILE_SIZE = 256;
        final double[] CBMTILE_SCALES = {
            1 / 38364.660062653464D,
            1 / 22489.62831258996D,
            1 / 13229.193125052918D,
            1 / 7937.5158750317505D,
            1 / 4630.2175937685215D,
            1 / 2645.8386250105837D,
            1 / 1587.5031750063501D,
            1 / 926.0435187537042D,
            1 / 529.1677250021168D,
            1 / 317.50063500127004D,
            1 / 185.20870375074085D,
            1 / 111.12522225044451D,
            1 / 66.1459656252646D,
            1 / 38.36466006265346D,
            1 / 22.48962831258996D,
            1 / 13.229193125052918D,
            1 / 7.9375158750317505D,
            1 / 4.6302175937685215D,
            1 / 2.6458386250105836D,
            1 / 1.5875031750063502D,
            1 / 0.92604351875370428D,
            1 / 0.52916772500211673D,
            1 / 0.31750063500127002D,
            1 / 0.18520870375074083D,
            1 / 0.11112522225044451D,
            1 / 0.066145965625264591D
        };
        final Point CBMTILE_TILE_ORIGIN = new Point(-34655800D, 39310000D);
        TiledCRSParams cbmTiledCrsParams =
                new TiledCRSParams(
                        CBMTILE_NAME,
                        CBMTILE_CODE,
                        CBMTILE_BOUNDS,
                        CBMTILE_TILE_SIZE,
                        CBMTILE_TILE_ORIGIN,
                        CBMTILE_SCALES);
        BUILT_IN_TILED_CRS_DEFINITIONS.put(CBMTILE_NAME, cbmTiledCrsParams);
        BUILT_IN_TILED_CRS_DEFINITIONS.put(CBMTILE_SRSNAME, cbmTiledCrsParams);
        BUILT_IN_TILED_CRS_DEFINITIONS.put(CBMTILE_CODE, cbmTiledCrsParams);
        BUILT_IN_TILED_CRS_BY_SRS_NAME.put(CBMTILE_SRSNAME, cbmTiledCrsParams);
        BUILT_IN_TILED_CRS.put(CBMTILE_NAME, new TiledCRS(CBMTILE_NAME, cbmTiledCrsParams));

        /* Arctic Polar Stereographic, origin and scales defined by map service at http://maps8.arcgisonline.com/arcgis/rest/services/Arctic_Polar_Ocean_Base/MapServer */
        final String APSTILE_NAME = "APSTILE";
        final String APSTILE_CODE = "urn:x-ogc:def:crs:EPSG:5936";
        final CoordinateReferenceSystem CRS_APSTILE;
        String APSTILE_SRSNAME = null;
        try {
            CRS_APSTILE = CRS.decode(APSTILE_CODE);
            APSTILE_SRSNAME = CRS.toSRS(CRS_APSTILE);
        } catch (Exception e) {
        }
        final Bounds APSTILE_BOUNDS =
                new Bounds(
                        new Point(-28567784.109254867D, -28567784.109254755D),
                        new Point(32567784.109255023D, 32567784.10925506D));
        final int APSTILE_TILE_SIZE = 256;
        final double[] APSTILE_SCALES = {
            /* "scale" is the reciprocal of "resolution", per Proj4Leaflet.js */
            1 / 238810.813354D,
            1 / 119405.406677D,
            1 / 59702.7033384999D,
            1 / 29851.3516692501D,
            1 / 14925.675834625D,
            1 / 7462.83791731252D,
            1 / 3731.41895865639D,
            1 / 1865.70947932806D,
            1 / 932.854739664032D,
            1 / 466.427369832148D,
            1 / 233.213684916074D,
            1 / 116.606842458037D,
            1 / 58.3034212288862D,
            1 / 29.1517106145754D,
            1 / 14.5758553072877D,
            1 / 7.28792765351156D,
            1 / 3.64396382688807D,
            1 / 1.82198191331174D,
            1 / 0.910990956788164D,
            1 / 0.45549547826179D
        };
        final Point APSTILE_TILE_ORIGIN = new Point(-28567784.109255D, 32567784.109255D);
        TiledCRSParams apsTiledCrsParams =
                new TiledCRSParams(
                        APSTILE_NAME,
                        APSTILE_CODE,
                        APSTILE_BOUNDS,
                        APSTILE_TILE_SIZE,
                        APSTILE_TILE_ORIGIN,
                        APSTILE_SCALES);
        BUILT_IN_TILED_CRS_DEFINITIONS.put(APSTILE_NAME, apsTiledCrsParams);
        BUILT_IN_TILED_CRS_DEFINITIONS.put(APSTILE_SRSNAME, apsTiledCrsParams);
        BUILT_IN_TILED_CRS_DEFINITIONS.put(APSTILE_CODE, apsTiledCrsParams);
        BUILT_IN_TILED_CRS_BY_SRS_NAME.put(APSTILE_SRSNAME, apsTiledCrsParams);
        BUILT_IN_TILED_CRS.put(APSTILE_NAME, new TiledCRS(APSTILE_NAME, apsTiledCrsParams));

        BUILT_IN_CRS_MAPPERS.add(
                new CRSMapper(
                        Set.of(
                                "EPSG:4326",
                                "URN:OGC:DEF:CRS:EPSG::4326",
                                "URN:OGC:DEF:CRS:MapML::WGS84"),
                        "EPSG:4326"));
        BUILT_IN_CRS_MAPPERS.add(
                new CRSMapper(
                        Set.of("EPSG:3857", "URN:OGC:DEF:CRS:EPSG::3857", "MAPML:OSMTILE"),
                        "EPSG:3857"));
        BUILT_IN_CRS_MAPPERS.add(
                new CRSMapper(
                        Set.of("EPSG:5936", "URN:OGC:DEF:CRS:EPSG::5936", "MAPML:APSTILE"),
                        "EPSG:5936"));
        BUILT_IN_CRS_MAPPERS.add(
                new CRSMapper(
                        Set.of("EPSG:3978", "URN:OGC:DEF:CRS:EPSG::3978", "MAPML:CBMTILE"),
                        "EPSG:3978"));
    }

    /**
     * @param identifier - an official CRS code / srsName OR TCRS NAME to look up
     * @return the TCRS corresponding to the code, long or short, or null if not found
     */
    public static TiledCRSParams lookupTCRSParams(String identifier) {
        return TiledCRSConstants.tiledCRSDefinitions.get(identifier);
    }

    /**
     * @param identifier - an official CRS code / srsName OR TCRS NAME to look up
     * @return the TCRS corresponding to the code, long or short, or null if not found
     */
    public static TiledCRS lookupTCRS(String identifier) {
        return TiledCRSConstants.TILED_CRS.get(identifier);
    }

    /**
     * @param code - an official CRS code / srsName OR a TCRS name (e.g. OSMTILE) to look up
     * @return the TCRS name corresponding to the code, long or short, or null if not found
     */
    public static String lookupTCRSName(String code) {
        TiledCRSParams tcrs = lookupTCRSParams(code);
        return tcrs != null ? tcrs.getName() : null;
    }

    private static void reloadBuiltInDefinitions() {
        tiledCRSBySrsName.clear();
        tiledCRSDefinitions.clear();
        TILED_CRS.clear();
        CRS_MAPPERS.clear();
        tiledCRSBySrsName.putAll(BUILT_IN_TILED_CRS_BY_SRS_NAME);
        tiledCRSDefinitions.putAll(BUILT_IN_TILED_CRS_DEFINITIONS);
        TILED_CRS.putAll(BUILT_IN_TILED_CRS);
        CRS_MAPPERS.addAll(BUILT_IN_CRS_MAPPERS);
    }

    /**
     * Reload the TileCRS definition by setting up BuiltIN TCRS and setting up the ones matching the
     * gridsets defined in the dedicated Settings.
     */
    public static void reloadDefinitions() {
        reloadBuiltInDefinitions();
        GeoServer config = GeoServerExtensions.bean(GeoServer.class);
        MetadataMap metadata = config.getGlobal().getSettings().getMetadata();
        if (metadata.containsKey(TCRS_METADATA_KEY)) {
            Serializable gridSetList = metadata.get(TCRS_METADATA_KEY);
            if (gridSetList instanceof List) {
                GWC gwc = GeoServerExtensions.bean(GWC.class);
                Map<String, TiledCRSParams> additionalTiledCRS =
                        MapMLGridsets.getTiledCRSs(gwc, (List<String>) gridSetList);

                for (String name : additionalTiledCRS.keySet()) {
                    TiledCRSParams param = additionalTiledCRS.get(name);
                    String code = param.getCode();
                    tiledCRSDefinitions.put(name.toUpperCase(), param);
                    tiledCRSDefinitions.put(code, param);
                    tiledCRSBySrsName.put(code, param);
                    TiledCRS tiledCRS = new TiledCRS(name.toUpperCase(), param);
                    TILED_CRS.put(code, tiledCRS);
                    TILED_CRS.put(name, tiledCRS);
                    TILED_CRS.put(("MAPML:" + name).toUpperCase(), tiledCRS);
                    if (code.toUpperCase().startsWith("EPSG:")) {
                        String urnEPSGcode = code.replace("EPSG:", "URN:OGC:DEF:CRS:EPSG::");
                        tiledCRSDefinitions.put(urnEPSGcode, param);
                        TILED_CRS.put(urnEPSGcode, tiledCRS);
                        CRSMapper mapper =
                                new CRSMapper(Set.of(code, urnEPSGcode, "MAPML:" + name), code);
                        CRS_MAPPERS.add(mapper);
                    }
                }
            }
        }
    }
}
