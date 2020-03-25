package eu.arrowhead.demo.web;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.demo.onboarding.SSLHandler;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpServer {

    private final Logger logger = LogManager.getLogger();

    private final SSLHandler sslHandler;
    private final String host;
    private final int port;
    private final ResourceConfig config;
    private final AtomicReference<LifeCycle> state;
    private final HttpServerCustomizer[] customizers;

    private SSLContext sslContext;
    private org.glassfish.grizzly.http.server.HttpServer server;
    private boolean needClientAuth = false;
    private boolean wantClientAuth = false;

    @Autowired
    public HttpServer(final SSLHandler sslHandler, @Value("${server.address:0.0.0.0}") final String host,
                      @Value("${server.port:8080}") final int port, final HttpServerCustomizer... customizers) {
        this.customizers = customizers;
        this.sslHandler = sslHandler;
        this.host = host;
        this.port = port;

        config = new ResourceConfig();
        state = new AtomicReference<>(LifeCycle.NEW);
    }

    public void registerClasses(final Class<?>... classes) {
        config.registerClasses(classes);
    }

    public void registerInstances(final Object... objects) {
        config.registerInstances(objects);
    }

    public void registerPackages(final String... packages) {
        config.packages(packages);
    }

    public void setSSLContext(final SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public void configureName(final String commonName) {
        config.property("server_common_name", commonName);
    }

    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }

    public void setWantClientAuth(boolean wantClientAuth) {
        this.wantClientAuth = wantClientAuth;
    }

    public void init() {
        stop();

        logger.debug("Initializing HTTP Server");
        state.set(LifeCycle.NEW);

        for (HttpServerCustomizer customizer : customizers) {
            customizer.customize(this);
        }

        final SSLContextConfigurator sslConfigurator = convert(sslHandler.getSslProperties());

        if (Objects.isNull(sslContext) && sslHandler.isSslEnabled()) {
            sslContext = sslConfigurator.createSSLContext(true);
        }

        server = createServer();
        server.getServerConfiguration().setAllowPayloadForUndefinedHttpMethods(true);
        state.set(LifeCycle.INITIALIZED);
    }

    public synchronized void stop() {
        if (state.compareAndExchange(LifeCycle.STARTED, LifeCycle.STOPPING) == LifeCycle.STARTED) {
            logger.info("Stopping HTTP Server");
            final var shutdown = server.shutdown(30, TimeUnit.SECONDS);
            if (!shutdown.isDone()) {
                server.shutdownNow();
            }
            state.set(LifeCycle.INITIALIZED);
        } else {
            logger.warn("Stop denied. Current state={}", state.get());
        }
    }

    private SSLContextConfigurator convert(final SSLProperties sslProperties) {
        final SSLContextConfigurator sslCon = new SSLContextConfigurator();
        sslCon.setKeyStoreFile(sslProperties.getKeyStore().getFilename());
        sslCon.setKeyStorePass(sslProperties.getKeyStorePassword());
        sslCon.setKeyPass(sslProperties.getKeyPassword());
        sslCon.setTrustStoreFile(sslProperties.getTrustStore().getFilename());
        sslCon.setTrustStorePass(sslProperties.getTrustStorePassword());
        return sslCon;
    }

    private org.glassfish.grizzly.http.server.HttpServer createServer() {
        if (sslHandler.isSslEnabled()) {
            final var sslEngineConfigurator = new SSLEngineConfigurator(sslContext, false, needClientAuth,
                                                                        wantClientAuth);
            return GrizzlyHttpServerFactory.createHttpServer(getBaseUri(), config, true, sslEngineConfigurator, false);
        } else {
            return GrizzlyHttpServerFactory.createHttpServer(getBaseUri(), config, false);
        }
    }

    private URI getBaseUri() {
        final String scheme = sslHandler.isSslEnabled() ? CommonConstants.HTTPS : CommonConstants.HTTP;
        return Utilities.createURI(scheme, host, port, "/").toUri();
    }

    public synchronized void start() throws IOException {
        if (state.compareAndExchange(LifeCycle.INITIALIZED, LifeCycle.STARTING) == LifeCycle.INITIALIZED) {
            logger.info("Starting HTTP Server");
            server.start();
            state.set(LifeCycle.STARTED);
        }
    }

    @PreDestroy
    public synchronized void shutdown() {
        if (Objects.nonNull(server)) {
            logger.info("Shutting down HTTP Server");
            server.shutdown(5, TimeUnit.SECONDS);
            server.shutdownNow();
        }
    }

    public int getPort() {
        return port;
    }

    private enum LifeCycle {
        NEW, INITIALIZED, STARTING, STARTED, STOPPING;
    }
}
