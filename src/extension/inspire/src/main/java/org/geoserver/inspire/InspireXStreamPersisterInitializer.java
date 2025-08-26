/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.springframework.stereotype.Component;

@Component
public class InspireXStreamPersisterInitializer implements XStreamPersisterInitializer {
    @Override
    public void init(XStreamPersister persister) {
        XStream xs = persister.getXStream();
        xs.allowTypes(new Class<?>[] {
            org.geoserver.inspire.UniqueResourceIdentifiers.class, org.geoserver.inspire.UniqueResourceIdentifier.class
        });
        xs.registerConverter(new URIListConverter(xs.getMapper()), XStream.PRIORITY_VERY_HIGH);
        persister.registerBreifMapComplexType("inspire.spatialDatesetIdentifier", UniqueResourceIdentifiers.class);
    }
}
