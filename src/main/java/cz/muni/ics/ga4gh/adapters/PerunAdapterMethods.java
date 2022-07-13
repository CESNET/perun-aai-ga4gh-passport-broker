package cz.muni.ics.ga4gh.adapters;

import cz.muni.ics.ga4gh.model.Affiliation;
import cz.muni.ics.ga4gh.model.AttributeMapping;

import java.util.List;
import java.util.Set;

public interface PerunAdapterMethods {

    /**
     * Fetch user based on his principal (extLogin and extSource) from Perun
     *
     * @return PerunUser with id of found user
     */
    Long getPreauthenticatedUserId(String extLogin, String extSourceName);

    boolean isUserInGroup(Long userId, Long groupId);

    List<Affiliation> getGroupAffiliations(Long userId, String groupAffiliationsAttr);

    Set<Long> getUserIdsByAttributeValue(AttributeMapping attrName, String attrValue);
}
