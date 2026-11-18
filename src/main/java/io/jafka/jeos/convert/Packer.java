package io.jafka.jeos.convert;

import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import io.jafka.jeos.core.common.transaction.PackedTransaction;
import io.jafka.jeos.core.common.transaction.TransactionAction;
import io.jafka.jeos.core.request.chain.json2bin.BuyRamArg;
import io.jafka.jeos.core.request.chain.json2bin.CreateAccountArg;
import io.jafka.jeos.core.request.chain.json2bin.DelegatebwArg;
import io.jafka.jeos.core.request.chain.json2bin.TransferArg;
import io.jafka.jeos.util.Raw;
import io.jafka.jeos.util.SHA;
import io.jafka.jeos.util.ecc.Hex;

/**
 * Binary EOSIO (de)serialization helpers.
 *
 * <p>The transaction packing here follows the {@code transaction} struct of EOSIO: expiration,
 * ref_block_num, ref_block_prefix, max_net_usage_words, max_cpu_usage_ms, delay_sec,
 * context_free_actions, actions and transaction_extensions. The transaction id is the SHA-256 of
 * those bytes, while the signature digest is the SHA-256 of
 * {@code chain_id || transaction || 32 zero bytes} (the context free data hash).</p>
 *
 * @author adyliu (imxylz@gmail.com)
 * @since 2018年9月6日
 */
public class Packer {

    /** Number of zero bytes appended to the transaction before hashing for the signature digest. */
    private static final int CONTEXT_FREE_DATA_HASH_SIZE = 32;

    public static String packTransfer(TransferArg arg) {
        Raw raw = new Raw();
        raw.packName(arg.getFrom());
        raw.packName(arg.getTo());
        raw.packAsset(arg.getQuantity());
        raw.pack(arg.getMemo());
        return raw.toHex();
    }

    public static String packBuyrambytes(BuyRamArg arg) {
        Raw raw = new Raw();
        raw.packName(arg.getPayer());
        raw.packName(arg.getReceiver());
        raw.pack(arg.getBytes());
        return raw.toHex();
    }

    public static String packDelegatebw(DelegatebwArg arg) {
        Raw raw = new Raw();
        raw.packName(arg.getFrom());
        raw.packName(arg.getReceiver());
        raw.packAsset(arg.getStakeNetQuantity());
        raw.packAsset(arg.getStakeCpuQuantity());
        raw.packVarint32(arg.getTransfer());
        return raw.toHex();
    }

    public static String packCreateAccount(CreateAccountArg arg) {
        Raw raw = new Raw();
        raw.packName(arg.getCreator());
        raw.packName(arg.getName());

        Arrays.asList(arg.getOwner(), arg.getActive()).stream().filter(x -> x != null).forEach(r -> {
            // owner
            raw.packUint32(r.getThreshold());
            // ownwer.keys
            raw.packVarint32(r.getKeys().size());
            r.getKeys().forEach(k -> {
                raw.packPublicKey(k.getKey());
                raw.packUint16(k.getWeight());
            });
            // ownwer.accounts
            raw.packVarint32(r.getAccounts().size());
            r.getAccounts().forEach(a -> {
                raw.packName(a.getPermission().getActor());
                raw.packName(a.getPermission().getPermission());
                raw.packUint16(a.getWeight());
            });
            // ownwer.waits
            raw.packVarint32(r.getWaits().size());
            r.getWaits().forEach(w -> {
                raw.packUint32(w.getWeightSec());
                raw.packUint16(w.getWeight());
            });
        });
        return raw.toHex();
    }

    /**
     * Packs the {@code transaction} body, i.e. everything the server hashes for the transaction id.
     *
     * <p>The old implementation dropped {@code transaction_extensions} and never serialized
     * context free actions, so locally produced signatures could not be verified by a node.</p>
     */
    public static Raw packTransaction(PackedTransaction t) {
        Raw raw = new Raw();
        // expiration
        raw.packUint32(t.getExpiration().toEpochSecond(ZoneOffset.ofHours(0)));
        // ref_block_num
        raw.packUint16(t.getRefBlockNum().intValue());
        // ref_block_prefix
        raw.packUint32(t.getRefBlockPrefix());
        // max_net_usage_words
        raw.packVarint32(t.getMaxNetUsageWords() == null ? 0 : t.getMaxNetUsageWords());
        // max_cpu_usage_ms
        raw.packUint8(t.getMaxCpuUsageMs() == null ? 0 : t.getMaxCpuUsageMs());
        // delay_sec
        raw.packVarint32(t.getDelaySec() == null ? 0 : t.getDelaySec());
        // context_free_actions
        packActions(raw, t.getContextFreeActions());
        // actions
        packActions(raw, t.getActions());
        // transaction_extensions
        packExtensions(raw, t.getTransactionExtensions());
        return raw;
    }

    private static void packActions(Raw raw, List<TransactionAction> actions) {
        if (actions == null) {
            raw.packVarint32(0);
            return;
        }
        raw.packVarint32(actions.size());
        actions.forEach(a -> {
            // action.account
            raw.packName(a.getAccount())//
                    .packName(a.getName())//
                    .packVarint32(a.getAuthorization().size());
            // action.authorization
            a.getAuthorization().forEach(au -> raw.packName(au.getActor())//
                    .packName(au.getPermission()));
            // action.data
            byte[] dat = Hex.toBytes(a.getData());
            raw.packVarint32(dat.length);
            raw.pack(dat);
        });
    }

    private static void packExtensions(Raw raw, List<String> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            raw.packVarint32(0);
            return;
        }
        raw.packVarint32(extensions.size());
        for (String extension : extensions) {
            byte[] dat = Hex.toBytes(extension);
            // extension type is a uint16, application specific; 0 keeps the byte stream valid
            raw.packUint16(0);
            raw.packVarint32(dat.length);
            raw.pack(dat);
        }
    }

    /**
     * Packs the chain id followed by the transaction body. Append
     * {@link #CONTEXT_FREE_DATA_HASH_SIZE} zero bytes to obtain the bytes that are hashed for the
     * signature digest.
     */
    public static Raw packPackedTransaction(String chainId, PackedTransaction t) {
        Raw raw = new Raw();
        raw.pack(Hex.toBytes(chainId));
        raw.pack(packTransaction(t).bytes());
        return raw;
    }

    /**
     * @return the hex encoded transaction id: the SHA-256 of the packed transaction body
     */
    public static String transactionId(PackedTransaction t) {
        return Hex.toHex(SHA.sha256(packTransaction(t).bytes()));
    }

    /**
     * @return the hex encoded digest that has to be signed for {@code chainId}
     */
    public static String signDigest(String chainId, PackedTransaction t) {
        byte[] body = packTransaction(t).bytes();
        byte[] payload = Raw.concat(Raw.concat(Hex.toBytes(chainId), body), new byte[CONTEXT_FREE_DATA_HASH_SIZE]);
        return Hex.toHex(SHA.sha256(payload));
    }
}