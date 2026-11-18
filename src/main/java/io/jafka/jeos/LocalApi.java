package io.jafka.jeos;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jafka.jeos.convert.Packer;
import io.jafka.jeos.core.common.SignArg;
import io.jafka.jeos.core.common.transaction.PackedTransaction;
import io.jafka.jeos.core.common.transaction.TransactionAction;
import io.jafka.jeos.core.request.chain.transaction.PushTransactionRequest;
import io.jafka.jeos.impl.EosApiServiceGenerator;

/**
 * Local API with EOS without RPC: keys, packing and signing happen on the calling machine.
 *
 * <p>This is the supported path for nodeos builds without {@code wallet_api_plugin}
 * (upstream issue #15): build a {@link PushTransactionRequest} locally and push it through
 * {@link EosApi#pushTransaction(PushTransactionRequest)}.</p>
 *
 * @author adyliu (imxylz@gmail.com)
 * @since 2018年9月6日
 */
public interface LocalApi {

    /**
     * Creates a WIF private key from a fresh {@link java.security.SecureRandom} seed.
     */
    String createPrivateKey();

    /**
     * Creates a WIF private key from caller supplied entropy (stretched with SHA-256).
     */
    String createPrivateKey(byte[] entropy);

    String toPublicKey(String privateKey);

    default PushTransactionRequest transfer(SignArg arg, String privateKey, String from, String to, String quantity, String memo) {
        return transfer(arg, privateKey, "eosio.token", from, to, quantity, memo);
    }

    PushTransactionRequest transfer(SignArg arg, String privateKey, String account, String from, String to, String quantity, String memo);

    /**
     * Signs an arbitrary action list offline and returns the request to push.
     *
     * @param arg        chain state fetched by {@link EosApi#getSignArg(int)}
     * @param privateKey WIF private key of the authorizing account
     * @param actions    actions to execute, already packed with {@link Packer}
     */
    PushTransactionRequest createTransaction(SignArg arg, String privateKey, List<TransactionAction> actions);

    /**
     * Computes the transaction id (SHA-256 of the packed transaction) before it is broadcast, so
     * callers can track the transaction without waiting for the push response (upstream issue #12).
     */
    default String getTransactionId(PackedTransaction transaction) {
        return Packer.transactionId(transaction);
    }

    default ObjectMapper getObjectMapper() {
        return EosApiServiceGenerator.getMapper();
    }
}