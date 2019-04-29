package com.globo.pepe.chapolin.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import java.util.concurrent.ExecutionException;

@Service
public class StackstormService {

    private String  triggerPath = "/api/triggertypes/";
    private String  triggerName = "pepe.event2";
    private String  triggerCreatePath = "/v1/webhooks/st2";

    private final RequestService requestService;

    public StackstormService(RequestService requestService) {
        this.requestService = requestService;
    }

    public void send(JsonNode jsonNode) {
        // WIP
    }

    public void createTrigger() throws ExecutionException, InterruptedException {
        Boolean triggerExists =  requestService.checkIfTriggerExists(triggerPath, triggerName);
        if (!triggerExists){
            requestService.createTrigger(triggerCreatePath, triggerName);
        }
        requestService.closeAsyncHttpClient();
    }
}
