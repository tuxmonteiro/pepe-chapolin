/*
 * Copyright (c) 2019 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.globo.pepe.chapolin.services;

import static com.globo.pepe.chapolin.services.StackstormAuthService.ST2_API_KEY_HEADER;
import static com.globo.pepe.common.util.Constants.PACK_NAME;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.globo.pepe.chapolin.configuration.HttpClientConfiguration.HttpClient;
import java.util.Collections;
import java.util.Map;
import org.asynchttpclient.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class RequestService {

    static final String X_PEPE_TRIGGER_HEADER = "X-Pepe-Trigger";

    @Value("${pepe.chapolin.stackstorm.api}")
    private String stackStormApiUrl;

    private final ObjectMapper mapper;
    private final StackstormAuthService authService;
    private final HttpClient httpClient;

    public RequestService(ObjectMapper mapper, StackstormAuthService authService, HttpClient httpClient) {
        this.mapper = mapper;
        this.authService = authService;
        this.httpClient = httpClient;
    }

    private Response get(String checkTriggerURL) throws Exception {
        Map<CharSequence, Iterable<String>> headers = Map.of(
            ST2_API_KEY_HEADER, Collections.singleton(authService.getStackstormKey())
        );
        return httpClient.get(checkTriggerURL, headers);
    }

    private Response post(String createTriggerURL, String requestBody, String triggerName) throws Exception {
        Assert.notNull(triggerName, "Trigger name is NULL");
        Assert.notNull(triggerName, "Request body is NULL");

        Map<CharSequence, Iterable<String>> headers = Map.of(
            ST2_API_KEY_HEADER, Collections.singleton(authService.getStackstormKey()),
            CONTENT_TYPE, Collections.singleton(APPLICATION_JSON_VALUE),
            X_PEPE_TRIGGER_HEADER, Collections.singleton(triggerName)
        );
        return httpClient.post(createTriggerURL, requestBody, headers);
    }

    public Boolean checkIfTriggerExists(String triggerName) throws Exception {
        Assert.notNull(triggerName, "Trigger name is NULL");

        String checkTriggerURL = stackStormApiUrl + "/triggertypes/" + triggerName;
        Response response = get(checkTriggerURL);
        if (response.getStatusCode() >= 500) {
            throw new RuntimeException("ST2 Server Error " + response.getStatusCode() + " : " + response.getResponseBody());
        }
        JsonNode ref;
        return response.getStatusCode() == 200 &&
                (ref = mapper.readTree(response.getResponseBody()).get("ref")) != null &&
                triggerName.equals(ref.asText());
    }

    public boolean createTrigger(JsonNode schema) throws Exception {
        String triggerName = PACK_NAME + "." + schema.get("name").asText();
        String createTriggerURL = stackStormApiUrl + "/triggertypes";
        Response response = post(createTriggerURL, schema.toString(), triggerName);
        if (response.getStatusCode() == 201) {
            return true;
        } else if (response.getStatusCode() == 409) {
            return false;
        }
        throw new RuntimeException("ST2 Server Error " + response.getStatusCode() + " : " + response.getResponseBody());
    }

    public boolean sendToTrigger(String triggerName, JsonNode payload) throws Exception {
        Assert.notNull(triggerName, "Trigger name is NULL");
        Assert.notNull(payload, "Payload is NULL");

        String sendToTriggerURL = stackStormApiUrl + "/webhooks/st2";
        final ObjectNode requestBody = mapper.createObjectNode();
        requestBody.put("trigger", triggerName).set("payload", payload);
        Response response = post(sendToTriggerURL, requestBody.toString(), triggerName);
        if (response.getStatusCode() == 202) {
            return true;
        }
        throw new RuntimeException("ST2 Server Error: " + response.getResponseBody());
    }

}


