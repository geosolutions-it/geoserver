/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerSecurityTestSupport;
import org.geoserver.security.validation.MasterPasswordChangeException;
import org.geoserver.test.SystemTest;
import org.geotools.util.URLs;
import org.junit.Test;
import org.junit.experimental.categories.Category;

// @TestSetup(run=TestSetupFrequency.REPEAT)
@Category(SystemTest.class)
public class MasterPasswordResetTest extends GeoServerSecurityTestSupport {

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add(
                getClass().getResource(getClass().getSimpleName() + "-context.xml").toString());
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        applicationContext
                .getBeanFactory()
                .registerSingleton("testMasterPasswordProvider", new TestMasterPasswordProvider());
    }

    @Test
    public void testMasterPasswordReset() throws Exception {
        String masterPWAsString = getMasterPassword();
        MasterPasswordConfig config = getSecurityManager().getMasterPasswordConfig();

        URLMasterPasswordProviderConfig mpConfig =
                (URLMasterPasswordProviderConfig)
                        getSecurityManager()
                                .loadMasterPassswordProviderConfig(config.getProviderName());

        assertTrue(
                mpConfig.getURL()
                        .toString()
                        .endsWith(URLMasterPasswordProviderConfig.MASTER_PASSWD_FILENAME));
        getSecurityManager().getKeyStoreProvider().reloadKeyStore();

        try {
            getSecurityManager().saveMasterPasswordConfig(config, null, null, null);
            fail();
        } catch (MasterPasswordChangeException ex) {
        }

        ///// First change reset property
        mpConfig = new URLMasterPasswordProviderConfig();
        mpConfig.setName("rw");
        mpConfig.setClassName(URLMasterPasswordProvider.class.getCanonicalName());
        mpConfig.setReadOnly(false);
        mpConfig.setReset(true);
        
        File tmp = new File(getSecurityManager().get("security").dir(), "mpw1.properties");
        mpConfig.setURL(URLs.fileToUrl(tmp));
        getSecurityManager().saveMasterPasswordProviderConfig(mpConfig);

        config = getSecurityManager().getMasterPasswordConfig();
        config.setProviderName(mpConfig.getName());
        
        getSecurityManager().getKeyStoreProvider().commitMasterPasswordChange();
        
        System.out.println(getSecurityManager().getKeyStoreProvider().getConfigPasswordKey());
        
        getSecurityManager().saveMasterPasswordConfig(config);
        
        mpConfig.setReset(false);
        mpConfig.setURL(URLs.fileToUrl(tmp));
        
        // getSecurityManager().saveMasterPasswordProviderConfig(mpConfig);
        getSecurityManager().saveMasterPasswordConfig(config);
        
        System.out.println(getSecurityManager().getKeyStoreProvider().getConfigPasswordKey());
        
        System.out.println(getMasterPassword());
        
        System.out.println(getSecurityManager().getKeyStoreProvider().getConfigPasswordKey());
        
        try {
            getSecurityManager()
                    .saveMasterPasswordConfig(
                            config,
                            masterPWAsString.toCharArray(),
                            "geoserver1".toCharArray(),
                            "geoserver1".toCharArray());
            fail();
        } catch (Exception ex) {
            // Any operation from now on will throw a tempered exception until the SecurityManager is not reloaded with the random password.
            assertTrue(ex.getCause().getMessage().contains("Keystore was tampered with, or password was incorrect"));
        }
        
        System.out.println(FileUtils.readFileToString(tmp));
    }
}
