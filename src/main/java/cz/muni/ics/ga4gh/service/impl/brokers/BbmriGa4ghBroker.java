package cz.muni.ics.ga4gh.service.impl.brokers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import cz.muni.ics.ga4gh.adapters.PerunAdapter;
import cz.muni.ics.ga4gh.config.BrokerConfig;
import cz.muni.ics.ga4gh.config.Ga4ghConfig;
import cz.muni.ics.ga4gh.model.Affiliation;
import cz.muni.ics.ga4gh.model.Ga4ghClaimRepository;
import cz.muni.ics.ga4gh.service.JWTSigningAndValidationService;
import cz.muni.ics.ga4gh.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cz.muni.ics.ga4gh.model.Ga4ghPassportVisa.BY_PEER;
import static cz.muni.ics.ga4gh.model.Ga4ghPassportVisa.BY_SELF;
import static cz.muni.ics.ga4gh.model.Ga4ghPassportVisa.BY_SO;
import static cz.muni.ics.ga4gh.model.Ga4ghPassportVisa.BY_SYSTEM;
import static cz.muni.ics.ga4gh.model.Ga4ghPassportVisa.TYPE_ACCEPTED_TERMS_AND_POLICIES;
import static cz.muni.ics.ga4gh.model.Ga4ghPassportVisa.TYPE_AFFILIATION_AND_ROLE;
import static cz.muni.ics.ga4gh.model.Ga4ghPassportVisa.TYPE_LINKED_IDENTITIES;
import static cz.muni.ics.ga4gh.model.Ga4ghPassportVisa.TYPE_RESEARCHER_STATUS;

@Service
@Profile("bbmri")
public class BbmriGa4ghBroker extends Ga4ghBroker {

    private static final String BONA_FIDE_URL = "https://doi.org/10.1038/s41431-018-0219-y";
    private static final String BBMRI_ERIC_ORG_URL = "https://www.bbmri-eric.eu/";
    private static final String BBMRI_ID = "bbmri_id";
    private static final String FACULTY_AT = "faculty@";

    private final String bonaFideStatusAttr;
    private final String groupAffiliationsAttr;
    private final Long termsAndPoliciesGroupId;

    @Autowired
    public BbmriGa4ghBroker(BrokerConfig brokerConfig, PerunAdapter adapter, JWTSigningAndValidationService jwtService, Ga4ghConfig ga4ghConfig) throws URISyntaxException, MalformedURLException {
        super(adapter, jwtService, ga4ghConfig, brokerConfig);

        bonaFideStatusAttr = brokerConfig.getBonaFideStatusAttr();
        groupAffiliationsAttr = brokerConfig.getGroupAffiliationsAttr();
        termsAndPoliciesGroupId = Objects.requireNonNullElse(brokerConfig.getTermsAndPoliciesGroupId(), 10432L);
    }

    @Override
    protected void addAffiliationAndRoles(long now, ArrayNode passport, List<Affiliation> affiliations, String sub, Long userId)
    {
        //by=system for users with affiliation asserted by their IdP (set in UserExtSource attribute "affiliation")
        if (affiliations == null) {
            return;
        }

        for (Affiliation affiliation: affiliations) {
            //expires 1 year after the last login from the IdP asserting the affiliation
            long expires = Utils.getOneYearExpires(affiliation.getAsserted());
            if (expires < now) {
                continue;
            }

            JsonNode visa = createPassportVisa(TYPE_AFFILIATION_AND_ROLE, sub, userId, affiliation.getValue(), affiliation.getSource(), BY_SYSTEM, affiliation.getAsserted(), expires, null);
            if (visa != null) {
                passport.add(visa);
            }
        }
    }

    @Override
    protected void addAcceptedTermsAndPolicies(long now, ArrayNode passport, Long userId, String sub) {
        //by=self for members of the group 10432 "Bona Fide Researchers"
        boolean userInGroup = adapter.isUserInGroup(userId, termsAndPoliciesGroupId);
        if (!userInGroup) {
            return;
        }

        long asserted = now;
        if (bonaFideStatusAttr != null) {
            String bonaFideStatusCreatedAt = adapter.getAdapterRpc().getUserAttributeCreatedAt(userId, bonaFideStatusAttr);
            if (bonaFideStatusCreatedAt != null) {
                asserted = Timestamp.valueOf(bonaFideStatusCreatedAt).getTime() / 1000L;
            }
        }

        long expires = Utils.getExpires(asserted, 100L);
        if (expires < now) {
            return;
        }

        JsonNode visa = createPassportVisa(TYPE_ACCEPTED_TERMS_AND_POLICIES, sub, userId, BONA_FIDE_URL, BBMRI_ERIC_ORG_URL, BY_SELF, asserted, expires, null);
        if (visa != null) {
            passport.add(visa);
        }
    }

    @Override
    protected void addResearcherStatuses(long now, ArrayNode passport, List<Affiliation> affiliations, String sub, Long userId)
    {
        addResearcherStatusFromBonaFideAttribute(now, passport, userId, sub);
        addResearcherStatusFromAffiliation(affiliations, now, passport, sub, userId);
        addResearcherStatusGroupAffiliations(now, passport, sub, userId);
    }

    @Override
    protected void addControlledAccessGrants(long now, ArrayNode passport, String sub, Long userId) {
        if (claimRepositories.isEmpty()) {
            return;
        }

        Set<String> linkedIdentities = new HashSet<>();
        for (Ga4ghClaimRepository repo: claimRepositories) {
            callPermissionsJwtAPI(repo, Collections.singletonMap(BBMRI_ID, sub), passport, linkedIdentities);
        }

        if (linkedIdentities.isEmpty()) {
            return;
        }

        for (String linkedIdentity : linkedIdentities) {
            long expires = Utils.getOneYearExpires(now);

            JsonNode visa = createPassportVisa(TYPE_LINKED_IDENTITIES, sub, userId, linkedIdentity, BBMRI_ERIC_ORG_URL, BY_SYSTEM, now, expires, null);
            if (visa != null) {
                passport.add(visa);
            }
        }
    }

    private void addResearcherStatusFromBonaFideAttribute(long now, ArrayNode passport, Long userId, String sub)
    {
        //by=peer for users with attribute elixirBonaFideStatusREMS
        String bbmriBonaFideStatusCreatedAt = adapter.getAdapterRpc().getUserAttributeCreatedAt(userId, bonaFideStatusAttr);

        if (bbmriBonaFideStatusCreatedAt == null) {
            return;
        }

        long asserted = Timestamp.valueOf(bbmriBonaFideStatusCreatedAt).getTime() / 1000L;
        long expires = Utils.getOneYearExpires(asserted);

        if (expires > now) {
            JsonNode visa = createPassportVisa(TYPE_RESEARCHER_STATUS, sub, userId, BONA_FIDE_URL, BBMRI_ERIC_ORG_URL, BY_PEER, asserted, expires, null);

            if (visa != null) {
                passport.add(visa);
            }
        }
    }

    private void addResearcherStatusFromAffiliation(List<Affiliation> affiliations, long now, ArrayNode passport, String sub, Long userId)
    {
        //by=system for users with faculty affiliation asserted by their IdP (set in UserExtSource attribute "affiliation")
        if (affiliations == null) {
            return;
        }

        for (Affiliation affiliation: affiliations) {
            if (!StringUtils.startsWithIgnoreCase(affiliation.getValue(), FACULTY_AT)) {
                continue;
            }

            long expires = Utils.getOneYearExpires(affiliation.getAsserted());
            if (expires < now) {
                continue;
            }

            JsonNode visa = createPassportVisa(TYPE_RESEARCHER_STATUS, sub, userId, BONA_FIDE_URL, affiliation.getSource(), BY_SYSTEM, affiliation.getAsserted(), expires, null);
            if (visa != null) {
                passport.add(visa);
            }
        }
    }

    private void addResearcherStatusGroupAffiliations(long now, ArrayNode passport, String sub, Long userId) {
        //by=so for users with faculty affiliation asserted by membership in a group with groupAffiliations attribute
        List<Affiliation> groupAffiliations = adapter.getGroupAffiliations(userId, groupAffiliationsAttr);
        if (groupAffiliations == null) {
            return;
        }

        for (Affiliation affiliation: groupAffiliations) {
            if (!StringUtils.startsWithIgnoreCase(affiliation.getValue(), FACULTY_AT)) {
                continue;
            }

            long expires = Utils.getOneYearExpires(now);

            JsonNode visa = createPassportVisa(TYPE_RESEARCHER_STATUS, sub, userId, BONA_FIDE_URL, BBMRI_ERIC_ORG_URL, BY_SO, affiliation.getAsserted(), expires, null);
            if (visa != null) {
                passport.add(visa);
            }
        }
    }
}
