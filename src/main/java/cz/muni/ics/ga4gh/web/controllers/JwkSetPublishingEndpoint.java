package cz.muni.ics.ga4gh.web.controllers;

import static cz.muni.ics.ga4gh.web.security.WebSecurityConfigurer.PUBLIC_ENDPOINTS_PATH;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import cz.muni.ics.ga4gh.service.JWTSigningAndValidationService;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(originPatterns = "*")
@RestController
@RequestMapping(PUBLIC_ENDPOINTS_PATH)
public class JwkSetPublishingEndpoint {

    public static final String URL = "jwk";

    private final JWTSigningAndValidationService jwtService;

    @Autowired
    public JwkSetPublishingEndpoint(JWTSigningAndValidationService jwtService) {
        this.jwtService = jwtService;
    }

    @RequestMapping(value = "/" + URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getJwk() {
        // map from key id to key
        Map<String, JWK> keys = jwtService.getPublicKeys();
        JWKSet jwkSet = new JWKSet(new ArrayList<>(keys.values()));
        return jwkSet.toString();
    }
}
