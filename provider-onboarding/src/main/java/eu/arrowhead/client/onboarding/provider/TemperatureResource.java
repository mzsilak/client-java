/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.onboarding.provider;

import eu.arrowhead.client.common.model.MeasurementEntry;
import eu.arrowhead.client.common.model.TemperatureReadout;
import java.time.Instant;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
//REST service example
public class TemperatureResource {

  static final String SERVICE_URI = "temperature";

  @GET
  @Path(SERVICE_URI)
  public Response getIt() {
    if (FullProviderMain.customResponsePayload != null) {
      return Response.status(200).entity(FullProviderMain.customResponsePayload).build();
    } else {
      double temperature = 21.0;
      MeasurementEntry entry = new MeasurementEntry("Temperature_IndoorTemperature", temperature, 0);
      TemperatureReadout readout = new TemperatureReadout("TemperatureSensor", Instant.now().getEpochSecond(), "Cel", 1);
      readout.getE().add(entry);
      return Response.status(200).entity(readout).build();
    }
  }

}
