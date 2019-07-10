package eu.arrowhead.demo.model;

import eu.arrowhead.client.common.model.ArrowheadService;

public class ServiceQueryForm {
    private ArrowheadService service;
    private boolean pingProviders;
    private boolean metadataSearch;
    private Integer version;

    public ServiceQueryForm()
    {

    }

    public ArrowheadService getService() {
        return service;
    }

    public void setService(ArrowheadService service) {
        this.service = service;
    }

    public boolean isPingProviders() {
        return pingProviders;
    }

    public void setPingProviders(boolean pingProviders) {
        this.pingProviders = pingProviders;
    }

    public boolean isMetadataSearch() {
        return metadataSearch;
    }

    public void setMetadataSearch(boolean metadataSearch) {
        this.metadataSearch = metadataSearch;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
