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
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

public class VirtualCoverageReader extends SingleGridCoverage2DReader {
    
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
//                    List<VirtualCoverageBand> bands = virtualCoverage.getCoverageBands(); 
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
    
    private VirtualCoverage virtualCoverage;
    
    private String referenceName;
    
    private GridCoverage2DReader delegate;
    
    private Hints hints;
    
    private CoverageInfo coverageInfo;
    
    private GridCoverageFactory coverageFactory;
    
    public VirtualCoverageReader(GridCoverage2DReader delegate, VirtualCoverage virtualCoverage, CoverageInfo coverageInfo, Hints hints) {
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
        final int bandSize = bands.size();
//        for (int i=0; i < bandSize; i++) {
        for (VirtualCoverageBand band : bands) {
            // Refactor this once supporting complex compositions 
            String coverageName = band.getInputCoverageBands().get(0).getCoverageName();
            GridCoverageReader reader = wrap(delegate, coverageName, coverageInfo);
            GridCoverage2D coverage = (GridCoverage2D)reader.read(parameters);
            coverages.add(coverage);
            dims.addAll(Arrays.asList(coverage.getSampleDimensions()));
        }
        
        
        GridCoverage2D sampleCoverage = coverages.get(0);

        // TODO: Implement bandMerges
        RenderedImage image = BandMergeDescriptor.create(sampleCoverage.getRenderedImage(), coverages.get(1).getRenderedImage(), null);
//        image.getData(new Rectangle(0,0,1000,1000));
//        ImageIO.write(coverages.get(1).getRenderedImage(), "tiff", new File("C:\\merged2.tif"));
        GridSampleDimension[] wrappedDims = new GridSampleDimension[bandSize];
        
        if (coverageInfo.getDimensions() != null) {
            int i = 0;
//            for (SampleDimension dim: dims) {
//                wrappedDims[i] = new WrappedSampleDimension((GridSampleDimension) dim, 
//                        storedDimensions.get(outputDims != inputDims ? (i > (inputDims - 1 ) ? inputDims - 1 : i) : i));
//                i++;
//            }
        }
        return coverageFactory.create(coverageInfo.getName()/*virtualCoverage.getName()*/, image, sampleCoverage.getGridGeometry(), dims.toArray(new GridSampleDimension[dims.size()]), null, /*props*/ null);
    }

    public static GridCoverageReader wrap(GridCoverage2DReader delegate, String coverageName, CoverageInfo info) {
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
        // It's virtual... TODO: add checks 
        
    }
}
