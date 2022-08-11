package cz.muni.ics.ga4gh.base;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@Getter
public class JWKSetKeyStore {

    private JWKSet jwkSet;
    private Resource location;

    @Autowired
    public JWKSetKeyStore(Resource location) {
        setLocation(location);
    }

    public JWKSetKeyStore(JWKSet jwkSet) {
        this.setJwkSet(jwkSet);
        initializeJwkSet();
    }

    public void setJwkSet(JWKSet jwkSet) {
        if (jwkSet == null) {
            throw new IllegalArgumentException("Argument cannot be null");
        }

        this.jwkSet = jwkSet;
        initializeJwkSet();
    }

    public void setLocation(Resource location) {
        this.location = location;
        initializeJwkSet();
    }

    public List<JWK> getKeys() {
        if (jwkSet == null) {
            initializeJwkSet();
        }

        return jwkSet.getKeys();
    }

    private void initializeJwkSet() {
        if (jwkSet != null) {
            return;
        } else if (location == null) {
            return;
        }

        if (location.exists() && location.isReadable()) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(location.getInputStream(), StandardCharsets.UTF_8))
            ) {
                String s = br.lines().collect(Collectors.joining());
                jwkSet = JWKSet.parse(s);
            } catch (IOException e) {
                throw new IllegalArgumentException("Key Set resource could not be read: " + location);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Key Set resource could not be parsed: " + location);
            }
        } else {
            throw new IllegalArgumentException("Key Set resource could not be read: " + location);
        }
    }
}
