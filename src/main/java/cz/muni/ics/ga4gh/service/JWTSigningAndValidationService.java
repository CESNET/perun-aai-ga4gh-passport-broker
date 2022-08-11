package cz.muni.ics.ga4gh.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import java.util.Map;

public interface JWTSigningAndValidationService {


    JWSAlgorithm getSigningAlgorithm();

    String getSignerKeyId();

    Map<String, JWK> getPublicKeys();

    /**
     * Called to sign a jwt in place for a client that hasn't registered a preferred signing algorithm.
     * Use the default algorithm to sign.
     *
     * @param jwt the jwt to sign
     * @throws IllegalStateException when calling default signing with no default signer ID set
     */
    void signJwt(SignedJWT jwt);

}
