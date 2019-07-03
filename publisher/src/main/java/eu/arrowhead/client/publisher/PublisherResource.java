/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.publisher;

import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("publisher")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PublisherResource {

  //Method which can be called to check, if the web server works or not
  @GET
  public Response getIt() {
    return Response.ok().build();
  }

  /*REST interface for the Event Handler to call with the results of the event publishing. The Map contains the URLs of the subscribers and a
    boolean indicating if notifying the subscriber was successful or not. */
  @POST
  @Path("feedback")
  public Response receiveEvent(Map<String, Boolean> results) {
    System.out.println("Event publishing results:");
    System.out.println(results.toString());
    return Response.ok().build();
  }

}
