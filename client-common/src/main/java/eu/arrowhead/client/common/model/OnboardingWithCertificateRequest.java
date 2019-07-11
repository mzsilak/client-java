package eu.arrowhead.client.common.model;

public class OnboardingWithCertificateRequest {

  private String certificateRequest;

  public OnboardingWithCertificateRequest() {
    super();
  }

  public OnboardingWithCertificateRequest(final String certificateRequest) {
    this.certificateRequest = certificateRequest;
  }

  public String getCertificateRequest() {
    return certificateRequest;
  }

  public void setCertificateRequest(final String certificateRequest) {
    this.certificateRequest = certificateRequest;
  }
}
