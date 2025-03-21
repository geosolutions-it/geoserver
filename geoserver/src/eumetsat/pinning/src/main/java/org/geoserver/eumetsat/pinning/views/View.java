package org.geoserver.eumetsat.pinning.views;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class View {

    @JsonProperty("id")
    private Long viewId;

    @JsonProperty("disabled")
    private Boolean disabled;

    @JsonProperty("preference")
    private String preferenceJson; // JSON string to be parsed

    public Long getViewId() {
        return viewId;
    }

    public void setViewId(Long viewId) {
        this.viewId = viewId;
    }

    public String getPreferenceJson() {
        return preferenceJson;
    }

    public void setPreferenceJson(String preferenceJson) {
        this.preferenceJson = preferenceJson;
    }

    public Boolean getDisabled() { return disabled; }

    public void setDisabled(Boolean disabled) { this.disabled = disabled; }
}
