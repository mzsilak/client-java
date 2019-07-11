package eu.arrowhead.client.common.model;

public class OnboardingResponse {
    private boolean success;
    private ServiceEndpoint[] services;
    private String onboardingCertificate;
    private String immediateCertificate;
    private String rootCertificate;
    private String keyAlgorithm;
    private String keyFormat;
    private byte[] privateKey;
    private byte[] publicKey;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ServiceEndpoint[] getServices() {
        return services;
    }

    public void setServices(ServiceEndpoint[] services) {
        this.services = services;
    }

    public String getOnboardingCertificate() {
        return onboardingCertificate;
    }

    public void setOnboardingCertificate(String onboardingCertificate) {
        this.onboardingCertificate = onboardingCertificate;
    }

    public String getImmediateCertificate() {
        return immediateCertificate;
    }

    public void setImmediateCertificate(String immediateCertificate) {
        this.immediateCertificate = immediateCertificate;
    }

    public String getRootCertificate() {
        return rootCertificate;
    }

    public void setRootCertificate(String rootCertificate) {
        this.rootCertificate = rootCertificate;
    }

    public String getKeyAlgorithm() {
        return keyAlgorithm;
    }

    public void setKeyAlgorithm(String keyAlgorithm) {
        this.keyAlgorithm = keyAlgorithm;
    }

    public String getKeyFormat() {
        return keyFormat;
    }

    public void setKeyFormat(String keyFormat) {
        this.keyFormat = keyFormat;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(byte[] privateKey) {
        this.privateKey = privateKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }
}
