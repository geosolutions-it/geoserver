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
public interface XMPPOutputVisitor {
    
    /**
     * 
     * @param visitor
     * @param outputs
     * @param value
     * @param type
     * @param pID
     * @param xmppClient
     * @param publish
     * @throws Exception 
     */
    public Object visit( XMPPTextualOutput visitor, Object value, String type, String pID, String baseURL, XMPPClient xmppClient, boolean publish, String defaultStyle, String targetWorkspace ) throws Exception;
    public Object visit( XMPPRawDataOutput visitor, Object value, String type, String pID, String baseURL, XMPPClient xmppClient, boolean publish, String defaultStyle, String targetWorkspace ) throws Exception;
}
