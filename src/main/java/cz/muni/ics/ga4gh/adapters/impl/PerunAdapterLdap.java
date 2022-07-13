package cz.muni.ics.ga4gh.adapters.impl;

import cz.muni.ics.ga4gh.adapters.PerunAdapterMethods;
import cz.muni.ics.ga4gh.adapters.PerunAdapterMethodsLdap;
import cz.muni.ics.ga4gh.config.AttributesConfig;
import cz.muni.ics.ga4gh.connectors.PerunConnectorLdap;
import cz.muni.ics.ga4gh.model.Affiliation;
import cz.muni.ics.ga4gh.model.AttributeMapping;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.apache.directory.ldap.client.template.EntryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.directory.ldap.client.api.search.FilterBuilder.and;
import static org.apache.directory.ldap.client.api.search.FilterBuilder.equal;
import static org.apache.directory.ldap.client.api.search.FilterBuilder.or;

@Component
public class PerunAdapterLdap implements PerunAdapterMethods, PerunAdapterMethodsLdap {

    public static final String OBJECT_CLASS = "objectClass";
    public static final String OU_PEOPLE = "ou=People";

    public static final String PERUN_USER_ID = "perunUserId";
    public static final String MEMBER_OF = "memberOf";

    public static final String PERUN_GROUP = "perunGroup";
    public static final String PERUN_USER = "perunUser";
    public static final String PERUN_GROUP_ID = "perunGroupId";
    public static final String UNIQUE_MEMBER = "uniqueMember";

    public static final String PERUN_VO_ID = "perunVoId";
    public static final String EDU_PERSON_PRINCIPAL_NAMES = "eduPersonPrincipalNames";

    private final PerunConnectorLdap connectorLdap;
    private final Map<String, AttributeMapping> attributeMappings;

    @Autowired
    PerunAdapterLdap(PerunConnectorLdap connectorLdap, AttributesConfig attributesConfig) {
        this.connectorLdap = connectorLdap;
        this.attributeMappings = attributesConfig.getAttributeMappings();
    }

    @Override
    public Long getPreauthenticatedUserId(String extLogin, String extSourceName) {
        FilterBuilder filter = and(
                equal(OBJECT_CLASS, PERUN_USER), equal(EDU_PERSON_PRINCIPAL_NAMES, extLogin)
        );

        return getPerunUserId(filter);
    }

    @Override
    public boolean isUserInGroup(Long userId, Long groupId) {
        String uniqueMemberValue = PERUN_USER_ID + '=' + userId + ',' + OU_PEOPLE + ',' + connectorLdap.getBaseDN();

        FilterBuilder filter = and(
                equal(OBJECT_CLASS, PERUN_GROUP),
                equal(PERUN_GROUP_ID, String.valueOf(groupId)),
                equal(UNIQUE_MEMBER, uniqueMemberValue)
        );

        EntryMapper<Long> mapper = e -> Long.parseLong(e.get(PERUN_GROUP_ID).getString());
        String[] attributes = new String[] { PERUN_GROUP_ID };
        List<Long> ids = connectorLdap.search(null, filter, SearchScope.SUBTREE, attributes, mapper);

        return ids.stream().filter(groupId::equals).count() == 1L;
    }

    @Override
    public List<Affiliation> getGroupAffiliations(Long userId, String groupAffiliationsAttr) {
        Set<Long> userGroupIds = getGroupIdsWhereUserIsMember(userId, null);
        if (userGroupIds.isEmpty()) {
            return new ArrayList<>();
        }

        FilterBuilder[] groupIdFilters = new FilterBuilder[userGroupIds.size()];
        int i = 0;

        for (Long id: userGroupIds) {
            groupIdFilters[i++] = equal(PERUN_GROUP_ID, String.valueOf(id));
        }

        AttributeMapping affiliationsMapping = attributeMappings.get(groupAffiliationsAttr);
        FilterBuilder filterBuilder = and(equal(OBJECT_CLASS, PERUN_GROUP), or(groupIdFilters));
        String[] attributes = new String[] { affiliationsMapping.getLdapName() };

        EntryMapper<Set<Affiliation>> mapper = e -> {
            Set<Affiliation> affiliations = new HashSet<>();
            if (!checkHasAttributes(e, attributes)) {
                return affiliations;
            }

            Attribute a = e.get(affiliationsMapping.getLdapName());
            long linuxTime = System.currentTimeMillis() / 1000L;
            a.iterator().forEachRemaining(v -> affiliations.add(new Affiliation(null, v.getString(), linuxTime)));

            return affiliations;
        };

        List<Set<Affiliation>> affiliationSets = connectorLdap.search(null, filterBuilder, SearchScope.SUBTREE, attributes, mapper);

        return affiliationSets.stream().flatMap(Set::stream).distinct().collect(Collectors.toList());
    }

    @Override
    public Set<Long> getUserIdsByAttributeValue(AttributeMapping attrName, String attrValue) {
        if (!StringUtils.hasText(attrName.getLdapName())) {
            return new HashSet<>();
        }

        FilterBuilder filter = and(
                equal(OBJECT_CLASS, PERUN_USER),
                equal(attrName.getLdapName(), attrValue)
        );

        SearchScope scope = SearchScope.ONELEVEL;
        String[] attributes = new String[]{PERUN_USER_ID};
        EntryMapper<Long> mapper = e -> Long.parseLong(e.get(PERUN_USER_ID).getString());

        List<Long> result = connectorLdap.search(OU_PEOPLE, filter, scope, attributes, mapper);

        return Set.copyOf(result);
    }

    private Set<Long> getGroupIdsWhereUserIsMember(Long userId, Long voId) {
        String dnPrefix = getDnPrefixForUserId(userId);
        String[] attributes = new String[] { MEMBER_OF };

        EntryMapper<Set<Long>> mapper = e -> {
            Set<Long> ids = new HashSet<>();
            if (checkHasAttributes(e, attributes)) {
                Attribute a = e.get(MEMBER_OF);
                a.iterator().forEachRemaining(id -> {
                    String fullVal = id.getString();
                    String[] parts = fullVal.split(",", 3);

                    String groupId = parts[0];
                    groupId = groupId.replace(PERUN_GROUP_ID + '=', "");

                    String voIdStr = parts[1];
                    voIdStr = voIdStr.replace(PERUN_VO_ID + '=', "");

                    if (voId == null || voId.equals(Long.parseLong(voIdStr))) {
                        ids.add(Long.parseLong(groupId));
                    }
                });
            }

            return ids;
        };

        Set<Long> res = connectorLdap.lookup(dnPrefix, attributes, mapper);
        if (res == null) {
            res = new HashSet<>();
        }

        return res;
    }

    private String getDnPrefixForUserId(Long userId) {
        return PERUN_USER_ID + '=' + userId + ',' + OU_PEOPLE;
    }

    private boolean checkHasAttributes(Entry e, String[] attributes) {
        if (e == null) {
            return false;
        } else if (attributes == null) {
            return true;
        }

        for (String attr: attributes) {
            if (e.get(attr) == null) {
                return false;
            }
        }

        return true;
    }

    private Long getPerunUserId(FilterBuilder filter) {
        SearchScope scope = SearchScope.ONELEVEL;
        String[] attributes = new String[]{PERUN_USER_ID};
        EntryMapper<Long> mapper = e -> Long.parseLong(e.get(PERUN_USER_ID).getString());

        return connectorLdap.searchFirst(OU_PEOPLE, filter, scope, attributes, mapper);
    }
}
