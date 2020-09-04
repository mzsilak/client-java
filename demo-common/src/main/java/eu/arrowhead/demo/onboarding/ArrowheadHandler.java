package eu.arrowhead.demo.onboarding;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.core.CoreSystem;
import eu.arrowhead.common.core.CoreSystemService;
import eu.arrowhead.common.dto.shared.DeviceRegistryOnboardingWithNameRequestDTO;
import eu.arrowhead.common.dto.shared.DeviceRegistryOnboardingWithNameResponseDTO;
import eu.arrowhead.common.dto.shared.DeviceResponseDTO;
import eu.arrowhead.common.dto.shared.OnboardingWithNameRequestDTO;
import eu.arrowhead.common.dto.shared.OnboardingWithNameResponseDTO;
import eu.arrowhead.common.dto.shared.OrchestrationFlags;
import eu.arrowhead.common.dto.shared.OrchestrationFormRequestDTO;
import eu.arrowhead.common.dto.shared.OrchestrationResponseDTO;
import eu.arrowhead.common.dto.shared.OrchestrationResultDTO;
import eu.arrowhead.common.dto.shared.ServiceEndpoint;
import eu.arrowhead.common.dto.shared.ServiceQueryFormDTO;
import eu.arrowhead.common.dto.shared.ServiceQueryResultDTO;
import eu.arrowhead.common.dto.shared.ServiceRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.ServiceRegistryResponseDTO;
import eu.arrowhead.common.dto.shared.SystemRegistryOnboardingWithNameRequestDTO;
import eu.arrowhead.common.dto.shared.SystemRegistryOnboardingWithNameResponseDTO;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.common.dto.shared.SystemResponseDTO;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.demo.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
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

    public void onboard(final OnboardingWithNameRequestDTO onboardingRequest)
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
                                   responseDTO.getOnboardingCertificate(), responseDTO.getIntermediateCertificate(),
                                   responseDTO.getRootCertificate());

        httpClient.setSecure();
    }

    public String getAuthInfo() {
        return sslHandler.getEncodedPublicKey();
    }

    public DeviceResponseDTO registerDevice(final DeviceRegistryOnboardingWithNameRequestDTO registryRequestDTO)
        throws NoSuchAlgorithmException, CertificateException, InvalidKeySpecException, KeyStoreException,
               IOException, SSLException {
        var httpResponse = register(registryRequestDTO, DeviceRegistryOnboardingWithNameResponseDTO.class,
                                    deviceRegistry.getUri());

        sslHandler.adaptSSLContext(registryRequestDTO.getCertificateCreationRequest().getCommonName(),
                                   httpResponse.getCertificateResponse());
        httpClient.setSecure();
        return httpResponse.getDevice();
    }

    private <REQ, RES> RES register(final REQ registryRequest, final Class<RES> responseCls, final URI uri) {
        final UriComponents registryUri = UriComponentsBuilder.fromUri(uri).build();
        final ResponseEntity<RES> responseEntity = httpClient
            .sendRequest(registryUri, HttpMethod.POST, responseCls, registryRequest);
        return responseEntity.getBody();
    }

    public SystemResponseDTO registerSystem(final SystemRegistryOnboardingWithNameRequestDTO registryRequestDTO)
        throws NoSuchAlgorithmException, CertificateException, InvalidKeySpecException, KeyStoreException,
               IOException, SSLException {
        var httpResponse = register(registryRequestDTO, SystemRegistryOnboardingWithNameResponseDTO.class,
                                    systemRegistry.getUri());
        sslHandler.adaptSSLContext(registryRequestDTO.getCertificateCreationRequest().getCommonName(),
                                   httpResponse.getCertificateResponse());
        httpClient.setSecure();
        return httpResponse.getSystem();
    }

    public ServiceRegistryResponseDTO registerService(final ServiceRegistryRequestDTO registryRequestDTO) {
        return register(registryRequestDTO, ServiceRegistryResponseDTO.class, serviceRegistry.getUri());
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

    public UriComponents createUri(final ServiceQueryFormDTO serviceQueryFormDTO, final SystemRequestDTO requester) {
        final OrchestrationResponseDTO orchestration = lookupOrchestration(serviceQueryFormDTO, requester);
        final List<OrchestrationResultDTO> response = orchestration.getResponse();
        return createUri(response.get(0));
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

    public UriComponents createUri(final OrchestrationResultDTO resultDTO) {
        return Utilities
            .createURI(httpClient.getScheme(), resultDTO.getProvider().getAddress(), resultDTO.getProvider().getPort(),
                       resultDTO.getServiceUri());
    }

    public UriComponents orchestrationUri() {
        return UriComponentsBuilder.fromUri(orchestrationService.getUri()).build();
    }

    private ServiceQueryResultDTO lookupServiceRegistry(final ServiceQueryFormDTO srQueryForm) {
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

    public UriComponents createUri(final ServiceRegistryResponseDTO srResponseDto) {
        return Utilities.createURI(httpClient.getScheme(), srResponseDto.getProvider().getAddress(),
                                   srResponseDto.getProvider().getPort(), srResponseDto.getServiceUri());
    }
}
