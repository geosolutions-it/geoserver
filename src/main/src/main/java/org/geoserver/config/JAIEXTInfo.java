package org.geoserver.config;

import java.io.Serializable;
import java.util.Set;

public interface JAIEXTInfo extends Serializable, Cloneable {
    
    Set<String> getJAIOperations();
    void setJAIOperations(Set<String> operations);
    
    Set<String> getJAIEXTOperations();
    void setJAIEXTOperations(Set<String> operations);
//    
//    boolean isAllJAIExtEnabled();
//    void setAllJAIExtEnabled(boolean allJAIExtEnabled);
//    
//    boolean isAllJAIEnabled();
//    void setAllJAIEnabled(boolean allJAIEnabled);
}
