package com.globo.pepe.chapolin.services;


import static com.globo.pepe.chapolin.services.RequestService.X_PEPE_TRIGGER_HEADER;
import static com.globo.pepe.chapolin.suites.PepeSuiteTests.mockApiServer;
import static com.globo.pepe.chapolin.suites.PepeSuiteTests.mockApiServerApiKeyCreated;
import static com.globo.pepe.common.util.Constants.PACK_NAME;
import static com.globo.pepe.common.util.Constants.TRIGGER_PREFIX;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.globo.pepe.common.model.Event;
import com.globo.pepe.common.model.Metadata;
import com.globo.pepe.common.services.JsonLoggerService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
        "pepe.logging.tags=default",
        "pepe.chapolin.stackstorm.api=http://127.0.0.1:9101/api/v1",
        "pepe.chapolin.stackstorm.auth=http://127.0.0.1:9100/auth/v1",
        "pepe.chapolin.stackstorm.login=u_pepe",
        "pepe.chapolin.stackstorm.password=u_pepe"
})
@ContextConfiguration(classes = {StackstormService.class, RequestService.class, JsonLoggerService.class, ObjectMapper.class, JsonSchemaGeneratorService.class}, loader = AnnotationConfigContextLoader.class)
public class StackstormServiceTests {

    private static final String triggerNameCreated = "triggerNameOK";
    private static final String triggerFullNameCreated = PACK_NAME + "." + TRIGGER_PREFIX + "." + triggerNameCreated;

    private static final String triggerNameNotCreated = "triggerNameNotCreated";
    private static final String triggerFullNameNotCreated = PACK_NAME + "." + TRIGGER_PREFIX + "." + triggerNameNotCreated;

    private static final String triggerNameEmpty = "";
    private static final String triggerFullNameEmpty = PACK_NAME + "." + TRIGGER_PREFIX + "." + triggerNameEmpty;

    @Autowired
    public StackstormService stackstormService;

    @Autowired
    private ObjectMapper mapper;

    public void mockSendTriggerCreated() throws IOException {
        mockApiServer.reset();

        mockApiServerApiKeyCreated();

        InputStream triggerExists = StackstormServiceTests.class.getResourceAsStream("/trigger-exists.json");
        String bodyTriggerExists = IOUtils.toString(triggerExists, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("GET").withPath("/api/v1/triggertypes/"+ triggerFullNameCreated))
                .respond(response().withBody(bodyTriggerExists).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(200));

        InputStream eventSent = StackstormServiceTests.class.getResourceAsStream("/event-sent.json");
        String bodyEventSent = IOUtils.toString(eventSent, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("POST").withPath("/api/v1/webhooks/st2").withHeader(X_PEPE_TRIGGER_HEADER, triggerFullNameCreated))
                .respond(response().withBody(bodyEventSent).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(202));

    }

    public void mockSendTriggerNotCreated() throws IOException {
        mockApiServer.reset();

        mockApiServerApiKeyCreated();

        InputStream triggerExistsFail = StackstormServiceTests.class.getResourceAsStream("/trigger-exists-fail.json");
        String bodyTriggerExistsFail = IOUtils.toString(triggerExistsFail, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("GET").withPath("/api/v1/triggertypes/" + triggerFullNameNotCreated))
                .respond(response().withBody(bodyTriggerExistsFail).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(404));

        InputStream triggerCreated = StackstormServiceTests.class.getResourceAsStream("/trigger-created.json");
        String bodyTriggerCreated = IOUtils.toString(triggerCreated, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("POST").withPath("/api/v1/triggertypes").withHeader(X_PEPE_TRIGGER_HEADER, triggerFullNameNotCreated))
                .respond(response().withBody(bodyTriggerCreated).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(201));

        InputStream eventSentWithNewTrigger = StackstormServiceTests.class.getResourceAsStream("/event-sent.json");
        String bodyEventSentWithNewTrigger = IOUtils.toString(eventSentWithNewTrigger, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("POST").withPath("/api/v1/webhooks/st2").withHeader(X_PEPE_TRIGGER_HEADER, triggerFullNameNotCreated))
                .respond(response().withBody(bodyEventSentWithNewTrigger).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(202));

    }

    public void mockCreateTriggerWithoutName() throws IOException {
        mockApiServer.reset();

        mockApiServerApiKeyCreated();

        InputStream triggerNotFoundWithoutName = StackstormServiceTests.class.getResourceAsStream("/trigger-without-name.json");
        String bodyTriggerExistsFail = IOUtils.toString(triggerNotFoundWithoutName, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("GET").withPath("/api/v1/triggertypes/"+ triggerFullNameEmpty))
                .respond(response().withBody(bodyTriggerExistsFail).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(404));

        InputStream triggerNotCreated = StackstormServiceTests.class.getResourceAsStream("/trigger-creation-failed-without-name.json");
        String bodyTriggerNotCreated = IOUtils.toString(triggerNotCreated, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("POST").withPath("/api/v1/triggertypes").withHeader(X_PEPE_TRIGGER_HEADER, triggerFullNameEmpty))
                .respond(response().withBody(bodyTriggerNotCreated).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(400));

    }

    public void mockTriggerExist500() throws IOException {
        mockApiServer.reset();

        mockApiServerApiKeyCreated();

        mockApiServer.when(request().withMethod("GET").withPath("/api/v1/triggertypes/"+ triggerFullNameCreated))
            .respond(response().withStatusCode(500));
    }

    public void mockCreateTrigger500() throws IOException {
        mockApiServer.reset();

        mockApiServerApiKeyCreated();

        InputStream triggerExistsFail = StackstormServiceTests.class.getResourceAsStream("/trigger-exists-fail.json");
        String bodyTriggerExistsFail = IOUtils.toString(triggerExistsFail, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("GET").withPath("/api/v1/triggertypes/"+ triggerFullNameNotCreated))
                .respond(response().withBody(bodyTriggerExistsFail).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(404));

        mockApiServer.when(request().withMethod("POST").withPath("/api/v1/triggertypes"))
                .respond(response().withStatusCode(500));
    }

    public void mockSendTrigger500() throws IOException {
        mockApiServer.reset();

        mockApiServerApiKeyCreated();

        InputStream triggerExistsFail = StackstormServiceTests.class.getResourceAsStream("/trigger-exists-fail.json");
        String bodyTriggerExistsFail = IOUtils.toString(triggerExistsFail, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("GET").withPath("/api/v1/triggertypes/" + triggerFullNameNotCreated))
                .respond(response().withBody(bodyTriggerExistsFail).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(404));

        InputStream triggerCreated = StackstormServiceTests.class.getResourceAsStream("/trigger-created.json");
        String bodyTriggerCreated = IOUtils.toString(triggerCreated, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("POST").withPath("/api/v1/triggertypes").withHeader(X_PEPE_TRIGGER_HEADER, triggerFullNameNotCreated))
                .respond(response().withBody(bodyTriggerCreated).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(201));

        mockApiServer.when(request().withMethod("POST").withPath("/api/v1/webhooks/st2").withHeader(X_PEPE_TRIGGER_HEADER, triggerFullNameNotCreated))
                .respond(response().withStatusCode(500));
    }

    @Test
    public void sendTriggerCreatedTest() throws IOException {
        Event event = new Event();
        event.setId("1");

        Metadata metadata = new Metadata();
        metadata.setTriggerName(triggerNameCreated);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("attribute1", "value1");

        event.setMetadata(metadata);
        event.setPayload(payload);

        mockSendTriggerCreated();

        assertTrue(stackstormService.send(mapper.valueToTree(event)));
    }

    @Test
    public void sendTriggerNotCreatedTest() throws IOException {
        Event event = new Event();
        event.setId("2");

        Metadata metadata = new Metadata();
        metadata.setTriggerName(triggerNameNotCreated);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("attribute1", "value1");

        event.setMetadata(metadata);
        event.setPayload(payload);

        mockSendTriggerNotCreated();

        assertTrue(stackstormService.send(mapper.valueToTree(event)));
    }

    @Test
    public void createTriggerWithoutNameTest() throws IOException {
        Event event = new Event();
        event.setId("3");

        Metadata metadata = new Metadata();
        metadata.setTriggerName("");

        ObjectNode payload = mapper.createObjectNode();
        payload.put("attribute1", "value1");

        event.setMetadata(metadata);
        event.setPayload(payload);

        mockCreateTriggerWithoutName();

        assertFalse(stackstormService.send(mapper.valueToTree(event)));
    }

    @Test(expected = RuntimeException.class)
    public void SendToTrigger500Test() throws Exception {
        Event event = new Event();
        event.setId("1");

        Metadata metadata = new Metadata();
        metadata.setTriggerName(triggerNameCreated);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("attribute1", "value1");

        event.setMetadata(metadata);
        event.setPayload(payload);

        mockTriggerExist500();

        stackstormService.sender(mapper.valueToTree(event)).createTriggerIfNecessary();
    }

    @Test
    public void CreateTrigger500Test() throws Exception {
        Event event = new Event();
        event.setId("1");

        Metadata metadata = new Metadata();
        metadata.setTriggerName(triggerNameNotCreated);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("attribute1", "value1");

        event.setMetadata(metadata);
        event.setPayload(payload);

        mockCreateTrigger500();

        assertFalse(stackstormService.send(mapper.valueToTree(event)));

    }

    @Test
    public void SendTrigger500Test() throws Exception {
        Event event = new Event();
        event.setId("1");

        Metadata metadata = new Metadata();
        metadata.setTriggerName(triggerNameNotCreated);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("attribute1", "value1");

        event.setMetadata(metadata);
        event.setPayload(payload);

        mockSendTrigger500();

        assertFalse(stackstormService.send(mapper.valueToTree(event)));
    }

}
