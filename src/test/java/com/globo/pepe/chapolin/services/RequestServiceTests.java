package com.globo.pepe.chapolin.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.globo.pepe.common.model.Event;
import com.globo.pepe.common.model.Metadata;
import com.globo.pepe.common.services.JsonLoggerService;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;

import static com.globo.pepe.chapolin.services.RequestService.X_PEPE_TRIGGER_HEADER;
import static com.globo.pepe.chapolin.suites.PepeSuiteTests.mockServer;
import static com.globo.pepe.common.util.Constants.PACK_NAME;
import static com.globo.pepe.common.util.Constants.TRIGGER_PREFIX;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
        "pepe.logging.tags=default",
        "pepe.chapolin.stackstorm.url=http://127.0.0.1:9101/api/v1",
        "pepe.chapolin.stackstorm.key=mykey"
})
@ContextConfiguration(classes = {StackstormService.class, RequestService.class, JsonLoggerService.class, ObjectMapper.class, JsonSchemaGeneratorService.class}, loader = AnnotationConfigContextLoader.class)
public class RequestServiceTests {

    private static final String triggerNameCreated = "triggerNameOK";
    private static final String triggerFullNameCreated = PACK_NAME + "." + TRIGGER_PREFIX + "." + triggerNameCreated;

    @Autowired
    public RequestService requestService;

    @Autowired
    public JsonSchemaGeneratorService jsonSchemaGeneratorService;

    ObjectMapper mapper = new ObjectMapper();

    public void mockSendTriggerDuplicated() throws IOException {
        mockServer.reset();

        InputStream triggerDuplicated = StackstormServiceTests.class.getResourceAsStream("/trigger-duplicate.json");
        String bodyTriggerDuplicated = IOUtils.toString(triggerDuplicated, Charset.defaultCharset());
        mockServer.when(request().withMethod("POST").withPath("/api/v1/triggertypes").withHeader(X_PEPE_TRIGGER_HEADER, triggerFullNameCreated))
                .respond(response().withBody(bodyTriggerDuplicated).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(409));
    }

    @Ignore
    @Test(expected = ExecutionException.class)
    public void checkConnectionFailedCheckIfTriggerExistsTest() throws Exception {

        Event event = new Event();
        event.setId("1");

        Metadata metadata = new Metadata();
        metadata.setTriggerName(triggerNameCreated);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("attribute1", "value1");

        event.setMetadata(metadata);
        event.setPayload(payload);

        requestService.checkIfTriggerExists(triggerNameCreated);
    }

    @Ignore
    @Test(expected = ExecutionException.class)
    public void checkConnectionFailedCreateTriggerTest() throws Exception {

        Event event = new Event();
        event.setId("1");

        Metadata metadata = new Metadata();
        metadata.setTriggerName(triggerNameCreated);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("attribute1", "value1");

        event.setMetadata(metadata);
        event.setPayload(payload);

        JsonNode schema = jsonSchemaGeneratorService.extract(mapper.valueToTree(event));
        requestService.createTrigger(schema);
    }


    @Test
    public void sendTriggerDuplicatedTest() throws Exception {
        Event event = new Event();
        event.setId("2");

        Metadata metadata = new Metadata();
        metadata.setTriggerName(triggerNameCreated);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("attribute1", "value1");

        event.setMetadata(metadata);
        event.setPayload(payload);

        mockSendTriggerDuplicated();

        JsonNode schema = jsonSchemaGeneratorService.extract(mapper.valueToTree(event));
        requestService.createTrigger(schema);
    }
}
