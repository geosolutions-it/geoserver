/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin.output;

import org.geoserver.wps.remote.plugin.XMPPClient;

/**
 * @author Alessio
 * 
 */
public interface XMPPOutputType {

    /**
     * 
     * @param visitor
     * @param outputs
     * @param value
     * @param type
     * @param pID
     * @param baseURL 
     * @param xmppClient
     * @param publish
     * @param wpsOutputValue 
     * @return 
     * @throws Exception 
     */
    public Object accept(XMPPOutputVisitor visitor, Object value, String type, String pID, String baseURL, XMPPClient xmppClient, boolean publish) throws Exception;
    
    /**
     * 
     * @param outputs
     * @param value
     * @param type
     * @param pID
     * @param xmppClient
     * @param publish
     * @throws Exception 
     */
    public Object produceOutput(Object value, String type, String pID, String baseURL, XMPPClient xmppClient, boolean publish) throws Exception;
}
