package org.translate.com.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;

@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION - 1)
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!"OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            return;
        }

        String origin = requestContext.getHeaderString("Origin");
        Response.ResponseBuilder builder = Response.ok();
        applyCorsHeaders(builder, origin);
        requestContext.abortWith(builder.build());
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {
        String origin = requestContext.getHeaderString("Origin");
        applyCorsHeaders(responseContext.getHeaders(), origin);
    }

    private void applyCorsHeaders(Response.ResponseBuilder builder, String origin) {
        String allowOrigin = (origin == null || origin.isBlank()) ? "*" : origin;
        builder.header("Access-Control-Allow-Origin", allowOrigin);
        builder.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        builder.header("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization");
        builder.header("Access-Control-Expose-Headers", "WWW-Authenticate");
        builder.header("Vary", "Origin");
        if (!"*".equals(allowOrigin)) {
            builder.header("Access-Control-Allow-Credentials", "true");
        }
    }

    private void applyCorsHeaders(MultivaluedMap<String, Object> headers, String origin) {
        String allowOrigin = (origin == null || origin.isBlank()) ? "*" : origin;
        headers.putSingle("Access-Control-Allow-Origin", allowOrigin);
        headers.putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        headers.putSingle("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization");
        headers.putSingle("Access-Control-Expose-Headers", "WWW-Authenticate");
        headers.putSingle("Vary", "Origin");
        if (!"*".equals(allowOrigin)) {
            headers.putSingle("Access-Control-Allow-Credentials", "true");
        }
    }
}
