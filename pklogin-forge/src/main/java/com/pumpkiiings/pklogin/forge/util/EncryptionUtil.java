package com.pumpkiiings.pklogin.forge.util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;

import com.pumpkiiings.pklogin.forge.PkLoginForge;

public class EncryptionUtil {
    public static final int VERIFY_TOKEN_LENGTH = 4;
    public static final String KEY_PAIR_ALGORITHM = "RSA";
    private static final int RSA_LENGTH = 1024;

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
}
