package eu.arrowhead.demo.web;

@FunctionalInterface
public interface HttpServerCustomizer {

    void customize(final HttpServer server);
}
