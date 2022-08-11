package cz.muni.ics.ga4gh.base.adapters;

import cz.muni.ics.ga4gh.base.model.Affiliation;
import cz.muni.ics.ga4gh.base.model.UserExtSource;
import java.util.List;

public interface PerunAdapterMethodsRpc {

    String getUserAttributeCreatedAt(Long userId, String attrName);

    List<Affiliation> getUserExtSourcesAffiliations(Long userId, String affiliationsAttr, String orgUrlAttr);

    List<UserExtSource> getIdpUserExtSources(Long perunUserId);

}
