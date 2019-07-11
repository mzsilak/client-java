package eu.arrowhead.client.common.model;

import java.net.URI;

public class ServiceEndpoint {

    public enum Type {
        DEVICE_REGISTRY, SYSTEM_REGISTRY, SERVICE_REGISTRY
    }

    private Type system;
    private URI uri;

    public Type getSystem() {
        return system;
    }

    public void setSystem(Type system) {
        this.system = system;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
}

