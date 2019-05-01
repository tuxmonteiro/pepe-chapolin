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

import static com.globo.pepe.common.util.Constants.TRIGGER_PREFIX;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globo.pepe.common.model.Event;
import com.globo.pepe.common.services.JsonLoggerService;
import com.globo.pepe.common.services.JsonLoggerService.JsonLogger;
import java.util.concurrent.ExecutionException;
import org.springframework.stereotype.Service;

@Service
public class StackstormService {

    private final RequestService requestService;
    private final ObjectMapper mapper;
    private final JsonLoggerService loggerService;
    private final JsonSchemaGeneratorService jsonSchemaGeneratorService;

    public StackstormService(
        RequestService requestService,
        ObjectMapper mapper,
        JsonLoggerService loggerService,
        JsonSchemaGeneratorService jsonSchemaGeneratorService) {

        this.requestService = requestService;
        this.mapper = mapper;
        this.loggerService = loggerService;
        this.jsonSchemaGeneratorService = jsonSchemaGeneratorService;
    }

    public boolean send(JsonNode jsonNode) {
        try {
            return new Sender(jsonNode).send();
        } catch (JsonProcessingException e) {
            loggerService.newLogger(getClass()).message(e.getMessage()).sendError(e);
        }
        return false;
    }

    class Sender {

        private final Event event;
        private final String triggerName;
        private final JsonNode originalJson;

        private Sender(JsonNode jsonNode) throws JsonProcessingException {
            this.event = mapper.treeToValue(jsonNode, Event.class);
            this.triggerName = extractTriggerInfo();
            this.originalJson = jsonNode;
        }

        public boolean send() {
            try {
                createTriggerIfNecessary();
                sendToTrigger();
                return true;
            } catch (Exception e) {
                loggerService.newLogger(getClass()).message(e.getMessage()).sendError(e);
            }
            return false;
        }

        public void sendToTrigger() {
            final JsonLogger logger = loggerService.newLogger(getClass());
            try {
                logger.message("send event ID " + event.getId() +
                    " to trigger " + TRIGGER_PREFIX + "." + triggerName).sendInfo();
                if (logger.isDebugEnabled()) {
                    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(originalJson));
                }
            } catch (JsonProcessingException e) {
                logger.message(e.getMessage()).sendError();
            }
        }

        public String extractTriggerInfo() throws RuntimeException {
            return event.getMetadata().getTriggerName();
        }

        public void createTriggerIfNecessary() {
            final JsonLogger logger = loggerService.newLogger(getClass());
            try {
                Boolean triggerExists = requestService.checkIfTriggerExists(triggerName);
                if (!triggerExists) {
                    JsonNode schema = jsonSchemaGeneratorService.extract(originalJson);
                    requestService.createTrigger(triggerName, schema);
                    logger.message("trigger " + triggerName + " created. Using schema: " + schema.toString()).sendInfo();
                } else {
                    logger.message("trigger " + triggerName + " already exist.").sendInfo();
                }
            } catch (ExecutionException | InterruptedException e) {
                logger.message(e.getMessage()).sendError(e);
            }
        }


    }
}
