package io.jafka.jeos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jafka.jeos.convert.Packer;
import io.jafka.jeos.core.common.transaction.PackedTransaction;
import io.jafka.jeos.core.common.transaction.TransactionAction;
import io.jafka.jeos.core.common.transaction.TransactionAuthorization;
import io.jafka.jeos.util.KeyUtil;

/**
 * Transaction packing, transaction id and offline signature (upstream issues #12 and #15).
 */
class TransactionPackingTest {

    private static final String CHAIN_ID = "aca376f206b8fc25a6ed44dbdc66547c36c6c33e3a119ffbeaef943642f0e906";

    private PackedTransaction transaction() {
        PackedTransaction transaction = new PackedTransaction();
        transaction.setExpiration(LocalDateTime.parse("2020-01-01T00:03:00"));
        transaction.setRefBlockNum(56141L);
        transaction.setRefBlockPrefix(3654011199L);
        transaction.setMaxNetUsageWords(0);
        transaction.setMaxCpuUsageMs(0);
        transaction.setDelaySec(0);
        List<TransactionAuthorization> authorization = Arrays
                .asList(new TransactionAuthorization("acca", "active"));
        transaction.setActions(Arrays.asList(new TransactionAction("eosio.token", "transfer", authorization,
                "0000000000601032000000000070103240420f000000000004535953000000000fe68891e698afe4b8ade59bbd")));
        return transaction;
    }

    @Test
    @DisplayName("the transaction id is the sha256 of the packed body and is stable")
    void transactionIdIsStable() {
        String id = Packer.transactionId(transaction());
        assertEquals(64, id.length());
        assertEquals(id, Packer.transactionId(transaction()));
        assertTrue(id.equals(id.toLowerCase()), "transaction ids are lowercase hex");
    }

    @Test
    @DisplayName("the signature digest covers the chain id and is not the transaction id")
    void digestDiffersFromId() {
        PackedTransaction transaction = transaction();
        String digest = Packer.signDigest(CHAIN_ID, transaction);
        assertEquals(64, digest.length());
        assertNotEquals(Packer.transactionId(transaction), digest);
    }

    @Test
    @DisplayName("an offline signature verifies against the signer public key")
    void offlineSignatureVerifies() {
        String privateKey = KeyUtil.createPrivateKey(new byte[] { 11, 22, 33 });
        String publicKey = KeyUtil.toPublicKey(privateKey);
        PackedTransaction transaction = transaction();

        String digest = Packer.signDigest(CHAIN_ID, transaction);
        String signature = KeyUtil.signDigest(privateKey, digest);

        assertTrue(KeyUtil.verifyDigest(publicKey, digest, signature));
        assertTrue(KeyUtil.verifyDigest(publicKey, Packer.signDigest(CHAIN_ID, transaction), signature));
    }
}