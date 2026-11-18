package io.jafka.jeos.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;

import io.jafka.jeos.util.ecc.Ecdsa;
import io.jafka.jeos.util.ecc.Hex;
import io.jafka.jeos.util.ecc.Point;
import io.jafka.jeos.util.ecc.Ripemd160;
import io.jafka.jeos.util.ecc.Secp256k;

/**
 * EOS key utilities: wallet-import-format (WIF) private keys, public keys in the
 * {@code EOS} format and {@code SIG_K1_} signatures.
 *
 * <p>The private key material is always generated from a {@link SecureRandom} instance or
 * derived from caller supplied entropy by hashing it with SHA-256, so a key is always
 * exactly 32 bytes of key space. See issue #7 of the upstream project: deriving key material
 * from {@link java.util.UUID} was both low entropy and partially predictable.</p>
 *
 * @author adyliu (imxylz@gmail.com)
 * @since 2018年9月5日
 */
public class KeyUtil {

    public static final String address_prefix = "EOS";

    public static final String SIGNATURE_PREFIX = "SIG_K1_";

    public static final Secp256k secp = new Secp256k();

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Creates a private key from a fresh {@link SecureRandom} seed.
     */
    public static String createPrivateKey() {
        byte[] entropy = new byte[32];
        SECURE_RANDOM.nextBytes(entropy);
        return createPrivateKey(entropy);
    }

    /**
     * Creates a private key from caller supplied entropy; the entropy is stretched to the
     * 32 bytes of the secp256k1 group order by SHA-256, so arbitrary seeds are accepted.
     */
    public static String createPrivateKey(byte[] entropy) {
        Objects.requireNonNull(entropy, "entropy");
        return encodePrivateKey(SHA.sha256(entropy));
    }

    /**
     * Creates a private key from a seed string.
     */
    public static String createPrivateKey(String seed) {
        Objects.requireNonNull(seed, "seed");
        return createPrivateKey(seed.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Encodes 32 bytes of private key material into WIF (base58check with version {@code 0x80}).
     */
    public static String encodePrivateKey(byte[] keyMaterial) {
        Objects.requireNonNull(keyMaterial, "keyMaterial");
        byte[] key = normalize(keyMaterial);
        byte[] payload = Raw.concat(new byte[] { (byte) 0x80 }, key);
        byte[] checksum = SHA.sha256(SHA.sha256(payload));
        return Base58.encode(Raw.concat(payload, Raw.copy(checksum, 0, 4)));
    }

    /**
     * Decodes a WIF private key, verifying its checksum, and returns the private scalar.
     *
     * @throws IllegalArgumentException if the key is not a valid WIF string
     */
    public static BigInteger toPrivateKey(String pk) {
        Objects.requireNonNull(pk, "private key");
        byte[] decoded = Base58.decode(pk.trim());
        if (decoded.length < 5) {
            throw new IllegalArgumentException("Invalid WIF private key: too short");
        }
        byte[] payload = Raw.copy(decoded, 0, decoded.length - 4);
        byte[] checksum = Raw.copy(decoded, decoded.length - 4, 4);
        byte[] expected = Raw.copy(SHA.sha256(SHA.sha256(payload)), 0, 4);
        if (!Arrays.equals(checksum, expected)) {
            throw new IllegalArgumentException("Invalid WIF private key: checksum mismatch");
        }
        byte version = payload[0];
        if (version != (byte) 0x80) {
            throw new IllegalArgumentException(
                    "Expected WIF version 128 (0x80) but got " + (version & 0xff));
        }
        int offset = (payload.length == 34 && payload[1] == 0) ? 2 : 1;
        byte[] key = Raw.copy(payload, offset, payload.length - offset);
        if (key.length != 32) {
            throw new IllegalArgumentException("Invalid WIF private key: expected 32 key bytes but got " + key.length);
        }
        BigInteger d = new BigInteger(1, key);
        if (d.signum() == 0 || d.compareTo(secp.n()) >= 0) {
            throw new IllegalArgumentException("Invalid WIF private key: scalar out of range");
        }
        return d;
    }

    private static BigInteger privateKey(String pk) {
        return toPrivateKey(pk);
    }

    /**
     * Derives the public key ({@code EOS...}) of a WIF private key.
     */
    public static String toPublicKey(String privateKey) {
        Objects.requireNonNull(privateKey, "privateKey");
        BigInteger d = privateKey(privateKey);
        Point ep = secp.G().multiply(d);
        return encodePublicKey(ep);
    }

    /**
     * Encodes a curve point into the {@code EOS...} public key format.
     */
    public static String encodePublicKey(Point point) {
        byte[] pub = point.getEncoded();
        byte[] csum = Raw.copy(Ripemd160.from(pub).bytes(), 0, 4);
        return address_prefix + Base58.encode(Raw.concat(pub, csum));
    }

    /**
     * Decodes an {@code EOS...} public key into its 33 compressed bytes.
     *
     * @throws IllegalArgumentException if the key is not a valid public key string
     */
    public static byte[] decodePublicKey(String publicKey) {
        Objects.requireNonNull(publicKey, "publicKey");
        String key = publicKey.trim();
        if (key.startsWith(address_prefix)) {
            key = key.substring(address_prefix.length());
        }
        byte[] decoded = Base58.decode(key);
        if (decoded.length < 5) {
            throw new IllegalArgumentException("Invalid public key: too short");
        }
        byte[] payload = Raw.copy(decoded, 0, decoded.length - 4);
        byte[] checksum = Raw.copy(decoded, decoded.length - 4, 4);
        byte[] expected = Raw.copy(Ripemd160.from(payload).bytes(), 0, 4);
        if (!Arrays.equals(checksum, expected)) {
            throw new IllegalArgumentException("Invalid public key: checksum mismatch");
        }
        return payload;
    }

    /**
     * Signs the SHA-256 hash of {@code data} and returns a {@code SIG_K1_...} signature.
     */
    public static String signHash(String pk, byte[] data) {
        String digest = Hex.toHex(SHA.sha256(data));
        return signDigest(pk, digest);
    }

    /**
     * Signs an already computed SHA-256 digest (hex encoded).
     */
    public static String signDigest(String pk, String dataSha256) {
        BigInteger e = new BigInteger(dataSha256, 16);
        int i = 0;
        BigInteger d = privateKey(pk);
        Point Q = secp.G().multiply(d);
        Ecdsa ecd = new Ecdsa(secp);
        Ecdsa.SignBigInt sign;
        int nonce = 0;
        while (true) {
            sign = ecd.sign(dataSha256, d, nonce++);
            byte der[] = sign.getDer();
            byte lenR = der[3];
            byte lenS = der[5 + lenR];
            if (lenR == 32 && lenS == 32) {
                i = ecd.calcPubKeyRecoveryParam(e, sign, Q);
                i += 4; // compressed
                i += 27; // compact // 24 or 27 :( forcing odd-y 2nd key candidate)
                break;
            }
        }
        byte[] pub_buf = new byte[65];
        pub_buf[0] = (byte) i;
        byte[] r = Point.integerToBytes(sign.getR(), 32);
        byte[] s = Point.integerToBytes(sign.getS(), 32);
        System.arraycopy(r, 0, pub_buf, 1, 32);
        System.arraycopy(s, 0, pub_buf, 33, 32);

        byte[] checksum = Ripemd160.from(Raw.concat(pub_buf, "K1".getBytes(StandardCharsets.UTF_8))).bytes();
        byte[] signatureString = Raw.concat(pub_buf, Raw.copy(checksum, 0, 4));
        return SIGNATURE_PREFIX + Base58.encode(signatureString);
    }

    /**
     * Verifies a {@code SIG_K1_...} signature over {@code data} for the given {@code EOS...} public key.
     *
     * <p>This is the counterpart of {@link #signHash(String, byte[])} requested in issue #13: the
     * signature is decoded, its checksum validated, and the signing public key recovered and compared
     * with the expected one.</p>
     */
    public static boolean verify(String publicKey, byte[] data, String signature) {
        Objects.requireNonNull(data, "data");
        return verifyDigest(publicKey, Hex.toHex(SHA.sha256(data)), signature);
    }

    /**
     * Verifies a {@code SIG_K1_...} signature over an already computed SHA-256 digest (hex encoded).
     */
    public static boolean verifyDigest(String publicKey, String dataSha256, String signature) {
        Objects.requireNonNull(publicKey, "publicKey");
        Objects.requireNonNull(signature, "signature");
        String sig = signature.trim();
        if (sig.startsWith(SIGNATURE_PREFIX)) {
            sig = sig.substring(SIGNATURE_PREFIX.length());
        }
        byte[] decoded = Base58.decode(sig);
        if (decoded.length < 69) {
            throw new IllegalArgumentException("Invalid signature: too short");
        }
        byte[] payload = Raw.copy(decoded, 0, decoded.length - 4);
        byte[] checksum = Raw.copy(decoded, decoded.length - 4, 4);
        byte[] expectedChecksum = Raw.copy(
                Ripemd160.from(Raw.concat(payload, "K1".getBytes(StandardCharsets.UTF_8))).bytes(), 0, 4);
        if (!Arrays.equals(checksum, expectedChecksum)) {
            throw new IllegalArgumentException("Invalid signature: checksum mismatch");
        }
        int header = payload[0] & 0xff;
        int recovery = header - 27 - 4;
        if (recovery < 0 || recovery > 3) {
            throw new IllegalArgumentException("Invalid signature: unsupported header " + header);
        }
        Ecdsa.SignBigInt big = new Ecdsa.SignBigInt();
        big.setR(new BigInteger(1, Raw.copy(payload, 1, 32)));
        big.setS(new BigInteger(1, Raw.copy(payload, 33, 32)));
        BigInteger e = new BigInteger(dataSha256, 16);
        Point recovered = new Ecdsa(secp).recoverPubKey(e, big, recovery);
        return Arrays.equals(recovered.getEncoded(), decodePublicKey(publicKey));
    }

    private static byte[] normalize(byte[] keyMaterial) {
        BigInteger d = new BigInteger(1, keyMaterial);
        BigInteger n = secp.n();
        if (d.signum() == 0) {
            throw new IllegalArgumentException("Invalid private key: zero scalar");
        }
        if (d.compareTo(n) >= 0) {
            throw new IllegalArgumentException("Invalid private key: scalar out of range");
        }
        return Point.integerToBytes(d, 32);
    }
}