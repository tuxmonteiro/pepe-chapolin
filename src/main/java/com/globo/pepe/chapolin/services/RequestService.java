package com.globo.pepe.chapolin.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.globo.pepe.common.services.JsonLoggerService;
import org.asynchttpclient.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.asynchttpclient.util.HttpConstants;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Component
public class RequestService {

    private AsyncHttpClient asyncHttpClient = Dsl.asyncHttpClient();
    private String responseBody;
    private String url = "http://admin.pepe.dev.globoi.com";

    private final ObjectMapper mapper;
    private final JsonLoggerService jsonLoggerService;

    public RequestService(ObjectMapper mapper, JsonLoggerService jsonLoggerService) {
        this.mapper = mapper;
        this.jsonLoggerService = jsonLoggerService;
    }

    public void closeAsyncHttpClient(){
        try {
            this.asyncHttpClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getResponseBody() throws ExecutionException, InterruptedException {
        return asyncHttpClient.prepareGet("http://admin.pepe.dev.globoi.com").execute().get().getResponseBody();

    }

    public Boolean checkIfTriggerExists(String triggerPath, String triggerName) throws ExecutionException, InterruptedException {
        String checkTriggerURL = String.format("%s%s%s", url,triggerPath,triggerName);
        System.out.println(checkTriggerURL);
        String response = asyncHttpClient.prepareGet(checkTriggerURL)
                .addHeader("St2-Api-Key", "ZjAxM2JlNzk0MjJlMmQyYTE2ZjY2ZDBlNjZjODJiODk2YTk5MzEwODdkZDIzMjUzMDQ1ZTZhZWM4OTZlZjAyMQ")
                .execute()
                .get()
                .getResponseBody();
        JsonNode ref;
        try {
            return (ref = mapper.readTree(response).get("ref")) != null && triggerName.equals(ref.asText());
        } catch (IOException e) {
            jsonLoggerService.newLogger(getClass()).put("short_message", e.getMessage()).sendError();
            e.printStackTrace();
        }
        return false;
    }

    public boolean createTrigger(String triggerCreatePath, String triggerName) throws ExecutionException, InterruptedException {
        String createTriggerURL = String.format("%s%s", url,triggerCreatePath);
        System.out.println(createTriggerURL);
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("trigger", triggerName).set("payload", mapper.createObjectNode().put("attribute1", "value1"));
        Response response = asyncHttpClient.preparePost(createTriggerURL)
                .addHeader("St2-Api-Key", "ZjAxM2JlNzk0MjJlMmQyYTE2ZjY2ZDBlNjZjODJiODk2YTk5MzEwODdkZDIzMjUzMDQ1ZTZhZWM4OTZlZjAyMQ")
                .setBody(((JsonNode)objectNode).toString())
                .execute()
                .get();
        return response.getStatusCode() == 201;
    }
}


