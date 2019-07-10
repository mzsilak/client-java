/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.publisher;

import eu.arrowhead.client.common.ArrowheadClientMain;
import eu.arrowhead.client.common.Utility;
import eu.arrowhead.client.common.misc.ClientType;
import eu.arrowhead.client.common.model.ArrowheadSystem;
import eu.arrowhead.client.common.model.Event;
import eu.arrowhead.client.common.model.PublishEvent;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

//This class extends ArrowheadClientMain, which is responsible for starting and stopping the web server
//The publisher only uses a web server in order to provide an interface for the Event Handler to signal back the result of the event publishing
public class PublisherMain extends ArrowheadClientMain {

  private PublisherMain(String[] args) {
    //Start the web server, read in the command line arguments
    Set<Class<?>> classes = new HashSet<>(Collections.singleton(PublisherResource.class));
    String[] packages = {"eu.arrowhead.client.common"};
    init(ClientType.PUBLISHER, args, classes, packages);

    //Publish the event to the Event Handler Core System
    publishEvent();
    //This method listens for a shutdown, and makes sure everything closes gracefully
    listenForInput();
  }

  public static void main(String[] args) {
    new PublisherMain(args);
  }

  private void publishEvent() {
    //Read in the Event Handler address related properties, create the full URL with the getUri() utility method
    String ehAddress = props.getProperty("eh_address", "0.0.0.0");
    int ehPort = isSecure ? props.getIntProperty("eh_secure_port", 8455) : props.getIntProperty("eh_insecure_port", 8454);
    String ehUri = Utility.getUri(ehAddress, ehPort, "eventhandler/publish", isSecure, false);

    //Read in the fields needed to create the event
    String systemName = isSecure ? props.getProperty("secure_system_name") : props.getProperty("insecure_system_name");
    String address = props.getProperty("address", "0.0.0.0");
    int insecurePort = props.getIntProperty("insecure_port", ClientType.PUBLISHER.getInsecurePort());
    int securePort = props.getIntProperty("secure_port", ClientType.PUBLISHER.getSecurePort());
    int usedPort = isSecure ? securePort : insecurePort;
    String type = props.getProperty("event_type");
    String payload = props.getProperty("event_payload");

    //Put together the event POJO and send the request to the Event Handler
    ArrowheadSystem source = new ArrowheadSystem(systemName, address, usedPort, base64PublicKey);
    Event event = new Event(type, payload, ZonedDateTime.now(), null);
    PublishEvent eventPublishing = new PublishEvent(source, event, "publisher/feedback");
    Utility.sendRequest(ehUri, "POST", eventPublishing);
    System.out.println("Event published to EH.");
  }

}
