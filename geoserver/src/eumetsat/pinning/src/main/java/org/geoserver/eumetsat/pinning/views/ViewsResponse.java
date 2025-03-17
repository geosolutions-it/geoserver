package org.geoserver.eumetsat.pinning.views;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ViewsResponse {

    @JsonProperty("_embedded")
    private Embedded embedded;

    public Embedded getEmbedded() {
        return embedded;
    }

    public void setEmbedded(Embedded embedded) {
        this.embedded = embedded;
    }

    public static class Embedded {
        @JsonProperty("preferences")
        private List<View> preferences;

        public List<View> getPreferences() {
            return preferences;
        }

        public void setPreferences(List<View> preferences) {
            this.preferences = preferences;
        }
    }
}
