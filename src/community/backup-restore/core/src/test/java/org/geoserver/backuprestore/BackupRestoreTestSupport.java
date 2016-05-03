/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.SystemUtils;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Before;

/**
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class BackupRestoreTestSupport extends GeoServerSystemTestSupport {
    
    static final Set<String> DEFAULT_STYLEs = new HashSet<String>() {{
        add(StyleInfo.DEFAULT_POINT);
        add(StyleInfo.DEFAULT_LINE);
        add(StyleInfo.DEFAULT_GENERIC);
        add(StyleInfo.DEFAULT_POLYGON);
        add(StyleInfo.DEFAULT_RASTER);
    }};
    

    protected Backup backupFacade;
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        
        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("wms", "http://www.opengis.net/wms");
        
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }
    
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no pre-existing test data needed
        testData.setUpSecurity();
    }
    
    @After
    public void cleanCatalog() throws IOException {
        for (StoreInfo s : getCatalog().getStores(StoreInfo.class)) {
            removeStore(s.getWorkspace().getName(), s.getName());
        }
        for (StyleInfo s : getCatalog().getStyles()) {
            String styleName = s.getName();
            if(!DEFAULT_STYLEs.contains(styleName)) {
                removeStyle(null, styleName);
            }
        }
    }
    
    @Before
    public void setupBackupField() {
        backupFacade = (Backup) applicationContext.getBean("backupFacade");
    }
    
    @Override
    protected boolean isMemoryCleanRequired() {
        return SystemUtils.IS_OS_WINDOWS;
    }
    
}
