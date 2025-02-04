package br.com.pw2.resources;

import br.com.pw2.WaAutomateNodejs;
import br.com.pw2.entities.AdvertisingEntity;
import br.com.pw2.entities.Category;
import br.com.pw2.entities.CommunityEntity;
import br.com.pw2.entities.ContactEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Path("/API")
@ApplicationScoped
public class Endpoints implements HealthCheck {

    private static final Logger LOGGER = Logger.getLogger(Endpoints.class.getName());

    @Inject
    @RestClient
    private WaAutomateNodejs waAutomateNodejs;

    @Inject
    private EntityManager entityManager;

    @Inject
    private ContactEntityResource contactEntityResource;

    private static String initialMenu(String recipient) {
        return Json.createObjectBuilder()
                .add("args",
                        Json.createObjectBuilder()
                                .add("to", recipient)
                                .add("content",
                                        """
                                                Olá! 👋 Bem-vindo ao bot de whatsapp da cadeira de Programação para web 2! (mensagens automáticas)
                                                📩 Para receber aviso regularmente na comunidade, adicione esse contato caso ainda não esteja adicionado e responda com a palavra "avisos".
                                                📚 Para ver o resultado do health check desse serviço, responda com a palavra "health".
                                                """)
                                .build())
                .build().toString();
    }

    private static String subscribedToCommunitySuccesfully(String recipient) {
        return Json.createObjectBuilder()
                .add("args",
                        Json.createObjectBuilder()
                                .add("to", recipient)
                                .add("content",
                                        """
                                                🎉 Você foi adicionado com sucesso à comunidade da cadeira de Programação para web 2!
                                                ❌ Caso mude de idéia, basta selecionar a opção de sair diretamente na comunidade.
                                                """)
                                .build())
                .build().toString();
    }

    private static String alreadySubscribedToCommunity(String recipient) {
        return Json.createObjectBuilder()
                .add("args",
                        Json.createObjectBuilder()
                                .add("to", recipient)
                                .add("content", "✅ Identificamos que você já está inscrito em uma comunidade.")
                                .build())
                .build().toString();
    }

    private static String contactNoticesNotAdded(String recipient) {
        return Json.createObjectBuilder()
                .add("args",
                        Json.createObjectBuilder()
                                .add("to", recipient)
                                .add("content", "Verifique se você adicionou esse número aos seus contatos e tente novamente.")
                                .build())
                .build().toString();
    }

    private static String healtCheck(String recipient, boolean isUp) {
        String obs = "OBS.: Embora possa ser usado, o Health checks não se destina como uma solução de monitoramento de serviços para operadores humanos";
        String statusMessage = isUp ? "Status do serviço é up. ".concat(obs) : "Status do serviço é down".concat(obs);
        return Json.createObjectBuilder()
                .add("args",
                        Json.createObjectBuilder()
                                .add("to", recipient)
                                .add("content", statusMessage)
                                .build())
                .build().toString();
    }

    @Path("/sendText")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String sendText(@QueryParam("to") String to, @QueryParam("content") String content) {
        String jsonBody = Json.createObjectBuilder()
                .add("args",
                        Json.createObjectBuilder()
                                .add("to", to)
                                .add("content", content)
                                .build())
                .build().toString();
        return waAutomateNodejs.sendText(jsonBody);
    }

    @Path("/tick")
    @Transactional
    @POST
    public void tick() {
        TypedQuery<Object[]> query = entityManager.createNamedQuery("AdvertisingEntity.findAllNotSentYet", Object[].class);
        // retorna apenas o id (Object[0]) e zonedDateTime (Object[1]) de todos os advertisings que ainda nao foram enviados

        query.getResultList().forEach(adWithNoImage -> {
            if ((ZonedDateTime.now()).isAfter((ZonedDateTime) adWithNoImage[1])) {
                AdvertisingEntity adWithImage = entityManager.find(AdvertisingEntity.class, adWithNoImage[0]);
                List<CommunityEntity> communities = CommunityEntity.find("category", Category.ALUNOS).list();
                communities.forEach(community -> {
                    if (adWithImage.getImageDataAsBase64() != null)
                        CompletableFuture.runAsync(() -> sendImage(community.number, adWithImage.getImageDataAsBase64(), adWithImage.message));
                    else
                        CompletableFuture.runAsync(() -> sendText(community.number, adWithImage.message));
                });
                adWithImage.sent = true;
                adWithImage.persist();
            }
        });
    }

    @Path("/sendTextToAll")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response sendTextToAll(@FormParam("message") String message, @FormParam("url") String url) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(getAllChatIds()); // Parse JSON into a JsonNode
        JsonNode responseNode = rootNode.get("response"); // Extract "response" array
        if (url == null || url.isBlank()) {
            responseNode.forEach(node -> {
                if (node.asText().contains("@g.us")) sendText(node.asText(), message);
            });

        } else {
            try {
                URI uri = new URI(url);
                HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                // Check if the URL is valid and points to an image
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    return Response.status(Response.Status.BAD_REQUEST).entity("Failed to fetch the image").build();
                }

                String contentType = connection.getContentType();
                if (!contentType.startsWith("image/")) {
                    return Response.status(Response.Status.BAD_REQUEST).entity("URL does not point to a valid image").build();
                }

                // Read the image data as bytes
                try (InputStream inputStream = connection.getInputStream()) {
                    byte[] imageBytes = inputStream.readAllBytes();

                    // Convert the bytes to a Base64 string
                    String base64String = Base64.getEncoder().encodeToString(imageBytes);

                    // Format as a Data URI
                    String base64DataUri = "data:" + contentType + ";base64," + base64String;

                    responseNode.forEach(node -> {
                        if (node.asText().contains("@g.us")) sendImage(node.asText(), base64DataUri, message);
                    });

                }
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        }
        return Response.status(Response.Status.OK).build();
    }

    @Path("/getAllCommunityIds")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllCommunityIds() {
        List<String> names = CommunityEntity.find("select name from CommunityEntity").project(String.class).list();
        return Response.ok(names).build();
    }

    @Path("/getAllChatIds")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @CacheResult(cacheName = "chatIdsCache")
    public String getAllChatIds() {
        return waAutomateNodejs.getAllChatIds();
    }

    @Path("/listener")
    @POST
    public Response listener(String body) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(body);
        String event = rootNode.get("event").asText();

        try {
            if ("onMessage".equals(event)) {
                JsonNode data = rootNode.get("data");
                String from = data.get("from").asText();
                String to = data.get("to").asText();
                String messageBody = data.get("body").asText();

                if ("555186559754@c.us".equals(to) && from.contains("@c.us")) {
                    String name = data.get("chat").get("contact").get("pushname").asText();
                    LOGGER.info("Message: " + messageBody + ", from: " + from + ", name: " + name);
                    if ("pw2".equalsIgnoreCase(messageBody)) {
                        LOGGER.info("Sending initial menu to: " + from);
                        waAutomateNodejs.sendText(initialMenu(from));
                        return Response.status(200).build();
                    } else if ("health".equalsIgnoreCase(messageBody)) {
                        LOGGER.info("Sending health check to: " + from);
                        if (call().getStatus().equals(HealthCheckResponse.Status.UP))
                            waAutomateNodejs.sendText(healtCheck(from, true));
                        else
                            waAutomateNodejs.sendText(healtCheck(from, false));
                        return Response.status(200).build();
                    } else if ("avisos".equalsIgnoreCase(messageBody)) {
                        contactEntityResource.create(from, name);
                        var resultAddParticipant = contactEntityResource.addParticipant(from);
                        Thread.sleep(4000);
                        if (resultAddParticipant.getStatus() == 201) {
                            var communityNumber = ((ContactEntity) resultAddParticipant.getEntity()).communityEntity.number;
                            if (contactEntityResource.checkIfParticipantWasAdded(communityNumber, from))
                                waAutomateNodejs.sendText(subscribedToCommunitySuccesfully(from));
                            else waAutomateNodejs.sendText(contactNoticesNotAdded(from));
                        } else if (resultAddParticipant.getStatus() == 200)
                            waAutomateNodejs.sendText(alreadySubscribedToCommunity(from));
                        return resultAddParticipant;
                    }
                }
            }
            return Response.status(500).entity("Event not treated yet").build();
        } catch (Exception e) {
            LOGGER.error("Error processing listener event", e);
            return Response.status(500).entity("Error processing listener event").build();
        }
    }

    // esse endpoint itera sobre todas as comunidades e, para cada uma delas, chama o metodo contactEntityResource.removeParticipant
    // que por sua vez, atualiza o campo numberOfContacts para cada comunidade, alem de atualizar o campo communityEntity
    // para os contatos que sairam das comunidades. Isso eh necessario pois nao encontrei uma forma de capturar o evento de saida de um participante das comunidades
    // ha uma rotina configurada na cron para executar esse metodo a cada hora
    // 0       *       *       *       *       /usr/bin/curl -X POST http://localhost:8080/API/checkCommunityParticipants
    @PUT
    @Path("/checkCommunityParticipants")
    @Transactional
    public void checkCommunityParticipants() {
        List<CommunityEntity> communityEntities = CommunityEntity.listAll();
        communityEntities.forEach(community -> {
            try {
                contactEntityResource.removeParticipant(community.number);
            } catch (JsonProcessingException e) {
                LOGGER.error("Error removing participant for community number: " + community.number, e);
                throw new RuntimeException(e);
            }
        });
    }

    @Path("/sendImage")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String sendImage(@QueryParam("to") String to, @QueryParam("file") String
            file, @QueryParam("caption") String caption) {
        String jsonBody = Json.createObjectBuilder().add("args",
                        Json.createObjectBuilder()
                                .add("to", to)
                                .add("file", file)
                                .add("caption", caption)
                                .add("filename", "guarapari.jpg")
                                .add("quotedMsgId", "false_447123456789@c.us_9C4D0965EA5C09D591334AB6BDB07FEB")
                                // quoteMsgId nao esta passando o campo que deveria passar, talvez tenhamos que alterar isso futuramente
                                .add("waitForId", false)
                                .add("ptt", false)
                                .add("withoutPreview", false)
                                .add("hideTags", false)
                                .add("viewOnce", false)
                                .build())
                .build().toString();
        return waAutomateNodejs.sendImage(jsonBody);
    }

    @Override
    @GET
    @Path("/health")
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder response = HealthCheckResponse.named(Endpoints.class.getName());
        try {
            String chatIds = waAutomateNodejs.getAllChatIds();
            if (!chatIds.isEmpty())
                response.up().withData("Whatsapp API service", "up");
            else
                response.down().withData("Whatsapp API service", "no chat ids returned");
        } catch (Exception e) {
            response.down().withData("Whatsapp API service", "error: " + e.getMessage());
        }
        return response.build();
    }

}
