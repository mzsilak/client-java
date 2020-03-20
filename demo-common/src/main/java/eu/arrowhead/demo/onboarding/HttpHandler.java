package eu.arrowhead.demo.onboarding;

import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.demo.ssl.SSLException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
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
public class HttpHandler extends HttpService {

  private final Logger logger = LogManager.getLogger();
  private final AtomicBoolean secureMode = new AtomicBoolean(false);

  private final SSLHandler sslHandler;
  private final String sharedSecret;

  @Autowired
  public HttpHandler(final SSLHandler sslHandler, @Value("${sharedSecret:#{null}}") final String sharedSecret) {
    this.sslHandler = sslHandler;
    this.sharedSecret = sharedSecret;
  }

  @Override
  protected <P> HttpEntity<P> getHttpEntity(P payload) {
    final MultiValueMap<String,String> headers = new LinkedMultiValueMap<>();
    headers.put(org.apache.http.HttpHeaders.ACCEPT, Arrays.asList(MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE));
    if (payload != null) {
      headers.put(org.apache.http.HttpHeaders.CONTENT_TYPE, Collections.singletonList(MediaType.APPLICATION_JSON_VALUE));
    }

    if (Objects.nonNull(sharedSecret)) {
      final String authString = ":" + sharedSecret;
      byte[] base64Credential = Base64.getEncoder().encode(authString.getBytes());
      headers.add(HttpHeaders.AUTHORIZATION, "Basic " + new String(base64Credential));
    }

    return payload != null ? new HttpEntity<>(payload, headers) : new HttpEntity<>(headers);
  }

  @Override
  protected SSLContext createSSLContext()
      throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException {
    if (secureMode.get()) {
      return sslHandler.createSSLContext();
    } else {
      return sslHandler.createInsecureSSLContext();
    }
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
}
