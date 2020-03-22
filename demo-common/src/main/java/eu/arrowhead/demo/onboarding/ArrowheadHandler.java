package eu.arrowhead.demo.onboarding;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.core.CoreSystem;
import eu.arrowhead.common.core.CoreSystemService;
import eu.arrowhead.common.dto.shared.DeviceRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.DeviceRegistryResponseDTO;
import eu.arrowhead.common.dto.shared.OnboardingWithNameRequestDTO;
import eu.arrowhead.common.dto.shared.OnboardingWithNameResponseDTO;
import eu.arrowhead.common.dto.shared.OrchestrationFlags;
import eu.arrowhead.common.dto.shared.OrchestrationFormRequestDTO;
import eu.arrowhead.common.dto.shared.OrchestrationResponseDTO;
import eu.arrowhead.common.dto.shared.ServiceEndpoint;
import eu.arrowhead.common.dto.shared.ServiceQueryFormDTO;
import eu.arrowhead.common.dto.shared.ServiceQueryResultDTO;
import eu.arrowhead.common.dto.shared.ServiceRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.ServiceRegistryResponseDTO;
import eu.arrowhead.common.dto.shared.SystemRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.SystemRegistryResponseDTO;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.demo.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ArrowheadHandler {

    private final Logger logger = LogManager.getLogger(ArrowheadHandler.class);

    private final HttpClient httpClient;
    private final SSLHandler sslHandler;
    private final String onboardingHost;

    private ServiceEndpoint deviceRegistry;
    private ServiceEndpoint systemRegistry;
    private ServiceEndpoint serviceRegistry;
    private ServiceEndpoint orchestrationService;

    @Autowired
    public ArrowheadHandler(final HttpClient httpClient, final SSLHandler sslHandler,
                            @Value("${onboarding.host}") final String onboardingHost) {
        this.httpClient = httpClient;
        this.sslHandler = sslHandler;
        this.onboardingHost = onboardingHost;
    }

    public void performOnboarding(final OnboardingWithNameRequestDTO onboardingRequest)
        throws NoSuchAlgorithmException, CertificateException, InvalidKeySpecException, KeyStoreException,
               IOException, SSLException {

        logger.info("Starting onboarding for {}", onboardingRequest);
        httpClient.setInsecure();

        final UriComponents onboardingUri = Utilities
            .createURI(CommonConstants.HTTPS, onboardingHost, CoreSystem.ONBOARDING_CONTROLLER.getDefaultPort(),
                       CoreSystemService.ONBOARDING_WITH_SHARED_SECRET_AND_NAME.getServiceUri());

        final ResponseEntity<OnboardingWithNameResponseDTO> httpResponse = httpClient
            .sendRequest(onboardingUri, HttpMethod.POST, OnboardingWithNameResponseDTO.class, onboardingRequest);

        final OnboardingWithNameResponseDTO responseDTO = httpResponse.getBody();
        assert responseDTO != null;

        deviceRegistry = responseDTO.getDeviceRegistry();
        systemRegistry = responseDTO.getSystemRegistry();
        serviceRegistry = responseDTO.getServiceRegistry();
        orchestrationService = responseDTO.getOrchestrationService();

        logger.info("Saving certificates ...");
        sslHandler.adaptSSLContext(onboardingRequest.getCreationRequestDTO().getCommonName(),
                                   responseDTO.getCertificateType(), responseDTO.getOnboardingCertificate(),
                                   responseDTO.getIntermediateCertificate(), responseDTO.getRootCertificate());

        httpClient.setSecure();
    }

    public String getAuthInfo() {
        return sslHandler.getEncodedPublicKey();
    }

    public void registerDevice(final DeviceRegistryRequestDTO registryRequestDTO) {
        register(registryRequestDTO, DeviceRegistryResponseDTO.class, deviceRegistry.getUri());
    }

    private <REQ, RES> RES register(final REQ registryRequest, final Class<RES> responseCls, final URI uri) {
        final UriComponents registryUri = UriComponentsBuilder.fromUri(uri).build();
        final ResponseEntity<RES> responseEntity = httpClient
            .sendRequest(registryUri, HttpMethod.POST, responseCls, registryRequest);
        return responseEntity.getBody();
    }

    public void registerSystem(final SystemRegistryRequestDTO registryRequestDTO) {
        register(registryRequestDTO, SystemRegistryResponseDTO.class, systemRegistry.getUri());
    }

    public void registerService(final ServiceRegistryRequestDTO registryRequestDTO) {
        register(registryRequestDTO, ServiceRegistryResponseDTO.class, serviceRegistry.getUri());
    }

    public void unregisterDevice(final String deviceName, final String macAddress) {
        try {
            final UriComponentsBuilder builder = UriComponentsBuilder.fromUri(deviceRegistry.getUri()).replacePath(
                CommonConstants.DEVICE_REGISTRY_URI + CommonConstants.OP_DEVICE_REGISTRY_UNREGISTER_URI);
            addNonNull(builder, "device_name", deviceName);
            addNonNull(builder, "mac_address", macAddress);
            httpClient.sendRequest(builder.build(), HttpMethod.DELETE, null);
        } catch (final Exception ex) {
            // ignore
        }
    }

    private void addNonNull(final UriComponentsBuilder builder, final String key, final Object value) {
        if (Objects.nonNull(value)) {
            builder.queryParam(key, value);
        }
    }

    public void unregisterSystem(final String systemName, final String address, final Integer port) {
        try {
            final UriComponentsBuilder builder = UriComponentsBuilder.fromUri(systemRegistry.getUri()).replacePath(
                CommonConstants.SYSTEM_REGISTRY_URI + CommonConstants.OP_SYSTEM_REGISTRY_UNREGISTER_URI);
            addNonNull(builder, "system_name", systemName);
            addNonNull(builder, "address", address);
            addNonNull(builder, "port", port);
            httpClient.sendRequest(builder.build(), HttpMethod.DELETE, null);
        } catch (final Exception ex) {
            // ignore
        }
    }

    public void unregisterService(final String serviceDef, final String systemName, final String address,
                                  final Integer port) {
        try {
            final UriComponentsBuilder builder = UriComponentsBuilder.fromUri(serviceRegistry.getUri()).replacePath(
                CommonConstants.SERVICE_REGISTRY_URI + CommonConstants.OP_SERVICE_REGISTRY_UNREGISTER_URI);
            addNonNull(builder, "service_definition", serviceDef);
            addNonNull(builder, "system_name", systemName);
            addNonNull(builder, "address", address);
            addNonNull(builder, "port", port);
            httpClient.sendRequest(builder.build(), HttpMethod.DELETE, null);
        } catch (final Exception ex) {
            // ignore
        }
    }

    public OrchestrationResponseDTO lookupOrchestration(final ServiceQueryFormDTO queryFormDTO,
                                                        final SystemRequestDTO requester) {
        final OrchestrationFormRequestDTO orchForm = new OrchestrationFormRequestDTO.Builder(requester)
            .requestedService(queryFormDTO).flag(OrchestrationFlags.Flag.OVERRIDE_STORE, true).build();

        final ResponseEntity<OrchestrationResponseDTO> httpResponse = httpClient
            .sendRequest(orchestrationUri(), HttpMethod.POST, OrchestrationResponseDTO.class, orchForm);
        final OrchestrationResponseDTO orchQueryResult = httpResponse.getBody();

        if (Objects.isNull(orchQueryResult) || orchQueryResult.getResponse().isEmpty()) {
            throw new ArrowheadException("Unable to find " + queryFormDTO.getServiceDefinitionRequirement());
        }

        return orchQueryResult;
    }

    public UriComponents orchestrationUri() {
        return UriComponentsBuilder.fromUri(orchestrationService.getUri()).build();
    }

    public UriComponents createUri(final String serviceDef) {
        final ServiceQueryFormDTO serviceQueryFormDTO = new ServiceQueryFormDTO.Builder(serviceDef)
            .interfaces(CommonConstants.HTTP_SECURE_JSON).build();
        return createUri(serviceQueryFormDTO);
    }

    public UriComponents createUri(final ServiceQueryFormDTO serviceQueryFormDTO) {
        final ServiceQueryResultDTO serviceQueryResultDTO = lookupServiceRegistry(serviceQueryFormDTO);
        final ServiceRegistryResponseDTO responseDto = serviceQueryResultDTO.getServiceQueryData().get(0);
        return createUri(responseDto);
    }

    public UriComponents createUri(final ServiceRegistryResponseDTO srResponseDto) {
        return Utilities.createURI(httpClient.getScheme(), srResponseDto.getProvider().getAddress(),
                                   srResponseDto.getProvider().getPort(), srResponseDto.getServiceUri());
    }

    public ServiceQueryResultDTO lookupServiceRegistry(final ServiceQueryFormDTO srQueryForm) {
        final UriComponents srQueryUri = UriComponentsBuilder.fromUri(serviceRegistry.getUri()).replacePath(
            CommonConstants.SERVICE_REGISTRY_URI + CommonConstants.OP_SERVICE_REGISTRY_QUERY_URI).build();

        final ResponseEntity<ServiceQueryResultDTO> httpResponse = httpClient
            .sendRequest(srQueryUri, HttpMethod.POST, ServiceQueryResultDTO.class, srQueryForm);
        final ServiceQueryResultDTO srQueryResult = httpResponse.getBody();

        if (Objects.isNull(srQueryResult) || srQueryResult.getServiceQueryData().isEmpty()) {
            throw new ArrowheadException("Unable to find service");
        }

        return srQueryResult;
    }

}
