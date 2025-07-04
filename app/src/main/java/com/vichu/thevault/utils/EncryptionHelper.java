package com.vichu.thevault.utils;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionHelper {

    private static final int KEY_SIZE = 256;
    private static final int ITERATION_COUNT = 65536;
    private static final int IV_SIZE = 16;
    private static final String ENC_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    // Generate a new salt
    public static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    // Encrypt the password using private key and provided salt
    public static String encrypt(String password, String privateKey, String base64Salt) throws Exception {
        byte[] salt = Base64.decode(base64Salt, Base64.NO_WRAP);

        // Derive key from password and salt
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        KeySpec spec = new PBEKeySpec(privateKey.toCharArray(), salt, ITERATION_COUNT, KEY_SIZE);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

        // Generate random IV
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Encrypt
        Cipher cipher = Cipher.getInstance(ENC_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));

        // Prepend IV to ciphertext
        byte[] ivAndEncrypted = new byte[IV_SIZE + encrypted.length];
        System.arraycopy(iv, 0, ivAndEncrypted, 0, IV_SIZE);
        System.arraycopy(encrypted, 0, ivAndEncrypted, IV_SIZE, encrypted.length);

        return Base64.encodeToString(ivAndEncrypted, Base64.NO_WRAP);
    }

    // Decrypt the password using private key and provided salt
    public static String decrypt(String base64Encrypted, String privateKey, String base64Salt) throws Exception {
        byte[] encryptedData = Base64.decode(base64Encrypted, Base64.NO_WRAP);
        byte[] salt = Base64.decode(base64Salt, Base64.NO_WRAP);

        // Derive key
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        KeySpec spec = new PBEKeySpec(privateKey.toCharArray(), salt, ITERATION_COUNT, KEY_SIZE);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

        // Extract IV
        byte[] iv = new byte[IV_SIZE];
        System.arraycopy(encryptedData, 0, iv, 0, IV_SIZE);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Extract ciphertext
        byte[] ciphertext = new byte[encryptedData.length - IV_SIZE];
        System.arraycopy(encryptedData, IV_SIZE, ciphertext, 0, ciphertext.length);

        // Decrypt
        Cipher cipher = Cipher.getInstance(ENC_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        byte[] original = cipher.doFinal(ciphertext);

        return new String(original, StandardCharsets.UTF_8);
    }
}
