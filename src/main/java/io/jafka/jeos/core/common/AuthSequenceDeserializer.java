package io.jafka.jeos.core.common;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Reads an {@code auth_sequence} entry from either of the two shapes nodeos has used:
 * {@code ["eosio", 42]} or {@code {"account":"eosio","sequence":42}}.
 */
public class AuthSequenceDeserializer extends JsonDeserializer<AuthSequence> {

    @Override
    public AuthSequence deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.currentToken();
        if (token == JsonToken.START_ARRAY) {
            List<?> values = p.readValueAs(List.class);
            if (values.size() < 2) {
                return null;
            }
            return new AuthSequence(String.valueOf(values.get(0)), asLong(values.get(1)));
        }
        JsonNode node = p.readValueAsTree();
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            return new AuthSequence(node.path(0).asText(), node.path(1).asLong());
        }
        String account = text(node, "account", "actor");
        Long sequence = node.hasNonNull("sequence") ? node.get("sequence").asLong() : null;
        return new AuthSequence(account, sequence);
    }

    private static String text(JsonNode node, String... names) {
        for (String name : names) {
            if (node.hasNonNull(name)) {
                return node.get(name).asText();
            }
        }
        for (Map.Entry<String, JsonNode> entry : node.properties()) {
            if (entry.getValue().isTextual()) {
                return entry.getValue().asText();
            }
        }
        return null;
    }

    private static Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String text = String.valueOf(value);
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}