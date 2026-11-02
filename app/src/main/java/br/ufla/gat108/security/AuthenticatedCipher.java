package br.ufla.gat108.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Realiza cifragem autenticada AES-GCM (AES/GCM/NoPadding) com vetor de inicialização (IV)
 * dinâmico para proteção de registros e logs locais do trabalhador.
 */
public class AuthenticatedCipher {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    /**
     * Cifra um vetor de bytes usando AES-GCM com chave do KeyStore e IV dinâmico.
     * Retorna um buffer contendo [IV (12 bytes) || Ciphertext (com Tag GCM de autenticação)].
     */
    public static byte[] encrypt(byte[] plaintext) throws Exception {
        SecretKey key = KeyStoreManager.getOrCreateSecretKey();
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

        byte[] ciphertext = cipher.doFinal(plaintext);

        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
        return combined;
    }

    /**
     * Decifra o buffer [IV || Ciphertext] validando a autenticidade e integridade.
     */
    public static byte[] decrypt(byte[] ivAndCiphertext) throws Exception {
        if (ivAndCiphertext == null || ivAndCiphertext.length < GCM_IV_LENGTH_BYTES) {
            throw new IllegalArgumentException("Payload cifrado inválido ou muito curto.");
        }

        SecretKey key = KeyStoreManager.getOrCreateSecretKey();
        byte[] iv = Arrays.copyOfRange(ivAndCiphertext, 0, GCM_IV_LENGTH_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(ivAndCiphertext, GCM_IV_LENGTH_BYTES, ivAndCiphertext.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

        return cipher.doFinal(ciphertext);
    }

    /**
     * Cifra uma String e retorna em formato Base64.
     */
    public static String encryptString(String plaintext) throws Exception {
        if (plaintext == null) return null;
        byte[] encryptedBytes = encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Decifra uma String em formato Base64.
     */
    public static String decryptString(String base64Ciphertext) throws Exception {
        if (base64Ciphertext == null) return null;
        byte[] decodedBytes = Base64.getDecoder().decode(base64Ciphertext);
        byte[] decryptedBytes = decrypt(decodedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
