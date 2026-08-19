package com.samuelfilho_dev.finance_module.support;

import com.samuelfilho_dev.finance_module.auth.entities.AuthenticatedUser;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;

public final class TestSupport {

    private TestSupport() {
    }

    public static void authenticate(String userId) {
        var principal = new AuthenticatedUser(userId, "user@test.com", "secret", List.of());
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    public static KeyResources rsaKeyResources() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            return new KeyResources(
                    pemResource("PRIVATE KEY", pair.getPrivate().getEncoded()),
                    pemResource("PUBLIC KEY", pair.getPublic().getEncoded())
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA keys for tests", ex);
        }
    }

    private static Resource pemResource(String label, byte[] encoded) {
        var body = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded);
        var pem = "-----BEGIN " + label + "-----\n" + body + "\n-----END " + label + "-----";
        return new ByteArrayResource(pem.getBytes(StandardCharsets.UTF_8));
    }

    public record KeyResources(Resource privateKey, Resource publicKey) {
    }
}
