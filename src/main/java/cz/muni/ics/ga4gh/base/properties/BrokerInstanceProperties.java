package cz.muni.ics.ga4gh.base.properties;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

@Getter
@ToString
@Slf4j

@Validated
@ConstructorBinding
public class BrokerInstanceProperties {

    @NotBlank
    private final String name;

    @NotBlank
    private final String brokerClass;

    @NotBlank
    private final String identifierAttribute;

    private final Long membershipVoId;

    private final String bonaFideStatusAttr;

    private final String bonaFideStatusRemsAttr;

    private final String groupAffiliationsAttr;

    private final String affiliationsAttr;

    private final String orgUrlAttr;

    private final Long termsAndPoliciesGroupId;

    private final String source;

    private final List<Ga4ghClaimRepositoryProperties> passportRepositories = new ArrayList<>();

    private final List<String> whitelistedLinkedIdentitySources = new ArrayList<>();

    public BrokerInstanceProperties(String name,
                                    String brokerClass,
                                    String identifierAttribute,
                                    Long membershipVoId,
                                    String bonaFideStatusAttr,
                                    String bonaFideStatusRemsAttr,
                                    String groupAffiliationsAttr,
                                    String affiliationsAttr,
                                    String orgUrlAttr,
                                    Long termsAndPoliciesGroupId,
                                    String source,
                                    List<String> whitelistedLinkedIdentitySources,
                                    List<Ga4ghClaimRepositoryProperties> passportRepositories)
    {
        this.name = name;
        this.brokerClass = brokerClass;
        this.identifierAttribute = identifierAttribute;
        this.membershipVoId = membershipVoId;
        this.bonaFideStatusAttr = bonaFideStatusAttr;
        this.bonaFideStatusRemsAttr = bonaFideStatusRemsAttr;
        this.groupAffiliationsAttr = groupAffiliationsAttr;
        this.termsAndPoliciesGroupId = termsAndPoliciesGroupId;
        this.affiliationsAttr = affiliationsAttr;
        this.orgUrlAttr = orgUrlAttr;
        this.source = source;
        if (whitelistedLinkedIdentitySources != null) {
            this.whitelistedLinkedIdentitySources.addAll(whitelistedLinkedIdentitySources);
        }
        if (passportRepositories != null) {
            this.passportRepositories.addAll(passportRepositories);
        }
    }

    @PostConstruct
    public void init() {
        log.info("Initialized '{}' properties", this.getClass().getSimpleName());
        log.debug("{}", this);
    }

}
