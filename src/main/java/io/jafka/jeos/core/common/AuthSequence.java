package io.jafka.jeos.core.common;

import java.util.Objects;

/**
 * EOSIO {@code auth_sequence} entry.
 *
 * <p>nodeos &lt; 1.8 serialized an entry as a two element array ({@code ["eosio", 42]}), nodeos &gt;= 1.8
 * serializes it as an object ({@code {"account":"eosio","sequence":42}}). Both forms are accepted by
 * {@link AuthSequenceDeserializer} so {@code getActions} works against old and new nodes alike
 * (upstream issue #19).</p>
 */
public class AuthSequence {

    private String account;
    private Long sequence;

    public AuthSequence() {
    }

    public AuthSequence(String account, Long sequence) {
        this.account = account;
        this.sequence = sequence;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    @Override
    public String toString() {
        return account + ":" + sequence;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AuthSequence)) {
            return false;
        }
        AuthSequence that = (AuthSequence) other;
        return Objects.equals(account, that.account) && Objects.equals(sequence, that.sequence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(account, sequence);
    }
}