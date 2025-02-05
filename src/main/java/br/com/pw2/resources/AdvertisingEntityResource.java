package br.com.pw2.resources;

import br.com.pw2.entities.AdvertisingEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("advertising")
@ApplicationScoped
public class AdvertisingEntityResource {

    private static final Logger LOGGER = Logger.getLogger(AdvertisingEntityResource.class.getName());

    @Inject
    EntityManager entityManager;

    @GET
    @Path("{id}")
    public Response getSingle(@PathParam("id") Long id) {
        AdvertisingEntity entity = AdvertisingEntity.findById(id);
        if (entity == null) throw new WebApplicationException("Advertising not found", 404);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("id", entity.id);
        responseMap.put("message", entity.message);
        responseMap.put("zonedDateTime", entity.zonedDateTime.toString());

        if (entity.imageData != null) {
            String base64Image = Base64.getEncoder().encodeToString(entity.imageData);
            responseMap.put("imageData", base64Image);
        }

        return Response.ok(responseMap).build();
    }

    @GET
    public List<Object[]> findAll() {
        TypedQuery<Object[]> query = entityManager.createNamedQuery("AdvertisingEntity.findAll", Object[].class);
        return query.getResultList();
    }

    @POST
    @Transactional
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response create(@FormParam("message") String message, @FormParam("image") InputStream image,
                           @FormParam("zonedDateTime") ZonedDateTime zonedDateTime) {
        LOGGER.info("Starting create method");
        try {
            if (message == null || image == null || zonedDateTime == null) {
                LOGGER.warn("Missing required fields");
                throw new WebApplicationException("Missing required fields", 422);
            }

            AdvertisingEntity advertising = new AdvertisingEntity(message, image.readAllBytes(), zonedDateTime);
            advertising.persist();
            LOGGER.info("Advertising created successfully");

            return Response.status(201).entity(advertising).build();
        } catch (Exception e) {
            LOGGER.error("Error creating advertising", e);
            return Response.status(500).entity("Error creating advertising").build();
        }
    }

    @PUT
    @Path("/edit/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response editAdvertising(@PathParam("id") Long id,
                                    @FormParam("message") String message,
                                    @FormParam("zonedDateTime") String zonedDateTimeString,
                                    @FormParam("image") InputStream image) throws IOException {
        AdvertisingEntity entity = AdvertisingEntity.findById(id);
        if (entity == null) return Response.status(404).entity("Advertising not found").build();
        if (message != null) entity.message = message;
        if (zonedDateTimeString != null) {
            try {
                // Parse the received string into a ZonedDateTime
                ZonedDateTime userZonedDateTime = ZonedDateTime.parse(zonedDateTimeString);

                // Convert to UTC and store
                entity.zonedDateTime = userZonedDateTime.withZoneSameInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException e) {
                return Response.status(400).entity("Invalid date format: " + zonedDateTimeString).build();
            }
        }
        if (image != null && image.available() > 0) entity.imageData = image.readAllBytes(); // Store the raw bytes if needed for retrieval

        entity.persist();
        return Response.ok().entity(entity).build();
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        LOGGER.info("Starting delete method for id: " + id);
        try {
            if (id == null || id <= 0) {
                LOGGER.warn("Invalid ID provided: " + id);
                throw new WebApplicationException("Invalid ID provided", 400);
            }

            AdvertisingEntity entity = AdvertisingEntity.findById(id);
            if (entity == null) {
                LOGGER.warn("Advertising with id " + id + " does not exist.");
                throw new WebApplicationException("Advertising with id " + id + " does not exist.", 404);
            }
            entity.delete();
            LOGGER.info("Advertising deleted successfully with id: " + id);
            return Response.status(204).build();
        } catch (Exception e) {
            LOGGER.error("Error deleting advertising with id: " + id, e);
            return Response.status(500).entity("Error deleting advertising").build();
        }
    }

}
