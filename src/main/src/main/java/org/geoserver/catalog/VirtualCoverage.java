/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.Serializable;
import java.util.List;

import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.util.Utilities;

/**
 * Class containing main definition of a Virtual Coverage, such as, originating coverageStore and composing coverageNames/bands.
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 * 
 */
public class VirtualCoverage implements Serializable {

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder("VirtualCoverage name=").append(name);
        output.append("\n\tBands");
        for (VirtualCoverageBand band : coverageBands) {
            output.append("\t").append(band.toString());
        }
        return output.toString();

    }

    public static final String BAND_SEPARATOR = "@";

    public static enum CompositionType {
        BAND_SELECT /* , MATH */;

        public static CompositionType getDefault() {
            return BAND_SELECT;
        }
    }

    /**
     * Definition of Input Coverage Bands composing a single {@link VirtualCoverageBand} A {@link VirtualCoverageBand} may be composed of different
     * {@link InputCoverageBand}s.
     * 
     * Current implementation only deal with {@link VirtualCoverageBand}s made of a single {@link InputCoverageBand}. Once we allows for Scripts and
     * Math on bands compositions (like WindSpeedBand = SQRT(UBand^2 + VBand^2)) we will have a {@link VirtualCoverageBand} built on top of multiple
     * {@link InputCoverageBand}s
     */
    public static class InputCoverageBand implements Serializable {
        @Override
        public String toString() {
            return "InputCoverageBand [coverageName=" + coverageName + ", band=" + band + "]";
        }

        public InputCoverageBand(String coverageName, String band) {
            super();
            this.coverageName = coverageName;
            this.band = band;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((band == null) ? 0 : band.hashCode());
            result = prime * result + ((coverageName == null) ? 0 : coverageName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            InputCoverageBand other = (InputCoverageBand) obj;
            if (band == null) {
                if (other.band != null)
                    return false;
            } else if (!band.equals(other.band))
                return false;
            if (coverageName == null) {
                if (other.coverageName != null)
                    return false;
            } else if (!coverageName.equals(other.coverageName))
                return false;
            return true;
        }

        public String getCoverageName() {
            return coverageName;
        }

        public void setCoverageName(String coverageName) {
            this.coverageName = coverageName;
        }

        public String getBand() {
            return band;
        }

        public void setBand(String band) {
            this.band = band;
        }

        private String coverageName;

        private String band;
    }

    /**
     * Definition of a Virtual Coverage Band composing the Virtual coverage A {@link VirtualCoverageBand} is made of - a list of
     * {@link InputCoverageBand}s defining which coverages and which bands have been used to compose this band - The type of composition used to
     * configure this band (Currently, only BAND_SELECT is supported) - The definition of this band (It may contain the script, or the RULE to
     * compose that band) - The index in the output coverage (Wondering if this can be removed)
     * */
    public static class VirtualCoverageBand implements Serializable {

        public VirtualCoverageBand(List<InputCoverageBand> inputCoverageBands, String definition,
                int index, CompositionType compositionType) {
            super();
            this.inputCoverageBands = inputCoverageBands;
            this.definition = definition;
            this.index = index;
            this.compositionType = compositionType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((compositionType == null) ? 0 : compositionType.hashCode());
            result = prime * result + ((definition == null) ? 0 : definition.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            VirtualCoverageBand other = (VirtualCoverageBand) obj;
            if (compositionType != other.compositionType)
                return false;
            if (definition == null) {
                if (other.definition != null)
                    return false;
            } else if (!definition.equals(other.definition))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "VirtualCoverageBand [inputCoverageBands=" + inputCoverageBands
                    + ", definition=" + definition + ", index=" + index + ", compositionType="
                    + compositionType + "]";
        }

        /** The InputCoverageBands composing this band */
        private List<InputCoverageBand> inputCoverageBands;

        /**
         * The definition of this virtual band. Currently it simply contains the name of the input band. Once we support different compositions, it
         * will contain the maths.
         */
        private String definition;

        private int index;

        /**
         * Type of composition used to define this band. Currently, only {@link CompositionType#BAND_SELECT} is supported.
         */
        private CompositionType compositionType;

        public String getDefinition() {
            return definition;
        }

        public void setDefinition(String definition) {
            this.definition = definition;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public CompositionType getCompositionType() {
            return compositionType;
        }

        public void setCompositionType(CompositionType compositionType) {
            this.compositionType = compositionType;
        }

        public List<InputCoverageBand> getInputCoverageBands() {
            return inputCoverageBands;
        }

        public void setInputCoverageBands(List<InputCoverageBand> inputCoverageBands) {
            this.inputCoverageBands = inputCoverageBands;
        }
    }

    /** A key to be assigned to VirtualCoverage object into metadata maps */
    public static String VIRTUAL_COVERAGE = "VIRTUAL_COVERAGE";

    public VirtualCoverage(String name, List<VirtualCoverageBand> coverageBands) {
        super();
        this.name = name;
        this.coverageBands = coverageBands;
    }

    /** The list of VirtualCoverageBands composing this VirtualCoverage */
    private List<VirtualCoverageBand> coverageBands;

    /** The name assigned to the virtual coverage */
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<VirtualCoverageBand> getCoverageBands() {
        return coverageBands;
    }

    public void setCoverageBands(List<VirtualCoverageBand> coverageBands) {
        this.coverageBands = coverageBands;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((coverageBands == null) ? 0 : coverageBands.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VirtualCoverage other = (VirtualCoverage) obj;
        if (coverageBands == null) {
            if (other.coverageBands != null)
                return false;
        } else if (!coverageBands.equals(other.coverageBands))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    /**
     * Create a CoverageInfo
     * 
     * @param builder
     * @param storeInfo
     * @param cinfo
     * @param name
     * @return
     * @throws Exception
     */
    private CoverageInfo buildCoverageInfo(CatalogBuilder builder, CoverageStoreInfo storeInfo,
            CoverageInfo cinfo, String name) throws Exception {
        Catalog catalog = storeInfo.getCatalog();

        // Get a reader from the pool for this Sample CoverageInfo (we have to pass it down a VirtualCoverage definition)
        cinfo.setStore(storeInfo);
        cinfo.getMetadata().put(VirtualCoverage.VIRTUAL_COVERAGE, this);
        cinfo.setName(name);
        cinfo.setNativeCoverageName(name);

        GridCoverage2DReader reader = (GridCoverage2DReader) catalog.getResourcePool()
                .getGridCoverageReader(cinfo, name, null);
        builder.setStore(storeInfo);
        return builder.buildCoverage(reader, name, null);
    }

    /** Create a new CoverageInfo for this virtual coverage */
    public CoverageInfo createVirtualCoverageInfo(String name, CoverageStoreInfo storeInfo,
            CatalogBuilder builder) throws Exception {
        Catalog catalog = storeInfo.getCatalog();

        CoverageInfo coverageInfo = catalog.getFactory().createCoverage();
        CoverageInfo info = buildCoverageInfo(builder, storeInfo, coverageInfo, name);

        info.getMetadata().put(VirtualCoverage.VIRTUAL_COVERAGE, this);
        info.setName(name);
        info.setNativeCoverageName(name);
        return info;
    }

    /**
     * Update the specified CoverageInfo with the updated VirtualCoverage stored within its metadata
     * 
     * @param name
     * @param storeInfo
     * @param builder
     * @param coverageInfo
     * @throws Exception
     */
    public void updateVirtualCoverageInfo(String name, CoverageStoreInfo storeInfo,
            CatalogBuilder builder, CoverageInfo coverageInfo) throws Exception {
        Utilities.ensureNonNull("coverageInfo", coverageInfo);

        // clean up coverage dimensions for the update
        coverageInfo.getDimensions().clear();
        CoverageInfo info = buildCoverageInfo(builder, storeInfo, coverageInfo, name);
        coverageInfo.getMetadata().put(VirtualCoverage.VIRTUAL_COVERAGE, this);
        coverageInfo.getDimensions().addAll(info.getDimensions());
    }

    public VirtualCoverageBand getBand(int i) {
        return coverageBands.get(i);
    }

    public VirtualCoverageBand getBand(String coverageName) {
        for (VirtualCoverageBand coverageBand : coverageBands) {
            for (InputCoverageBand inputBand : coverageBand.getInputCoverageBands()) {
                if (inputBand.getCoverageName().equalsIgnoreCase(coverageName)) {
                    return coverageBand;
                }
            }
        }
        return null;
    }

    public int getSize() {
        return coverageBands != null ? coverageBands.size() : 0;
    }
}
