package eu.arrowhead.client.station;

import static eu.arrowhead.client.station.Constants.CONTROLLER_PATH;
import static eu.arrowhead.client.station.Constants.OP_REGISTER_URI;
import static eu.arrowhead.client.station.Constants.OP_UNREGISTER_URI;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.demo.dto.RegisterRequestDTO;
import eu.arrowhead.demo.dto.RegisterResponseDTO;
import eu.arrowhead.demo.dto.UnregisterRequestDTO;
import eu.arrowhead.demo.dto.UnregisterResponseDTO;
import java.time.ZonedDateTime;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;

@Singleton
@Path(CONTROLLER_PATH)
@Produces(MediaType.APPLICATION_JSON_VALUE)
@Consumes(MediaType.APPLICATION_JSON_VALUE)
public class ChargingStationController {


    public static ChargingStationService chargingService = null;
    private final Logger logger = LogManager.getLogger();

    @GET
    @Path(CommonConstants.ECHO_URI)
    public String echo() {
        logger.info("echo with {}", chargingService);
        return Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.now());
    }

    @POST
    @Path(OP_REGISTER_URI)
    public RegisterResponseDTO register(final RegisterRequestDTO request) {
        logger.info("register");
        return new RegisterResponseDTO(true);
    }

    @POST
    @Path(OP_UNREGISTER_URI)
    public UnregisterResponseDTO unregister(final UnregisterRequestDTO request) {
        logger.info("unregister");
        return new UnregisterResponseDTO(true);
    }

}
