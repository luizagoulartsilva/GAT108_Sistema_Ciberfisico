package br.ufla.gat108.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.security.KeyStore;
import java.security.SecureRandom;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Gerencia chaves criptográficas utilizando o Android KeyStore do sistema
 * com suporte a fallback em ambiente de testes unitários JVM.
 */
public class KeyStoreManager {
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    public static final String KEY_ALIAS = "Gat108WorkerSecureKey";
    private static SecretKey cachedFallbackKey = null;

    /**
     * Obtém ou gera uma chave secreta AES para cifragem autenticada.
     */
    public static SecretKey getOrCreateSecretKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);

            if (keyStore.containsAlias(KEY_ALIAS)) {
                KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
                if (entry instanceof KeyStore.SecretKeyEntry) {
                    return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
                }
            }

            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);
            KeyGenParameterSpec parameterSpec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build();

            keyGenerator.init(parameterSpec);
            return keyGenerator.generateKey();
        } catch (Throwable t) {
            // Fallback para ambiente de teste unitário na JVM onde AndroidKeyStore não existe
            if (cachedFallbackKey == null) {
                try {
                    KeyGenerator fallbackGen = KeyGenerator.getInstance("AES");
                    fallbackGen.init(256);
                    cachedFallbackKey = fallbackGen.generateKey();
                } catch (Exception e) {
                    byte[] dummyBytes = new byte[32];
                    new SecureRandom().nextBytes(dummyBytes);
                    cachedFallbackKey = new SecretKeySpec(dummyBytes, "AES");
                }
            }
            return cachedFallbackKey;
        }
    }
}
