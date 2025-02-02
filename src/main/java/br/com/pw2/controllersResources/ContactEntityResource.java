package br.com.pw2.controllersResources;

import br.com.pw2.WaAutomateNodejs;
import br.com.pw2.entities.Category;
import br.com.pw2.entities.CommunityEntity;
import br.com.pw2.entities.ContactEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;

@Path("contact")
@ApplicationScoped
public class ContactEntityResource {

    private static final Logger LOGGER = Logger.getLogger(ContactEntityResource.class.getName());

    @Inject
    @RestClient
    private WaAutomateNodejs waAutomateNodejs;

    private static String createJson(ContactEntity contact) {
        return Json.createObjectBuilder()
                .add("args",
                        Json.createObjectBuilder()
                                .add("groupId", contact.communityEntity.number)
                                .add("participantId", contact.number)
                                .build())
                .build().toString();
    }

    private static String createGetCommunityParticipantsArgs(String communityId) {
        return Json.createObjectBuilder()
                .add("args",
                        Json.createObjectBuilder()
                                .add("communityId", communityId)
                                .build())
                .build().toString();
    }

    @GET
    public List<ContactEntity> get() {
        return ContactEntity.listAll(Sort.by("number"));
    }

    @GET
    @Path("{number}")
    public ContactEntity getSingle(@PathParam("number") String number) {
        ContactEntity entity = ContactEntity.findByNumber(number);
        if (entity == null)
            throw new WebApplicationException("Contact with number of " + number + " does not exist.", 404);
        return entity;
    }

    @Transactional
    public void create(String number, String name) {
        try {
            ContactEntity tmpContact = ContactEntity.findByNumber(number);
            if (tmpContact != null) LOGGER.info("create() - Contact already exists with number: " + number);
            else {
                ContactEntity contact = new ContactEntity(number, name);
                contact.persist();
                LOGGER.info("Contact created successfully with number: " + number);
            }
        } catch (Exception e) {
            LOGGER.error("Error creating contact with number: " + number, e);
        }
    }

    @Transactional
    public Response addParticipant(@NotNull String number) {
        try {
            ContactEntity contact = ContactEntity.findByNumber(number);
            if (contact.communityEntity != null) {
                LOGGER.info("Contact already has a community entity.");
                return Response.status(200).entity(contact).build();
            }

            contact.communityEntity = CommunityEntity.find("receivingNewContacts = ?1 and category = ?2", true, Category.ALUNOS).firstResult();
            waAutomateNodejs.addParticipant(createJson(contact));
            contact.persist();

            return Response.status(201).entity(contact).build();
        } catch (Exception e) {
            LOGGER.error("Error adding participant for contact with number: " + number, e);
            return Response.status(500).entity("Error adding participant").build();
        }
    }

    @Transactional
    public void removeParticipant(@NotNull String communityNumber) throws JsonProcessingException {
        LOGGER.info("Starting removeParticipant method for community number: " + communityNumber);
        String body = waAutomateNodejs.getCommunityParticipantIds(createGetCommunityParticipantsArgs(communityNumber));
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(body);
        int numberOfContacts = rootNode.get("response").get(0).get("participants").size();
        String communityParticipantsCorrect = rootNode.get("response").get(0).get("participants").toString();

        CommunityEntity community = CommunityEntity.find("number", communityNumber).firstResult();
        community.numberOfContacts = numberOfContacts;
        community.persist();
        List<ContactEntity> communityParticipantsInDatabase = ContactEntity.list("communityEntity", community);

        communityParticipantsInDatabase.forEach(contact -> {
            if (!communityParticipantsCorrect.contains(contact.number)) {
                LOGGER.info("Removing participant: " + contact.number + " " + contact.name);
                contact.communityEntity = null;
                contact.persist();
            }
        });
        LOGGER.info("Completed removeParticipant method for community number: " + communityNumber);
    }

    @Transactional
    public boolean checkIfParticipantWasAdded(String communityNumber, String clientNumber) throws JsonProcessingException {
        String body = waAutomateNodejs.getCommunityParticipantIds(createGetCommunityParticipantsArgs(communityNumber));
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(body);
        String communityParticipantsCorrect = rootNode.get("response").get(0).get("participants").toString();
        boolean participantAdded = communityParticipantsCorrect.contains(clientNumber);
        if (!participantAdded) {
            ContactEntity contact = ContactEntity.findByNumber(clientNumber);
            if (contact != null) {
                contact.communityEntity = null;
                contact.persist();
            }
            LOGGER.info("checkIfParticipantWasAdded() - " + clientNumber + " was NOT added to community number: " + communityNumber);
            return false;
        }
        LOGGER.info("checkIfParticipantWasAdded() - " + clientNumber + " was added to community number: " + communityNumber);
        return true;
    }

}