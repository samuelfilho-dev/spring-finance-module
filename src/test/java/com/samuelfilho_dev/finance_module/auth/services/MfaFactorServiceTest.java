package com.samuelfilho_dev.finance_module.auth.services;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MfaFactorServiceTest {

    private final MfaFactorService mfaFactorService = new MfaFactorService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mfaFactorService, "issue", "finance_module");
    }

    @Test
    void generateSecret_shouldReturnNonBlankValue() {
        var secret = mfaFactorService.generateSecret();
        assertNotNull(secret);
        assertFalse(secret.isBlank());
    }

    @Test
    void buildOtpAuthUrl_shouldContainEmailIssuerAndSecret() {
        var secret = mfaFactorService.generateSecret();
        var url = mfaFactorService.buildOtpAuthUrl("user@test.com", secret);

        assertTrue(url.startsWith("otpauth://totp/"));
        assertTrue(url.contains("user@test.com") || url.contains("user%40test.com"));
        assertTrue(url.contains("finance_module") || url.contains("issuer=finance_module"));
        assertTrue(url.contains(secret));
    }

    @Test
    void generateQrCodeImageBase64_shouldReturnDataUri() {
        var secret = mfaFactorService.generateSecret();
        var qr = mfaFactorService.generateQrCodeImageBase64("user@test.com", secret);
        assertTrue(qr.startsWith("data:image/png;base64,"));
    }

    @Test
    void verifyCode_shouldAcceptCurrentTotpAndRejectInvalid() throws Exception {
        var secret = mfaFactorService.generateSecret();
        var generator = new DefaultCodeGenerator();
        var timeProvider = new SystemTimeProvider();
        var validCode = generator.generate(secret, Math.floorDiv(timeProvider.getTime(), 30));

        assertTrue(mfaFactorService.verifyCode(secret, validCode));
        assertFalse(mfaFactorService.verifyCode(secret, "000000"));
    }
}
