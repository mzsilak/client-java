package eu.arrowhead.client.common;

import eu.arrowhead.client.common.exception.ArrowheadException;
import eu.arrowhead.client.common.exception.AuthException;
import eu.arrowhead.client.common.exception.ExceptionType;
import eu.arrowhead.client.common.misc.ClientType;
import eu.arrowhead.client.common.misc.SecurityUtils;
import eu.arrowhead.client.common.model.ArrowheadDevice;
import eu.arrowhead.client.common.model.ArrowheadService;
import eu.arrowhead.client.common.model.ArrowheadSystem;
import eu.arrowhead.client.common.model.DeviceRegistryEntry;
import eu.arrowhead.client.common.model.OnboardingResponse;
import eu.arrowhead.client.common.model.OnboardingWithCertificateRequest;
import eu.arrowhead.client.common.model.ServiceRegistryEntry;
import eu.arrowhead.client.common.model.SystemEndpoint;
import eu.arrowhead.client.common.model.SystemRegistryEntry;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
  protected KeyStore[] keyStores;

  @Override
  protected void init(ClientType client, String[] args, Set<Class<?>> classes, String[] packages) {
    try {
      parseArguments(client, args);
      keyStores = performOnboarding();

      if (isSecure) {
        // build a new ssl context with out certificate
        sslContext = createSSLContextConfigurator().createSSLContext(true);
        Utility.setSSLContext(sslContext);
      }

      startHttpServer(classes, packages);
    } catch (Exception e) {
      throw new RuntimeException("Onboarding failed: " + e.getMessage(), e);
    }
  }

  @Override
  protected void parseArguments(ClientType client, String[] args) {
    super.parseArguments(client, args);
    onboardingUri = getOnboardingServiceUrl();
    deviceRegUri = getDeviceRegistryUrl();
    systemRegUri = getSystemRegistryUrl();
    serviceRegUri = getServiceRegistryUrl();

    if (isSecure) {
      sslContext = createOnboardingSSLContextConfigurator().createSSLContext(true);
      Utility.setSSLContext(sslContext);
    }
  }

  protected SSLContextConfigurator createOnboardingSSLContextConfigurator() {
    SSLContextConfigurator sslCon = new SSLContextConfigurator();
    sslCon.setTrustStoreFile(props.getProperty("truststore"));
    sslCon.setTrustStorePass(props.getProperty("truststorepass"));
    return sslCon;
  }

  protected KeyStore[] performOnboarding() throws InvalidKeySpecException, NoSuchAlgorithmException {
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

    //Get the reconstructed certs from the CA response
    X509Certificate signedCert = getCertFromString(response.getOnboardingCertificate());
    X509Certificate cloudCert = getCertFromString(response.getIntermediateCertificate());
    X509Certificate rootCert = getCertFromString(response.getRootCertificate());

    final String commonName = SecurityUtils.getCertCNFromSubject(signedCert.getSubjectDN().getName());
    final String cloudName = SecurityUtils.getCertCNFromSubject(cloudCert.getSubjectDN().getName());

    final String keyStorePassword =
        !Utility.isBlank(props.getProperty("keystorepass")) ? props.getProperty("keystorepass")
                                                            : Utility.getRandomPassword();
    final String trustStorePassword =
        !Utility.isBlank(props.getProperty("truststorepass")) ? props.getProperty("truststorepass")
                                                              : Utility.getRandomPassword();

    final String keyStoreFile = props.getProperty("keystore");
    final String trustStoreFile = props.getProperty("truststore");

    final byte[] rawPrivateKey = SecurityUtils.loadPEM(props.getProperty("private_key"));
    final PrivateKey privateKey = decode(response.getKeyAlgorithm(), rawPrivateKey);

    //Create the System KeyStore
    KeyStore[] keyStores = new KeyStore[2];
    try (final FileOutputStream out = new FileOutputStream(keyStoreFile)) {
      KeyStore ks = KeyStore.getInstance("pkcs12");
      ks.load(null, keyStorePassword.toCharArray());
      Certificate[] chain = new Certificate[]{signedCert, cloudCert, rootCert};
      ks.setKeyEntry(commonName, privateKey, keyStorePassword.toCharArray(), chain);
      keyStores[0] = ks;

      ks.store(out, keyStorePassword.toCharArray());

    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      throw new ArrowheadException("System key store creation failed!", e);
    }

    /*
      Create the Cloud KeyStore (with a different KeyStore Entry type,
      since we do not have the private key for the cloud cert)
     */
    try (final FileOutputStream out = new FileOutputStream(trustStoreFile)) {
      KeyStore ks = KeyStore.getInstance("pkcs12");
      ks.load(null, trustStorePassword.toCharArray());
      KeyStore.Entry certEntry = new KeyStore.TrustedCertificateEntry(cloudCert);
      ks.setEntry(cloudName, certEntry, null);
      keyStores[1] = ks;

      ks.store(out, trustStorePassword.toCharArray());

    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      throw new ArrowheadException("System key store creation failed!", e);
    }

    logger.info("Onboarding is successful!");
    return keyStores;
  }

  private PrivateKey decode(final String algorithm, final byte[] rawPrivateKey)
      throws NoSuchAlgorithmException, InvalidKeySpecException {

    final KeyFactory kf = KeyFactory.getInstance(algorithm);

    //byte[] privateKeyData = Base64.getDecoder().decode(rawPrivateKey);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(rawPrivateKey);
    return kf.generatePrivate(spec);
  }

  //Convert PEM encoded cert back to an X509Certificate
  @SuppressWarnings("Duplicates")
  private static X509Certificate getCertFromString(String encodedCert) {
    try {
      byte[] rawCert = Base64.getDecoder().decode(encodedCert);
      ByteArrayInputStream bIn = new ByteArrayInputStream(rawCert);
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      return (X509Certificate) cf.generateCertificate(bIn);
    } catch (CertificateException e) {
      throw new AuthException("Encapsulated exceptions...", e);
    }
  }

  protected void alterEndpoints(final SystemEndpoint[] services) {
    logger.debug("Received services from onboarding controller: {}", Arrays.toString(services));

    for (SystemEndpoint service : services) {
      switch (service.getSystem()) {
        case DEVICE_REGISTRY_SERVICE:
          deviceRegUri = service.getUri().toString();
          break;
        case SYSTEM_REGISTRY_SERVICE:
          systemRegUri = service.getUri().toString();
          break;
        case SERVICE_REGISTRY_SERVICE:
          serviceRegUri = service.getUri().toString();
          break;
      }
    }
  }


  protected SystemRegistryEntry compileSystemRegistrationPayload(final ArrowheadDevice device) {
    final SystemRegistryEntry entry = new SystemRegistryEntry();
    final ArrowheadSystem system = new ArrowheadSystem();

    system.setAddress(ipAddress);
    system.setPort(port);
    system.setSystemName(props.getProperty("system_name"));

    entry.setProvider(device);
    entry.setProvidedSystem(system);
    entry.setServiceURI(props.getProperty("service_uri"));

    return entry;
  }

  protected DeviceRegistryEntry compileDeviceRegistrationPayload() {
    final DeviceRegistryEntry entry = new DeviceRegistryEntry();
    final ArrowheadDevice device = new ArrowheadDevice();

    device.setDeviceName(props.getProperty("device_name"));

    entry.setMacAddress(findMacAddress());
    entry.setProvidedDevice(device);
    return entry;
  }

  private String findMacAddress() {
    try {
      InetAddress address = InetAddress.getByName(ipAddress);
      return Utility.getHardwareAddress(address);
    } catch (UnknownHostException e) {
      return null;
    }
  }


  protected ServiceRegistryEntry compileServiceRegistrationPayload(final ArrowheadSystem system) {
    //Compile the ArrowheadService (providedService)
    String serviceDef = props.getProperty("service_name");
    String serviceUri = props.getProperty("service_uri");
    String interfaceList = props.getProperty("interfaces");
    Set<String> interfaces = new HashSet<>();
    if (interfaceList != null && !interfaceList.isEmpty()) {
      //Interfaces are read from a comma separated list
      interfaces.addAll(Arrays.asList(interfaceList.replaceAll("\\s+", "").split(",")));
    }
    Map<String, String> metadata = new HashMap<>();
    String metadataString = props.getProperty("metadata");
    if (metadataString != null && !metadataString.isEmpty()) {
      //Metadata in the properties file: key1-value1, key2-value2, ...
      String[] parts = metadataString.split(",");
      for (String part : parts) {
        String[] pair = part.split("-");
        metadata.put(pair[0], pair[1]);
      }
    }
    ArrowheadService service = new ArrowheadService(serviceDef, interfaces, metadata);

    //Return the complete request payload
    return new ServiceRegistryEntry(service, system, serviceUri);
  }

  private void registerToServiceRegistry(ServiceRegistryEntry entry) {
    //Create the full URL (appending "register" to the base URL)

    //Send the registration request
    try {
      Utility.sendRequest(serviceRegUri, "POST", entry);
    } catch (ArrowheadException e) {
      /*
        Service Registry might return duplicate entry exception, if a previous instance of the web server already
        registered this service,
        and the deregistration did not happen. It's better to unregister the old entry, in case the request payload
        changed.
       */
      if (e.getExceptionType() == ExceptionType.DUPLICATE_ENTRY) {
        System.out
            .println("Received DuplicateEntryException from SR, sending delete request and then registering again.");
        unregisterFromServiceRegistry(entry);
        Utility.sendRequest(serviceRegUri, "POST", entry);
      } else {
        throw e;
      }
    }
    System.out.println("Registering service is successful!");
  }

  private void unregisterFromServiceRegistry(ServiceRegistryEntry entry) {
    //Create the full URL (appending "remove" to the base URL)
    String removeUri = serviceRegUri.replace("register", "remove");
    Utility.sendRequest(removeUri, "PUT", entry);
    System.out.println("Removing service is successful!");
  }


  protected <T> T register(final T o, final String url) {
    return register(o, url, "publish", "unpublish");
  }

  protected <T> T register(final T o, final String url, final String suffix, final String errorSuffix) {
    final String registerUri = UriBuilder.fromPath(url).toString();
    Response response;
    T entity;

    //Send the registration request
    try {
      response = Utility.sendRequest(registerUri, "POST", o);
      entity = (T) response.readEntity(o.getClass());
    } catch (ArrowheadException e) {
      /*
        Service Registry might return duplicate entry exception, if a previous instance of the web server already
        registered this service,
        and the deregistration did not happen. It's better to unregister the old entry, in case the request payload
        changed.
       */
      if (e.getExceptionType() == ExceptionType.DUPLICATE_ENTRY) {
        logger.info("Received DuplicateEntryException from SR, sending delete request and then registering again.");
        unregister(o, url, suffix, errorSuffix);
        response = Utility.sendRequest(registerUri, "POST", o);
        entity = (T) response.readEntity(o.getClass());
      } else {
        throw e;
      }
    }
    logger.info("Registration successful!");
    return entity;
  }

  protected <T> T registerService(final T o, final String url, final String suffix, final String errorSuffix) {
    final String registerUri = UriBuilder.fromPath(url).toString();
    Response response;
    T entity;

    //Send the registration request
    try {
      response = Utility.sendRequest(registerUri, "POST", o);
      entity = (T) response.readEntity(o.getClass());
    } catch (ArrowheadException e) {
      /*
        Service Registry might return duplicate entry exception, if a previous instance of the web server already
        registered this service,
        and the deregistration did not happen. It's better to unregister the old entry, in case the request payload
        changed.
       */
      if (e.getExceptionType() == ExceptionType.DUPLICATE_ENTRY) {
        logger.info("Received DuplicateEntryException from SR, sending delete request and then registering again.");
        unregisterService(o, url, suffix, errorSuffix);
        response = Utility.sendRequest(registerUri, "POST", o);
        entity = (T) response.readEntity(o.getClass());
      } else {
        throw e;
      }
    }
    logger.info("Registration successful!");
    return entity;
  }

  protected void unregister(final Object o, final String url) {
    unregister(o, url, "publish", "unpublish");
  }

  protected void unregister(final Object o, final String url, final String suffix, final String errorSuffix) {
    try {

      String removeUri = url.replace(suffix, errorSuffix).toString();
      logger.debug("Contacting {}", removeUri);
      Utility.sendRequest(removeUri, "POST", o);
      logger.info("Removed object successfully!");
    } catch (Exception ex) {
      logger.warn("Unknown exception during unregister: {}", ex.getMessage());
    }
  }

  protected void unregisterService(final Object o, final String url, final String suffix, final String errorSuffix) {
    try {

      String removeUri = url.replace(suffix, errorSuffix).toString();
      logger.debug("Contacting {}", removeUri);
      Utility.sendRequest(removeUri, "PUT", o);
      logger.info("Removed object successfully!");
    } catch (Exception ex) {
      logger.warn("Unknown exception during unregister: {}", ex.getMessage());
    }
  }

  protected OnboardingWithCertificateRequest createOnboardingRequest() {
    final OnboardingWithCertificateRequest request = new OnboardingWithCertificateRequest();
    final byte[] rawData = SecurityUtils.loadPEM(props.getProperty("cert_sign_req"));
    request.setCertificateRequest(Base64.getEncoder().encodeToString(rawData));
    return request;
  }

  @Override
  protected void startServer(Set<Class<?>> classes, String[] packages) {
    final ResourceConfig config = createResourceConfig(classes, packages);
  }

  @Override
  protected void startSecureServer(Set<Class<?>> classes, String[] packages) {

    final ResourceConfig config = createResourceConfig(classes, packages);
  }

  protected ResourceConfig createResourceConfig(Set<Class<?>> classes, String[] packages) {
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
