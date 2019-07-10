package eu.arrowhead.demo.model;

public class OnboardingRequest {
    private String name;
    private String sharedKey;

    public OnboardingRequest() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSharedKey() {
        return sharedKey;
    }

    public void setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;
    }
}
