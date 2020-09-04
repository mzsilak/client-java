package eu.arrowhead.client.plc;

import static eu.arrowhead.demo.dto.Constants.PLC_CONTROLLER_PATH;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
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
@Path(PLC_CONTROLLER_PATH)
@Produces(MediaType.APPLICATION_JSON_VALUE)
@Consumes(MediaType.APPLICATION_JSON_VALUE)
public class PlcController {


    private final Logger logger = LogManager.getLogger();

    @GET
    @Path(CommonConstants.ECHO_URI)
    public String echo() {
        logger.info("echo");
        return Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.now());
    }
}
