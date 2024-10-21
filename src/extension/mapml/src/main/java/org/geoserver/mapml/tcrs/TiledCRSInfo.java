package org.geoserver.mapml.tcrs;

import java.io.Serializable;
import java.util.Set;

public interface TiledCRSInfo extends Cloneable, Serializable {

    Set<String> getAvailableGridsets();

    Set<String> getSelectedTCRSs();
}
