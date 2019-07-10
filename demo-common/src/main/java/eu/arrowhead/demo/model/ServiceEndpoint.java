package eu.arrowhead.demo.model;

import java.net.URI;

public class ServiceEndpoint {

    private String system;
    private URI uri;

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
}

