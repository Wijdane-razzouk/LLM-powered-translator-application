package org.translate.com.config;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/api")
public class TranslatorApplication extends ResourceConfig {

    public TranslatorApplication() {

        packages("org.translate.com.api", "org.translate.com.security", "org.translate.com.config");

        register(org.glassfish.jersey.jackson.JacksonFeature.class);

        register(CorsFilter.class);

        System.out.println(">>> TranslatorApplication loaded successfully");
    }
}
