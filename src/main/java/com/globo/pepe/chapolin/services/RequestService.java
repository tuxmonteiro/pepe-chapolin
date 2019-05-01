package com.globo.pepe.chapolin.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globo.pepe.common.services.JsonLoggerService;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RequestService {

    private static final String ST2_API_KEY_HEADER  = "St2-Api-Key";

    @Value("${pepe.chapolin.stackstorm.url}")
    private String stackStormUrl;

    @Value("${pepe.chapolin.stackstorm.key}")
    private String stackStormKey;

    private AsyncHttpClient asyncHttpClient = Dsl.asyncHttpClient(Dsl.config()
        .setConnectionTtl(10000)
        .setPooledConnectionIdleTimeout(5000)
        .setMaxConnections(10)
        .build());

    private final ObjectMapper mapper;
    private final JsonLoggerService jsonLoggerService;

    public RequestService(ObjectMapper mapper, JsonLoggerService jsonLoggerService) {
        this.mapper = mapper;
        this.jsonLoggerService = jsonLoggerService;
    }

    public String getResponseBody() throws ExecutionException, InterruptedException {
        return asyncHttpClient.prepareGet(stackStormUrl).execute().get().getResponseBody();

    }

    public Boolean checkIfTriggerExists(String triggerName) throws ExecutionException, InterruptedException {
        String checkTriggerURL = stackStormUrl + "/api/triggertypes/" + triggerName;
        System.out.println(checkTriggerURL);
        String response = asyncHttpClient.prepareGet(checkTriggerURL)
                .addHeader(ST2_API_KEY_HEADER, stackStormKey)
                .execute()
                .get()
                .getResponseBody();
        JsonNode ref;
        try {
            return (ref = mapper.readTree(response).get("ref")) != null && triggerName.equals(ref.asText());
        } catch (IOException e) {
            jsonLoggerService.newLogger(getClass()).message(e.getMessage()).sendError();
            e.printStackTrace();
        }
        return false;
    }

    public boolean createTrigger(String triggerName, JsonNode schema) throws ExecutionException, InterruptedException {
        String createTriggerURL = stackStormUrl + "/v1/webhooks/st2";
        Response response = asyncHttpClient.preparePost(createTriggerURL)
                .addHeader(ST2_API_KEY_HEADER, stackStormKey)
                .setBody((schema).toString())
                .execute()
                .get();
        return response.getStatusCode() == 201;
    }
}


