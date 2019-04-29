package com.globo.pepe.chapolin.services;

import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
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

    public void send() throws ExecutionException, InterruptedException {
        System.out.println(requestService.getResponseBody());
        requestService.closeAsyncHttpClient();
    }

    @PostConstruct
    public void createTrigger() throws ExecutionException, InterruptedException {
        Boolean triggerExists =  requestService.checkIfTriggerExists(triggerPath, triggerName);
        if (!triggerExists){
            requestService.createTrigger(triggerCreatePath, triggerName);
        }
        requestService.closeAsyncHttpClient();
    }
}
