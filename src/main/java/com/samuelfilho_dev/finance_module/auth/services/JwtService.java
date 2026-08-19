package com.samuelfilho_dev.finance_module.auth.services;


import com.samuelfilho_dev.finance_module.users.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {
    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_USER_ID = "userId";
    public static final String TYPE_ACCESS = "ACCESS";
    public static final String TYPE_PRE_AUTH = "PRE_AUTH";
    public static final String TYPE_SETUP = "SETUP_2FA";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long accessTokenExpirationMs;
    private final long preAuthTokenExpirationMs;
    private final long setupTokenExpirationMs;
    private final String issue;

    public JwtService(
            @Value("${spring.application.name}") String issue,
            @Value("${app.jwt.private-key-location}") Resource privateKeyResource,
            @Value("${app.jwt.public-key-location}") Resource publicKeyResource,
            @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${app.jwt.pre-auth-token-expiration-ms}") long preAuthTokenExpirationMs,
            @Value("${app.jwt.setup-token-expiration-ms}") long setupTokenExpirationMs
    ) {
        try {
            this.privateKey = loadPrivateKey(privateKeyResource);
            this.publicKey = loadPublicKey(publicKeyResource);
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao carregar par de chaves RSA para assinatura do JWT", ex);
        }

        this.issue = issue;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.preAuthTokenExpirationMs = preAuthTokenExpirationMs;
        this.setupTokenExpirationMs = setupTokenExpirationMs;
    }

    public String generateAccessToken(User user) {
        var now = new Date();
        var expiry = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claims(Map.of(
                        CLAIM_TYPE, TYPE_ACCESS,
                        CLAIM_ROLES, List.of(user.getRole()),
                        CLAIM_USER_ID, user.getId()
                ))
                .issuer(issue)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();

    }

    public String generatePreAuthToken(User user) {
        var now = new Date();
        var expiry = new Date(now.getTime() + preAuthTokenExpirationMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claims(Map.of(CLAIM_TYPE, TYPE_PRE_AUTH, CLAIM_USER_ID, user.getId()))
                .issuer(issue)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public String generateSetupToken(User user) {
        var now = new Date();
        var expiry = new Date(now.getTime() + setupTokenExpirationMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claims(Map.of(CLAIM_TYPE, TYPE_SETUP, CLAIM_USER_ID, user.getId()))
                .issuer(issue)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public Claims parseAndValidate(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(Claims claims) {
        return claims.getSubject();
    }

    public String extractType(Claims claims) {
        return claims.get(CLAIM_TYPE, String.class);
    }

    public String extractUserId(Claims claims) {
        return claims.get(CLAIM_USER_ID, String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        return claims.get(CLAIM_ROLES, List.class);
    }


    private PrivateKey loadPrivateKey(Resource resource) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        var pem = this.readPem(resource, "PRIVATE KEY");
        var decoded = Base64.getDecoder().decode(pem);
        var spec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private PublicKey loadPublicKey(Resource resource) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        var pem = this.readPem(resource, "PUBLIC KEY");
        var decoded = Base64.getDecoder().decode(pem);
        var spec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private String readPem(Resource resource, String label) throws IOException {
        try (InputStream is = resource.getInputStream()) {
            var content = new String(is.readAllBytes());
            return content.replace("-----BEGIN " + label + "-----", "").replace("-----END " + label + "-----", "").replaceAll("\\s", "");
        }
    }

}
