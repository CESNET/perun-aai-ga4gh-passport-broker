package cz.muni.ics.ga4gh.service.impl.brokers;

import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.BY_SYSTEM;
import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.TYPE_ACCEPTED_TERMS_AND_POLICIES;
import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.TYPE_AFFILIATION_AND_ROLE;
import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.TYPE_CONTROLLED_ACCESS_GRANTS;
import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.TYPE_LINKED_IDENTITIES;
import static cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa.TYPE_RESEARCHER_STATUS;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.ics.ga4gh.base.Utils;
import cz.muni.ics.ga4gh.base.adapters.PerunAdapter;
import cz.muni.ics.ga4gh.base.model.Affiliation;
import cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa;
import cz.muni.ics.ga4gh.base.model.UserExtSource;
import cz.muni.ics.ga4gh.base.properties.BrokerInstanceProperties;
import cz.muni.ics.ga4gh.base.properties.Ga4ghBrokersProperties;
import cz.muni.ics.ga4gh.service.JWTSigningAndValidationService;
import cz.muni.ics.ga4gh.service.PassportAssemblyContext;
import cz.muni.ics.ga4gh.service.impl.VisaAssemblyParameters;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Validated
public class PerunGa4ghBroker extends Ga4ghBroker {

    @NotBlank
    private final String idAttribute;
    
    @NotBlank
    private final String source;

    private final Set<String> ignoreLastAccessIdps = new HashSet<>();

    public PerunGa4ghBroker(BrokerInstanceProperties instanceProperties,
                            Ga4ghBrokersProperties brokersProperties,
                            PerunAdapter adapter,
                            JWTSigningAndValidationService jwtService) {
        super(instanceProperties, brokersProperties, adapter, jwtService);
        this.idAttribute = instanceProperties.getIdentifierAttribute();
        this.source = instanceProperties.getSource();
        if (instanceProperties.getWhitelistedLinkedIdentitySources() != null) {
            this.ignoreLastAccessIdps.addAll(
                instanceProperties.getWhitelistedLinkedIdentitySources());
        }
    }

    @Override
    protected String getSubAttribute() {
        return idAttribute;
    }

    @Override
    protected Long getCommunityVoId() {
        return null;
    }

    @Override
    protected void addAffiliationAndRoles(PassportAssemblyContext ctx)
    {
        String type = TYPE_AFFILIATION_AND_ROLE;
        logAddingVisas(type);

        if (ctx.getIdentityAffiliations() == null || ctx.getIdentityAffiliations().isEmpty()) {
            log.debug("No affiliations available, not adding any visas");
            return;
        }

        for (Affiliation affiliation: ctx.getIdentityAffiliations()) {
            long expires = Utils.getOneYearExpires(affiliation.getAsserted());
            Ga4ghPassportVisa visa = createVisa(
                VisaAssemblyParameters.builder()
                    .type(type)
                    .sub(ctx.getSubject())
                    .userId(ctx.getPerunUserId())
                    .value(affiliation.getValue())
                    .source(affiliation.getSource())
                    .by(BY_SYSTEM)
                    .asserted(affiliation.getAsserted())
                    .expires(expires)
                    .conditions(null)
                    .build()
            );

            if (visa != null) {
                ctx.getResultVisas().add(visa);
                logAddedVisa(type, affiliation.getValue());
            }
        }
    }

    @Override
    protected void addAcceptedTermsAndPolicies(PassportAssemblyContext ctx) {
        logAddingVisas(TYPE_ACCEPTED_TERMS_AND_POLICIES);
        // no policies - extend with Perun AUP?
    }

    @Override
    protected void addControlledAccessGrants(PassportAssemblyContext ctx) {
        logAddingVisas(TYPE_CONTROLLED_ACCESS_GRANTS);
        // no repositories
    }

    @Override
    protected void addLinkedIdentities(PassportAssemblyContext ctx) {
        String type = TYPE_LINKED_IDENTITIES;
        logAddingVisas(type);

        List<UserExtSource> userExtSources = adapter.getAdapterRpc().getIdpUserExtSources(ctx.getPerunUserId());
        for (UserExtSource ues: userExtSources) {
            long asserted = ues.getLastAccess().getTime() / 1000L;
            long expires = Utils.getOneYearExpires(asserted);

            String idp = ues.getExtSource().getName();
            if (ignoreLastAccessIdps.contains(idp)) {
                expires = Utils.getExpires(asserted, 100L);
            }
            String value = Utils.constructLinkedIdentity(ues.getLogin(), ues.getExtSource().getName());
            Ga4ghPassportVisa visa = createVisa(
                VisaAssemblyParameters.builder()
                    .type(type)
                    .sub(ctx.getSubject())
                    .userId(ctx.getPerunUserId())
                    .value(value)
                    .source(this.source)
                    .by(BY_SYSTEM)
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
    }

    @Override
    protected void addResearcherStatuses(PassportAssemblyContext ctx)
    {
        logAddingVisas(TYPE_RESEARCHER_STATUS);
        // Perun does not have researcher status defined
    }

}
