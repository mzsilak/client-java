package eu.arrowhead.demo.onboarding;

import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.CertificateResponseDTO;
import eu.arrowhead.demo.ssl.TrustAllX509TrustManager;
import eu.arrowhead.demo.utils.SSLUtilities;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;

@Component
public class SSLHandler {

    private final Logger logger = LogManager.getLogger();
    private final SSLProperties sslProperties;
    private final KeyStore keyStore;
    private final KeyStore trustStore;

    @Autowired
    public SSLHandler(final SSLProperties sslProperties)
        throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
        Assert.notNull(sslProperties, "SSLProperties must not be null");

        this.sslProperties = sslProperties;

        logger.info("Initializing KeyStore");
        this.keyStore = initializeStore(sslProperties.getKeyStore(), sslProperties.getKeyStoreType(),
                                        sslProperties.getKeyStorePassword());
        logger.info("Initializing TrustStore");
        this.trustStore = initializeStore(sslProperties.getTrustStore(), sslProperties.getKeyStoreType(),
                                          sslProperties.getTrustStorePassword());
    }

    private KeyStore initializeStore(final Resource location, final String type, final String password)
        throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        Assert.notNull(location, "Store location must not be null");
        Assert.notNull(type, "Store type must not be null");
        Assert.notNull(password, "Store password must not be null");

        final KeyStore store = createEmptyStore(type, password);

        if (location.exists()) {
            loadStore(store, location, type, password);
        } else {
            saveStore(store, location, password);
        }

        return store;
    }

    private KeyStore createEmptyStore(final String type, final String password)
        throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        Assert.notNull(type, "Store type must not be null");
        Assert.notNull(password, "Store password must not be null");

        logger.debug("Creating empty store...");
        final KeyStore store = KeyStore.getInstance(type);
        store.load(null, null);
        return store;
    }

    private void loadStore(final KeyStore store, final Resource location, final String type, final String password)
        throws IOException, CertificateException, NoSuchAlgorithmException {
        Assert.notNull(location, "Store location must not be null");
        Assert.notNull(type, "Store type must not be null");
        Assert.notNull(password, "Store password must not be null");

        logger.debug("Loading store from resource: {}", location);
        try (final InputStream stream = location.getInputStream()) {
            store.load(stream, password.toCharArray());
        }
    }

    private void saveStore(final KeyStore store, final Resource location, final String password)
        throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        Assert.notNull(store, "Store object must not be null");
        Assert.notNull(location, "Store location must not be null");
        Assert.notNull(location.getFilename(), "Store location must not be null");
        Assert.notNull(password, "Store password must not be null");

        logger.debug("Saving store to resource: {}", location);
        try (final FileOutputStream stream = new FileOutputStream(location.getFilename())) {
            store.store(stream, password.toCharArray());
        }
    }

    public boolean isSslEnabled() {
        return sslProperties.isSslEnabled();
    }

    public void adaptSSLContext(final String name, final String certificateType, final CertificateResponseDTO response,
                                final String cloudCert, final String rootCert)
        throws CertificateException, IOException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException {
        logger.info("Adapting SSLContext ...");

        logger.debug("Decoding private key ...");
        final PrivateKey privateKey = SSLUtilities
            .parsePrivateKey(response.getPrivateKey(), response.getKeyAlgorithm());

        logger.debug("Decoding certificates ...");
        final Certificate[] chain = new Certificate[3];
        chain[2] = parseCertificate(response.getCertificate(), certificateType);
        chain[1] = parseCertificate(cloudCert, certificateType);
        chain[0] = parseCertificate(rootCert, certificateType);

        storeKeyEntry(name, privateKey, chain);
        storeCertificateEntry("arrowhead-intermediate-certificate", chain[1]);
        storeCertificateEntry("arrowhead-root-certificate", chain[2]);
    }

    private Certificate parseCertificate(final String certificateString, final String format)
        throws CertificateException, IOException {
        final CertificateFactory certificateFactory = CertificateFactory.getInstance(format);
        final byte[] certificateBytes = Base64Utils.decodeFromString(certificateString);

        final Certificate certificate;

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(certificateBytes)) {
            certificate = certificateFactory.generateCertificate(byteArrayInputStream);
        } catch (final CertificateException | IOException e) {
            logger.error("Unable to generate certificate from Base64: {}", certificateString);
            throw e;
        }

        return certificate;
    }

    private void storeKeyEntry(final String alias, final PrivateKey privateKey, final Certificate[] chain)
        throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
        logger.debug("Saving PrivateKey and certificate chain in keyStore as '{}'", alias);
        keyStore.setKeyEntry(alias, privateKey, sslProperties.getKeyPassword().toCharArray(), chain);
        saveStore(keyStore, sslProperties.getKeyStore(), sslProperties.getKeyStorePassword());
    }

    private void storeCertificateEntry(String alias, Certificate certificate)
        throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        if (certificate instanceof X509Certificate) {
            logger.debug("Saving trusted certificate '{}' as '{}'",
                         ((X509Certificate) certificate).getSubjectX500Principal(), alias);
        } else {
            logger.debug("Saving trusted certificate (type {}) as '{}'", certificate.getType(), alias);
        }

        keyStore.setCertificateEntry(alias, certificate);
        trustStore.setCertificateEntry(alias, certificate);

        saveStore(keyStore, sslProperties.getKeyStore(), sslProperties.getKeyStorePassword());
        saveStore(trustStore, sslProperties.getTrustStore(), sslProperties.getTrustStorePassword());
    }

    public SSLContext createSSLContext()
        throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, KeyManagementException {
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(createKeyManagers(), createTrustManagers(), null);
        return sslContext;
    }

    private KeyManager[] createKeyManagers()
        throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        final KeyManagerFactory keyManagerFactory = KeyManagerFactory
            .getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, sslProperties.getKeyPassword().toCharArray());
        return keyManagerFactory.getKeyManagers();
    }

    private TrustManager[] createTrustManagers() throws NoSuchAlgorithmException, KeyStoreException {
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory
            .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        return trustManagerFactory.getTrustManagers();
    }

    public SSLContext createInsecureSSLContext()
        throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(createKeyManagers(), createTrustAllTrustManagers(), null);
        return sslContext;
    }

    private TrustManager[] createTrustAllTrustManagers() {
        return new TrustManager[]{new TrustAllX509TrustManager()};
    }

    public SSLProperties getSslProperties() {
        return sslProperties;
    }

    public String getEncodedPublicKey() {
        X509Certificate serverCert = Utilities.getFirstCertFromKeyStore(keyStore);
        return Base64.getEncoder().encodeToString(serverCert.getPublicKey().getEncoded());
    }
}
