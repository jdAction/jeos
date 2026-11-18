package io.jafka.jeos.core.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Reads {@code inline_traces} whether the node reports an array (all known nodeos versions) or a
 * single object, instead of failing with a START_OBJECT / START_ARRAY mismatch.
 */
public class InlineTracesDeserializer extends JsonDeserializer<List<Object>> {

    @Override
    public List<Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        List<Object> traces = new ArrayList<>();
        JsonToken token = p.currentToken();
        if (token == JsonToken.START_ARRAY) {
            while (p.nextToken() != JsonToken.END_ARRAY) {
                traces.add(p.readValueAs(Object.class));
            }
            return traces;
        }
        if (token == JsonToken.VALUE_NULL) {
            return traces;
        }
        Object single = p.readValueAs(Object.class);
        if (single != null) {
            traces.add(single);
        }
        return traces;
    }
}