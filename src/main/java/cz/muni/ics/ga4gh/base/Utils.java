package cz.muni.ics.ga4gh.base;

import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.ASSERTED;
import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.BY;
import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.CONDITIONS;
import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.SOURCE;
import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.TYPE;
import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.VALUE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import cz.muni.ics.ga4gh.base.model.ClaimRepositoryHeader;
import cz.muni.ics.ga4gh.base.model.Ga4ghClaimRepository;
import cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa;
import cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisaV1;
import cz.muni.ics.ga4gh.base.properties.BrokerInstanceProperties;
import cz.muni.ics.ga4gh.base.properties.Ga4ghClaimRepositoryProperties;
import cz.muni.ics.ga4gh.service.JWTSigningAndValidationService;
import cz.muni.ics.ga4gh.service.impl.VisaAssemblyParameters;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class Utils {

    public static void initializeClaimRepositories(BrokerInstanceProperties brokerProperties,
                                                   List<Ga4ghClaimRepository> claimRepositories,
                                                   Map<URI, RemoteJWKSet<SecurityContext>> remoteJwkSets,
                                                   Map<URI, String> signers)
    {
        for (Ga4ghClaimRepositoryProperties ga4ghClaimRepositoryProperties : brokerProperties.getPassportRepositories()) {
            initializeClaimRepository(ga4ghClaimRepositoryProperties, claimRepositories);
            initializeSigner(signers, remoteJwkSets, ga4ghClaimRepositoryProperties.getName(), ga4ghClaimRepositoryProperties.getJwks());
        }
    }

    public static Ga4ghPassportVisa createVisa(VisaAssemblyParameters parameters)
    {
        long now = System.currentTimeMillis() / 1000L;

        if (parameters.getAsserted() > now) {
            log.warn("Visa asserted in future, it will be ignored!");
            log.debug("Visa information: {}", parameters);
            return null;
        }

        if (parameters.getExpires() <= now) {
            log.warn("Visa is expired, it will be ignored!");
            log.debug("Visa information: {}", parameters);
            return null;
        }

        Ga4ghPassportVisaV1 ga4ghVisaV1 = new Ga4ghPassportVisaV1();
        ga4ghVisaV1.setAsserted(parameters.getAsserted());
        ga4ghVisaV1.setBy(parameters.getBy());
        ga4ghVisaV1.setValue(parameters.getValue());
        ga4ghVisaV1.setType(parameters.getType());
        ga4ghVisaV1.setSource(parameters.getSource());
        ga4ghVisaV1.setConditions(parameters.getConditions());

        Ga4ghPassportVisa visa = new Ga4ghPassportVisa();
        visa.setKid(parameters.getJwtService().getSignerKeyId());
        visa.setTyp(JOSEObjectType.JWT);
        visa.setJku(parameters.getJku());

        visa.setIss(parameters.getIssuer());
        visa.setIat(new Date());
        visa.setExp(new Date(parameters.getExpires() * 1000L));
        visa.setSub(parameters.getSub());
        visa.setJti(UUID.randomUUID().toString());
        visa.setGa4ghVisaV1(ga4ghVisaV1);

        visa.setVerified(true);
        visa.setSigner(parameters.getSigner());
        visa.generateSignedJwt(parameters.getJwtService());

        return visa;
    }

    public static Ga4ghPassportVisa parseVisa(String jwtString) {
        try {
            SignedJWT signedJWT = (SignedJWT) JWTParser.parse(jwtString);
            return parseVisa(signedJWT);
        } catch (Exception ex) {
            log.error("Visa '{}' cannot be parsed", jwtString, ex);
        }
        return null;
    }

    public static Ga4ghPassportVisa parseVisa(SignedJWT jwt)
        throws ParseException, JsonProcessingException
    {
        JWSHeader header = jwt.getHeader();
        JWTClaimsSet payloadClaimSet = jwt.getJWTClaimsSet();

        Ga4ghPassportVisa visa = new Ga4ghPassportVisa();

        JsonNode visaPayload = new ObjectMapper().readValue(jwt.getPayload().toString(), JsonNode.class);
        JsonNode visaV1 = visaPayload.path(Ga4ghPassportVisaV1.GA4GH_VISA_V1);
        visa.setVerified(checkVisaHeader(header) && checkVisaValue(visaV1));

        if (!visa.isVerified()) {
            log.debug("Visa '{}' (payload '{}') did not pass verification", jwt, visaPayload);
            return visa;
        }

        visa.setKid(header.getKeyID());
        visa.setJku(header.getJWKURL());
        visa.setTyp(header.getType());

        visa.setSub(payloadClaimSet.getSubject());
        visa.setIss(payloadClaimSet.getIssuer());
        visa.setIat(payloadClaimSet.getIssueTime());
        visa.setExp(payloadClaimSet.getExpirationTime());
        visa.setJti(payloadClaimSet.getJWTID());
        visa.setGa4ghVisaV1(parseVisaV1(visaV1));
        visa.setLinkedIdentity(Utils.constructLinkedIdentity(visa.getSub(), visa.getIss()));
        visa.setJwt(jwt);

        return visa;
    }

    private static boolean checkVisaExpiration(Ga4ghPassportVisa visa) {
        long exp = visa.getExp().getTime();
        boolean expirationValid = exp > Instant.now().getEpochSecond();
        if (!expirationValid) {
            log.warn("Visa did not pass expiration validation. Visa expired on '{}'.", isoDateTime(exp));
        }
        return expirationValid;
    }

    public static Ga4ghPassportVisaV1 parseVisaV1(JsonNode visaV1Json) {
        Ga4ghPassportVisaV1 visaV1 = new Ga4ghPassportVisaV1();
        visaV1.setAsserted(visaV1Json.path(ASSERTED).longValue());
        visaV1.setSource(visaV1Json.path(SOURCE).textValue());
        visaV1.setType(visaV1Json.path(TYPE).textValue());
        visaV1.setValue(visaV1Json.path(VALUE).textValue());
        if (visaV1Json.hasNonNull(BY)) {
            visaV1.setBy(visaV1Json.path(BY).textValue());
        }
        if (visaV1Json.hasNonNull(CONDITIONS)) {
            visaV1.setConditions(visaV1Json.get(CONDITIONS));
        }
        return visaV1;
    }

    private static boolean checkVisaHeader(JWSHeader visaHeader) {
        boolean valid = visaHeader.getJWKURL() != null
            && visaHeader.getType() != null
            && StringUtils.hasText(visaHeader.getKeyID());
        if (!valid) {
            log.debug("Visa header did not pass verification");
        }
        return valid;
    }

    private static boolean checkVisaValue(JsonNode visaV1) {
        if (visaV1.isMissingNode() || visaV1.isNull() || visaV1.isEmpty()) {
            log.warn("Visa value ({}) is not present. Visa did not pass value verification.",
                Ga4ghPassportVisaV1.GA4GH_VISA_V1);
            return false;
        }
        boolean keysValid = checkKeyPresence(visaV1, TYPE)
            && checkKeyPresence(visaV1, Ga4ghPassportVisa.ASSERTED)
            && checkKeyPresence(visaV1, Ga4ghPassportVisa.VALUE)
            && checkKeyPresence(visaV1, Ga4ghPassportVisa.SOURCE);
        if (!keysValid) {
            log.debug("Visa value did not pass verification of required keys presence.");
        }
        return keysValid;
    }

    private static boolean checkKeyPresence(JsonNode jsonNode, String key) {
        if (jsonNode.path(key).isMissingNode()) {
            log.warn("Key '{}' is missing in the Visa.", key);
            return false;
        }
        return true;
    }

    public static void verifyVisa(Ga4ghPassportVisa visa,
                                  Map<URI, String> signers,
                                  Map<URI, RemoteJWKSet<SecurityContext>> remoteJwkSets)
    {
        SignedJWT jwt = visa.getJwt();
        try {
            boolean expirationVerified = checkVisaExpiration(visa);
            if (!expirationVerified) {
                log.warn("Visa is expired, Visa verification failed");
                visa.setVerified(false);
                return;
            }

            URI jku = visa.getJku();
            if (jku == null) {
                log.warn("JKU is missing in Visa, verification did not pass ");
                visa.setVerified(false);
                return;
            }

            String signer = signers.getOrDefault(jku, null);
            if (signer == null) {
                log.warn("No signer found for JKU '{}, Visa verification failed'", jku);
                visa.setVerified(false);
                return;
            } else {
                visa.setSigner(signer);
            }

            RemoteJWKSet<SecurityContext> remoteJWKSet = remoteJwkSets.getOrDefault(jku, null);
            if (remoteJWKSet == null) {
                log.error("Trusted key sets does not contain JKU '{}', Visa verification failed", jku);
                visa.setVerified(false);
                return;
            }

            List<JWK> keys = remoteJWKSet.get(new JWKSelector(
                new JWKMatcher.Builder().keyID(jwt.getHeader().getKeyID()).build()), null);
            RSASSAVerifier verifier = new RSASSAVerifier(((RSAKey) keys.get(0)).toRSAPublicKey());
            boolean signatureVerified = jwt.verify(verifier);
            if (!signatureVerified) {
                log.warn("Visa signature verification failed, Visa verification failed");
                visa.setVerified(false);
                return;
            }
            visa.setVerified(true);
        } catch (Exception ex) {
            log.error("Visa '{}' cannot be verified", jwt, ex);
        }
    }

    public static long getOneYearExpires(long asserted) {
        return getExpires(asserted, 1L);
    }

    public static long getExpires(long asserted, long addYears) {
        return Instant.ofEpochSecond(asserted).atZone(ZoneId.systemDefault()).plusYears(addYears).toEpochSecond();
    }

    private static void initializeSigner(Map<URI, String> signers,
                                         Map<URI, RemoteJWKSet<SecurityContext>> remoteJwkSets,
                                         String name,
                                         String jwks)
    {
        try {
            URL jku = new URL(jwks);
            remoteJwkSets.put(jku.toURI(), new RemoteJWKSet<>(jku));
            signers.put(jku.toURI(), name);

            log.info("JWKS Signer '{}' added with keys '{}'", name, jwks);
        } catch (MalformedURLException | URISyntaxException e) {
            log.error("cannot add to RemoteJWKSet map: '{}' -> '{}'", name, jwks, e);
        }
    }

    private static void initializeClaimRepository(
        Ga4ghClaimRepositoryProperties ga4ghClaimRepositoryProperties, List<Ga4ghClaimRepository> claimRepositories) {
        String name = ga4ghClaimRepositoryProperties.getName();
        String actionURL = ga4ghClaimRepositoryProperties.getUrl();
        List<ClaimRepositoryHeader> headers = ga4ghClaimRepositoryProperties.getHeaders();

        if (actionURL == null || headers.isEmpty()) {
            log.error("claim repository '{}' not defined with url|auth_header|auth_value",
                ga4ghClaimRepositoryProperties);
            return;
        }

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(
                new InterceptingClientHttpRequestFactory(restTemplate.getRequestFactory(), getClientHttpRequestInterceptors(headers))
        );

        claimRepositories.add(new Ga4ghClaimRepository(name, actionURL, restTemplate));
        log.info("GA4GH Claims Repository '{}' configured at '{}'", name, actionURL);
    }

    private static String isoDate(long linuxTime) {
        return isoFormat(linuxTime, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static String isoDateTime(long linuxTime) {
        return isoFormat(linuxTime, DateTimeFormatter.ISO_DATE_TIME);
    }

    private static String isoFormat(long linuxTime, DateTimeFormatter formatter) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(linuxTime), ZoneId.systemDefault());
        return formatter.format(zdt);
    }

    private static List<ClientHttpRequestInterceptor> getClientHttpRequestInterceptors(List<ClaimRepositoryHeader> headers) {
        return new ArrayList<>(headers);
    }

    public static String constructLinkedIdentity(String sub, String iss) {
        return URLEncoder.encode(sub, StandardCharsets.UTF_8)
            + ','
            + URLEncoder.encode(iss, StandardCharsets.UTF_8);
    }

}
