package io.jafka.jeos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jafka.jeos.util.KeyUtil;

/**
 * Key generation, key derivation and signature verification (upstream issues #7 and #13).
 */
class KeyUtilTest {

    @Test
    @DisplayName("generated private keys are random, importable and produce distinct public keys")
    void createPrivateKeyIsRandom() {
        String first = KeyUtil.createPrivateKey();
        String second = KeyUtil.createPrivateKey();
        assertNotEquals(first, second);
        assertTrue(first.startsWith("5"), "WIF keys of the main net start with 5, got " + first);
        assertNotEquals(KeyUtil.toPublicKey(first), KeyUtil.toPublicKey(second));
        assertTrue(KeyUtil.toPublicKey(first).startsWith("EOS"));
    }

    @Test
    @DisplayName("a private key round-trips through WIF encoding without losing a byte")
    void wifRoundTrip() {
        byte[] material = new byte[32];
        for (int i = 0; i < material.length; i++) {
            material[i] = (byte) (i + 1);
        }
        String wif = KeyUtil.encodePrivateKey(material);
        assertArrayEquals(material, toBytes(KeyUtil.toPrivateKey(wif)));
        assertNotEquals(wif, KeyUtil.encodePrivateKey(new byte[] { 1 }));
    }

    @Test
    @DisplayName("a signature verifies against its public key and fails for other data or keys")
    void signatureVerification() {
        String privateKey = KeyUtil.createPrivateKey(new byte[] { 42, 42, 42 });
        String publicKey = KeyUtil.toPublicKey(privateKey);
        byte[] data = "jeos offline transfer".getBytes(StandardCharsets.UTF_8);

        String signature = KeyUtil.signHash(privateKey, data);
        assertTrue(signature.startsWith(KeyUtil.SIGNATURE_PREFIX));
        assertTrue(KeyUtil.verify(publicKey, data, signature));
        assertFalse(KeyUtil.verify(publicKey, "tampered".getBytes(StandardCharsets.UTF_8), signature));
        assertFalse(KeyUtil.verify(KeyUtil.toPublicKey(KeyUtil.createPrivateKey()), data, signature));
    }

    @Test
    @DisplayName("a corrupted WIF key is rejected instead of silently producing another key")
    void invalidWifIsRejected() {
        String wif = KeyUtil.createPrivateKey(new byte[] { 7 });
        String corrupted = wif.substring(0, wif.length() - 1)
                + (wif.charAt(wif.length() - 1) == 'A' ? 'B' : 'A');
        assertThrows(IllegalArgumentException.class, () -> KeyUtil.toPrivateKey(corrupted));
    }

    private static byte[] toBytes(java.math.BigInteger value) {
        return io.jafka.jeos.util.ecc.Point.integerToBytes(value, 32);
    }
}