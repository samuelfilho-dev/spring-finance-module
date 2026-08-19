package com.samuelfilho_dev.finance_module.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AESServiceTest {

    private static final String VALID_KEY = "12345678901234567890123456789012";

    private final AESService aesService = new AESService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aesService, "mfaSecret", VALID_KEY);
    }

    @Test
    void encryptAndDecrypt_shouldRoundTrip() {
        var encrypted = aesService.encrypt("totp-secret");
        assertNotEquals("totp-secret", encrypted);
        assertEquals("totp-secret", aesService.decrypt(encrypted));
    }

    @Test
    void encrypt_shouldProduceDifferentCipherTexts() {
        var first = aesService.encrypt("same");
        var second = aesService.encrypt("same");
        assertNotEquals(first, second);
        assertEquals("same", aesService.decrypt(first));
        assertEquals("same", aesService.decrypt(second));
    }

    @Test
    void encrypt_shouldRejectInvalidKeyLength() {
        ReflectionTestUtils.setField(aesService, "mfaSecret", "too-short");
        assertThrows(IllegalStateException.class, () -> aesService.encrypt("value"));
    }

    @Test
    void decrypt_shouldRejectInvalidPayload() {
        assertThrows(RuntimeException.class, () -> aesService.decrypt("@@@not-base64@@@"));
    }

    @Test
    void decrypt_shouldRejectInvalidKeyLength() {
        ReflectionTestUtils.setField(aesService, "mfaSecret", "too-short");
        var encrypted = new AESService();
        ReflectionTestUtils.setField(encrypted, "mfaSecret", VALID_KEY);
        var cipherText = encrypted.encrypt("value");

        assertThrows(IllegalStateException.class, () -> aesService.decrypt(cipherText));
    }

    @Test
    void decrypt_shouldRejectTamperedCipherText() {
        var cipherText = aesService.encrypt("value");
        var tampered = cipherText.substring(0, cipherText.length() - 2) + "AA";
        assertThrows(IllegalStateException.class, () -> aesService.decrypt(tampered));
    }
}
