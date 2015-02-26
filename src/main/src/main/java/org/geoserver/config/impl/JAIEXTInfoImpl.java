package org.geoserver.config.impl;

import it.geosolutions.jaiext.ConcurrentOperationRegistry.OperationItem;
import it.geosolutions.jaiext.JAIExt;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.geoserver.config.JAIEXTInfo;

public class JAIEXTInfoImpl implements JAIEXTInfo {

    /**
     * Available JAI operations
     */
    public static final Set<String> JAI_OPS = new TreeSet<String>();

    private Set<String> jaiOperations = JAI_OPS;
    
    /**
     * Available JAIEXT operations
     */
    public static final TreeSet<String> JAIEXT_OPS = new TreeSet<String>();
    
    private Set<String> jaiExtOperations = JAIEXT_OPS;

    static{
        JAIExt.initJAIEXT();
        List<OperationItem> jaiextOps = JAIExt.getJAIEXTOperations();
        for(OperationItem item : jaiextOps){
            String name = item.getName();
            if (name.equalsIgnoreCase("algebric") || name.equalsIgnoreCase("operationConst")
                    || name.equalsIgnoreCase("Stats")) {
                JAIEXT_OPS.add(name);
            }else if(JAIExt.isJAIAPI(name)){
                JAIEXT_OPS.add(name);
            }
        }
    }



//    public static final boolean ALL_JAIEXT = true;
//
//    private boolean allJaiExtOp = ALL_JAIEXT;
//    
//    public static final boolean ALL_JAI = false;
//
//    private boolean allJaiOp = ALL_JAI;
    
    public JAIEXTInfoImpl() {
        if(jaiOperations == null ){
            jaiOperations = JAI_OPS;
        }
        if(jaiExtOperations == null ){
            jaiExtOperations = JAIEXT_OPS;
        }
        List<OperationItem> jaiextOps = JAIExt.getJAIEXTOperations();
        for(OperationItem item : jaiextOps){
            String name = item.getName();
            if (name.equalsIgnoreCase("algebric") || name.equalsIgnoreCase("operationConst")
                    || name.equalsIgnoreCase("Stats")) {
                jaiExtOperations.add(name);
            }else if(JAIExt.isJAIAPI(name)){
                jaiExtOperations.add(name);
            }
        }
    }

    @Override
    public Set<String> getJAIOperations() {
        if(jaiOperations == null ){
            jaiOperations = JAI_OPS;
        }
        return jaiOperations;
    }

    @Override
    public void setJAIOperations(Set<String> operations) {
        this.jaiOperations = new TreeSet<String>(operations);
        //jaiOperations.addAll(operations);
    }

    @Override
    public Set<String> getJAIEXTOperations() {
        if(jaiExtOperations == null){
            jaiExtOperations = JAIEXT_OPS;
        }
        return jaiExtOperations;
    }

    @Override
    public void setJAIEXTOperations(Set<String> operations) {
        this.jaiExtOperations = new TreeSet<String>(operations);
        //jaiExtOperations.addAll(operations);
    }
    
//    @Override
//    public boolean isAllJAIExtEnabled() {
//        return allJaiExtOp;
//    }
//
//    @Override
//    public void setAllJAIExtEnabled(boolean allJAIExtEnabled) {
//        this.allJaiExtOp = allJAIExtEnabled;
//    }
//
//    @Override
//    public boolean isAllJAIEnabled() {
//        return allJaiOp;
//    }
//
//    @Override
//    public void setAllJAIEnabled(boolean allJAIEnabled) {
//        this.allJaiOp = allJAIEnabled;
//    }
}
