package eu.arrowhead.demo.ssl;

public class SSLException extends Exception {

  public SSLException() {
    super();
  }

  public SSLException(String message) {
    super(message);
  }

  public SSLException(String message, Throwable cause) {
    super(message, cause);
  }

  public SSLException(Throwable cause) {
    super(cause);
  }

  public SSLException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
