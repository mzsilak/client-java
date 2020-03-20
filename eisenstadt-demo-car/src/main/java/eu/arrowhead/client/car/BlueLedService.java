package eu.arrowhead.client.car;

import eu.arrowhead.client.common.ArrowheadClientMain;
import eu.arrowhead.client.common.Utility;
import eu.arrowhead.client.common.exception.ArrowheadException;
import eu.arrowhead.client.common.exception.ExceptionType;
import eu.arrowhead.client.common.misc.ClientType;
import eu.arrowhead.client.common.model.ArrowheadService;
import eu.arrowhead.client.common.model.ArrowheadSystem;
import eu.arrowhead.client.common.model.ServiceRegistryEntry;
import eu.arrowhead.demo.grovepi.ButtonPressedListener;
import eu.arrowhead.demo.grovepi.GroveButtonObserver;
import eu.arrowhead.demo.grovepi.ControllableLed;
import eu.arrowhead.demo.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iot.raspberry.grovepi.GrovePi;
import org.iot.raspberry.grovepi.pi4j.GrovePi4J;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/*
  BasicProviderMain class has the following mandatory functionalities:
    1) Read in command line, and config file properties
    2) Start a normal HTTP server with REST resource classes
    3) Register its service into the Service Registry
 */
public class BlueLedService extends ArrowheadClientMain implements ButtonPressedListener {

    private static String SR_BASE_URI;
    private final Logger logger = LogManager.getLogger();
    final ExecutorService executorService;
    private final ControllableLed onboardingStatus;
    private final GroveButtonObserver buttonObserver;
    private final AtomicBoolean connected;

    private DeviceRegistryEntry deviceRegistryEntry;
    private ArrowheadDevice arrowheadDevice;
    private SystemRegistryEntry systemRegistryEntry;
    private ArrowheadSystem arrowheadSystem;
    private ServiceRegistryEntry serviceRegistryEntry;
    private ArrowheadService arrowheadService;

    private BlueLedService(String[] args) throws IOException {
        final GrovePi grovePi = new GrovePi4J();
        BlueLedResource.GROVE_PI = grovePi;

        //Register the application components the REST library need to know about
        Set<Class<?>> classes = new HashSet<>(Collections.singletonList(BlueLedResource.class));
        String[] packages = {"eu.arrowhead.client.common","eu.arrowhead.client.provider"};
        //This (inherited) method reads in the configuration properties, and starts the web server
        init(ClientType.PROVIDER, args, classes, packages);

        executorService = Executors.newCachedThreadPool();

        buttonObserver = new GroveButtonObserver(grovePi);
        buttonObserver.setListener(this);
        executorService.submit(buttonObserver);

        connected = new AtomicBoolean(false);
        onboardingStatus = new ControllableLed(executorService, grovePi);
        onboardingStatus.turnOff();

        //Listen for a stop command
        listenForInput();

        try {
            finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        System.getProperties().put("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        //Remove the command line argument for secure mode, if present, since the Basic Provider only operates in insecure mode.
        List<String> list = new ArrayList<>(Arrays.asList(args));
        list.remove("-tls");
        args = list.toArray(new String[0]);
        new BlueLedService(args);
    }

    @Override
    public void trigger() {
        onboardingStatus.turnOff();

        if (connected.get()) {
            try {
                logger.info("Performing disconnect!");
                disconnectFromOnboarding();
                connected.set(false);
                logger.info("Disconnected!");
            } catch (final Exception ex) {
                onboardingStatus.error();
                logger.fatal(ex.getMessage(), ex);
            }
        } else {
            try {
                logger.info("Performing connect!");
                onboardingStatus.blink();
                performOnboarding();
                connected.set(true);
                onboardingStatus.turnOn();
                logger.info("Connected!");
            } catch (final Exception ex) {
                onboardingStatus.error();
                logger.fatal(ex.getMessage(), ex);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        disconnectFromOnboarding();

        onboardingStatus.turnOff();
        buttonObserver.stop();
        executorService.shutdown();
    }

    private void performOnboarding() {
        final String onboardingUrl = getOnboardingServiceUrl();
        final OnboardingRequest request = new OnboardingRequest();
        final OnboardingResponse response;

        request.setName("demo-provider");
        request.setSharedKey("secret");

        response = connectToOnboardingController(request, onboardingUrl);
        final ServiceEndpoint[] services = response.getServices();

        logger.debug("Received services from onboarding controller: {}", Arrays.toString(services));

        //Compile the base of the Service Registry URL
        getServiceRegistryUrl();

        logger.debug("Registering Device");
        deviceRegistryEntry = register(compileDeviceRegistrationPayload(), getDeviceRegistryUrl());
        arrowheadDevice = deviceRegistryEntry.getProvidedDevice();

        logger.debug("Registering System");
        systemRegistryEntry = register(compileSystemRegistrationPayload(), getSystemRegistryUrl());
        arrowheadSystem = systemRegistryEntry.getProvidedSystem();

        logger.debug("Registering Service");
        serviceRegistryEntry = registerService(compileServiceRegistrationPayload(), SR_BASE_URI, "register", "remove");
        arrowheadService = serviceRegistryEntry.getProvidedService();
    }

    private void disconnectFromOnboarding() {
        try{

            if (serviceRegistryEntry != null) {
                unregisterService(serviceRegistryEntry, SR_BASE_URI, "remove");
                serviceRegistryEntry = null;
                arrowheadService = null;
            }
            if (systemRegistryEntry != null) {
                unregister(systemRegistryEntry, getSystemRegistryUrl());
                systemRegistryEntry = null;
                arrowheadService = null;
            }
            if (deviceRegistryEntry != null) {
                unregister(deviceRegistryEntry, getDeviceRegistryUrl());
                deviceRegistryEntry = null;
                arrowheadDevice = null;
            }
        }
        catch(Exception ex)
        {
            logger.warn("Error during disconnect: {}", ex.getMessage());
        }
        finally
        {
            onboardingStatus.turnOff();
        }
    }

    private String getOnboardingServiceUrl() {
        String srAddress = props.getProperty("onboarding_address", "0.0.0.0");
        int srPort = props.getIntProperty("onboarding_insecure_port", 8434);
        return Utility.getUri(srAddress, srPort, "onboarding", false, false);
    }

    private String getDeviceRegistryUrl() {
        String srAddress = props.getProperty("device_address", "0.0.0.0");
        int srPort = props.getIntProperty("device_insecure_port", 8438);
        return Utility.getUri(srAddress, srPort, "deviceregistry", false, false);
    }

    private String getSystemRegistryUrl() {
        String srAddress = props.getProperty("system_address", "0.0.0.0");
        int srPort = props.getIntProperty("system_insecure_port", 8436);
        return Utility.getUri(srAddress, srPort, "systemregistry", false, false);
    }

    private void getServiceRegistryUrl() {
        String srAddress = props.getProperty("sr_address", "0.0.0.0");
        int srPort = props.getIntProperty("sr_insecure_port", 8442);
        SR_BASE_URI = Utility.getUri(srAddress, srPort, "serviceregistry", false, false);
    }

    private DeviceRegistryEntry compileDeviceRegistrationPayload() {
        if (deviceRegistryEntry == null) {
            deviceRegistryEntry = new DeviceRegistryEntry();
            deviceRegistryEntry.setProvidedDevice(compileArrowheadDevice());
        }
        return deviceRegistryEntry;
    }

    private ArrowheadDevice compileArrowheadDevice() {
        if (arrowheadDevice == null) {
            arrowheadDevice = new ArrowheadDevice();
            arrowheadDevice.setDeviceName("BlueLedDevice");
        }
        return arrowheadDevice;
    }

    private SystemRegistryEntry compileSystemRegistrationPayload() {
        if (systemRegistryEntry == null) {
            systemRegistryEntry = new SystemRegistryEntry();
            systemRegistryEntry.setProvider(compileArrowheadDevice());
            systemRegistryEntry.setProvidedSystem(compileArrowheadSystem());
        }
        return systemRegistryEntry;
    }

    private ArrowheadSystem compileArrowheadSystem() {
        if (arrowheadSystem == null) {
            arrowheadSystem = new ArrowheadSystem();
            arrowheadSystem.setSystemName("RaspberryPi");
            arrowheadSystem.setPort(props.getIntProperty("insecure_port", 8460));
            try {
                arrowheadSystem.setAddress(InetAddress.getLocalHost().getHostAddress());
            } catch (final UnknownHostException e) {
                arrowheadSystem.setAddress("0.0.0.0");
            }
        }
        return arrowheadSystem;
    }

    private ServiceRegistryEntry compileServiceRegistrationPayload() {
        if (serviceRegistryEntry == null) {
            serviceRegistryEntry = new ServiceRegistryEntry();
            serviceRegistryEntry.setProvider(compileArrowheadSystem());
            serviceRegistryEntry.setProvidedService(compileArrowheadService());
            serviceRegistryEntry.setServiceUri(BlueLedResource.SERVICE_URI);
            serviceRegistryEntry.setUdp(false);
        }
        return serviceRegistryEntry;
    }

    private ArrowheadService compileArrowheadService() {
        if (arrowheadService == null) {
            final Set<String> interfaces = new HashSet<>();
            interfaces.add("JSON");

            arrowheadService = new ArrowheadService();
            arrowheadService.setServiceDefinition("BlueLedService");
            arrowheadService.setInterfaces(interfaces);
        }
        return arrowheadService;
    }

    private OnboardingResponse connectToOnboardingController(OnboardingRequest request, String onboardingUrl) {
        //Create the full URL (appending "register" to the base URL)
        final String registerUri = UriBuilder.fromPath(onboardingUrl).path("sharedKey").toString();
        final OnboardingResponse response;


        //Send the registration request
        final Response r = Utility.sendRequest(registerUri, "POST", request);
        response = r.readEntity(OnboardingResponse.class);

        if (!response.isSuccess()) {
            throw new ArrowheadException("Onboarding failed for unknown reasons!");
        }
        logger.info("Onboarding is successful!");
        return response;
    }

    private <T> T register(final T o, final String url) {
        return register(o, url, "publish", "unpublish");
    }

    private <T> T  register(final T o, final String url, final String suffix, final String errorSuffix) {
        final String registerUri = UriBuilder.fromPath(url).path(suffix).toString();
        Response response;
        T entity;

        //Send the registration request
        try {
            response = Utility.sendRequest(registerUri, "POST", o);
            entity = (T) response.readEntity(o.getClass());
        } catch (ArrowheadException e) {
      /*
        Service Registry might return duplicate entry exception, if a previous instance of the web server already registered this service,
        and the deregistration did not happen. It's better to unregister the old entry, in case the request payload changed.
       */
            if (e.getExceptionType() == ExceptionType.DUPLICATE_ENTRY) {
                logger.info("Received DuplicateEntryException from SR, sending delete request and then registering again.");
                unregister(o, url, errorSuffix);
                response = Utility.sendRequest(registerUri, "POST", o);
                entity = (T) response.readEntity(o.getClass());
            } else {
                throw e;
            }
        }
        logger.info("Registration successful!");
        return entity;
    }

    private <T> T  registerService(final T o, final String url, final String suffix, final String errorSuffix) {
        final String registerUri = UriBuilder.fromPath(url).path(suffix).toString();
        Response response;
        T entity;

        //Send the registration request
        try {
            response = Utility.sendRequest(registerUri, "POST", o);
            entity = (T) response.readEntity(o.getClass());
        } catch (ArrowheadException e) {
      /*
        Service Registry might return duplicate entry exception, if a previous instance of the web server already registered this service,
        and the deregistration did not happen. It's better to unregister the old entry, in case the request payload changed.
       */
            if (e.getExceptionType() == ExceptionType.DUPLICATE_ENTRY) {
                logger.info("Received DuplicateEntryException from SR, sending delete request and then registering again.");
                unregister(o, url, errorSuffix);
                response = Utility.sendRequest(registerUri, "PUT", o);
                entity = (T) response.readEntity(o.getClass());
            } else {
                throw e;
            }
        }
        logger.info("Registration successful!");
        return entity;
    }
    private void unregister(final Object o, final String url) {
        unregister(o, url, "unpublish");
    }

    private void unregister(final Object o, final String url, final String suffix) {
        try {

            String removeUri = UriBuilder.fromPath(url).path(suffix).toString();
            logger.debug("Contacting {}", removeUri);
            Utility.sendRequest(removeUri, "POST", o);
            logger.info("Removed object successfully!");
        } catch(Exception ex)
        {
            logger.warn("Unknown exception during unregister: {}", ex.getMessage());
        }
    }

    private void unregisterService(final Object o, final String url, final String suffix) {
        try {

            String removeUri = UriBuilder.fromPath(url).path(suffix).toString();
            logger.debug("Contacting {}", removeUri);
            Utility.sendRequest(removeUri, "PUT", o);
            logger.info("Removed object successfully!");
        } catch(Exception ex)
        {
            logger.warn("Unknown exception during unregister: {}", ex.getMessage());
        }
    }
}