/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.subscriber;

import eu.arrowhead.client.common.model.Event;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.log4j.Logger;

@Path("notify")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SubscriberResource {

  private static final Logger log = Logger.getLogger(SubscriberResource.class.getName());

  //Method which can be called to check, if the web server works or not
  @GET
  public Response getIt() {
    return Response.ok().build();
  }

  /*REST interface for the Event Handler to call with an event the subscriber asked for. The event contains the event type, a payload, timestamp
    and metadata.*/
  @POST
  public Response receiveEvent(Event event) {
    log.info("Received new event: " + event.toString());
    //business logic here reacting to the event
    return Response.ok().build();
  }

}
