/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.passwd;

import static net.sf.ezmorph.test.ArrayAssertions.assertEquals;
import static net.sf.json.test.JSONAssert.assertNotNull;

import org.apache.wicket.util.tester.TagTester;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.junit.Test;

public class PasswordPageTest extends AbstractSecurityWicketTestSupport {

    @Test
    public void testMasterPasswordLink() {
        tester.startPage(PasswordPage.class);
        tester.assertRenderedPage(PasswordPage.class);
        TagTester link = tester.getTagById("masterPasswordInfo");
        assertNotNull("Missing master password info link", link);
        assertEquals("../../rest/security/masterpw.xml", link.getAttribute("href"));
        assertEquals("_blank", link.getAttribute("target"));
    }
}
