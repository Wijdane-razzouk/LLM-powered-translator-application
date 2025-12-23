package org.translate.com;

import java.io.IOException;
import java.net.URI;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.translate.com.config.EnvConfig;
import org.translate.com.config.TranslatorApplication;


public class EmbeddedServer {

    public static void main(String[] args) throws IOException, InterruptedException {
        EnvConfig.loadDotEnv();
        String baseUri = System.getProperty("translator.api.uri", "http://localhost:8080/");
        ResourceConfig config = new TranslatorApplication();

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(baseUri), config);
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));

        System.out.println("Translator API running on " + baseUri + " (Ctrl+C to stop)");
        server.start();
        Thread.currentThread().join();
    }
}
