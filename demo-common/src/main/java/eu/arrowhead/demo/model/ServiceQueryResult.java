package eu.arrowhead.demo.model;

import eu.arrowhead.client.common.model.ServiceRegistryEntry;

import java.util.ArrayList;
import java.util.List;

public class ServiceQueryResult {
    private List<ServiceRegistryEntry> serviceQueryData = new ArrayList<>();

    public ServiceQueryResult() {
    }

    public List<ServiceRegistryEntry> getServiceQueryData() {
        return serviceQueryData;
    }

    public void setServiceQueryData(List<ServiceRegistryEntry> serviceQueryData) {
        this.serviceQueryData = serviceQueryData;
    }
}
