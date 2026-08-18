package com.samuelfilho_dev.finance_module.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@Slf4j
public class AESService {

    @Value("${app.secret.mfa-secret}")
    private String mfaSecret;

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH = 32;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();


    public String encrypt(String plainText) {
        try {
            var iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            var key = generateKey();
            var cipher = Cipher.getInstance(ALGORITHM);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    key,
                    new GCMParameterSpec(TAG_LENGTH, iv)
            );

            var encrypted = cipher.doFinal(
                    plainText.getBytes(StandardCharsets.UTF_8)
            );
            var result = new byte[iv.length + encrypted.length];

            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(result);
        } catch (GeneralSecurityException e) {
            log.error("Erro ao criptografar o conteúdo utilizando AES.", e);
            throw new IllegalStateException(
                    "Não foi possível criptografar o conteúdo.",
                    e
            );
        }
    }

    public String decrypt(String cipherText) {
        try {
            var data = Base64.getDecoder().decode(cipherText);

            var iv = new byte[IV_LENGTH];
            var encrypted = new byte[data.length - IV_LENGTH];

            System.arraycopy(data, 0, iv, 0, IV_LENGTH);
            System.arraycopy(data, IV_LENGTH, encrypted, 0, encrypted.length);

            var key = generateKey();
            var cipher = Cipher.getInstance(ALGORITHM);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(TAG_LENGTH, iv)
            );

            var decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            log.error("Erro ao descriptografar o conteúdo utilizando AES.", e);
            throw new IllegalStateException(
                    "Não foi possível descriptografar o conteúdo.",
                    e
            );
        }
    }

    private SecretKeySpec generateKey() {
        var keyBytes = mfaSecret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length != KEY_LENGTH) {
            throw new IllegalStateException(
                    "A chave AES deve possuir exatamente %d bytes".formatted(KEY_LENGTH)
            );
        }

        return new SecretKeySpec(
                keyBytes, "AES"
        );
    }
}
