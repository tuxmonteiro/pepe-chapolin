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

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globo.pepe.chapolin.configuration.HttpClientConfiguration.HttpClient;
import com.globo.pepe.common.services.JsonLoggerService;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import org.asynchttpclient.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class StackstormAuthService {

    private static final String ST2_TOKEN_HEADER   = "X-Auth-Token";
    static final         String ST2_API_KEY_HEADER = "St2-Api-Key";

    @Value("${pepe.chapolin.stackstorm.api}")
    private String stackStormApiUrl;

    @Value("${pepe.chapolin.stackstorm.auth}")
    private String stackStormAuthUrl;

    @Value("${pepe.chapolin.stackstorm.login}")
    private String stackstormLogin;

    @Value("${pepe.chapolin.stackstorm.password}")
    private String stackstormPassword;

    private String stackstormKey = null;

    private final ObjectMapper mapper;
    private final HttpClient httpClient;
    private final JsonLoggerService loggerService;

    public StackstormAuthService(ObjectMapper mapper, HttpClient httpClient, JsonLoggerService loggerService) {
        this.mapper = mapper;
        this.httpClient = httpClient;
        this.loggerService = loggerService;
    }

    public String getStackstormKey() throws Exception {
        resetIfNecessaryStackstormKey();
        Assert.hasLength(stackstormKey, "Stackstorm authentication problem");
        return stackstormKey;
    }

    private void resetIfNecessaryStackstormKey() throws Exception {
        if (stackstormKey == null) {
            loggerService.newLogger(getClass()).message("Resetting stackstorm apikey").sendWarn();
            String credentials = getBasicCredentialsB64();
            final Map<CharSequence, Iterable<String>> tokenHeaders = Map.of(
                CONTENT_TYPE, Collections.singleton(APPLICATION_JSON_VALUE),
                AUTHORIZATION, Collections.singleton("Basic " + credentials)
            );
            Response responseToken = httpClient.post(stackStormAuthUrl + "/tokens", "{}", tokenHeaders);
            String token = null;
            if (responseToken.getStatusCode() == HttpStatus.CREATED.value()) {
                String bodyTokenStr = responseToken.getResponseBody();
                JsonNode bodyTokenJson = mapper.readTree(bodyTokenStr);
                JsonNode tokenJson = bodyTokenJson.get("token");
                token = tokenJson != null ? tokenJson.asText() : null;
            }
            if (token != null) {
                final Map<CharSequence, Iterable<String>> apikeyHeaders = Map.of(
                    CONTENT_TYPE, Collections.singleton(APPLICATION_JSON_VALUE),
                    ST2_TOKEN_HEADER, Collections.singleton(token)
                );
                Response responseApikey = httpClient.post(stackStormApiUrl + "/apikeys", "{}", apikeyHeaders);
                String apikey = null;
                if (responseApikey.getStatusCode() == HttpStatus.CREATED.value()) {
                    String bodyApikeyStr = responseApikey.getResponseBody();
                    JsonNode bodyApikeyJson = mapper.readTree(bodyApikeyStr);
                    JsonNode apikeyJson = bodyApikeyJson.get("key");
                    apikey = apikeyJson != null ? apikeyJson.asText() : null;
                }
                if (apikey != null) {
                    stackstormKey = apikey;
                    loggerService.newLogger(getClass()).message("Using new stackstorm apikey").sendWarn();
                } else {
                    throw new RuntimeException("Stackstorm authentication problem");
                }
            }
        }
    }

    private String getBasicCredentialsB64() {
        return Base64.getEncoder().encodeToString((stackstormLogin + ":" + stackstormPassword).getBytes());
    }

}
