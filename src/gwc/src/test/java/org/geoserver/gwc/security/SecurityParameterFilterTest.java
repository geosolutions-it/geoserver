/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class SecurityParameterFilterTest {

    @Test
    public void testApplyPassesThrough() {
        SecurityParameterFilter filter = new SecurityParameterFilter(SecurityParameterFilter.ACCESS_LIMITS_KEY);
        assertEquals("somekey", filter.apply("somekey"));
    }

    @Test
    public void testApplyNullReturnsDefault() {
        SecurityParameterFilter filter = new SecurityParameterFilter(SecurityParameterFilter.ACCESS_LIMITS_KEY);
        assertEquals("", filter.apply(null));
    }

    @Test
    public void testReadResolveRejected() {
        // synthetic filter must never be reconstructed from persisted config
        SecurityParameterFilter filter = new SecurityParameterFilter(SecurityParameterFilter.ACCESS_LIMITS_KEY);
        assertThrows(UnsupportedOperationException.class, filter::readResolve);
    }
}
