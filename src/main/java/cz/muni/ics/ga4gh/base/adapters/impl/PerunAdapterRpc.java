package cz.muni.ics.ga4gh.base.adapters.impl;

import static cz.muni.ics.ga4gh.base.enums.MemberStatus.VALID;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.ics.ga4gh.base.adapters.PerunAdapterMethods;
import cz.muni.ics.ga4gh.base.adapters.PerunAdapterMethodsRpc;
import cz.muni.ics.ga4gh.base.adapters.PerunRpcAdapterMapper;
import cz.muni.ics.ga4gh.base.connectors.PerunConnectorRpc;
import cz.muni.ics.ga4gh.base.model.Affiliation;
import cz.muni.ics.ga4gh.base.model.AttributeMapping;
import cz.muni.ics.ga4gh.base.model.Group;
import cz.muni.ics.ga4gh.base.model.Member;
import cz.muni.ics.ga4gh.base.model.PerunAttributeValue;
import cz.muni.ics.ga4gh.base.model.UserExtSource;
import cz.muni.ics.ga4gh.base.properties.AttributeMappingProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public class PerunAdapterRpc implements PerunAdapterMethods, PerunAdapterMethodsRpc {

    public static final String EXT_SOURCE_IDP = "cz.metacentrum.perun.core.impl.ExtSourceIdp";
    public static final String EXT_LOGIN = "extLogin";
    public static final String EXT_SOURCE_NAME = "extSourceName";
    public static final String USER_EXT_SOURCE = "userExtSource";
    public static final String ATTR_NAMES = "attrNames";
    public static final String VALUE_CREATED_AT = "valueCreatedAt";

    public static final String ID = "id";
    public static final String VO = "vo";
    public static final String USER = "user";
    public static final String MEMBER = "member";
    public static final String GROUP = "group";

    public static final String ATTRIBUTE_NAME = "attributeName";
    public static final String ATTRIBUTE_VALUE = "attributeValue";

    public static final String ATTRIBUTES_MANAGER = "attributesManager";
    public static final String GROUPS_MANAGER = "groupsManager";
    public static final String MEMBERS_MANAGER = "membersManager";
    public static final String USERS_MANAGER = "usersManager";

    public static final String GET_USER_BY_EXT_SOURCE_NAME_AND_EXT_LOGIN = "getUserByExtSourceNameAndExtLogin";
    public static final String GET_USER_EXT_SOURCES = "getUserExtSources";
    public static final String GET_GROUP_BY_ID = "getGroupById";
    public static final String GET_MEMBER_BY_USER = "getMemberByUser";
    public static final String GET_MEMBERS_BY_USER = "getMembersByUser";
    public static final String GET_MEMBER_GROUPS = "getMemberGroups";
    public static final String IS_GROUP_MEMBER = "isGroupMember";
    public static final String GET_ATTRIBUTE = "getAttribute";
    public static final String GET_ATTRIBUTES = "getAttributes";
    public static final String GET_USERS_BY_ATTRIBUTE_VALUE = "getUsersByAttributeValue";

    private final PerunConnectorRpc connectorRpc;
    private final Map<String, AttributeMapping> attributeMappings;

    public PerunAdapterRpc(PerunConnectorRpc connectorRpc,
                    AttributeMappingProperties attributeMappingProperties)
    {
        this.connectorRpc = connectorRpc;
        this.attributeMappings = attributeMappingProperties.getAttributeMappings();
    }

    @Override
    public Long getPerunUserId(String extLogin, String extSourceName) {
        if (!StringUtils.hasText(extLogin)) {
            throw new IllegalArgumentException("Empty extLogin passed");
        } else if (!StringUtils.hasText(extSourceName)) {
            throw new IllegalArgumentException("Empty extSourceName passed");
        }

        if (!connectorRpc.isEnabled()) {
            return null;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(EXT_LOGIN, extLogin);
        map.put(EXT_SOURCE_NAME, extSourceName);

        JsonNode response = connectorRpc.post(USERS_MANAGER, GET_USER_BY_EXT_SOURCE_NAME_AND_EXT_LOGIN, map);

        return response.get(ID) == null ? null : response.get(ID).asLong();
    }

    @Override
    public boolean isUserInGroup(Long userId, Long groupId) {
        if (userId == null) {
            throw new IllegalArgumentException("Null user ID passed");
        } else if (groupId == null) {
            throw new IllegalArgumentException("Null group ID passed");
        }

        if (!connectorRpc.isEnabled()) {
            return false;
        }

        Map<String, Object> groupParams = new LinkedHashMap<>();
        groupParams.put(ID, groupId);
        JsonNode groupResponse = connectorRpc.post(GROUPS_MANAGER, GET_GROUP_BY_ID, groupParams);
        Group group = PerunRpcAdapterMapper.mapGroup(groupResponse);

        Map<String, Object> memberParams = new LinkedHashMap<>();
        memberParams.put(VO, group.getVoId());
        memberParams.put(USER, userId);
        JsonNode memberResponse = connectorRpc.post(MEMBERS_MANAGER, GET_MEMBER_BY_USER, memberParams);
        Member member = PerunRpcAdapterMapper.mapMember(memberResponse);

        Map<String, Object> isGroupMemberParams = new LinkedHashMap<>();
        isGroupMemberParams.put(GROUP, groupId);
        isGroupMemberParams.put(MEMBER, member.getId());
        JsonNode res = connectorRpc.post(GROUPS_MANAGER, IS_GROUP_MEMBER, isGroupMemberParams);

        return res.asBoolean(false);
    }

    @Override
    public List<Affiliation> getGroupAffiliations(Long userId, String groupAffiliationsAttr) {
        if (userId == null) {
            throw new IllegalArgumentException("Null user ID passed");
        } else if (!StringUtils.hasText(groupAffiliationsAttr)) {
            throw new IllegalArgumentException("Empty group affiliations attribute name passed");
        }
        if (!connectorRpc.isEnabled()) {
            return new ArrayList<>();
        }

        List<Affiliation> affiliations = new ArrayList<>();
        List<Member> userMembers = getMembersByUser(userId);

        for (Member member : userMembers) {
            if (!VALID.equals(member.getStatus())) {
                continue;
            }
            List<Group> memberGroups = getMemberGroups(member.getId());
            for (Group group : memberGroups) {
                PerunAttributeValue attrValue = getGroupAttributeValue(group, groupAffiliationsAttr);
                if (attrValue != null && attrValue.valueAsList() != null) {
                    long linuxTime = System.currentTimeMillis() / 1000L;

                    for (String value : attrValue.valueAsList()) {
                        Affiliation affiliation = new Affiliation(null, value, linuxTime);
                        log.debug("found {} on group {}", value, group.getName());
                        affiliations.add(affiliation);
                    }
                }
            }
        }

        return affiliations;
    }

    @Override
    public List<Affiliation> getGroupAffiliations(Long userId, Long voId,
                                                  String groupAffiliationsAttr)
    {
        if (userId == null) {
            throw new IllegalArgumentException("Null user ID passed");
        } else if (voId == null) {
            throw new IllegalArgumentException("Null vo ID passed");
        } else if (!StringUtils.hasText(groupAffiliationsAttr)) {
            throw new IllegalArgumentException("Empty group affiliations attribute name passed");
        }
        if (!connectorRpc.isEnabled()) {
            return new ArrayList<>();
        }

        List<Affiliation> affiliations = new ArrayList<>();
        List<Member> userMembers = getMembersByUser(userId);

        for (Member member : userMembers) {
            if (!VALID.equals(member.getStatus())) {
                continue;
            } else if (!Objects.equals(voId, member.getVoId())) {
                continue;
            }
            List<Group> memberGroups = getMemberGroups(member.getId());
            for (Group group : memberGroups) {
                PerunAttributeValue attrValue = getGroupAttributeValue(group, groupAffiliationsAttr);
                if (attrValue != null && attrValue.valueAsList() != null) {
                    long linuxTime = System.currentTimeMillis() / 1000L;

                    for (String value : attrValue.valueAsList()) {
                        Affiliation affiliation = new Affiliation(null, value, linuxTime);
                        log.debug("found {} on group {}", value, group.getName());
                        affiliations.add(affiliation);
                    }
                }
            }
        }

        return affiliations;
    }

    @Override
    public String getUserAttributeCreatedAt(Long userId, String attrName) {
        if (userId == null) {
            throw new IllegalArgumentException("Null user ID passed");
        } else if (!StringUtils.hasText(attrName)) {
            throw new IllegalArgumentException("Empty attribute name passed");
        }
        if (!connectorRpc.isEnabled()) {
            return null;
        }

        AttributeMapping mapping = getAttributeMapping(attrName);
        if (mapping == null) {
            return null;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(USER, userId);
        map.put(ATTRIBUTE_NAME, mapping.getRpcName());

        JsonNode res = connectorRpc.post(ATTRIBUTES_MANAGER, GET_ATTRIBUTE, map);
        if (res == null || !res.hasNonNull(VALUE_CREATED_AT)) {
            return null;
        }
        return res.get(VALUE_CREATED_AT).asText();
    }

    @Override
    public List<Affiliation> getUserExtSourcesAffiliations(Long userId,
                                                           String affiliationsAttr,
                                                           String orgUrlAttr)
    {
        if (userId == null) {
            throw new IllegalArgumentException("Null user ID passed");
        } else if (!StringUtils.hasText(affiliationsAttr)) {
            throw new IllegalArgumentException("Empty affiliations attr name passed");
        } else if (!StringUtils.hasText(orgUrlAttr)) {
            throw new IllegalArgumentException("Empty organizationURL attr name passed");
        }

        if (!connectorRpc.isEnabled()) {
            return new ArrayList<>();
        }

        List<UserExtSource> userExtSources = getUserExtSources(userId);
        if (userExtSources.isEmpty()) {
            return new ArrayList<>();
        }

        List<Affiliation> affiliations = new ArrayList<>();
        for (UserExtSource ues : userExtSources) {
            if (!EXT_SOURCE_IDP.equals(ues.getExtSource().getType())) {
                continue;
            }
            Map<String, PerunAttributeValue> uesAttrValues = getUserExtSourceAttributeValues(
                ues.getId(), Arrays.asList(affiliationsAttr, orgUrlAttr));

            long asserted = ues.getLastAccess().getTime() / 1000L;

            String affs = uesAttrValues.get(affiliationsAttr).valueAsString();
            String orgUrl = uesAttrValues.get(orgUrlAttr).valueAsString();

            if (affs != null) {
                for (String aff : affs.split(";")) {
                    String source = ( (orgUrl != null) ? orgUrl : ues.getExtSource().getName() );
                    Affiliation affiliation = new Affiliation(source, aff, asserted);
                    log.debug("found {} from IdP {} with orgURL {} asserted at {}", aff, ues.getExtSource().getName(),
                            orgUrl, asserted);
                    affiliations.add(affiliation);
                }
            }
        }

        return affiliations;
    }

    @Override
    public Set<Long> getUserIdsByAttributeValue(String attrName, String attrValue) {
        if (!StringUtils.hasText(attrName)) {
            throw new IllegalArgumentException("Empty attribute name passed");
        } else if (!StringUtils.hasText(attrValue)) {
            throw new IllegalArgumentException("Empty attribute value passed");
        }

        if (!connectorRpc.isEnabled()) {
            return new HashSet<>();
        }

        AttributeMapping mapping = getAttributeMapping(attrName);
        if (mapping == null) {
            return new HashSet<>();
        }

        Set<Long> result = new HashSet<>();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(ATTRIBUTE_NAME, mapping.getRpcName());
        map.put(ATTRIBUTE_VALUE, attrValue);

        JsonNode res = connectorRpc.post(USERS_MANAGER, GET_USERS_BY_ATTRIBUTE_VALUE, map);
        if (res != null) {
            for (int i = 0; i < res.size(); i++) {
                result.add(res.get(i).get(ID).asLong());
            }
        }
        return result;
    }

    @Override
    public String getUserSub(Long userId, String subAttribute) {
        if (userId == null) {
            throw new IllegalArgumentException("Null user ID passed");
        } else if (!StringUtils.hasText(subAttribute)) {
            throw new IllegalArgumentException("Empty sub attribute name passed");
        }
        if (!connectorRpc.isEnabled()) {
            return null;
        }

        AttributeMapping mapping = getAttributeMapping(subAttribute);
        if (mapping == null) {
            return null;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(USER, userId);
        map.put(ATTRIBUTE_NAME, mapping.getRpcName());
        JsonNode res = connectorRpc.post(ATTRIBUTES_MANAGER, GET_ATTRIBUTE, map);

        PerunAttributeValue value = PerunRpcAdapterMapper.mapAttributeValue(res);
        if (value != null) {
            return value.getValue().textValue();
        } else {
            return null;
        }
    }
    @Override
    public boolean isUserInVo(Long userId, Long voId) {
        if (userId == null) {
            throw new IllegalArgumentException("Null user ID passed");
        } else if (voId == null) {
            throw new IllegalArgumentException("Null vo ID passed");
        }

        if (!connectorRpc.isEnabled()) {
            return false;
        }

        Map<String, Object> memberParams = new LinkedHashMap<>();
        memberParams.put(VO, voId);
        memberParams.put(USER, userId);
        JsonNode memberResponse = connectorRpc.post(MEMBERS_MANAGER, GET_MEMBER_BY_USER, memberParams);
        Member member = PerunRpcAdapterMapper.mapMember(memberResponse);

        return member != null && member.getStatus() == VALID;
    }

    @Override
    public List<UserExtSource> getIdpUserExtSources(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Null user ID passed");
        }

        List<UserExtSource> userExtSources = getUserExtSources(userId);
        if (userExtSources.isEmpty()) {
            return new ArrayList<>();
        }
        userExtSources = userExtSources.stream()
            .filter(ues -> EXT_SOURCE_IDP.equals(ues.getExtSource().getType()))
            .collect(Collectors.toList());
        return userExtSources;
    }

    private List<Member> getMembersByUser(Long userId) {
        if (!this.connectorRpc.isEnabled()) {
            return new ArrayList<>();
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put(USER, userId);
        JsonNode jsonNode = connectorRpc.post(MEMBERS_MANAGER, GET_MEMBERS_BY_USER, params);

        return PerunRpcAdapterMapper.mapMembers(jsonNode);
    }

    private List<Group> getMemberGroups(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("Null member ID passed");
        }
        if (!this.connectorRpc.isEnabled()) {
            return new ArrayList<>();
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MEMBER, memberId);

        JsonNode response = connectorRpc.post(GROUPS_MANAGER, GET_MEMBER_GROUPS, map);
        return PerunRpcAdapterMapper.mapGroups(response);
    }

    private PerunAttributeValue getGroupAttributeValue(Group group, String attrToFetch) {
        if (group == null || group.getId() == null) {
            throw new IllegalArgumentException("Null group or group with null ID passed");
        } else if (!StringUtils.hasText(attrToFetch)) {
            throw new IllegalArgumentException("Empty attribute name passed");
        }
        AttributeMapping attributeMapping = getAttributeMapping(attrToFetch);
        if (attributeMapping == null) {
            return null;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(GROUP, group.getId());
        map.put(ATTRIBUTE_NAME, attributeMapping.getRpcName());
        JsonNode res = connectorRpc.post(ATTRIBUTES_MANAGER, GET_ATTRIBUTE, map);

        return PerunRpcAdapterMapper.mapAttributeValue(res);
    }

    private List<UserExtSource> getUserExtSources(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Null user ID passed");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(USER, userId);

        JsonNode response = connectorRpc.post(USERS_MANAGER, GET_USER_EXT_SOURCES, map);
        return PerunRpcAdapterMapper.mapUserExtSources(response);
    }

    private Map<String, PerunAttributeValue> getUserExtSourceAttributeValues(Long uesId,
                                                                             List<String> attributeNames)
    {
        if (uesId == null) {
            throw new IllegalArgumentException("Null user ext source ID passed");
        } else if (attributeNames == null || attributeNames.isEmpty()) {
            throw new IllegalArgumentException("Null or empty list of attribute names passed");
        }

        Map<String, Object> map = new LinkedHashMap<>();

        Map<String, AttributeMapping> mappings = new HashMap<>();
        for (String attrName: attributeNames) {
            AttributeMapping mapping = getAttributeMapping(attrName);
            if (mapping != null) {
                mappings.put(attrName, mapping);
            }
        }

        map.put(USER_EXT_SOURCE, uesId);
        map.put(ATTR_NAMES, mappings.values().stream()
            .map(AttributeMapping::getRpcName)
            .collect(Collectors.toList()));

        JsonNode response = connectorRpc.post(ATTRIBUTES_MANAGER, GET_ATTRIBUTES, map);

        return PerunRpcAdapterMapper.mapAttributes(response, mappings);
    }

    private AttributeMapping getAttributeMapping(String attribute) {
        if (!StringUtils.hasText(attribute)) {
            throw new IllegalArgumentException("Empty attribute name passed");
        }

        AttributeMapping mapping = attributeMappings.getOrDefault(attribute, null);
        if (mapping == null) {
            log.warn("No attribute mapping found for attribute '{}'", attribute);
            return null;
        } else if (!StringUtils.hasText(mapping.getRpcName())) {
            log.warn("No RPC name found in mapping for attribute '{}'", attribute);
            return null;
        } else {
            return mapping;
        }
    }

}
