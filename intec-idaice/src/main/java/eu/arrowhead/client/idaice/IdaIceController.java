package eu.arrowhead.client.idaice;


import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.demo.dto.Constants;
import eu.arrowhead.demo.dto.PlcResponseDTO;
import java.time.ZonedDateTime;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@Singleton
@Path(Constants.IDA_ICE_CONTROLLER_PATH)
@Produces(MediaType.APPLICATION_JSON_VALUE)
@Consumes(MediaType.APPLICATION_JSON_VALUE)
public class IdaIceController {

    public static PlcLookupService plcLookupService;
    private final Logger logger = LogManager.getLogger();


    @GET
    @Path(CommonConstants.ECHO_URI)
    public String echo() {
        logger.info("echo with {}", plcLookupService);
        return Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.now());
    }

    @GET
    @Path(Constants.OP_PLC_URI)
    public PlcResponseDTO plcLookup() {
        return new PlcResponseDTO(plcLookupService.getPlcAddress());
    }
}
