/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.Serializable;
import java.util.List;

import org.geotools.coverage.grid.io.GridCoverage2DReader;

/**
 * Class containing main definition of a Virtual Coverage, such as, originating coverageStore and composing coverageNames/bands.
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 * 
 */
public class VirtualCoverage implements Serializable {

    public static enum CompositionType {
        BAND_SELECT/*, MATH*/
    }
    
    /** Definition of Input Coverage Bands composing a VirtualCoverage band */
    public static class InputCoverageBand implements Serializable {
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

        String coverageName;

        String band;
    }
    
    public static class VirtualCoverageBand implements Serializable{

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
//              result = prime * result + ((coverageName == null) ? 0 : coverageName.hashCode());
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
//              if (coverageName == null) {
//                  if (other.coverageName != null)
//                      return false;
//              } else if (!coverageName.equals(other.coverageName))
//                  return false;
              if (definition == null) {
                  if (other.definition != null)
                      return false;
              } else if (!definition.equals(other.definition))
                  return false;
              return true;
          }
      
          List<InputCoverageBand> inputCoverageBands;
      
          String definition;
          
          int index;
      
          CompositionType compositionType;
      
//          public String getCoverageName() {
//              return coverageName;
//          }
//      
//          public void setCoverageName(String coverageName) {
//              this.coverageName = coverageName;
//          }
      
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
    
    public static String VIRTUAL_COVERAGE = "VIRTUAL_COVERAGE";

    public VirtualCoverage(String name, List<VirtualCoverageBand> coverageBands) {
        super();
        this.name = name;
        this.coverageBands = coverageBands;
        //TODO: Replace that with better logic once supporting multiple resolutions, coverages 
//        this.referenceName = coverageBands.get(0).getCoverageName();
    }

    List<VirtualCoverageBand> coverageBands;

    private String name;

//    /** Sample coverageName for info: It may be removed once we relax constraints */
//    private String referenceName;

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

//    public String getReferenceName() {
//        return referenceName;
//    }
//
//    public void setReferenceName(String referenceName) {
//        this.referenceName = referenceName;
//    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((coverageBands == null) ? 0 : coverageBands.hashCode());
        // result = prime * result + ((name == null) ? 0 : name.hashCode());
        // result = prime * result + ((storeInfo == null) ? 0 : storeInfo.hashCode());
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

    public CoverageInfo createVirtualCoverageInfo(String name, CoverageStoreInfo storeInfo,
            CatalogBuilder builder) throws Exception {
        Catalog catalog = storeInfo.getCatalog();
        CoverageInfo cinfo = catalog.getFactory().createCoverage();

        cinfo.setStore(storeInfo);
        cinfo.getMetadata().put(VirtualCoverage.VIRTUAL_COVERAGE, this);
        cinfo.setName(name);
        cinfo.setNativeCoverageName(name);

        // Get a reader from the pool for this Sample CoverageInfo (we have to pass it down a VirtualCoverage definition)
        GridCoverage2DReader reader = (GridCoverage2DReader) catalog.getResourcePool().getGridCoverageReader(cinfo, name, null);
        builder.setStore(storeInfo);

        CoverageInfo info = builder.buildCoverage(reader, name, null);
        info.getMetadata().put(VirtualCoverage.VIRTUAL_COVERAGE, this);
        info.setName(name);
        info.setNativeCoverageName(name);

        // TODO: CHECK CONSISTENCY
        return info;
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
