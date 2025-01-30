package br.com.pw2;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOGGER = Logger.getLogger(GenericExceptionMapper.class);

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof NotFoundException) {
            String attemptedPath = uriInfo.getAbsolutePath().toString();
            LOGGER.warn("Resource not found: " + exception.getMessage() + ", Path: " + attemptedPath);
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("The requested resource was not found.")
                    .build();
        } else if (exception instanceof IllegalArgumentException) {
            LOGGER.error("Invalid argument: " + exception.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid request.")
                    .build();
        }

        // Fallback for all other exceptions
        LOGGER.error("Unexpected error: ", exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("An unexpected error occurred.")
                .build();
    }
}

