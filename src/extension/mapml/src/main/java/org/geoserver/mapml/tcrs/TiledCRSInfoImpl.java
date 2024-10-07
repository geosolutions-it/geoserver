package org.geoserver.mapml.tcrs;

import java.util.Set;
import java.util.TreeSet;

public class TiledCRSInfoImpl implements TiledCRSInfo {

    public static final Set<String> AVAILABLE_GRIDSETS = new TreeSet<>();

    public static final TreeSet<String> SELECTED_TCRS = new TreeSet<>();

    private Set<String> availableGridsests;

    private Set<String> selectedTCRSs;

    public Set<String> getAvailableGridsets() {
        if (availableGridsests == null) {
            availableGridsests = AVAILABLE_GRIDSETS;
        }
        return availableGridsests;
    }

    public Set<String> getSelectedTCRSs() {
        if (selectedTCRSs == null) {
            selectedTCRSs = SELECTED_TCRS;
        }
        return selectedTCRSs;
    }

    public TiledCRSInfoImpl() {

        if (availableGridsests == null) {
            availableGridsests = AVAILABLE_GRIDSETS;
        }

        if (selectedTCRSs == null) {
            selectedTCRSs = SELECTED_TCRS;
        }
    }
}
