package br.com.pw2;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
public interface WaAutomateNodejs {
    @POST
    @Path("/sendText")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String sendText(String args);

    @POST
    @Path("/getAllChatIds")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String getAllChatIds();

    @POST
    @Path("/sendImage")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String sendImage(String args);

    @POST
    @Path("/addParticipant")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String addParticipant(String args);

    @POST
    @Path("/removeParticipant")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String removeParticipant(String args);

    @POST
    @Path("/sendContact")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String sendContact(String args);

    @POST
    @Path("/getCommunityParticipantIds")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String getCommunityParticipantIds(String args);
}
