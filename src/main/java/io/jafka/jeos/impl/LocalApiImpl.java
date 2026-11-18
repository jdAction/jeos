package io.jafka.jeos.impl;

import io.jafka.jeos.LocalApi;
import io.jafka.jeos.convert.Packer;
import io.jafka.jeos.core.common.SignArg;
import io.jafka.jeos.core.common.transaction.PackedTransaction;
import io.jafka.jeos.core.common.transaction.TransactionAction;
import io.jafka.jeos.core.common.transaction.TransactionAuthorization;
import io.jafka.jeos.core.request.chain.json2bin.TransferArg;
import io.jafka.jeos.core.request.chain.transaction.PushTransactionRequest;
import io.jafka.jeos.util.KeyUtil;

import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author adyliu (imxylz@gmail.com)
 * @since 2018年9月6日
 */
public class LocalApiImpl implements LocalApi {

    @Override
    public String createPrivateKey() {
        return KeyUtil.createPrivateKey();
    }

    @Override
    public String createPrivateKey(byte[] entropy) {
        return KeyUtil.createPrivateKey(entropy);
    }

    @Override
    public String toPublicKey(String privateKey) {
        return KeyUtil.toPublicKey(privateKey);
    }

    @Override
    public PushTransactionRequest transfer(SignArg arg, String privateKey, String account, String from, String to, String quantity, String memo) {
        // ① pack transfer data
        TransferArg transferArg = new TransferArg(from, to, quantity, memo);
        String transferData = Packer.packTransfer(transferArg);

        // ② create the authorization
        List<TransactionAuthorization> authorizations = Arrays.asList(new TransactionAuthorization(from, "active"));

        // ③ build the action
        List<TransactionAction> actions = Arrays.asList(//
                new TransactionAction(account, "transfer", authorizations, transferData)//
        );
        return createTransaction(arg, privateKey, actions);
    }

    @Override
    public PushTransactionRequest createTransaction(SignArg arg, String privateKey, List<TransactionAction> actions) {
        PackedTransaction packedTransaction = new PackedTransaction();
        packedTransaction.setExpiration(arg.getHeadBlockTime().plusSeconds(arg.getExpiredSecond()));
        packedTransaction.setRefBlockNum(arg.getLastIrreversibleBlockNum());
        packedTransaction.setRefBlockPrefix(arg.getRefBlockPrefix());

        packedTransaction.setMaxNetUsageWords(0);
        packedTransaction.setMaxCpuUsageMs(0);
        packedTransaction.setDelaySec(0);
        packedTransaction.setActions(actions);

        String signature = sign(privateKey, arg, packedTransaction);
        PushTransactionRequest req = new PushTransactionRequest();
        req.setCompression("none");
        req.setTransaction(packedTransaction);
        req.setSignatures(Arrays.asList(signature));
        return req;
    }

    private String sign(String privateKey, SignArg arg, PackedTransaction t) {
        // digest = sha256(chain_id || packed transaction || 32 zero bytes)
        String digest = Packer.signDigest(arg.getChainId(), t);
        return KeyUtil.signDigest(privateKey, digest);
    }
}