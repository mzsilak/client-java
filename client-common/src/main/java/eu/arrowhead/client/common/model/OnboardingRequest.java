package eu.arrowhead.client.common.model;

public class OnboardingRequest {
    private String name;

    public OnboardingRequest() {
        super();
    }

    public OnboardingRequest(final String name) {
        super();
        setName(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
