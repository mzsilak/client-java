package eu.arrowhead.client.common.model;

import java.util.Objects;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class OnboardingWithSharedKeyRequest extends OnboardingRequest {

  private String sharedKey;

  public OnboardingWithSharedKeyRequest() {
  }

  public OnboardingWithSharedKeyRequest(final String name, final String sharedKey) {
    super(name);
    setSharedKey(sharedKey);
  }

  public String getSharedKey() {
    return sharedKey;
  }

  public void setSharedKey(final String sharedKey) {
    this.sharedKey = sharedKey;
  }
}
