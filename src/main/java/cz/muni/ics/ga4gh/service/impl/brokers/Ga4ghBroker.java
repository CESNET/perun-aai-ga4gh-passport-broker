package cz.muni.ics.ga4gh.service.impl.brokers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import cz.muni.ics.ga4gh.base.Utils;
import cz.muni.ics.ga4gh.base.adapters.PerunAdapter;
import cz.muni.ics.ga4gh.base.model.Affiliation;
import cz.muni.ics.ga4gh.base.model.Ga4ghClaimRepository;
import cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa;
import cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisaV1;
import cz.muni.ics.ga4gh.base.properties.BrokerInstanceProperties;
import cz.muni.ics.ga4gh.base.properties.Ga4ghBrokersProperties;
import cz.muni.ics.ga4gh.service.JWTSigningAndValidationService;
import cz.muni.ics.ga4gh.service.PassportAssemblyContext;
import cz.muni.ics.ga4gh.service.impl.VisaAssemblyParameters;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
public abstract class Ga4ghBroker {

    public static final String GA4GH_CLAIM = "ga4gh_passport_v1";
    public static final String JSON = "json";

    public static final String REPOSITORY_URL_USER_ID = "user_id";

    protected final List<Ga4ghClaimRepository> claimRepositories = new ArrayList<>();
    protected final Map<URI, RemoteJWKSet<SecurityContext>> remoteJwkSets = new HashMap<>();
    protected final Map<URI, String> signers = new HashMap<>();

    protected final String issuer;
    protected final URI jku;

    @Getter
    private final String brokerName;

    protected PerunAdapter adapter;

    protected JWTSigningAndValidationService jwtService;

    private final String affiliationsAttr;

    private final String orgUrlAttr;

    public Ga4ghBroker(BrokerInstanceProperties instanceProperties,
                       Ga4ghBrokersProperties brokersProperties,
                       PerunAdapter adapter,
                       JWTSigningAndValidationService jwtService)
    {
        this.adapter = adapter;
        this.affiliationsAttr = instanceProperties.getAffiliationsAttr();
        this.orgUrlAttr = instanceProperties.getOrgUrlAttr();
        this.issuer = brokersProperties.getIssuer();
        this.jku = brokersProperties.getJku();
        this.jwtService = jwtService;
        this.brokerName = instanceProperties.getName();
        Utils.initializeClaimRepositories(instanceProperties, claimRepositories, remoteJwkSets, signers);
    }

    public List<Ga4ghPassportVisa> constructGa4ghPassportVisas(Long userId) {
        if (!isCommunityMember(userId)) {
            return new ArrayList<>();
        }

        String communityIdentifier = getUserSub(userId);

        List<Affiliation> identityAffiliations = new ArrayList<>();
        List<Ga4ghPassportVisa> controlledAccessGrantsFromRepositories = new ArrayList<>();
        Set<String> linkedIdentitiesFromRepositories = new HashSet<>();

        fillIdentityAffiliations(identityAffiliations, userId);
        callExternalRepositories(communityIdentifier, controlledAccessGrantsFromRepositories, linkedIdentitiesFromRepositories);

        long now = Instant.now().getEpochSecond();

        PassportAssemblyContext ctx = PassportAssemblyContext.builder()
            .resultVisas(new ArrayList<>())
            .perunAdapter(adapter)
            .identityAffiliations(identityAffiliations)
            .externalControlledAccessGrants(controlledAccessGrantsFromRepositories)
            .externalLinkedIdentities(linkedIdentitiesFromRepositories)
            .perunUserId(userId)
            .subject(communityIdentifier)
            .now(now)
            .build();

        addAffiliationAndRoles(ctx);
        addAcceptedTermsAndPolicies(ctx);
        addResearcherStatuses(ctx);
        addControlledAccessGrants(ctx);
        addLinkedIdentities(ctx);

        return ctx.getResultVisas();
    }

    private void callExternalRepositories(String sub,
                                          List<Ga4ghPassportVisa> controlledAccessGrantsFromRepositories,
                                          Set<String> linkedIdentitiesFromRepositories)
    {
        for (Ga4ghClaimRepository repository: claimRepositories) {
            callPermissionsJwtAPI(
                repository,
                Collections.singletonMap(REPOSITORY_URL_USER_ID, sub),
                controlledAccessGrantsFromRepositories,
                linkedIdentitiesFromRepositories
            );
        }

    }

    private void fillIdentityAffiliations(List<Affiliation> affiliationsList, Long userId) {
        if (affiliationsList == null) {
            affiliationsList = new ArrayList<>();
        }
        if (!StringUtils.hasText(affiliationsAttr) || !StringUtils.hasText(orgUrlAttr)) {
            log.warn("Affiliations attribute or orgUrl attribute is not defined");
        } else {
            List<Affiliation> affiliations = adapter.getAdapterRpc()
                .getUserExtSourcesAffiliations(userId, affiliationsAttr, orgUrlAttr);
            if (affiliations != null) {
                affiliationsList.addAll(affiliations);
            }
        }
    }

    protected abstract String getSubAttribute();

    protected abstract Long getCommunityVoId();

    protected abstract void addAffiliationAndRoles(PassportAssemblyContext ctx);

    protected abstract void addAcceptedTermsAndPolicies(PassportAssemblyContext ctx);

    protected abstract void addResearcherStatuses(PassportAssemblyContext ctx);

    protected abstract void addControlledAccessGrants(PassportAssemblyContext ctx);

    protected abstract void addLinkedIdentities(PassportAssemblyContext ctx);

    protected String getUserSub(Long userId) {
        return adapter.getUserSub(userId, getSubAttribute());
    }

    protected boolean isCommunityMember(Long userId) {
        Long communityVoId = getCommunityVoId();
        if (communityVoId == null) {
            return true;
        }
        return adapter.isUserInVo(userId, communityVoId);
    }

    protected void logAddingVisas(String type) {
        log.info("Adding '{}' visas", type);
    }

    protected void logAddedVisa(String type, String value) {
        log.debug("Added '{}' visa with value '{}'", type, value);
    }

    protected Ga4ghPassportVisa createVisa(VisaAssemblyParameters parameters)
    {
        parameters.setIssuer(issuer);
        parameters.setJku(jku);
        parameters.setSigner(issuer);
        parameters.setJwtService(jwtService);
        return Utils.createVisa(parameters);
    }

    protected void callPermissionsJwtAPI(Ga4ghClaimRepository repo,
                                         Map<String, String> uriVariables,
                                         List<Ga4ghPassportVisa> controlledAccessGrantsList,
                                         Set<String> linkedIdentitiesSet)
    {
        log.debug("Calling claim repository '{}' with parameters '{}'", repo, uriVariables);
        JsonNode response = callHttpJsonAPI(repo, uriVariables);
        if (response == null) {
            log.debug("No response returned");
            return;
        } else if (!response.hasNonNull(GA4GH_CLAIM)) {
            log.debug("Response does not contain non null value for key '{}'", GA4GH_CLAIM);
            return;
        }

        JsonNode visas = response.path(GA4GH_CLAIM);
        if (!visas.isArray()) {
            log.warn("'{}' claim is not an array. Received response '{}'", GA4GH_CLAIM, response);
        }
        for (JsonNode visaNode : visas) {
            if (!visaNode.isTextual()) {
                log.warn("Element '{}' of '{}' is not a String, skipping value", visaNode, GA4GH_CLAIM);
                continue;
            }
            Ga4ghPassportVisa visa = Utils.parseVisa(visaNode.asText());
            if (visa == null) {
                log.debug("Visa '{}' could not be parsed", visaNode);
                continue;
            }
            Utils.verifyVisa(visa, signers, remoteJwkSets);
            if (visa.isVerified()) {
                log.debug("Adding a visa to passport: {}", visa);
                controlledAccessGrantsList.add(visa);
                linkedIdentitiesSet.add(visa.getLinkedIdentity());
            } else {
                log.warn("Skipping visa: {}", visa);
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
                    log.debug("Calling Permissions API at {}", repo.getRestTemplate()
                        .getUriTemplateHandler().expand(repo.getActionURL(), uriVariables));
                }

                result = repo.getRestTemplate().getForObject(repo.getActionURL(), JsonNode.class, uriVariables);
            } catch (HttpClientErrorException ex) {
                MediaType contentType = null;
                if (ex.getResponseHeaders() != null) {
                    contentType = ex.getResponseHeaders().getContentType();
                }
                String body = ex.getResponseBodyAsString();

                log.error("HTTP ERROR: {}, URL: {}, Content-Type: {}",
                    ex.getRawStatusCode(), repo.getActionURL(), contentType);

                if (ex.getRawStatusCode() == 404) {
                    log.warn("Got status 404 from Permissions endpoint {}, user is not linked to user at Permissions API",
                            repo.getActionURL());
                    return null;
                }

                if (contentType != null && JSON.equals(contentType.getSubtype())) {
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
