/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.media.jai.ImageLayout;
import javax.media.jai.operator.BandMergeDescriptor;

import org.geoserver.catalog.VirtualCoverage.VirtualCoverageBand;
import org.geoserver.catalog.impl.CoverageDimensionImpl;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.DataSourceException;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

/**
 * A virtual Coverage reader which takes care of doing underlying coverage read operations
 * and recompositions.
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 *
 */
public class VirtualGridCoverageReader extends SingleGridCoverage2DReader {
    
    /**
     * A CoveragesConsistencyChecker checks if the composing coverages
     * respect the constraints which currently are:
     * 
     * - same CRS
     * - same resolution
     * - same bbox
     * - same data type
     * - same dimensions (same number of dimension, same type, and same name)
     */
    static class CoveragesConsistencyChecker {

        private static double DELTA = 1E-10;
        private Set<ParameterDescriptor<List>> dynamicParameters;
        private String[] metadataNames;
        private GridEnvelope gridRange;
        private GeneralEnvelope envelope;
        private CoordinateReferenceSystem crs;
        private ImageLayout layout;

        public CoveragesConsistencyChecker(GridCoverage2DReader reader) throws IOException {
            envelope = reader.getOriginalEnvelope();
            gridRange = reader.getOriginalGridRange();
            crs = reader.getCoordinateReferenceSystem();
            metadataNames = reader.getMetadataNames();
            dynamicParameters = reader.getDynamicParameters();
            layout = reader.getImageLayout();
        }
        
        /**
         * Check whether the coverages associated to the provided reader is consistent
         * with the reference coverage.
         * @param reader
         * @throws IOException
         */
        public void checkConsistency(GridCoverage2DReader reader) throws IOException {
            GeneralEnvelope envelope = reader.getOriginalEnvelope();
            GridEnvelope gridRange = reader.getOriginalGridRange();
            CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
            String[] metadataNames = reader.getMetadataNames();
            Set<ParameterDescriptor<List>> dynamicParameters = reader.getDynamicParameters();
            if (!envelope.equals(this.envelope, DELTA, true)) {
                throw new IllegalArgumentException("The coverage envelope must be the same");
            }
            
            //TODO: Improve these checks
            if (metadataNames.length != this.metadataNames.length) {
                throw new IllegalArgumentException("The coverage metadataNames should have the same size");
            } 
            
            MathTransform destinationToSourceTransform = null;
            if (!CRS.equalsIgnoreMetadata(crs, this.crs))
                try {
                    destinationToSourceTransform = CRS.findMathTransform(crs, this.crs, true);
                } catch (FactoryException e) {
                    throw new DataSourceException("Unable to inspect request CRS", e);
                }
            // now transform the requested envelope to source crs
            if (destinationToSourceTransform != null && !destinationToSourceTransform.isIdentity()) {
                throw new IllegalArgumentException("The coverage coordinateReferenceSystem should be the same");
            }
            if (layout.getSampleModel(null).getDataType() != this.layout.getSampleModel(null).getDataType()) {
                throw new IllegalArgumentException("The coverage dataType should be the same");
            }
        }
    }
    
    /**
     * A simple reader which will apply coverages customizations to the virtual coverage
     */
    static class CoverageDimensionVirtualCustomizerReader extends CoverageDimensionCustomizerReader {

        public CoverageDimensionVirtualCustomizerReader(GridCoverage2DReader delegate,
                String coverageName, CoverageInfo info) {
            super(delegate, coverageName, info);
        }
        
        
        protected GridSampleDimension[] wrapDimensions(SampleDimension[] dims) {
            GridSampleDimension[] wrappedDims = null;
            CoverageInfo info = getInfo();
            if (info != null) {
                List<CoverageDimensionInfo> storedDimensions = info.getDimensions();
                MetadataMap map = info.getMetadata();
                if (map.containsKey(VirtualCoverage.VIRTUAL_COVERAGE)) {
                    VirtualCoverage virtualCoverage = (VirtualCoverage) map.get(VirtualCoverage.VIRTUAL_COVERAGE);
                    VirtualCoverageBand band = virtualCoverage.getBand(getCoverageName());
                    
                    if (storedDimensions != null && storedDimensions.size() > 0) {
                        CoverageDimensionInfo dimensionInfo = storedDimensions.get(band.getIndex());
                        wrappedDims = new GridSampleDimension[1];
                        wrappedDims[0] = new WrappedSampleDimension((GridSampleDimension) dims[0], dimensionInfo);
                    } else {
                        wrappedDims = new GridSampleDimension[1];
                        CoverageDimensionInfo dimensionInfo = new CoverageDimensionImpl();
                        dimensionInfo.setName(band.getDefinition());
                        wrappedDims[0] = new WrappedSampleDimension((GridSampleDimension) dims[0], dimensionInfo); 
                    }
                } else {
                    super.wrapDimensions(wrappedDims);
                }
            }
            return wrappedDims;
        }
    }
    
    static class CoverageDimensionVirtualCustomizerStructuredReader extends CoverageDimensionVirtualCustomizerReader {

        public CoverageDimensionVirtualCustomizerStructuredReader(GridCoverage2DReader delegate,
                String coverageName, CoverageInfo info) {
            super(delegate, coverageName, info);
        }
        
    }
    
    /** The VirtualCoverage containing definition */
    private VirtualCoverage virtualCoverage;
    
    /** The name of the reference coverage, we can remove/revisit it once we relax some constraint */
    private String referenceName;
    
    private GridCoverage2DReader delegate;
    
    private Hints hints;
    
    /** The CoverageInfo associated to the VirtualCoverage */  
    private CoverageInfo coverageInfo;
    
    private GridCoverageFactory coverageFactory;
    
    public VirtualGridCoverageReader(GridCoverage2DReader delegate, VirtualCoverage virtualCoverage, CoverageInfo coverageInfo, Hints hints) {
        super(delegate, virtualCoverage.getName());
        this.delegate = delegate;
        this.virtualCoverage = virtualCoverage;
        this.coverageInfo = coverageInfo;
        this.hints = hints;
        // Refactor this once supporting heterogeneous elements
        referenceName = virtualCoverage.getBand(0).getInputCoverageBands().get(0).getCoverageName();
        if (this.hints != null && this.hints.containsKey(Hints.GRID_COVERAGE_FACTORY)) {
            final Object factory = this.hints.get(Hints.GRID_COVERAGE_FACTORY);
            if (factory != null && factory instanceof GridCoverageFactory) {
                this.coverageFactory = (GridCoverageFactory) factory;
            }
        }
        if (this.coverageFactory == null) {
            this.coverageFactory = CoverageFactoryFinder.getGridCoverageFactory(this.hints);
        }
    }
    @Override
    public String[] getMetadataNames() throws IOException {
        return super.getMetadataNames(referenceName);
    }
    @Override
    public String getMetadataValue(String name) throws IOException {
        return super.getMetadataValue(referenceName, name);
    }
    @Override
    public GeneralEnvelope getOriginalEnvelope() {
        return super.getOriginalEnvelope(referenceName);
    }
    @Override
    public GridEnvelope getOriginalGridRange() {
        return super.getOriginalGridRange(referenceName);
    }
    @Override
    public MathTransform getOriginalGridToWorld(PixelInCell pixInCell) {
        return super.getOriginalGridToWorld(referenceName, pixInCell);
    }
    @Override
    public GridCoverage2D read(GeneralParameterValue[] parameters) throws IllegalArgumentException,
            IOException {

        List<VirtualCoverageBand> bands = virtualCoverage.getCoverageBands();
        List<GridCoverage2D> coverages = new ArrayList<GridCoverage2D>();
        List<SampleDimension> dims = new ArrayList<SampleDimension>();
        
        // Use composition rule specific implementation
        CoveragesConsistencyChecker checker = null;
        for (VirtualCoverageBand band : bands) {
            // Refactor this once supporting complex compositions
            String coverageName = band.getInputCoverageBands().get(0).getCoverageName();
            GridCoverage2DReader reader = wrap(delegate, coverageName, coverageInfo);
            
            // Remove this when removing constraints
            if (checker == null) {
                checker = new CoveragesConsistencyChecker(reader);
            } else {
                checker.checkConsistency(reader);
            }
            
            GridCoverage2D coverage = (GridCoverage2D)reader.read(parameters);
            coverages.add(coverage);
            dims.addAll(Arrays.asList(coverage.getSampleDimensions()));
        }
        
        
        GridCoverage2D sampleCoverage = coverages.get(0);

        // TODO: Implement bandMerges
        RenderedImage image = null;
        if (coverages.size() > 0) {
           image = BandMergeDescriptor.create(sampleCoverage.getRenderedImage(), coverages.get(1).getRenderedImage(), null);
        } else {
            image = sampleCoverage.getRenderedImage();
        }
        return coverageFactory.create(coverageInfo.getName()/*virtualCoverage.getName()*/, image, sampleCoverage.getGridGeometry(), dims.toArray(new GridSampleDimension[dims.size()]), null, /*props*/ null);
    }

    public static GridCoverage2DReader wrap(GridCoverage2DReader delegate, String coverageName, CoverageInfo info) {
        GridCoverage2DReader reader = delegate;
        if (coverageName != null) {
            reader = SingleGridCoverage2DReader.wrap(delegate, coverageName);
        }
        if (reader instanceof StructuredGridCoverage2DReader) {
            return new CoverageDimensionVirtualCustomizerStructuredReader((StructuredGridCoverage2DReader) reader, coverageName, info);
        } else {
            return new CoverageDimensionVirtualCustomizerReader(reader, coverageName, info);
        }
    }
    
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return super.getCoordinateReferenceSystem(referenceName);
    }
    @Override
    public Set<ParameterDescriptor<List>> getDynamicParameters() throws IOException {
        return super.getDynamicParameters(referenceName);
    }
    @Override
    public double[] getReadingResolutions(OverviewPolicy policy, double[] requestedResolution)
            throws IOException {
        return super.getReadingResolutions(referenceName, policy, requestedResolution);
    }
    @Override
    public int getNumOverviews() {
        return super.getNumOverviews(referenceName);
    }
    @Override
    public double[][] getResolutionLevels() throws IOException {
        return super.getResolutionLevels(referenceName);
    }
    
    /**
     * @param coverageName
     */
    protected void checkCoverageName(String coverageName) {
        // It's virtual...  
        
    }
}
