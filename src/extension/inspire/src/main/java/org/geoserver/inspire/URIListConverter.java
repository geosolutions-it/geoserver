/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/** XStream converter for {@link UniqueResourceIdentifiers} */
public final class URIListConverter extends com.thoughtworks.xstream.converters.collections.CollectionConverter {
    public URIListConverter(com.thoughtworks.xstream.mapper.Mapper mapper) {
        super(mapper);
    }

    @Override
    public boolean canConvert(Class type) {
        return org.geoserver.inspire.UniqueResourceIdentifiers.class.isAssignableFrom(type);
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        java.util.List<UniqueResourceIdentifier> list = new java.util.ArrayList<>();
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            // This is the XML being produced by the inspire plugin with missing identifiers
            // <entry key="inspire.spatialDatasetIdentifier">
            //   <list>
            //      <size>0</size>
            //   </list>
            // </entry>
            if ("size".equals(reader.getNodeName())) {
                // Ignore legacy <size>0</size>
                reader.moveUp();
                continue;
            }
            list.add((UniqueResourceIdentifier) context.convertAnother(null, UniqueResourceIdentifiers.class));
            reader.moveUp();
        }
        return new org.geoserver.inspire.UniqueResourceIdentifiers(list);
    }
}
