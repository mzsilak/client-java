package eu.arrowhead.demo.onboarding;

import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.CertificateResponseDTO;
import eu.arrowhead.demo.ssl.TrustAllX509TrustManager;
import eu.arrowhead.demo.utils.SSLUtilities;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
            Files.createFile(Path.of(location.getFilename()));
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

        final String absolutePath = location.getFile().getAbsolutePath();
        logger.debug("Loading store from resource: {}", absolutePath);
        try (final FileInputStream stream = new FileInputStream(absolutePath)) {
            store.load(stream, password.toCharArray());
        }
    }

    private void saveStore(final KeyStore store, final Resource location, final String password)
        throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        Assert.notNull(store, "Store object must not be null");
        Assert.notNull(location, "Store location must not be null");
        Assert.notNull(location.getFilename(), "Store location must not be null");
        Assert.notNull(password, "Store password must not be null");

        final String absolutePath = location.getFile().getAbsolutePath();
        logger.debug("Saving store to resource: {}", absolutePath);
        try (final FileOutputStream stream = new FileOutputStream(absolutePath)) {
            store.store(stream, password.toCharArray());
        }
    }

    public boolean isSslEnabled() {
        return sslProperties.isSslEnabled();
    }

    public void adaptSSLContext(final String commonName, final String certificateType,
                                final CertificateResponseDTO response,
                                final String cloudCert, final String rootCert)
        throws CertificateException, IOException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException {
        logger.info("Adapting SSLContext ...");
        return; /*
        logger.debug("Decoding private key ...");
        final PrivateKey privateKey = SSLUtilities
            .parsePrivateKey(response.getPrivateKey(), response.getKeyAlgorithm());

        logger.debug("Decoding certificates ...");
        final X509Certificate[] chain = new X509Certificate[3];
        chain[0] = parseCertificate(response.getCertificate(), certificateType);
        chain[1] = parseCertificate(cloudCert, certificateType);
        chain[2] = parseCertificate(rootCert, certificateType);

        //final String alias = Utilities.getCertCNFromSubject(chain[0].getSubjectDN().getName());

        storeKeyEntry(commonName, privateKey, chain);
        storeCertificateEntry(chain[1]);
        storeCertificateEntry(chain[2]);

        loadStore(keyStore, sslProperties.getKeyStore(), sslProperties.getKeyStoreType(),
                  sslProperties.getKeyStorePassword());

        loadStore(trustStore, sslProperties.getTrustStore(), sslProperties.getKeyStoreType(),
                  sslProperties.getTrustStorePassword()); */
    }

    private X509Certificate parseCertificate(final String certificateString, final String format)
        throws CertificateException, IOException {
        final CertificateFactory certificateFactory = CertificateFactory.getInstance(format);
        final byte[] certificateBytes = Base64Utils.decodeFromString(certificateString);

        final X509Certificate certificate;

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(certificateBytes)) {
            certificate = (X509Certificate) certificateFactory.generateCertificate(byteArrayInputStream);
        } catch (final CertificateException | IOException e) {
            logger.error("Unable to generate certificate from Base64: {}", certificateString);
            throw e;
        }

        return certificate;
    }

    private void storeKeyEntry(final String alias, final PrivateKey privateKey, final X509Certificate[] chain)
        throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {

        final Object[] principals = Arrays.stream(chain)
                                          .map((c) -> Utilities.getCertCNFromSubject(c.getSubjectDN().getName()))
                                          .toArray();

        logger.debug("Saving PrivateKey and certificate chain ({}) in keyStore as '{}'", principals, alias);
        keyStore.setKeyEntry(alias, privateKey, sslProperties.getKeyPassword().toCharArray(), chain);
        saveStore(keyStore, sslProperties.getKeyStore(), sslProperties.getKeyStorePassword());
    }

    private void storeCertificateEntry(final X509Certificate certificate)
        throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        final String alias = Utilities.getCertCNFromSubject(certificate.getSubjectDN().getName());
        logger.debug("Saving trusted certificate '{}' as '{}'", certificate.getSubjectX500Principal(), alias);

        // keyStore.setCertificateEntry(alias, certificate);
        trustStore.setCertificateEntry(alias, certificate);

        saveStore(keyStore, sslProperties.getKeyStore(), sslProperties.getKeyStorePassword());
        saveStore(trustStore, sslProperties.getTrustStore(), sslProperties.getTrustStorePassword());
    }

    public SSLContext createSSLContext()
        throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, KeyManagementException {
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        logger.info("Using real SSLContext");
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
        logger.info("Using trust all SSLContext");

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(createKeyManagers(), createTrustAllTrustManagers(), null);
        return sslContext;

        /*
        return new SSLContextBuilder().loadTrustMaterial(trustStore, new TrustAllStrategy())
                                      .loadKeyMaterial(keyStore, sslProperties.getKeyPassword().toCharArray())
                                      .setKeyStoreType(sslProperties.getKeyStoreType()).build();
         */
    }

    private TrustManager[] createTrustAllTrustManagers() throws KeyStoreException, NoSuchAlgorithmException {
        final TrustManager[] existing = createTrustManagers();
        final TrustManager[] newManagers = new TrustManager[existing.length + 1];
        System.arraycopy(existing, 0, newManagers, 1, existing.length);
        newManagers[0] = new TrustAllX509TrustManager();
        return newManagers;
    }

    public SSLProperties getSslProperties() {
        return sslProperties;
    }

    public String getEncodedPublicKey() {
        X509Certificate serverCert = Utilities.getFirstCertFromKeyStore(keyStore);
        return Base64.getEncoder().encodeToString(serverCert.getPublicKey().getEncoded());
    }

    public boolean hasCertificates() {
        if (Objects.nonNull(keyStore)) {
            try {
                return keyStore.aliases().hasMoreElements();
            } catch (KeyStoreException e) {
                // noop
            }
        }
        return false;
    }
}
