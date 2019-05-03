package com.globo.pepe.chapolin;


import static com.globo.pepe.chapolin.services.RequestService.X_PEPE_TRIGGER_HEADER;
import static com.globo.pepe.common.util.Constants.PACK_NAME;
import static com.globo.pepe.common.util.Constants.TRIGGER_PREFIX;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.globo.pepe.chapolin.services.JsonSchemaGeneratorService;
import com.globo.pepe.chapolin.services.RequestService;
import com.globo.pepe.chapolin.services.StackstormService;
import com.globo.pepe.common.model.Event;
import com.globo.pepe.common.model.Metadata;
import com.globo.pepe.common.services.JsonLoggerService;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.mockserver.integration.ClientAndServer;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
        "pepe.logging.tags=default",
        "pepe.chapolin.stackstorm.url=http://127.0.0.1:9101/api/v1",
        "pepe.chapolin.stackstorm.key=mykey"
})
@ContextConfiguration(classes = {StackstormService.class, RequestService.class, JsonLoggerService.class, ObjectMapper.class, JsonSchemaGeneratorService.class}, loader = AnnotationConfigContextLoader.class)
public class StackstormServiceTests {

    private static final String triggerNameCreated = "triggerNameOK";
    private static final String triggerFullNameCreated = PACK_NAME + "." + TRIGGER_PREFIX + "." + triggerNameCreated;

    private static final String triggerNameNotCreated = "triggerNameNotCreated";
    private static final String triggerFullNameNotCreated = PACK_NAME + "." + TRIGGER_PREFIX + "." + triggerNameNotCreated;

    private static ClientAndServer mockServer;

    @Autowired
    public StackstormService stackstormService;

    ObjectMapper mapper = new ObjectMapper();

    @BeforeClass
    public static void setupClass() throws IOException {
        mockServer = ClientAndServer.startClientAndServer(9101);

        //1st case: Trigger already exists
        InputStream triggerExists = StackstormServiceTests.class.getResourceAsStream("/trigger-exists.json");
        String bodyTriggerExists = IOUtils.toString(triggerExists, Charset.defaultCharset());
        mockServer.when(request().withMethod("GET").withPath("/api/v1/triggertypes/"+ triggerFullNameCreated))
                .respond(response().withBody(bodyTriggerExists).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(200));

        InputStream eventSent = StackstormServiceTests.class.getResourceAsStream("/event-sent.json");
        String bodyEventSent = IOUtils.toString(eventSent, Charset.defaultCharset());
        mockServer.when(request().withMethod("POST").withPath("/api/v1/webhooks/st2").withHeader(X_PEPE_TRIGGER_HEADER, triggerFullNameCreated))
                .respond(response().withBody(bodyEventSent).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(202));


        //2nd case: Trigger don't exist
        InputStream triggerExistsFail = StackstormServiceTests.class.getResourceAsStream("/trigger-exists-fail.json");
        String bodyTriggerExistsFail = IOUtils.toString(triggerExistsFail, Charset.defaultCharset());
        mockServer.when(request().withMethod("GET").withPath("/api/v1/triggertypes/"+ triggerFullNameNotCreated))
                .respond(response().withBody(bodyTriggerExistsFail).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(404));

        InputStream triggerCreated = StackstormServiceTests.class.getResourceAsStream("/trigger-created.json");
        String bodyTriggerCreated = IOUtils.toString(triggerCreated, Charset.defaultCharset());
        mockServer.when(request().withMethod("POST").withPath("/api/v1/triggertypes").withHeader(X_PEPE_TRIGGER_HEADER, triggerFullNameNotCreated))
                .respond(response().withBody(bodyTriggerCreated).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(201));

        InputStream eventSentWithNewTrigger = StackstormServiceTests.class.getResourceAsStream("/event-sent.json");
        String bodyEventSentWithNewTrigger = IOUtils.toString(eventSentWithNewTrigger, Charset.defaultCharset());
        mockServer.when(request().withMethod("POST").withPath("/api/v1/webhooks/st2").withHeader(X_PEPE_TRIGGER_HEADER, triggerFullNameNotCreated))
                .respond(response().withBody(bodyEventSentWithNewTrigger).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(202));


    }

    @AfterClass
    public static void cleanup(){
        if (mockServer.isRunning()){
            mockServer.stop();
        }
    }


    @Test
    public void sendTriggerCreatedTest(){
        Event event = new Event();
        event.setId("1");

        Metadata metadata = new Metadata();
        metadata.setTriggerName(triggerNameCreated);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("attribute1", "value1");

        event.setMetadata(metadata);
        event.setPayload(payload);

        assertTrue(stackstormService.send(mapper.valueToTree(event)));

    }

    @Test
    public void sendTriggerNotCreatedTest(){
        Event event = new Event();
        event.setId("2");

        Metadata metadata = new Metadata();
        metadata.setTriggerName(triggerNameNotCreated);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("attribute1", "value1");

        event.setMetadata(metadata);
        event.setPayload(payload);

        assertTrue(stackstormService.send(mapper.valueToTree(event)));

    }
}
