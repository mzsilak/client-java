package eu.arrowhead.client.idaice;

import eu.arrowhead.common.dto.shared.OrchestrationResponseDTO;
import eu.arrowhead.common.dto.shared.OrchestrationResultDTO;
import eu.arrowhead.common.dto.shared.ServiceQueryFormDTO;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.common.dto.shared.SystemResponseDTO;
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
    private final IdaIceApplication application;

    @Autowired
    public PlcLookupService(ArrowheadHandler arrowheadHandler, IdaIceApplication application) {
        this.arrowheadHandler = arrowheadHandler;
        this.application = application;
    }

    public String getPlcAddress() {
        logger.info("PLC requested");

        final ServiceQueryFormDTO queryFormDTO = new ServiceQueryFormDTO();
        queryFormDTO.setServiceDefinitionRequirement(Constants.SERVICE_PLC_LOOKUP);

        final SystemResponseDTO systemResponseDTO = application.getSystemResponseDTO();
        final SystemRequestDTO systemRequestDTO = new SystemRequestDTO();
        systemRequestDTO.setSystemName(systemResponseDTO.getSystemName());
        systemRequestDTO.setAddress(systemResponseDTO.getAddress());
        systemRequestDTO.setPort(systemResponseDTO.getPort());

        final OrchestrationResponseDTO orchestrationResponseDTO = arrowheadHandler
            .lookupOrchestration(queryFormDTO, systemRequestDTO);
        final List<OrchestrationResultDTO> dtoList = orchestrationResponseDTO.getResponse();

        if (dtoList.size() > 0) {
            return dtoList.get(0).getServiceUri();
        } else {
            throw new ArrowheadException("PLC service not found");
        }
    }

    private void sleep() {
        try {
            Thread.sleep(500L);
        } catch (InterruptedException e) {
            logger.warn("Interrupted!", e);
        }
    }
}
