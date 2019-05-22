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

import static com.globo.pepe.common.util.Constants.PACK_NAME;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.util.Base64;
import javax.net.ssl.SSLException;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class RequestService {

    public static final String X_PEPE_TRIGGER_HEADER = "X-Pepe-Trigger";

    private static final String ST2_API_KEY_HEADER  = "St2-Api-Key";

    private static final String ST2_TOKEN_HEADER  = "X-Auth-Token";

    @Value("${pepe.chapolin.stackstorm.api}")
    private String stackStormApiUrl;

    @Value("${pepe.chapolin.stackstorm.auth}")
    private String stackStormAuthUrl;

    @Value("${pepe.chapolin.stackstorm.login}")
    private String stackstormLogin;

    @Value("${pepe.chapolin.stackstorm.password}")
    private String stackstormPassword;

    private String stackstormKey = null;

    private final AsyncHttpClient asyncHttpClient = Dsl.asyncHttpClient(Dsl.config()
        .setConnectionTtl(10000)
        .setPooledConnectionIdleTimeout(5000)
        .setMaxConnections(10)
        .setSslContext(SslContextBuilder.forClient().sslProvider(SslProvider.JDK).trustManager(InsecureTrustManagerFactory.INSTANCE).build())
        .build());

    private final ObjectMapper mapper;

    public RequestService(ObjectMapper mapper) throws SSLException {
        this.mapper = mapper;
    }

    private Response get(String checkTriggerURL) throws Exception {
        Exception lastException;
        try {
            return asyncHttpClient.prepareGet(checkTriggerURL)
                .addHeader(ST2_API_KEY_HEADER, getStackstormKey())
                .execute()
                .get();
        } catch (Exception e) {
            stackstormKey = null;
            lastException = e;
        }
        throw lastException;
    }

    private String getStackstormKey() throws Exception {
        if (stackstormKey == null) {
            String credentials = Base64.getEncoder().encodeToString((stackstormLogin + ":" + stackstormPassword).getBytes());
            Response responseToken = asyncHttpClient.preparePost(stackStormAuthUrl + "/token")
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .addHeader(AUTHORIZATION, "Basic " + credentials)
                .setBody("{}")
                .execute()
                .get();
            String token = null;
            if (responseToken.getStatusCode() == HttpStatus.CREATED.value()) {
                String bodyTokenStr = responseToken.getResponseBody();
                JsonNode bodyTokenJson = mapper.convertValue(bodyTokenStr, JsonNode.class);
                JsonNode tokenJson = bodyTokenJson.get("token");
                token = tokenJson != null ? tokenJson.asText() : null;
            }
            if (token != null) {
                Response responseApikey = asyncHttpClient.preparePost(stackStormApiUrl + "/apikey")
                    .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .addHeader(ST2_TOKEN_HEADER, token)
                    .setBody("{}")
                    .execute()
                    .get();
                String apikey = null;
                if (responseApikey.getStatusCode() == HttpStatus.CREATED.value()) {
                    String bodyApikeyStr = responseToken.getResponseBody();
                    JsonNode bodyApikeyJson = mapper.convertValue(bodyApikeyStr, JsonNode.class);
                    JsonNode apikeyJson = bodyApikeyJson.get("key");
                    apikey = apikeyJson != null ? apikeyJson.asText() : null;
                }
                if (apikey != null) {
                    stackstormKey = apikey;
                } else {
                    throw new RuntimeException("Stackstorm authentication problem");
                }
            }
        }
        return stackstormKey;
    }

    private Response post(String createTriggerURL, String requestBody, String triggerName) throws Exception {
        Assert.notNull(triggerName, "Trigger name is NULL");
        Assert.notNull(triggerName, "Request body is NULL");

        Exception lastException;
        try {
            return asyncHttpClient.preparePost(createTriggerURL)
                .addHeader(ST2_API_KEY_HEADER, getStackstormKey())
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .addHeader(X_PEPE_TRIGGER_HEADER, triggerName)
                .setBody(requestBody)
                .execute()
                .get();
        } catch (Exception e) {
            lastException = e;
            stackstormKey = null;
        }
        throw lastException;
    }

    public Boolean checkIfTriggerExists(String triggerName) throws Exception {
        Assert.notNull(triggerName, "Trigger name is NULL");

        String checkTriggerURL = stackStormApiUrl + "/triggertypes/" + triggerName;
        Response response = get(checkTriggerURL);
        if (response.getStatusCode() >= 500) {
            stackstormKey = null;
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
        stackstormKey = null;
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
        stackstormKey = null;
        throw new RuntimeException("ST2 Server Error: " + response.getResponseBody());
    }

}


