package eu.arrowhead.demo.ssl;

import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

public class TrustAllX509TrustManager implements X509TrustManager {

  public void checkClientTrusted(X509Certificate[] certs, String authType) {
    // void;
  }

  public void checkServerTrusted(X509Certificate[] certs, String authType) {
    // void;
  }

  public X509Certificate[] getAcceptedIssuers() {
    return new X509Certificate[0];
  }
}
