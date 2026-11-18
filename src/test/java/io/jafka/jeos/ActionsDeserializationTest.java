package io.jafka.jeos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jafka.jeos.core.response.history.action.Actions;
import io.jafka.jeos.impl.EosApiServiceGenerator;

/**
 * Deserialization of {@code getActions} (upstream issue #19): nodeos changed {@code auth_sequence}
 * from {@code [["account", seq]]} to {@code [{"account": ..., "sequence": ...}]} and the client used
 * to fail with "Cannot deserialize instance of java.util.ArrayList out of START_OBJECT token".
 */
class ActionsDeserializationTest {

    private final ObjectMapper mapper = EosApiServiceGenerator.getMapper();

    @Test
    @DisplayName("auth_sequence as objects (nodeos 1.8+) is accepted")
    void authSequenceAsObjects() throws Exception {
        String json = "{\"actions\":[{\"account_action_seq\":12,\"block_num\":9876,"
                + "\"block_time\":\"2020-01-01T00:00:00.000\",\"global_action_seq\":34,"
                + "\"action_trace\":{\"trx_id\":\"aa\",\"receiver\":\"eosio.token\","
                + "\"receipt\":{\"receiver\":\"eosio.token\",\"act_digest\":\"bb\","
                + "\"auth_sequence\":[{\"account\":\"eosio\",\"sequence\":1024}],"
                + "\"global_sequence\":2048,\"recv_sequence\":3,\"code_sequence\":1,\"abi_sequence\":1},"
                + "\"inline_traces\":[]}}],\"last_irreversible_block\":9000}";

        Actions actions = mapper.readValue(json, Actions.class);
        assertNotNull(actions.getActions());
        assertEquals(1, actions.getActions().size());
        assertEquals(9876L, actions.getActions().get(0).getBlockNum());
        assertEquals("eosio", actions.getActions().get(0).getActionTrace().getReceipt()
                .getAuthSequence().get(0).getAccount());
        assertEquals(1024L, actions.getActions().get(0).getActionTrace().getReceipt()
                .getAuthSequence().get(0).getSequence());
        assertEquals(9000L, actions.getLastIrreversibleBlock());
    }

    @Test
    @DisplayName("auth_sequence as arrays (nodeos < 1.8) keeps working")
    void authSequenceAsArrays() throws Exception {
        String json = "{\"actions\":[{\"account_action_seq\":1,\"action_trace\":{\"receipt\":"
                + "{\"auth_sequence\":[[\"eosio\",77]]},\"inline_traces\":[]}}],\"last_irreversible_block\":1}";

        Actions actions = mapper.readValue(json, Actions.class);
        assertEquals("eosio", actions.getActions().get(0).getActionTrace().getReceipt()
                .getAuthSequence().get(0).getAccount());
        assertEquals(77L, actions.getActions().get(0).getActionTrace().getReceipt()
                .getAuthSequence().get(0).getSequence());
    }

    @Test
    @DisplayName("EOSIO 2.1 action return values are read")
    void actionReturnValue() throws Exception {
        String json = "{\"actions\":[{\"action_trace\":{\"receipt\":{\"auth_sequence\":[]},"
                + "\"return_value_hex_data\":\"0a\",\"return_value_data\":\"ok\",\"inline_traces\":[]}}]}";

        Actions actions = mapper.readValue(json, Actions.class);
        assertEquals("0a", actions.getActions().get(0).getActionTrace().getReturnValueHexData());
        assertEquals("ok", actions.getActions().get(0).getActionTrace().getReturnValueData());
    }

    @Test
    @DisplayName("pushed transaction exposes the block number it was included in")
    void pushedTransactionBlockNum() throws Exception {
        String json = "{\"transaction_id\":\"cc\",\"processed\":{\"id\":\"cc\",\"block_num\":4242,"
                + "\"block_time\":\"2020-01-01T00:00:00.000\"}}";

        io.jafka.jeos.core.response.chain.transaction.PushedTransaction pushed =
                mapper.readValue(json, io.jafka.jeos.core.response.chain.transaction.PushedTransaction.class);
        assertEquals(4242L, pushed.getBlockNum());
        assertEquals("2020-01-01T00:00:00.000", pushed.getBlockTime());
    }
}