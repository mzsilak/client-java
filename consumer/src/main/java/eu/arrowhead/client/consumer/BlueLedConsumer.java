package eu.arrowhead.client.consumer;

import eu.arrowhead.client.common.ArrowheadClientMain;
import eu.arrowhead.client.common.Utility;
import eu.arrowhead.client.common.exception.ArrowheadException;
import eu.arrowhead.client.common.exception.ExceptionType;
import eu.arrowhead.client.common.misc.ClientType;
import eu.arrowhead.client.common.model.ArrowheadService;
import eu.arrowhead.client.common.model.ArrowheadSystem;
import eu.arrowhead.client.common.model.ServiceRegistryEntry;
import eu.arrowhead.demo.*;
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
public class BlueLedConsumer extends ArrowheadClientMain implements RotaryValueListener, ButtonPressedListener {

    private static String SR_BASE_URI;
    private final Logger logger = LogManager.getLogger();
    private final ExecutorService executorService;
    private final OnboardingStatus onboardingStatus;
    private final GroveButtonObserver buttonObserver;
    private final GroveRotaryObserver rotaryObserver;
    private final AtomicBoolean connected;

    private DeviceRegistryEntry deviceRegistryEntry;
    private ArrowheadDevice arrowheadDevice;
    private SystemRegistryEntry systemRegistryEntry;
    private ArrowheadSystem arrowheadSystem;
    private ServiceQueryForm serviceQueryForm;
    private ArrowheadService arrowheadService;

    private double lastValue = 0;

    private BlueLedConsumer(String[] args) throws IOException {
        //Register the application components the REST library need to know about
        Set<Class<?>> classes = new HashSet<>();
        String[] packages = {"eu.arrowhead.client.common", "eu.arrowhead.client.consumer"};
        //This (inherited) method reads in the configuration properties, and starts the web server
        init(ClientType.PROVIDER, args, classes, packages);

        final GrovePi grovePi = new GrovePi4J();
        executorService = Executors.newCachedThreadPool();

        buttonObserver = new GroveButtonObserver(grovePi);
        buttonObserver.setListener(this);
        executorService.submit(buttonObserver);

        rotaryObserver = new GroveRotaryObserver(grovePi);
        rotaryObserver.setListener(this);
        executorService.submit(rotaryObserver);

        connected = new AtomicBoolean(false);
        onboardingStatus = new OnboardingStatus(executorService, grovePi);
        onboardingStatus.disconnected();

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
        new BlueLedConsumer(args);
    }

    @Override
    public void trigger() {
        onboardingStatus.disconnected();

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
                onboardingStatus.connecting();
                performOnboarding();
                connected.set(true);
                onboardingStatus.connected();
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
        onboardingStatus.disconnected();
        buttonObserver.stop();
        rotaryObserver.stop();
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

        try {
            final String serviceUri = UriBuilder.fromPath(lookupServiceUri()).path("on").toString();
            logger.info("Contacting BlueLedService on {}", serviceUri);
            Utility.sendRequest(serviceUri, "POST", null);
            rotaryValueChanged(lastValue);
        } catch (LookupException ex) {
            logger.warn("Unable to lookup service");
        } catch (Exception ex) {
            logger.warn("Unable to turn on blue led");
        }
    }

    private void disconnectFromOnboarding() {

        try {
            final String serviceUri = UriBuilder.fromPath(lookupServiceUri()).path("off").toString();
            logger.info("Contacting service on {}", serviceUri);
            Utility.sendRequest(serviceUri, "POST", null);
        } catch (LookupException ex) {
            logger.warn("Unable to lookup service");
        } catch (Exception ex) {
            logger.warn("Unable to turn off blue led");
        }

        try{
            if (systemRegistryEntry != null) {
                unregister(systemRegistryEntry, getSystemRegistryUrl());
                systemRegistryEntry = null;
                arrowheadSystem = null;
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
            onboardingStatus.disconnected();
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
            arrowheadDevice.setDeviceName("RotarySensorDevice");
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

    private ServiceQueryForm compileServiceLookup() {
        if (serviceQueryForm == null) {
            serviceQueryForm = new ServiceQueryForm();
            serviceQueryForm.setService(compileArrowheadService());
        }
        return serviceQueryForm;
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

    private <T> T register(final T o, final String url, final String suffix, final String errorSuffix) {
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

    @Override
    public void rotaryValueChanged(double value) {

        lastValue = value;
        try {
            final String serviceUri = UriBuilder.fromPath(lookupServiceUri()).path("set").toString();
            logger.info("Setting rotary value to {} on {}", value, serviceUri);
            Utility.sendRequest(serviceUri, "POST", value);
        } catch (LookupException ex) {
            logger.trace("Unable to lookup service");
        } catch (Exception ex) {
            logger.warn(ex.getMessage());
        }
    }

    private String lookupServiceUri() throws LookupException {
        ServiceQueryForm query = compileServiceLookup();
        ServiceQueryResult result;
        Response r;

        String serviceUri = UriBuilder.fromPath(SR_BASE_URI).path("query").toString();
        logger.info("Searching for BlueLedService on {}", serviceUri);
        r = Utility.sendRequest(serviceUri, "PUT", query);
        result = r.readEntity(ServiceQueryResult.class);

        for (ServiceRegistryEntry entry : result.getServiceQueryData()) {
            ArrowheadSystem provider = entry.getProvider();
            String uri = Utility.getUri(provider.getAddress(), provider.getPort(), entry.getServiceUri(), false, false);
            logger.info("Found BlueLedService on {}", uri);
            return uri;
        }

        throw new LookupException();
    }
}