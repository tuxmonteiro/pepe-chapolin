package com.globo.pepe.chapolin.services;

import static com.globo.pepe.common.util.Constants.TRIGGER_PREFIX;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.globo.pepe.common.services.JsonLoggerService;
import java.io.IOException;
import java.util.Iterator;
import org.springframework.stereotype.Service;

@Service
public class JsonSchemaGeneratorService {

    private final ObjectMapper mapper;
    private final JsonLoggerService loggerService;

    public JsonSchemaGeneratorService(ObjectMapper mapper, JsonLoggerService loggerService) {
        this.mapper = mapper;
        this.loggerService = loggerService;
    }

    public JsonNode extract(JsonNode jsonNode) {
        return new Generator(jsonNode).extractSchema();
    }

    private class Generator {

        private final JsonNode jsonNode;
        private final String triggerName;

        public Generator(JsonNode jsonNode) {
            this.jsonNode = jsonNode;
            final JsonNode triggerNameJson = jsonNode.get("metadata").get("trigger_name");
            this.triggerName = triggerNameJson.asText();
        }

        public JsonNode extractSchema() {
            ObjectNode schema = mapper.createObjectNode().put("name", triggerName)
                .put("description", triggerName)
                .put("pack", "pepe.trigger")
                .put("uid", "trigger_type:" + TRIGGER_PREFIX + "." + triggerName);

            ObjectNode payload_schema = mapper.createObjectNode();
            ObjectNode properties = mapper.createObjectNode();

            ObjectNode id = mapper.createObjectNode();
            id.put("default", "UNDEF");
            id.put("type", "string");

            ObjectNode metadata = mapper.createObjectNode();
            ObjectNode metadataProperties = mapper.createObjectNode();
            metadata.set("properties", metadataProperties);
            metadata.put("type", "object");

            ObjectNode source = mapper.createObjectNode();
            source.put("type", "string");
            metadataProperties.set("source", source);

            ObjectNode project = mapper.createObjectNode();
            project.put("type", "string");
            metadataProperties.set("project", project);

            ObjectNode timestamp = mapper.createObjectNode();
            timestamp.put("type", "integer");
            metadataProperties.set("timestamp", timestamp);

            ObjectNode payloadProperties;
            try {
                String payloadPropertiesJsonSchema = outputAsString(jsonNode.get("payload"));
                payloadProperties = (ObjectNode) mapper.readTree(payloadPropertiesJsonSchema);
            } catch (IOException e) {
                loggerService.newLogger(getClass()).message(e.getMessage()).sendError(e);
                payloadProperties = mapper.createObjectNode();
            }

            ObjectNode payload = mapper.createObjectNode();
            payload.set("properties", payloadProperties);
            payload.put("type", "object");

            properties.set("id", id);
            properties.set("metadata", metadata);
            properties.set("payload", payload);

            payload_schema.set("properties", properties);
            payload_schema.put("type", "object");

            schema.set("parameters_schema",  payload_schema);
            schema.set("tags", mapper.createArrayNode());

            return schema;
        }

        private String outputAsString(JsonNode jsonNode) throws IOException {
            StringBuilder output = new StringBuilder();
            output.append("{");
            for (Iterator<String> iterator = jsonNode.fieldNames(); iterator.hasNext();) {
                String fieldName = iterator.next();
                JsonNodeType nodeType = jsonNode.get(fieldName).getNodeType();
                output.append(convertNodeToStringSchemaNode(jsonNode, nodeType, fieldName));
            }
            output.append("}");

            return cleanup(output.toString());
        }

        private String convertNodeToStringSchemaNode(JsonNode jsonNode, JsonNodeType nodeType, String key) throws IOException {
            StringBuilder result = new StringBuilder("\"" + key + "\": { \"type\": \"");
            JsonNode node;
            switch (nodeType) {
                case ARRAY :
                    node = jsonNode.get(key).get(0);
                    result.append("array\", \"items\": { \"properties\":");
                    result.append(outputAsString(node));
                    result.append("}},");
                    break;
                case BOOLEAN:
                    result.append("boolean\" },");
                    break;
                case NUMBER:
                    result.append("integer\" },");
                    break;
                case OBJECT:
                    node = jsonNode.get(key);
                    result.append("object\", \"properties\": ");
                    result.append(outputAsString(node));
                    result.append("},");
                    break;
                case STRING:
                    result.append("string\" },");
                    break;
            }

            return result.toString();
        }

        private String cleanup(String dirty) {
            return dirty.replaceAll(",}","}");
        }
    }
}
