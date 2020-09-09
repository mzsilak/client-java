package eu.arrowhead.client.idaice;

import eu.arrowhead.common.dto.shared.OrchestrationResponseDTO;
import eu.arrowhead.common.dto.shared.OrchestrationResultDTO;
import eu.arrowhead.common.dto.shared.ServiceQueryFormDTO;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.demo.dto.Constants;
import eu.arrowhead.demo.onboarding.ArrowheadHandler;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlcLookupService {

    private final Logger logger = LogManager.getLogger();
    private final ArrowheadHandler arrowheadHandler;
    private final SystemRequestDTO systemRequestDTO;

    @Autowired
    public PlcLookupService(ArrowheadHandler arrowheadHandler, SystemRequestDTO systemRequestDTO) {
        this.arrowheadHandler = arrowheadHandler;
        this.systemRequestDTO = systemRequestDTO;
    }

    public String getPlcAddress() {
        logger.info("PLC requested");
        final String uri;

        try {
            final ServiceQueryFormDTO queryFormDTO = new ServiceQueryFormDTO();
            queryFormDTO.setServiceDefinitionRequirement(Constants.SERVICE_PLC_LOOKUP);

            final OrchestrationResponseDTO orchestrationResponseDTO = arrowheadHandler
                .lookupOrchestration(queryFormDTO, systemRequestDTO);
            final List<OrchestrationResultDTO> dtoList = orchestrationResponseDTO.getResponse();

            if (dtoList.size() > 0) {
                uri = dtoList.get(0).getServiceUri();
            } else {
                throw new ArrowheadException("PLC service not found");
            }

            if (uri.startsWith("/")) {
                return uri.substring(1);
            } else {
                return uri;
            }
        } catch (ArrowheadException e) {
            logger.fatal(e.getMessage());
            throw e;
        }
    }
}
