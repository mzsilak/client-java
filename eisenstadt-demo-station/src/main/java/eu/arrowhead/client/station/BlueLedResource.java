/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.station;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iot.raspberry.grovepi.GrovePi;
import org.iot.raspberry.grovepi.devices.GroveLed;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;


@Path(BlueLedResource.SERVICE_URI)
@Produces(MediaType.APPLICATION_JSON)
//REST service example
public class BlueLedResource {

    public static final String SERVICE_URI = "led";
    public static final String TURN_ON_URI = "on";
    public static final String TURN_OFF_URI = "off";
    public static final String SET_VALUE_URI = "set";
    public static GrovePi GROVE_PI;

    private final Logger logger;
    private final GroveLed blue;

    public BlueLedResource() throws IOException {
        logger = LogManager.getLogger();
        blue = new GroveLed(GROVE_PI, 5); // D5
        turnOff();
    }

    @POST
    @Path(TURN_ON_URI)
    public Response turnOn() throws IOException {
        logger.info("Turning blue led on");
        blue.set(true);
        return Response.status(200).build();
    }

    @POST
    @Path(TURN_OFF_URI)
    public Response turnOff() throws IOException {
        logger.info("Turning blue led off");
        blue.set(false);
        return Response.status(200).build();
    }

    @POST
    @Path(SET_VALUE_URI)
    public Response setValue(double value) throws IOException {
        int strength = (int)(value*GroveLed.MAX_BRIGTHNESS);
        logger.info("Setting blue led strength to {}", strength);

        blue.set(strength);
        return Response.status(200).build();
    }
}
