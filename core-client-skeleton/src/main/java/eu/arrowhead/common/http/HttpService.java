package eu.arrowhead.common.http;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.UnavailableServerException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;

//@Component
public class HttpService {

    //=================================================================================================
    // members

    private static final String ERROR_MESSAGE_PART_PKIX_PATH = "PKIX path";
    private static final String ERROR_MESSAGE_PART_SUBJECT_ALTERNATIVE_NAMES =
        "doesn't match any of the subject " + "alternative names";

    private static final List<HttpMethod> NOT_SUPPORTED_METHODS = List
        .of(HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.TRACE);

    private final Logger logger = LogManager.getLogger(HttpService.class);
    protected RestTemplate template;
    protected RestTemplate sslTemplate;
    @Value(CommonConstants.$DISABLE_HOSTNAME_VERIFIER_WD)
    private boolean disableHostnameVerifier;
    @Value(CommonConstants.$HTTP_CLIENT_CONNECTION_TIMEOUT_WD)
    private int connectionTimeout;
    @Value(CommonConstants.$HTTP_CLIENT_SOCKET_TIMEOUT_WD)
    private int socketTimeout;
    @Value(CommonConstants.$HTTP_CLIENT_CONNECTION_MANAGER_TIMEOUT_WD)
    private int connectionManagerTimeout;
    @Autowired
    private SSLProperties sslProperties;
    private String clientName;
    @Autowired
    private ArrowheadHttpClientResponseErrorHandler errorHandler;

    //=================================================================================================
    // methods

    //-------------------------------------------------------------------------------------------------
    @PostConstruct
    public void init() throws Exception { //NOSONAR Exception needs here
        logger.debug("Initializing HttpService...");
        template = createTemplate(null);
        SSLContext sslContext;
        if (sslProperties.isSslEnabled()) {
            try {
                if (sslTemplate != null) {
                    closeRequestFactory(sslTemplate.getRequestFactory());
                }
                sslContext = createSSLContext();
            } catch (final KeyManagementException | UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException ex) {
                // it's initialization so we just logging the exception then let the application die
                logger.error("Error while creating SSL context: {}", ex.getMessage());
                logger.debug("Exception", ex);
                throw ex;
            }
            sslTemplate = createTemplate(sslContext);
        }
        logger.debug("HttpService is initialized.");
    }

    //-------------------------------------------------------------------------------------------------
    private RestTemplate createTemplate(final SSLContext sslContext) {
        final HttpClient client = createClient(sslContext);
        final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(client);
        /*
            final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(client) {
            // This modification is needed to reuse already established connections in subsequent HTTP calls.

            @Override
            protected HttpContext createHttpContext(final HttpMethod httpMethod, final URI uri) {
                final HttpContext context = new HttpClientContext(new BasicHttpContext());
                if (!Utilities.isEmpty(clientName)) {
                    logger.info("Setting USER_TOKEN to {}", clientName);
                    context.setAttribute(HttpClientContext.USER_TOKEN, new X500Principal(clientName));
                }

                return context;
            }
        };*/
        final RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setErrorHandler(errorHandler);
        return restTemplate;
    }

    //-------------------------------------------------------------------------------------------------
    private void closeRequestFactory(final ClientHttpRequestFactory requestFactory) {
        if (requestFactory instanceof HttpComponentsClientHttpRequestFactory) {
            try {
                final HttpClient httpClient = ((HttpComponentsClientHttpRequestFactory) requestFactory).getHttpClient();
                if (httpClient instanceof CloseableHttpClient) {
                    ((CloseableHttpClient) httpClient).close();
                }

                ((HttpComponentsClientHttpRequestFactory) requestFactory).destroy();
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }

    }

    //-------------------------------------------------------------------------------------------------
    protected SSLContext createSSLContext()
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException
        , UnrecoverableKeyException {
        final String messageNotDefined = " is not defined.";
        Assert.isTrue(!Utilities.isEmpty(sslProperties.getKeyStoreType()),
                      CommonConstants.KEYSTORE_TYPE + messageNotDefined);
        Assert.notNull(sslProperties.getKeyStore(), CommonConstants.KEYSTORE_PATH + messageNotDefined);
        Assert.isTrue(sslProperties.getKeyStore().exists(), CommonConstants.KEYSTORE_PATH + " file is not found.");
        Assert.notNull(sslProperties.getKeyStorePassword(), CommonConstants.KEYSTORE_PASSWORD + messageNotDefined);
        Assert.notNull(sslProperties.getKeyPassword(), CommonConstants.KEY_PASSWORD + messageNotDefined);
        Assert.notNull(sslProperties.getTrustStore(), CommonConstants.TRUSTSTORE_PATH + messageNotDefined);
        Assert.isTrue(sslProperties.getTrustStore().exists(), CommonConstants.TRUSTSTORE_PATH + " file is not found.");
        Assert.notNull(sslProperties.getTrustStorePassword(), CommonConstants.TRUSTSTORE_PASSWORD + messageNotDefined);

        final KeyStore keystore = KeyStore.getInstance(sslProperties.getKeyStoreType());
        keystore.load(sslProperties.getKeyStore().getInputStream(), sslProperties.getKeyStorePassword().toCharArray());

        if (keystore.aliases().hasMoreElements()) {
            final X509Certificate certFromKeyStore = Utilities.getFirstCertFromKeyStore(keystore);
            clientName = certFromKeyStore.getSubjectDN().getName();
        }

        return new SSLContextBuilder().setKeyStoreType(sslProperties.getKeyStoreType())
                                      .loadTrustMaterial(sslProperties.getTrustStore().getURL(),
                                                         sslProperties.getTrustStorePassword().toCharArray())
                                      .loadKeyMaterial(sslProperties.getKeyStore().getURL(),
                                                       sslProperties.getKeyStorePassword().toCharArray(),
                                                       sslProperties.getKeyPassword().toCharArray()).build();
    }

    //-------------------------------------------------------------------------------------------------
    private HttpClient createClient(final SSLContext sslContext) {
        HttpClient client;

        if (sslContext == null) {
            client = HttpClients.custom().setDefaultRequestConfig(createRequestConfig()).build();
        } else {
            if (disableHostnameVerifier) { // just for testing, DO NOT USE this in a production environment
                client = HttpClients.custom().setDefaultRequestConfig(createRequestConfig())
                                    .setConnectionReuseStrategy(new NoConnectionReuseStrategy())
                                    .setSSLContext(sslContext).setSSLHostnameVerifier((hostname, session) -> true)
                                    .build();
            } else {
                client = HttpClients.custom().setDefaultRequestConfig(createRequestConfig())
                                    .setConnectionReuseStrategy(new NoConnectionReuseStrategy())
                                    .setSSLContext(sslContext).build();
            }
        }

        return client;
    }

    //-------------------------------------------------------------------------------------------------
    private RequestConfig createRequestConfig() {
        return RequestConfig.custom().setConnectTimeout(connectionTimeout).setSocketTimeout(socketTimeout)
                            .setConnectionRequestTimeout(connectionManagerTimeout).build();
    }

    //=================================================================================================
    // assistant methods

    //-------------------------------------------------------------------------------------------------
    public <T, P> ResponseEntity<T> sendRequest(final UriComponents uri, final HttpMethod method,
                                                final Class<T> responseType, final P payload) {
        return sendRequest(uri, method, responseType, payload, null);
    }

    //-------------------------------------------------------------------------------------------------
    public <T, P> ResponseEntity<T> sendRequest(final UriComponents uri, final HttpMethod method,
                                                final Class<T> responseType, final P payload,
                                                final SSLContext givenContext) {
        Assert.notNull(method, "Request method is not defined.");
        logger.debug("Sending {} request to: {}", method, uri);

        if (uri == null) {
            logger.error("sendRequest() is called with null URI.");
            throw new NullPointerException(
                "HttpService.sendRequest method received null URI. This most likely means the invoking Core System "
                    + "could not " + "fetch the service of another Core System from the Service Registry!");
        }

        if (NOT_SUPPORTED_METHODS.contains(method)) {
            throw new UnsupportedOperationException(
                "Invalid method type was given to the HttpService.sendRequest() method" + ".");
        }

        final boolean secure = CommonConstants.HTTPS.equalsIgnoreCase(uri.getScheme());
        if (secure && sslTemplate == null) {
            logger.debug("sendRequest(): secure request sending was invoked in insecure mode.");
            throw new AuthException(
                "SSL Context is not set, but secure request sending was invoked. An insecure module can not send "
                    + "requests to secure modules.", HttpStatus.SC_UNAUTHORIZED);
        }

        RestTemplate usedTemplate;
        if (secure) { // to make SonarQube happy
            usedTemplate = givenContext != null ? createTemplate(givenContext) : sslTemplate;
        } else {
            usedTemplate = template;
        }

        final HttpEntity<P> entity = getHttpEntity(payload);
        try {
            return usedTemplate.exchange(uri.toUri(), method, entity, responseType);
        } catch (final ResourceAccessException ex) {
            if (ex.getMessage().contains(ERROR_MESSAGE_PART_PKIX_PATH)) {
                logger.error("The system at {} is not part of the same certificate chain of trust!", uri.toUriString());
                throw new AuthException(
                    "The system at " + uri.toUriString() + " is not part of the same certificate chain of trust!",
                    HttpStatus.SC_UNAUTHORIZED, ex);
            } else if (ex.getMessage().contains(ERROR_MESSAGE_PART_SUBJECT_ALTERNATIVE_NAMES)) {
                logger.error(
                    "The certificate of the system at {} does not contain the specified IP address or DNS name as a "
                        + "Subject Alternative Name.", uri.toString());
                throw new AuthException("The certificate of the system at " + uri.toString()
                                            + " does not contain the specified IP address or DNS name as a Subject "
                                            + "Alternative Name.");
            } else {
                logger.error("UnavailableServerException occurred at {}", uri.toUriString());
                logger.debug("Exception", ex);
                throw new UnavailableServerException("Could not get any response from: " + uri.toUriString(),
                                                     HttpStatus.SC_SERVICE_UNAVAILABLE, ex);
            }
        }
    }

    //-------------------------------------------------------------------------------------------------
    protected <P> HttpEntity<P> getHttpEntity(final P payload) {
        final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.put(HttpHeaders.ACCEPT, Arrays.asList(MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE));
        if (payload != null) {
            headers.put(HttpHeaders.CONTENT_TYPE, Collections.singletonList(MediaType.APPLICATION_JSON_VALUE));
        }

        return payload != null ? new HttpEntity<>(payload, headers) : new HttpEntity<>(headers);
    }

    //-------------------------------------------------------------------------------------------------
    public <T> ResponseEntity<T> sendRequest(final UriComponents uri, final HttpMethod method,
                                             final Class<T> responseType, final SSLContext givenContext) {
        return sendRequest(uri, method, responseType, null, givenContext);
    }

    //-------------------------------------------------------------------------------------------------
    public <T> ResponseEntity<T> sendRequest(final UriComponents uri, final HttpMethod method,
                                             final Class<T> responseType) {
        return sendRequest(uri, method, responseType, null, null);
    }
}