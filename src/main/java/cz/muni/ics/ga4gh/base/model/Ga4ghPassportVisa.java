package cz.muni.ics.ga4gh.base.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import cz.muni.ics.ga4gh.service.JWTSigningAndValidationService;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class Ga4ghPassportVisa {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String TYPE_AFFILIATION_AND_ROLE = "AffiliationAndRole";
    public static final String TYPE_ACCEPTED_TERMS_AND_POLICIES = "AcceptedTermsAndPolicies";
    public static final String TYPE_RESEARCHER_STATUS = "ResearcherStatus";
    public static final String TYPE_LINKED_IDENTITIES = "LinkedIdentities";
    public static final String TYPE_CONTROLLED_ACCESS_GRANTS = "ControlledAccessGrants";

    public static final String BY_SYSTEM = "system";
    public static final String BY_SO = "so";
    public static final String BY_PEER = "peer";
    public static final String BY_SELF = "self";

    public static final String SUB = "sub";
    public static final String ISS = "iss";
    public static final String IAT = "iat";
    public static final String EXP = "exp";
    public static final String JTI = "jti";
    public static final String TYPE = "type";
    public static final String ASSERTED = "asserted";
    public static final String VALUE = "value";
    public static final String SOURCE = "source";
    public static final String BY = "by";
    public static final String CONDITIONS = "conditions";

    // === VISA HEADER FIELDS ===
    private String kid;

    private JOSEObjectType typ;

    private URI jku;

    // === VISA PAYLOAD FIELDS ===

    private String iss;

    private String sub;

    private Date iat;

    private Date exp;

    private String jti;

    // value of the visa
    private Ga4ghPassportVisaV1 ga4ghVisaV1;

    // === CUSTOM FIELDS FOR WORKING WITH VISA ===

    @ToString.Exclude
    private String signer = null;

    private boolean verified = false;

    private String linkedIdentity;
    private SignedJWT jwt;

    public void generateSignedJwt(JWTSigningAndValidationService jwtService) {
        Map<String, Object> passportVisaObject = new HashMap<>();
        passportVisaObject.put(TYPE, ga4ghVisaV1.getType());
        passportVisaObject.put(Ga4ghPassportVisa.ASSERTED, ga4ghVisaV1.getAsserted());
        passportVisaObject.put(Ga4ghPassportVisa.VALUE, ga4ghVisaV1.getValue());
        passportVisaObject.put(Ga4ghPassportVisa.SOURCE, ga4ghVisaV1.getSource());
        passportVisaObject.put(Ga4ghPassportVisa.BY, ga4ghVisaV1.getBy());

        if (ga4ghVisaV1.getConditions() != null) {
            passportVisaObject.put(Ga4ghPassportVisa.CONDITIONS, ga4ghVisaV1.getConditions());
        }

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
            .issuer(iss)
            .issueTime(iat)
            .expirationTime(exp)
            .subject(sub)
            .jwtID(jti)
            .claim(Ga4ghPassportVisaV1.GA4GH_VISA_V1, passportVisaObject)
            .build();

        JWSHeader
            jwsHeader = new JWSHeader.Builder(JWSAlgorithm.parse(jwtService.getSigningAlgorithm().getName()))
            .keyID(jwtService.getSignerKeyId())
            .type(JOSEObjectType.JWT)
            .jwkURL(jku)
            .build();

        SignedJWT signedVisaJwt = new SignedJWT(jwsHeader, jwtClaimsSet);
        jwtService.signJwt(signedVisaJwt);
        this.jwt = signedVisaJwt;
    }

    public String serialize() {
        return jwt.serialize();
    }

}
