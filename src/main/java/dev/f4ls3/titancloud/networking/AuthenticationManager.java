package dev.f4ls3.titancloud.networking;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class AuthenticationManager {

    private static final List<DecodedJWT> verifiedTokens = new ArrayList<>();

    private static byte[] getMinionSecretOrGenerate(final UUID minionUUID) {
        if(!Documents.keys.contains(minionUUID.toString())) {
            Documents.keys.put(minionUUID.toString(), UUID.randomUUID().toString());
            Documents.keys.flush();
        }
        return Documents.keys.<String>get(minionUUID.toString()).getBytes();
    }

    private static byte[] getMinionSecretOrGenerate(final Minion minion) {
        return getMinionSecretOrGenerate(minion.getMinionUUID());
    }

    private static byte[] getMinionSecret(final UUID minionUUID) {
        if(!Documents.keys.contains(minionUUID.toString())) return null;
        return Documents.keys.<String>get(minionUUID.toString()).getBytes();
    }

    public static String generateToken(final Minion minion) {
        final Algorithm algorithm = Algorithm.HMAC256(getMinionSecretOrGenerate(minion));

        return JWT
                .create()
                .withSubject(minion.getMinionUUID().toString())
                .withClaim("minionID", minion.getMinionID())
                .withClaim("minionName", minion.getMinionName())
                .sign(algorithm);
    }

    public static boolean verifyTokenSignature(final String token) {
        if(token == null) return false;

        DecodedJWT unsafe = JWT.decode(token);

        final byte[] secret = getMinionSecret(UUID.fromString(unsafe.getSubject()));
        if(secret == null) return false;

        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT
                    .require(algorithm)
                    .withClaimPresence("minionID")
                    .withClaimPresence("minionName")
                    .build();
            verifiedTokens.add(verifier.verify(token));
            return true;
        } catch (JWTVerificationException e) {
            Networking.getLogger().warning("Couldn't verify token integrity!");
            return false;
        }
    }

    public static Minion resolveMinionFromToken(final String token) {
        DecodedJWT jwt = verifiedTokens.stream().filter(decodedJWT -> decodedJWT.getToken().equals(token)).findFirst().orElse(null);
        if(jwt == null) return null;

        return MinionRegistry.get(UUID.fromString(jwt.getSubject()));
    }
}
