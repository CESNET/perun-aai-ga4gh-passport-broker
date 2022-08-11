package cz.muni.ics.ga4gh.service.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import cz.muni.ics.ga4gh.base.JWKSetKeyStore;
import cz.muni.ics.ga4gh.service.JWTSigningAndValidationService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class JWTSigningAndValidationServiceImpl implements JWTSigningAndValidationService {

    private final Map<String, JWSSigner> signers = new HashMap<>();
    private final Map<String, JWSVerifier> verifiers = new HashMap<>();

    private String signerKeyId;
    private JWSAlgorithm signingAlgorithm;
    private final Map<String, JWK> keys = new HashMap<>();

    /**
     * Build this service based on the given keystore. All keys must have a key
     * id ({@code kid}) field in order to be used.
     *
     * @param keyStore The keystore to load all keys from.
     */
    @Autowired
    public JWTSigningAndValidationServiceImpl(JWKSetKeyStore keyStore) throws Exception {
        loadKeysFromStore(keyStore);
        initializeSignersAndVerifiers();

        if (keyStore != null) {
            JWK defaultKey = keyStore.getKeys().get(0);
            setSignerKeyId(defaultKey.getKeyID());
            setDefaultSigningAlgorithmName(defaultKey.getAlgorithm().getName());
        } else {
            throw new Exception("Failed to initialize keystore");
        }
    }

    @Override
    public String getSignerKeyId() {
        return signerKeyId;
    }

    public void setSignerKeyId(String defaultSignerId) {
        this.signerKeyId = defaultSignerId;
    }

    @Override
    public JWSAlgorithm getSigningAlgorithm() {
        return signingAlgorithm;
    }

    public void setDefaultSigningAlgorithmName(String algName) {
        signingAlgorithm = JWSAlgorithm.parse(algName);
    }

    @Override
    public void signJwt(SignedJWT jwt) {
        if (getSignerKeyId() == null) {
            throw new IllegalStateException("No signer key ID is set");
        }

        JWSSigner signer = signers.getOrDefault(getSignerKeyId(), null);
        if (signer == null) {
            throw new IllegalStateException("No signer found for set signer key ID");
        }

        try {
            jwt.sign(signer);
        } catch (JOSEException e) {
            log.error("Failed to sign JWT, error was: ", e);
        }
    }

    @Override
    public Map<String, JWK> getPublicKeys() {
        Map<String, JWK> pubKeys = new HashMap<>();

        keys.keySet().forEach(keyId -> {
            JWK key = keys.get(keyId);
            JWK pub = key.toPublicJWK();
            if (pub != null) {
                pubKeys.put(keyId, pub);
            }
        });

        return pubKeys;
    }

    private void loadKeysFromStore(JWKSetKeyStore keyStore) {
        if (keyStore != null && keyStore.getJwkSet() != null) {
            for (JWK key : keyStore.getKeys()) {
                if (StringUtils.hasText(key.getKeyID())) {
                    this.keys.put(key.getKeyID(), key);
                } else {
                    String fakeKid = UUID.randomUUID().toString();
                    this.keys.put(fakeKid, key);
                }
            }
        }
    }

    private void initializeSignersAndVerifiers() {
        for (Map.Entry<String, JWK> jwkEntry : keys.entrySet()) {
            String id = jwkEntry.getKey();
            JWK jwk = jwkEntry.getValue();

            try {
                if (jwk instanceof RSAKey) {
                    processRSAKey(signers, verifiers, jwk, id);
                } else if (jwk instanceof ECKey) {
                    processECKey(signers, verifiers, jwk, id);
                } else if (jwk instanceof OctetSequenceKey) {
                    processOctetKey(signers, verifiers, jwk, id);
                } else {
                    log.warn("Unknown key type: {}", jwk);
                }
            } catch (JOSEException e) {
                log.warn("Exception loading signer/verifier", e);
            }
        }

        if (signerKeyId == null && keys.size() == 1) {
            setSignerKeyId(keys.keySet().iterator().next());
        }
    }

    private void processOctetKey(Map<String, JWSSigner> signers, Map<String, JWSVerifier> verifiers, JWK jwk, String id)
            throws JOSEException
    {
        if (jwk.isPrivate()) {
            MACSigner signer = new MACSigner((OctetSequenceKey) jwk);
            signers.put(id, signer);
        }

        MACVerifier verifier = new MACVerifier((OctetSequenceKey) jwk);
        verifiers.put(id, verifier);
    }

    private void processECKey(Map<String, JWSSigner> signers, Map<String, JWSVerifier> verifiers, JWK jwk, String id)
            throws JOSEException
    {
        if (jwk.isPrivate()) {
            ECDSASigner signer = new ECDSASigner((ECKey) jwk);
            signers.put(id, signer);
        }

        ECDSAVerifier verifier = new ECDSAVerifier((ECKey) jwk);
        verifiers.put(id, verifier);
    }

    private void processRSAKey(Map<String, JWSSigner> signers, Map<String, JWSVerifier> verifiers, JWK jwk, String id)
            throws JOSEException
    {
        if (jwk.isPrivate()) {
            RSASSASigner signer = new RSASSASigner((RSAKey) jwk);
            signers.put(id, signer);
        }

        RSASSAVerifier verifier = new RSASSAVerifier((RSAKey) jwk);
        verifiers.put(id, verifier);
    }
}
