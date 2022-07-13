package cz.muni.ics.ga4gh.adapters;

import cz.muni.ics.ga4gh.model.Affiliation;

import java.util.List;

public interface PerunAdapterMethodsRpc {

    String getUserAttributeCreatedAt(Long userId, String attrName);

    List<Affiliation> getUserExtSourcesAffiliations(Long userId, String affiliationsAttr, String orgUrlAttr);
}
