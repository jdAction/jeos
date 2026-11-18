package io.jafka.jeos.core.response.chain.transaction;

import lombok.Data;

/**
 * Response of {@code push_transaction}.
 *
 * <p>{@code processed.block_num} is the block the transaction was included in, i.e. the block
 * number to report after a transfer has been chained, and {@link #getBlockNum()} is a convenience
 * accessor for it.</p>
 */
@Data
public class PushedTransaction {

    private Processed processed;
    private String transactionId;

    /**
     * @return the number of the block that included this transaction, or {@code null} when the node
     *         did not report one
     */
    public Long getBlockNum() {
        return processed == null ? null : processed.getBlockNum();
    }

    /**
     * @return the time of the block that included this transaction, or {@code null}
     */
    public String getBlockTime() {
        return processed == null ? null : processed.getBlockTime();
    }
}