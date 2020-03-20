package eu.arrowhead.demo.utils;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class SSLUtilities {

  private SSLUtilities() {
    super();
  }

  public static PrivateKey parsePrivateKey(final byte[] rawKey, final String keyAlgorithm)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    final KeyFactory kf = KeyFactory.getInstance(keyAlgorithm);

    try {
      return kf.generatePrivate(new PKCS8EncodedKeySpec(rawKey));
    } catch (InvalidKeySpecException e) {
      return kf.generatePrivate(new X509EncodedKeySpec(rawKey));
    }
  }

  public static PublicKey parsePublicKey(final byte[] rawKey, final String keyAlgorithm)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    final KeyFactory kf = KeyFactory.getInstance(keyAlgorithm);

    try {
      return kf.generatePublic(new PKCS8EncodedKeySpec(rawKey));
    } catch (InvalidKeySpecException e) {
      return kf.generatePublic(new X509EncodedKeySpec(rawKey));
    }
  }

}
