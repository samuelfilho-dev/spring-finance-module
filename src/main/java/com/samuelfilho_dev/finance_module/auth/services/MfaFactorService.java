package com.samuelfilho_dev.finance_module.auth.services;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import dev.samstevens.totp.util.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MfaFactorService {
    @Value("${spring.application.name}")
    private String issue;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), timeProvider);


    public String generateSecret() {
        return secretGenerator.generate();
    }

    public String buildOtpAuthUrl(String email, String secret) {
        return buildQrData(email, secret).getUri();
    }

    public String generateQrCodeImageBase64(String email, String secret) {
        try {
            var imgBytes = qrGenerator.generate(buildQrData(email, secret));
            return Utils.getDataUriForImage(imgBytes, qrGenerator.getImageMimeType());
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar QR Code do 2FA", e);
        }
    }

    public Boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }

    private QrData buildQrData(String email, String secret) {
        return new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(issue)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

    }
}
