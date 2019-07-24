package eu.arrowhead.client.common.model;

import java.net.URI;

public class SystemEndpoint {

    public enum Type {
        DEVICE_REGISTRY_SERVICE, SYSTEM_REGISTRY_SERVICE, SERVICE_REGISTRY_SERVICE, ORCH_SERVICE
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

