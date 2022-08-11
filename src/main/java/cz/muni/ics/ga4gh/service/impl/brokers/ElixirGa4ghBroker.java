package cz.muni.ics.ga4gh.service.impl.brokers;

import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.BY_PEER;
import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.BY_SELF;
import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.BY_SO;
import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.BY_SYSTEM;
import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.TYPE_ACCEPTED_TERMS_AND_POLICIES;
import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.TYPE_AFFILIATION_AND_ROLE;
import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.TYPE_CONTROLLED_ACCESS_GRANTS;
import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.TYPE_LINKED_IDENTITIES;
import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.TYPE_RESEARCHER_STATUS;

import cz.muni.ics.ga4gh.base.Utils;
import cz.muni.ics.ga4gh.base.adapters.PerunAdapter;
import cz.muni.ics.ga4gh.base.model.Affiliation;
import cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa;
import cz.muni.ics.ga4gh.base.properties.BrokerInstanceProperties;
import cz.muni.ics.ga4gh.base.properties.Ga4ghBrokersProperties;
import cz.muni.ics.ga4gh.service.JWTSigningAndValidationService;
import cz.muni.ics.ga4gh.service.PassportAssemblyContext;
import cz.muni.ics.ga4gh.service.impl.VisaAssemblyParameters;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public class ElixirGa4ghBroker extends Ga4ghBroker {

    private static final String BONA_FIDE_URL = "https://doi.org/10.1038/s41431-018-0219-y";
    private static final String ELIXIR_ORG_URL = "https://elixir-europe.org/";
    private static final String FACULTY_AT = "faculty@";

    private final String elixirIdAttribute;
    private final String bonaFideStatusAttr;
    private final String bonaFideStatusREMSAttr;
    private final String groupAffiliationsAttr;
    private final Long termsAndPoliciesGroupId;

    private final Long elixirVoId;

    public ElixirGa4ghBroker(BrokerInstanceProperties instanceProperties,
                             Ga4ghBrokersProperties brokersProperties,
                             PerunAdapter adapter,
                             JWTSigningAndValidationService jwtService)
    {
        super(instanceProperties, brokersProperties, adapter, jwtService);

        this.elixirIdAttribute = instanceProperties.getIdentifierAttribute();
        this.bonaFideStatusAttr = instanceProperties.getBonaFideStatusAttr();
        this.bonaFideStatusREMSAttr = instanceProperties.getBonaFideStatusRemsAttr();
        this.groupAffiliationsAttr = instanceProperties.getGroupAffiliationsAttr();
        this.termsAndPoliciesGroupId = instanceProperties.getTermsAndPoliciesGroupId();
        this.elixirVoId = instanceProperties.getMembershipVoId();
    }

    @Override
    protected String getSubAttribute() {
        return elixirIdAttribute;
    }

    @Override
    protected Long getCommunityVoId() {
        return elixirVoId;
    }

    @Override
    protected void addAffiliationAndRoles(PassportAssemblyContext ctx)
    {
        String type = TYPE_AFFILIATION_AND_ROLE;
        logAddingVisas(type);
        if (!isCommunityMember(ctx.getPerunUserId())) {
            log.debug("User is not member of the ELIXIR community, not adding any {} visas", type);
            return;
        }
        Affiliation affiliate = new Affiliation(null,
            "affiliate@elixir-europe.org", System.currentTimeMillis() / 1000L);
        Ga4ghPassportVisa affiliateVisa = createVisa(
            VisaAssemblyParameters.builder()
                .type(type)
                .sub(ctx.getSubject())
                .userId(ctx.getPerunUserId())
                .value(affiliate.getValue())
                .source(affiliate.getSource())
                .by(BY_SYSTEM)
                .asserted(affiliate.getAsserted())
                .expires(Utils.getOneYearExpires(affiliate.getAsserted()))
                .conditions(null)
                .build()
        );

        if (affiliateVisa != null) {
            ctx.getResultVisas().add(affiliateVisa);
            logAddedVisa(type, affiliate.getValue());
        }
    }

    @Override
    protected void addAcceptedTermsAndPolicies(PassportAssemblyContext ctx) {
        String type = TYPE_ACCEPTED_TERMS_AND_POLICIES;
        logAddingVisas(type);
        if (termsAndPoliciesGroupId == null) {
            log.debug("Group ID for accepted terms and policies is not defined, not adding any {} visas", type);
            return;
        }

        boolean userInGroup = adapter.isUserInGroup(ctx.getPerunUserId(), termsAndPoliciesGroupId);
        if (!userInGroup) {
            log.debug("User is not in the group representing terms and policies approval, not adding any {} visas", type);
            return;
        }

        long asserted = ctx.getNow();
        if (StringUtils.hasText(bonaFideStatusAttr)) {
            String bonaFideStatusCreatedAt = adapter.getAdapterRpc()
                .getUserAttributeCreatedAt(ctx.getPerunUserId(), bonaFideStatusAttr);
            if (bonaFideStatusCreatedAt != null) {
                asserted = Timestamp.valueOf(bonaFideStatusCreatedAt).getTime() / 1000L;
            }
        }

        long expires = Utils.getExpires(asserted, 100L);

        String value = BONA_FIDE_URL;
        Ga4ghPassportVisa visa = createVisa(
            VisaAssemblyParameters.builder()
                .type(type)
                .sub(ctx.getSubject())
                .userId(ctx.getPerunUserId())
                .value(value)
                .source(ELIXIR_ORG_URL)
                .by(BY_SELF)
                .asserted(asserted)
                .expires(expires)
                .conditions(null)
                .build()
        );

        if (visa != null) {
            ctx.getResultVisas().add(visa);
            logAddedVisa(type, value);
        }
    }

    @Override
    protected void addControlledAccessGrants(PassportAssemblyContext ctx) {
        String type = TYPE_CONTROLLED_ACCESS_GRANTS;
        logAddingVisas(type);
        List<Ga4ghPassportVisa> controlledAccessGrants = ctx.getExternalControlledAccessGrants();
        if (controlledAccessGrants == null || controlledAccessGrants.isEmpty()) {
            log.debug("No external {} visas available, not adding any {} visas", type, type);
            return;
        }
        ctx.getResultVisas().addAll(controlledAccessGrants);
    }

    @Override
    protected void addLinkedIdentities(PassportAssemblyContext ctx) {
        String type = TYPE_LINKED_IDENTITIES;
        logAddingVisas(type);
        Set<String> externalLinkedIdentities = ctx.getExternalLinkedIdentities();
        if (externalLinkedIdentities == null || externalLinkedIdentities.isEmpty()) {
            log.debug("No external {} visas available, not adding any {} visas", type, type);
            return;
        }
        for (String identity: externalLinkedIdentities) {
            Ga4ghPassportVisa visa = createVisa(
                VisaAssemblyParameters.builder()
                    .type(type)
                    .sub(ctx.getSubject())
                    .userId(ctx.getPerunUserId())
                    .value(identity)
                    .source(ELIXIR_ORG_URL)
                    .by(BY_SYSTEM)
                    .asserted(ctx.getNow())
                    .expires(Utils.getOneYearExpires(ctx.getNow()))
                    .conditions(null)
                    .build()
            );
            if (visa != null) {
                ctx.getResultVisas().add(visa);
                logAddedVisa(type, identity);
            }
        }
    }

    @Override
    protected void addResearcherStatuses(PassportAssemblyContext ctx)
    {
        logAddingVisas(TYPE_RESEARCHER_STATUS);
        addResearcherStatusFromRemsBonaFideAttribute(ctx);
        addResearcherStatusFromAffiliation(ctx);
        addResearcherStatusFromGroupAffiliations(ctx);
    }

    private void addResearcherStatusFromRemsBonaFideAttribute(PassportAssemblyContext ctx) {
        String type = TYPE_RESEARCHER_STATUS;
        log.debug("Adding {} visa (from REMS bona fide status)", type);
        if (!StringUtils.hasText(bonaFideStatusREMSAttr)) {
            log.debug("REMS bonaFideStatus attribute is not defined, not adding any {} visas (from REMS bona fide status)", type);
            return;
        }

        String elixirBonaFideStatusREMSCreatedAt = adapter.getAdapterRpc()
            .getUserAttributeCreatedAt(ctx.getPerunUserId(), bonaFideStatusREMSAttr);
        if (elixirBonaFideStatusREMSCreatedAt == null) {
            return;
        }

        long asserted = Timestamp.valueOf(elixirBonaFideStatusREMSCreatedAt).getTime() / 1000L;
        long expires = Utils.getOneYearExpires(asserted);

        String value = BONA_FIDE_URL;
        Ga4ghPassportVisa visa = createVisa(
            VisaAssemblyParameters.builder()
                .type(type)
                .sub(ctx.getSubject())
                .userId(ctx.getPerunUserId())
                .value(value)
                .source(ELIXIR_ORG_URL)
                .by(BY_PEER)
                .asserted(asserted)
                .expires(expires)
                .conditions(null)
                .build()
        );
        if (visa != null) {
            ctx.getResultVisas().add(visa);
            logAddedVisa(type, value);
        }
    }

    private void addResearcherStatusFromAffiliation(PassportAssemblyContext ctx) {
        String type = TYPE_RESEARCHER_STATUS;
        log.debug("Adding {} visa (from affiliations)", type);
        if (ctx.getIdentityAffiliations() == null || ctx.getIdentityAffiliations().isEmpty()) {
            log.debug("No affiliations available, not adding any {} visas (from affiliations)", type);
            return;
        }

        for (Affiliation affiliation: ctx.getIdentityAffiliations()) {
            if (!StringUtils.startsWithIgnoreCase(affiliation.getValue(), FACULTY_AT)) {
                continue;
            }

            String value = BONA_FIDE_URL;
            Ga4ghPassportVisa visa = createVisa(
                VisaAssemblyParameters.builder()
                    .type(type)
                    .sub(ctx.getSubject())
                    .userId(ctx.getPerunUserId())
                    .value(value)
                    .source(affiliation.getSource())
                    .by(BY_SYSTEM)
                    .asserted(affiliation.getAsserted())
                    .expires(Utils.getOneYearExpires(affiliation.getAsserted()))
                    .conditions(null)
                    .build()
            );
            if (visa != null) {
                ctx.getResultVisas().add(visa);
                logAddedVisa(type, value);
            }
        }
    }

    private void addResearcherStatusFromGroupAffiliations(PassportAssemblyContext ctx) {
        String type = TYPE_RESEARCHER_STATUS;
        log.debug("Adding {} visa (from group affiliations)", type);
        if (!StringUtils.hasText(groupAffiliationsAttr)) {
            log.debug("GroupAffiliations attribute is not defined, not adding any {} visas (from group affiliations)", type);
            return;
        }
        List<Affiliation> groupAffiliations = adapter.getGroupAffiliations(
            ctx.getPerunUserId(), elixirVoId, groupAffiliationsAttr);
        if (groupAffiliations == null || groupAffiliations.isEmpty()) {
            return;
        }

        for (Affiliation affiliation: groupAffiliations) {
            if (!StringUtils.startsWithIgnoreCase(affiliation.getValue(), FACULTY_AT)) {
                continue;
            }

            long expires = Utils.getOneYearExpires(ctx.getNow());

            String value = BONA_FIDE_URL;
            Ga4ghPassportVisa visa = createVisa(
                VisaAssemblyParameters.builder()
                    .type(type)
                    .sub(ctx.getSubject())
                    .userId(ctx.getPerunUserId())
                    .value(value)
                    .source(ELIXIR_ORG_URL)
                    .by(BY_SO)
                    .asserted(affiliation.getAsserted())
                    .expires(Utils.getOneYearExpires(affiliation.getAsserted()))
                    .conditions(null)
                    .build()
            );
            if (visa != null) {
                ctx.getResultVisas().add(visa);
                logAddedVisa(type, value);
            }
        }
    }

}
