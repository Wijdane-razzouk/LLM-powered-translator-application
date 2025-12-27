package org.translate.com.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import org.translate.com.config.EnvConfig;

/**
 * Basic authentication filter implemented with Jakarta Authentication concepts.
 * This keeps the example lightweight while still exercising the Basic scheme.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class BasicAuthFilter implements ContainerRequestFilter {

    private static final String REALM = "translator-api";
    private static final String BASIC_PREFIX = "Basic ";
    private static final Set<String> PUBLIC_PATHS = Set.of("translator/ping");

    private final String expectedUsername = EnvConfig.getPreferringFile("TRANSLATOR_USER");
    private final String expectedPassword = EnvConfig.getPreferringFile("TRANSLATOR_PASSWORD");
    private final boolean authEnabled = hasText(expectedUsername) && hasText(expectedPassword);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();
        if (PUBLIC_PATHS.contains(path)) {
            return;
        }

        if (!authEnabled) {
            return;
        }

        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BASIC_PREFIX)) {
            abortWithUnauthorized(requestContext, "Missing Authorization header");
            return;
        }

        String[] credentials = decode(authHeader.substring(BASIC_PREFIX.length()).trim());
        if (credentials.length != 2 || !isAuthorized(credentials[0], credentials[1])) {
            abortWithUnauthorized(requestContext, "Invalid credentials");
            return;
        }

        setSecurityContext(requestContext, credentials[0]);
    }

    private void setSecurityContext(ContainerRequestContext requestContext, String username) {
        SecurityContext original = requestContext.getSecurityContext();
        SecurityContext authenticated = new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> username;
            }

            @Override
            public boolean isUserInRole(String role) {
                return true;
            }

            @Override
            public boolean isSecure() {
                return original != null && original.isSecure();
            }

            @Override
            public String getAuthenticationScheme() {
                return SecurityContext.BASIC_AUTH;
            }
        };

        requestContext.setSecurityContext(authenticated);
    }

    private boolean isAuthorized(String username, String password) {
        return expectedUsername.equals(username) && expectedPassword.equals(password);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String[] decode(String base64Credentials) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Credentials);
            return new String(decoded, StandardCharsets.UTF_8).split(":", 2);
        } catch (IllegalArgumentException e) {
            return new String[0];
        }
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext, String message) {
        Response unauthorized = Response.status(Response.Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, BASIC_PREFIX.trim() + " realm=\"" + REALM + "\"")
                .entity(message)
                .build();
        requestContext.abortWith(unauthorized);
    }
}
