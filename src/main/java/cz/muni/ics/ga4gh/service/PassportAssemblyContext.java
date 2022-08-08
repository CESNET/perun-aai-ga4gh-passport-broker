package cz.muni.ics.ga4gh.service;

import cz.muni.ics.ga4gh.base.adapters.PerunAdapter;
import cz.muni.ics.ga4gh.base.model.Affiliation;
import cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
@Builder
public class PassportAssemblyContext {

    private long now;

    private PerunAdapter perunAdapter;

    private String subject;

    private long perunUserId;

    private List<Affiliation> identityAffiliations;

    private Set<String> externalLinkedIdentities;

    private List<Ga4ghPassportVisa> externalControlledAccessGrants;

    private List<Ga4ghPassportVisa> resultVisas;

}
