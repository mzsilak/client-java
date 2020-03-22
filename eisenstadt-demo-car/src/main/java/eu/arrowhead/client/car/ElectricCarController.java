package eu.arrowhead.client.car;


import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.demo.dto.Constants;
import eu.arrowhead.demo.dto.RfidResponseDTO;
import java.time.ZonedDateTime;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;

@Singleton
@Path(Constants.CAR_CONTROLLER_PATH)
@Produces(MediaType.APPLICATION_JSON_VALUE)
@Consumes(MediaType.APPLICATION_JSON_VALUE)
public class ElectricCarController {

    public static ElectricCarService carService = null;
    private final Logger logger = LogManager.getLogger();

    @GET
    @Path(CommonConstants.ECHO_URI)
    public String echo() {
        logger.info("echo with {}", carService);
        return Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.now());
    }

    @GET
    @Path(Constants.OP_CAR_RFID_URI)
    public RfidResponseDTO rfid() {
        return new RfidResponseDTO(carService.getRfid());
    }
}
