package eu.arrowhead.client.station;

import static eu.arrowhead.demo.dto.Constants.OP_STATION_CHARGE_URI;
import static eu.arrowhead.demo.dto.Constants.OP_STATION_REGISTER_URI;
import static eu.arrowhead.demo.dto.Constants.OP_STATION_UNREGISTER_URI;
import static eu.arrowhead.demo.dto.Constants.STATION_CONTROLLER_PATH;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.demo.dto.ChargeRequestDTO;
import eu.arrowhead.demo.dto.ChargeResponseDTO;
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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

@Singleton
@Path(STATION_CONTROLLER_PATH)
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
    @Path(OP_STATION_CHARGE_URI)
    public ChargeResponseDTO charge(final ChargeRequestDTO request) {
        logger.info("charge");
        Assert.notNull(request, "Request body empty");
        Assert.hasText(request.getRfid(), "RFID must be provided");
        boolean success;
        try {
            success = chargingService.charge(request.getRfid());
        } catch (Exception e) {
            logger.warn("Charging failed with: {}", e.getMessage());
            success = false;
        }
        return new ChargeResponseDTO(success);
    }

    @GET
    @Path(OP_STATION_CHARGE_URI + "/{rfid}")
    public void charge(@PathParam("rfid") final String rfid) {
        logger.info("charge");
        Assert.hasText(rfid, "RFID must be provided");
        boolean success;
        try {
            success = chargingService.charge(rfid);
        } catch (Exception e) {
            logger.warn("Charging failed with: {}", e.getMessage());
            success = false;
        }
    }

    @POST
    @Path(OP_STATION_REGISTER_URI)
    public RegisterResponseDTO register(final RegisterRequestDTO request) {
        logger.info("register");
        Assert.notNull(request, "Request body empty");
        Assert.hasText(request.getRfid(), "RFID must be provided");
        return new RegisterResponseDTO(chargingService.register(request.getRfid()));
    }

    @POST
    @Path(OP_STATION_UNREGISTER_URI)
    public UnregisterResponseDTO unregister(final UnregisterRequestDTO request) {
        logger.info("unregister");
        Assert.notNull(request, "Request body empty");
        Assert.hasText(request.getRfid(), "RFID must be provided");
        return new UnregisterResponseDTO(chargingService.unregister(request.getRfid()));
    }
}
