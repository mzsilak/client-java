package eu.arrowhead.client.common.model;

import java.time.LocalDateTime;

public class SystemRegistryEntry {
    private Long id;
    private ArrowheadSystem providedSystem;
    private ArrowheadDevice provider;
    private String serviceURI;
    private LocalDateTime endOfValidity;

    public SystemRegistryEntry() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ArrowheadSystem getProvidedSystem() {
        return providedSystem;
    }

    public void setProvidedSystem(ArrowheadSystem providedSystem) {
        this.providedSystem = providedSystem;
    }

    public ArrowheadDevice getProvider() {
        return provider;
    }

    public void setProvider(ArrowheadDevice provider) {
        this.provider = provider;
    }

    public String getServiceURI() {
        return serviceURI;
    }

    public void setServiceURI(String serviceURI) {
        this.serviceURI = serviceURI;
    }

    public LocalDateTime getEndOfValidity() {
        return endOfValidity;
    }

    public void setEndOfValidity(LocalDateTime endOfValidity) {
        this.endOfValidity = endOfValidity;
    }
}
