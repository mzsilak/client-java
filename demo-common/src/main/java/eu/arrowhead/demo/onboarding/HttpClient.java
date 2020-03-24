package eu.arrowhead.demo.onboarding;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.demo.ssl.SSLException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Component("httpService")
public class HttpClient extends HttpService {

    private final Logger logger = LogManager.getLogger();
    private final AtomicBoolean secureMode = new AtomicBoolean(true);

    private final SSLHandler sslHandler;
    private final String sharedSecret;

    @Autowired
    public HttpClient(final SSLHandler sslHandler, @Value("${sharedSecret:#{null}}") final String sharedSecret) {
        this.sslHandler = sslHandler;
        this.sharedSecret = sharedSecret;
    }

    @Override
    protected SSLContext createSSLContext()
        throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException,
               CertificateException, IOException {
        if (secureMode.get()) {
            return super.createSSLContext();
            // return sslHandler.createSSLContext();
            // return sslHandler.createInsecureSSLContext();
        } else {
            return sslHandler.createInsecureSSLContext();
        }
    }

    @Override
    protected <P> HttpEntity<P> getHttpEntity(P payload) {

        final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.put(org.apache.http.HttpHeaders.ACCEPT,
                    Arrays.asList(MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE));
        if (payload != null) {
            headers.put(org.apache.http.HttpHeaders.CONTENT_TYPE,
                        Collections.singletonList(MediaType.APPLICATION_JSON_VALUE));
        }

        if (sslHandler.hasCertificates() && secureMode.get()) {
            return super.getHttpEntity(payload);
        }

        if (Objects.nonNull(sharedSecret) && !secureMode.get()) {
            logger.debug("Adding Authorization header because we are in password mode");
            final String authString = ":" + sharedSecret;
            byte[] base64Credential = Base64.getEncoder().encode(authString.getBytes());
            headers.add(HttpHeaders.AUTHORIZATION, "Basic " + new String(base64Credential));
        }

        return payload != null ? new HttpEntity<>(payload, headers) : new HttpEntity<>(headers);
    }

    public void setSecure() throws SSLException {
        try {
            secureMode.set(true);
            super.init();
        } catch (Exception e) {
            throw new SSLException(e);
        }
    }

    public void setInsecure() throws SSLException {
        try {
            secureMode.set(false);
            super.init();
        } catch (Exception e) {
            throw new SSLException(e);
        }
    }

    public String getScheme() {
        return sslHandler.isSslEnabled() ? CommonConstants.HTTPS : CommonConstants.HTTP;
    }
}
