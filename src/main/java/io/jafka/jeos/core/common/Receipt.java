package io.jafka.jeos.core.common;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Data;

/**
 * Action receipt attached to every {@link ActionTrace}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Receipt {

    private Long abiSequence;

    private String actDigest;

    /**
     * nodeos returns either {@code [["eosio", 42]]} (before 1.8) or
     * {@code [{"account":"eosio","sequence":42}]} (since 1.8); both are accepted here so
     * {@code getActions} no longer fails with a START_OBJECT deserialization error (issue #19).
     */
    @JsonDeserialize(contentUsing = AuthSequenceDeserializer.class)
    private List<AuthSequence> authSequence = null;

    private Long codeSequence;

    private Long globalSequence;

    private String receiver;

    private Long recvSequence;
}