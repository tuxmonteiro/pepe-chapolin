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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;

import static com.globo.pepe.common.util.Constants.PACK_NAME;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
public class RequestService {

    public static final String X_PEPE_TRIGGER_HEADER = "X-Pepe-Trigger";

    private static final String ST2_API_KEY_HEADER  = "St2-Api-Key";

    @Value("${pepe.chapolin.stackstorm.url}")
    private String stackStormUrl;

    @Value("${pepe.chapolin.stackstorm.key}")
    private String stackstormKey;

    private AsyncHttpClient asyncHttpClient = Dsl.asyncHttpClient(Dsl.config()
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
        return asyncHttpClient.prepareGet(checkTriggerURL)
                .addHeader(ST2_API_KEY_HEADER, stackstormKey)
                .execute()
                .get();
    }

    private Response post(String createTriggerURL, String requestBody, String triggerName) throws Exception {
        return asyncHttpClient.preparePost(createTriggerURL)
                .addHeader(ST2_API_KEY_HEADER, stackstormKey)
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .addHeader(X_PEPE_TRIGGER_HEADER, triggerName)
                .setBody(requestBody)
                .execute()
                .get();
    }

    public Boolean checkIfTriggerExists(String triggerName) throws Exception {
        String checkTriggerURL = stackStormUrl + "/triggertypes/" + triggerName;
        Response response = get(checkTriggerURL);
        JsonNode ref;
        return response.getStatusCode() == 200 &&
                (ref = mapper.readTree(response.getResponseBody()).get("ref")) != null &&
                triggerName.equals(ref.asText());
    }

    public boolean createTrigger(JsonNode schema) throws Exception {
        String createTriggerURL = stackStormUrl + "/triggertypes";
        String triggerName = PACK_NAME + "." + schema.get("name").asText();
        Response response = post(createTriggerURL, schema.toString(), triggerName);
        return response.getStatusCode() == 201;
    }

    public boolean sendToTrigger(String triggerName, JsonNode payload) throws Exception {
        String sendToTriggerURL = stackStormUrl + "/webhooks/st2";
        final ObjectNode requestBody = mapper.createObjectNode();
        requestBody.put("trigger", triggerName).set("payload", payload);
        Response response = post(sendToTriggerURL, requestBody.toString(), triggerName);
        return response.getStatusCode() == 202;
    }

}


