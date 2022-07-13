package cz.muni.ics.ga4gh.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;

import java.util.Collection;
import java.util.Map;

public interface JWTSigningAndValidationService {

    Map<String, JWK> getAllPublicKeys();

    JWSAlgorithm getDefaultSigningAlgorithm();

    Collection<JWSAlgorithm> getAllSigningAlgsSupported();

    /**
     * Called to sign a jwt in place for a client that hasn't registered a preferred signing algorithm.
     * Use the default algorithm to sign.
     *
     * @param jwt the jwt to sign
     * @throws IllegalStateException when calling default signing with no default signer ID set
     */
    void signJwt(SignedJWT jwt);

    /**
     * Sign a jwt using the selected algorithm. The algorithm is selected using the String parameter values specified
     * in the JWT spec, section 6. I.E., "HS256" means HMAC with SHA-256 and corresponds to our HmacSigner class.
     *
     * @param jwt the jwt to sign
     * @param alg the name of the algorithm to use, as specified in JWS s.6
     */
    void signJwt(SignedJWT jwt, JWSAlgorithm alg);

    String getDefaultSignerKeyId();
}
