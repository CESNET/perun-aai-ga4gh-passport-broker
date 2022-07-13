package cz.muni.ics.ga4gh.service.impl.brokers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import cz.muni.ics.ga4gh.adapters.PerunAdapter;
import cz.muni.ics.ga4gh.config.BrokerConfig;
import cz.muni.ics.ga4gh.config.Ga4ghConfig;
import cz.muni.ics.ga4gh.model.Affiliation;
import cz.muni.ics.ga4gh.model.Ga4ghClaimRepository;
import cz.muni.ics.ga4gh.model.Ga4ghPassportVisa;
import cz.muni.ics.ga4gh.service.JWTSigningAndValidationService;
import cz.muni.ics.ga4gh.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
public abstract class Ga4ghBroker {

    public static final String GA4GH_CLAIM = "ga4gh_passport_v1";
    public static final String JSON = "json";

    protected final List<Ga4ghClaimRepository> claimRepositories = new ArrayList<>();
    protected final Map<URI, RemoteJWKSet<SecurityContext>> remoteJwkSets = new HashMap<>();
    protected final Map<URI, String> signers = new HashMap<>();
    protected final ObjectMapper mapper = new ObjectMapper();

    private final String issuer;
    private final URI jku;
    protected PerunAdapter adapter;

    protected JWTSigningAndValidationService jwtService;

    private final String affiliationsAttr;

    private final String orgUrlAttr;

    public Ga4ghBroker(PerunAdapter adapter, JWTSigningAndValidationService jwtService, Ga4ghConfig config, BrokerConfig brokerConfig) throws URISyntaxException, MalformedURLException {
        this.adapter = adapter;
        this.jwtService = jwtService;
        this.affiliationsAttr = brokerConfig.getAffiliationsAttr();
        this.orgUrlAttr = brokerConfig.getOrgUrlAttr();
        this.issuer = brokerConfig.getIssuer();
        this.jku = new URL(brokerConfig.getJku()).toURI();

        Utils.parseConfigFile(config, claimRepositories, remoteJwkSets, signers);
    }

    public ArrayNode constructGa4ghPassportVisa(Long userId, String sub) {
        List<Affiliation> affiliations = adapter.getAdapterRpc().getUserExtSourcesAffiliations(userId, affiliationsAttr, orgUrlAttr);

        ArrayNode ga4gh_passport_v1 = JsonNodeFactory.instance.arrayNode();
        long now = Instant.now().getEpochSecond();

        addAffiliationAndRoles(now, ga4gh_passport_v1, affiliations, sub, userId);
        addAcceptedTermsAndPolicies(now, ga4gh_passport_v1, userId, sub);
        addResearcherStatuses(now, ga4gh_passport_v1, affiliations, sub, userId);
        addControlledAccessGrants(now, ga4gh_passport_v1, sub, userId);

        return ga4gh_passport_v1;
    }


    protected abstract void addAffiliationAndRoles(long now, ArrayNode passport, List<Affiliation> affiliations, String sub, Long userId);

    protected abstract void addAcceptedTermsAndPolicies(long now, ArrayNode passport, Long userId, String sub);

    protected abstract void addResearcherStatuses(long now, ArrayNode passport, List<Affiliation> affiliations, String sub, Long userId);

    protected abstract void addControlledAccessGrants(long now, ArrayNode passport, String sub, Long userId);

    protected JsonNode createPassportVisa(String type, String sub, Long userId, String value, String source,
                                          String by, long asserted, long expires, JsonNode condition)
    {
        long now = System.currentTimeMillis() / 1000L;

        if (asserted > now) {
            log.warn("Visa asserted in future, it will be ignored!");
            log.debug("Visa information: perunUserId={}, sub={}, type={}, value={}, source={}, by={}, asserted={}",
                    userId, sub, type, value, source, by, Instant.ofEpochSecond(asserted));

            return null;
        }

        if (expires <= now) {
            log.warn("Visa is expired, it will be ignored!");
            log.debug("Visa information: perunUserId={}, sub={}, type={}, value={}, source={}, by={}, expired={}",
                    userId, sub, type, value, source, by, Instant.ofEpochSecond(expires));

            return null;
        }

        Map<String, Object> passportVisaObject = new HashMap<>();
        passportVisaObject.put(Ga4ghPassportVisa.TYPE, type);
        passportVisaObject.put(Ga4ghPassportVisa.ASSERTED, asserted);
        passportVisaObject.put(Ga4ghPassportVisa.VALUE, value);
        passportVisaObject.put(Ga4ghPassportVisa.SOURCE, source);
        passportVisaObject.put(Ga4ghPassportVisa.BY, by);

        if (condition != null && !condition.isNull() && !condition.isMissingNode()) {
            passportVisaObject.put(Ga4ghPassportVisa.CONDITION, condition);
        }

        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.parse(jwtService.getDefaultSigningAlgorithm().getName()))
                .keyID(jwtService.getDefaultSignerKeyId())
                .type(JOSEObjectType.JWT)
                .jwkURL(jku)
                .build();

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .issueTime(new Date())
                .expirationTime(new Date(expires * 1000L))
                .subject(sub)
                .jwtID(UUID.randomUUID().toString())
                .claim(Ga4ghPassportVisa.GA4GH_VISA_V1, passportVisaObject)
                .build();

        SignedJWT myToken = new SignedJWT(jwsHeader, jwtClaimsSet);
        jwtService.signJwt(myToken);

        return JsonNodeFactory.instance.textNode(myToken.serialize());
    }

    protected void callPermissionsJwtAPI(Ga4ghClaimRepository repo,
                                         Map<String, String> uriVariables,
                                         ArrayNode passport,
                                         Set<String> linkedIdentities)
    {
        log.debug("GA4GH: {}", uriVariables);
        JsonNode response = callHttpJsonAPI(repo, uriVariables);
        if (response != null) {
            JsonNode visas = response.path(GA4GH_CLAIM);
            if (visas.isArray()) {
                for (JsonNode visaNode : visas) {
                    if (visaNode.isTextual()) {
                        Ga4ghPassportVisa visa = Utils.parseAndVerifyVisa(visaNode.asText(), signers, remoteJwkSets, mapper);
                        if (visa.isVerified()) {
                            log.debug("Adding a visa to passport: {}", visa);
                            passport.add(passport.textNode(visa.getJwt()));
                            linkedIdentities.add(visa.getLinkedIdentity());
                        } else {
                            log.warn("Skipping visa: {}", visa);
                        }
                    } else {
                        log.warn("Element of {} is not a String: {}", GA4GH_CLAIM, visaNode);
                    }
                }
            } else {
                log.warn("{} is not an array in {}", GA4GH_CLAIM, response);
            }
        }
    }

    @SuppressWarnings("Duplicates")
    private static JsonNode callHttpJsonAPI(Ga4ghClaimRepository repo, Map<String, String> uriVariables) {
        //get permissions data
        try {
            JsonNode result;
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Calling Permissions API at {}", repo.getRestTemplate().getUriTemplateHandler().expand(repo.getActionURL(), uriVariables));
                }

                result = repo.getRestTemplate().getForObject(repo.getActionURL(), JsonNode.class, uriVariables);
            } catch (HttpClientErrorException ex) {
                MediaType contentType = ex.getResponseHeaders().getContentType();
                String body = ex.getResponseBodyAsString();

                log.error("HTTP ERROR: {}, URL: {}, Content-Type: {}", ex.getRawStatusCode(), repo.getActionURL(), contentType);

                if (ex.getRawStatusCode() == 404) {
                    log.warn("Got status 404 from Permissions endpoint {}, ELIXIR AAI user is not linked to user at Permissions API",
                            repo.getActionURL());

                    return null;
                }

                if (JSON.equals(contentType.getSubtype())) {
                    try {
                        log.error(new ObjectMapper().readValue(body, JsonNode.class).path("message").asText());
                    } catch (IOException e) {
                        log.error("cannot parse error message from JSON", e);
                    }
                } else {
                    log.error("cannot make REST call, exception: {} message: {}", ex.getClass().getName(), ex.getMessage());
                }

                return null;
            }
            log.debug("Permissions API response: {}", result);

            return result;
        } catch (Exception ex) {
            log.error("Cannot get dataset permissions", ex);
        }

        return null;
    }
}
