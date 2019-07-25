package eu.arrowhead.client.onboarding.provider;

import eu.arrowhead.client.common.OnboardingClientMain;
import eu.arrowhead.client.common.misc.ClientType;
import eu.arrowhead.client.common.model.DeviceRegistryEntry;
import eu.arrowhead.client.common.model.ServiceRegistryEntry;
import eu.arrowhead.client.common.model.SystemRegistryEntry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/*
  OnboardingClientProviderMain class has the following mandatory functionalities:
    1) Read in command line, and config file properties
    2) Start a normal HTTP server with REST resource classes
    3) Onboarding using the Onboarding Controller
    3) Register its service into the Device Registry, System Registry and Service Registry
 */
public class OnboardingClientProviderMain extends OnboardingClientMain {

  public static void main(String[] args) {
    new OnboardingClientProviderMain(args);
  }

  private String orchestrationUri;


  private OnboardingClientProviderMain(String[] args) {
    //Register the application components the REST library need to know about
    Set<Class<?>> classes = new HashSet<>(Arrays.asList(TemperatureResource.class, RestResource.class));
    String[] packages = {"eu.arrowhead.client.common"};
    //This (inherited) method reads in the configuration properties, and starts the web server
    init(ClientType.PROVIDER, args, classes, packages);

    //Compile the request payload
    DeviceRegistryEntry deviceEntry = compileDeviceRegistrationPayload();
    //Send the registration to the Service Registry
    register(deviceEntry, deviceRegUri);

    //Compile the request payload
    SystemRegistryEntry systemEntry = compileSystemRegistrationPayload(deviceEntry.getProvidedDevice());
    //Send the registration to the Service Registry
    register(systemEntry, systemRegUri);

    //Compile the request payload
    ServiceRegistryEntry serviceEntry = compileServiceRegistrationPayload(systemEntry.getProvidedSystem());
    //Send the registration to the Service Registry
    registerService(serviceEntry, serviceRegUri, "register", "remove");

    //Listen for a stop command
    listenForInput();
  }

}