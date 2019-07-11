package eu.arrowhead.client.common;

import eu.arrowhead.client.common.exception.ArrowheadException;
import eu.arrowhead.client.common.misc.ClientType;
import eu.arrowhead.client.common.model.OnboardingResponse;
import eu.arrowhead.client.common.model.OnboardingWithCertificateRequest;
import eu.arrowhead.client.common.model.ServiceEndpoint;
import java.util.Arrays;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OnboardingClientMain extends ArrowheadClientMain {

  private final Logger logger = LoggerFactory.getLogger(OnboardingClientMain.class);
  protected String onboardingUri;
  protected String deviceRegUri;
  protected String systemRegUri;
  protected String serviceRegUri;
  protected SSLContext sslContext;

  @Override
  protected void init(ClientType client, String[] args, Set<Class<?>> classes, String[] packages) {
    parseArguments(client, args);
    performOnboarding();
    startHttpServer(classes, packages);
  }

  @Override
  protected void parseArguments(ClientType client, String[] args) {
    super.parseArguments(client, args);
    onboardingUri = getOnboardingServiceUrl();
    deviceRegUri = getDeviceRegistryUrl();
    systemRegUri = getSystemRegistryUrl();
    serviceRegUri = getServiceRegistryUrl();

    if(isSecure)
    {
      sslContext = createSSLContextConfigurator().createSSLContext(true);
      Utility.setSSLContext(sslContext);
    }
  }


  protected SSLContextConfigurator createSSLContextConfigurator()
  {
    SSLContextConfigurator sslCon = new SSLContextConfigurator();
    sslCon.setTrustStoreFile(props.getProperty("truststore"));
    sslCon.setTrustStorePass(props.getProperty("truststorepass"));
    return sslCon;
  }

  protected void performOnboarding()
  {
    final OnboardingWithCertificateRequest request = createOnboardingRequest();
    final OnboardingResponse response;

    //Create the full URL (appending "register" to the base URL)
    final String registerUri = UriBuilder.fromPath(onboardingUri).path("certificate").toString();

    //Send the registration request
    final Response r = Utility.sendRequest(registerUri, "POST", request, sslContext);
    response = r.readEntity(OnboardingResponse.class);

    if (!response.isSuccess()) {
      throw new ArrowheadException("Onboarding failed for unknown reasons!");
    }
    alterEndpoints(response.getServices());




    logger.info("Onboarding is successful!");
  }

  protected void alterEndpoints(final ServiceEndpoint[] services)
  {
    logger.debug("Received services from onboarding controller: {}", Arrays.toString(services));

    for (ServiceEndpoint service : services) {
      switch(service.getSystem())
      {
        case DEVICE_REGISTRY:
          deviceRegUri = service.getUri().toString();
          break;
        case SYSTEM_REGISTRY:
          systemRegUri = service.getUri().toString();
          break;
        case SERVICE_REGISTRY:
          serviceRegUri = service.getUri().toString();
          break;
      }
    }
  }

  protected abstract OnboardingWithCertificateRequest createOnboardingRequest();

  @Override
  protected void startServer(Set<Class<?>> classes, String[] packages) {
    final ResourceConfig config = createResourceConfig(classes, packages);
  }

  @Override
  protected void startSecureServer(Set<Class<?>> classes, String[] packages) {

    final ResourceConfig config = createResourceConfig(classes, packages);
  }

  protected ResourceConfig createResourceConfig(Set<Class<?>> classes, String[] packages)
  {
    final ResourceConfig config = new ResourceConfig();
    config.registerClasses(classes);
    config.packages(packages);
    return config;
  }


  private String getOnboardingServiceUrl() {
    String onboardingAddress = props.getProperty("onboarding_address", "127.0.0.1");
    int onboardingPort = isSecure ? props.getIntProperty("onboarding_secure_port", 8435)
                                          : props.getIntProperty("onboarding_insecure_port", 8434);
    return Utility.getUri(onboardingAddress, onboardingPort, "onboarding", isSecure, false);
  }

  private String getDeviceRegistryUrl() {
    String devRegAddress = props.getProperty("device_registry_address", "127.0.0.1");
    int devRegPort = isSecure ? props.getIntProperty("device_registry_secure_port", 8439)
                              : props.getIntProperty("device_registry_insecure_port", 8438);
    return Utility.getUri(devRegAddress, devRegPort, "deviceregistry", isSecure, false);
  }

  private String getSystemRegistryUrl() {
    String sysRegAddress = props.getProperty("system_registry_address", "127.0.0.1");
    int sysRegPort = isSecure ? props.getIntProperty("system_registry_secure_port", 8437)
                              : props.getIntProperty("system_registry_insecure_port", 8436);
    return Utility.getUri(sysRegAddress, sysRegPort, "systemregistry", isSecure, false);
  }

  private String getServiceRegistryUrl() {
    String sysRegAddress = props.getProperty("service_registry_address", "127.0.0.1");
    int sysRegPort = isSecure ? props.getIntProperty("service_registry_secure_port", 8443)
                              : props.getIntProperty("service_registry_insecure_port", 8442);
    return Utility.getUri(sysRegAddress, sysRegPort, "serviceregistry", isSecure, false);
  }
}
