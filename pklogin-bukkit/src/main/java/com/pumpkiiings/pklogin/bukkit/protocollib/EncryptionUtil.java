package com.pumpkiiings.pklogin.bukkit.protocollib;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import com.pumpkiiings.pklogin.bukkit.PkLoginBukkit;

public class EncryptionUtil {
    public static final int VERIFY_TOKEN_LENGTH = 4;
    public static final String KEY_PAIR_ALGORITHM = "RSA";
    private static final int RSA_LENGTH = 1024;

    private static final PublicKey MOJANG_SESSION_KEY;
    private static final int LINE_LENGTH = 76;
    private static final Base64.Encoder KEY_ENCODER = Base64.getMimeEncoder(LINE_LENGTH, "\n".getBytes(StandardCharsets.UTF_8));
    private static final int MILLISECOND_SIZE = 8;
    private static final int UUID_SIZE = 2 * MILLISECOND_SIZE;

    static {
        try {
            MOJANG_SESSION_KEY = loadMojangSessionKey();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load Mojang session key", ex);
        }
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_PAIR_ALGORITHM);
            keyPairGenerator.initialize(RSA_LENGTH);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static byte[] generateVerifyToken(Random random) {
        byte[] token = new byte[VERIFY_TOKEN_LENGTH];
        random.nextBytes(token);
        return token;
    }

    public static SecretKey decryptSharedKey(PrivateKey privateKey, byte[] sharedKey)
            throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException,
            BadPaddingException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance(privateKey.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new SecretKeySpec(cipher.doFinal(sharedKey), "AES");
    }

    public static boolean verifyNonce(byte[] expected, PrivateKey decryptionKey, byte[] encryptedNonce)
            throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException,
            BadPaddingException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance(decryptionKey.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, decryptionKey);
        byte[] decryptedNonce = cipher.doFinal(encryptedNonce);
        return java.util.Arrays.equals(expected, decryptedNonce);
    }

    public static String getServerIdHashString(String serverId, SecretKey sharedSecret, PublicKey publicKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(serverId.getBytes(StandardCharsets.ISO_8859_1));
            digest.update(sharedSecret.getEncoded());
            digest.update(publicKey.getEncoded());
            return new BigInteger(digest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verifyClientKey(ClientPublicKey clientKey, Instant verifyTimestamp, UUID premiumId)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        if (clientKey.getExpiry().isBefore(verifyTimestamp)) {
            return false;
        }

        Signature verifier = Signature.getInstance("SHA1withRSA");
        verifier.initVerify(MOJANG_SESSION_KEY);
        verifier.update(toSignable(clientKey, premiumId));
        return verifier.verify(clientKey.getSignature());
    }

    private static byte[] toSignable(ClientPublicKey clientPublicKey, UUID ownerPremiumId) {
        if (ownerPremiumId == null) {
            long expiry = clientPublicKey.getExpiry().toEpochMilli();
            String encoded = KEY_ENCODER.encodeToString(clientPublicKey.getKey().getEncoded());
            return (expiry + "-----BEGIN RSA PUBLIC KEY-----\n" + encoded + "\n-----END RSA PUBLIC KEY-----\n")
                    .getBytes(StandardCharsets.US_ASCII);
        }

        byte[] keyData = clientPublicKey.getKey().getEncoded();
        return ByteBuffer.allocate(keyData.length + UUID_SIZE + MILLISECOND_SIZE)
                .putLong(ownerPremiumId.getMostSignificantBits())
                .putLong(ownerPremiumId.getLeastSignificantBits())
                .putLong(clientPublicKey.getExpiry().toEpochMilli())
                .put(keyData)
                .array();
    }

    public static boolean verifySignedNonce(byte[] nonce, PublicKey clientKey, long signatureSalt, byte[] signature)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(clientKey);

        verifier.update(nonce);
        byte[] saltBytes = ByteBuffer.allocate(8).putLong(signatureSalt).array();
        verifier.update(saltBytes);
        return verifier.verify(signature);
    }

    private static PublicKey loadMojangSessionKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        try (InputStream is = PkLoginBukkit.class.getClassLoader().getResourceAsStream("yggdrasil_session_pubkey.der")) {
            if (is == null) throw new IOException("Resource not found: yggdrasil_session_pubkey.der");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] keyData = buffer.toByteArray();
            KeySpec keySpec = new X509EncodedKeySpec(keyData);
            return KeyFactory.getInstance("RSA").generatePublic(keySpec);
        }
    }
}
