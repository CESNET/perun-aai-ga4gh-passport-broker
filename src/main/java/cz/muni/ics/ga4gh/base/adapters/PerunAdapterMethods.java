package cz.muni.ics.ga4gh.base.adapters;

import cz.muni.ics.ga4gh.base.model.Affiliation;
import java.util.List;
import java.util.Set;

public interface PerunAdapterMethods {

    /**
     * Fetch user based on his principal (extLogin and extSource) from Perun
     *
     * @return PerunUser with id of found user
     */
    Long getPerunUserId(String extLogin, String extSourceName);

    boolean isUserInGroup(Long userId, Long groupId);

    List<Affiliation> getGroupAffiliations(Long userId, String groupAffiliationsAttr);

    List<Affiliation> getGroupAffiliations(Long userId, Long voId, String groupAffiliationsAttr);

    Set<Long> getUserIdsByAttributeValue(String attrName, String attrValue);

    String getUserSub(Long userId, String subAttribute);

    boolean isUserInVo(Long userId, Long voId);

}
