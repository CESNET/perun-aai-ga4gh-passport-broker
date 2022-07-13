package cz.muni.ics.ga4gh.adapters.impl;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.ics.ga4gh.adapters.PerunAdapterMethods;
import cz.muni.ics.ga4gh.adapters.PerunAdapterMethodsRpc;
import cz.muni.ics.ga4gh.config.AttributesConfig;
import cz.muni.ics.ga4gh.config.RpcConfig;
import cz.muni.ics.ga4gh.connectors.PerunConnectorRpc;
import cz.muni.ics.ga4gh.mappers.RpcMapper;
import cz.muni.ics.ga4gh.model.Affiliation;
import cz.muni.ics.ga4gh.model.AttributeMapping;
import cz.muni.ics.ga4gh.model.Group;
import cz.muni.ics.ga4gh.model.Member;
import cz.muni.ics.ga4gh.model.PerunAttributeValue;
import cz.muni.ics.ga4gh.model.UserExtSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.muni.ics.ga4gh.enums.MemberStatus.VALID;

@Component
@Slf4j
public class PerunAdapterRpc implements PerunAdapterMethods, PerunAdapterMethodsRpc {

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

    @Autowired
    PerunAdapterRpc(PerunConnectorRpc connectorRpc, AttributesConfig attributesConfig) {
        this.connectorRpc = connectorRpc;
        this.attributeMappings = attributesConfig.getAttributeMappings();
    }

    @Override
    public Long getPreauthenticatedUserId(String extLogin, String extSourceName) {
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
        if (!connectorRpc.isEnabled()) {
            return false;
        }

        Map<String, Object> groupParams = new LinkedHashMap<>();
        groupParams.put(ID, groupId);
        JsonNode groupResponse = connectorRpc.post(GROUPS_MANAGER, GET_GROUP_BY_ID, groupParams);
        Group group = RpcMapper.mapGroup(groupResponse);

        Map<String, Object> memberParams = new LinkedHashMap<>();
        memberParams.put(VO, group.getVoId());
        memberParams.put(USER, userId);
        JsonNode memberResponse = connectorRpc.post(MEMBERS_MANAGER, GET_MEMBER_BY_USER, memberParams);
        Member member = RpcMapper.mapMember(memberResponse);

        Map<String, Object> isGroupMemberParams = new LinkedHashMap<>();
        isGroupMemberParams.put(GROUP, groupId);
        isGroupMemberParams.put(MEMBER, member.getId());
        JsonNode res = connectorRpc.post(GROUPS_MANAGER, IS_GROUP_MEMBER, isGroupMemberParams);

        return res.asBoolean(false);
    }

    @Override
    public List<Affiliation> getGroupAffiliations(Long userId, String groupAffiliationsAttr) {
        if (!connectorRpc.isEnabled()) {
            return new ArrayList<>();
        }

        List<Affiliation> affiliations = new ArrayList<>();
        List<Member> userMembers = getMembersByUser(userId);

        for (Member member : userMembers) {
            if (VALID.equals(member.getStatus())) {
                List<Group> memberGroups = getMemberGroups(member.getId());
                for (Group group : memberGroups) {
                    PerunAttributeValue attrValue = this.getGroupAttributeValue(group, groupAffiliationsAttr);
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
        }

        return affiliations;
    }

    @Override
    public String getUserAttributeCreatedAt(Long userId, String attrName) {
        if (!connectorRpc.isEnabled() || attributeMappings.get(attrName) == null) {
            return null;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(USER, userId);
        map.put(ATTRIBUTE_NAME, attributeMappings.get(attrName).getRpcName());

        JsonNode res = connectorRpc.post(ATTRIBUTES_MANAGER, GET_ATTRIBUTE, map);

        if (res == null || !res.hasNonNull(VALUE_CREATED_AT)) {
            return null;
        }

        return res.get(VALUE_CREATED_AT).asText();
    }

    @Override
    public List<Affiliation> getUserExtSourcesAffiliations(Long userId, String affiliationsAttr, String orgUrlAttr) {
        if (!connectorRpc.isEnabled()) {
            return new ArrayList<>();
        }

        List<UserExtSource> userExtSources = getUserExtSources(userId);
        List<Affiliation> affiliations = new ArrayList<>();

        Map<String, AttributeMapping> attrMappings = new HashMap<>();
        attrMappings.put(affiliationsAttr, attributeMappings.get(affiliationsAttr));
        attrMappings.put(orgUrlAttr, attributeMappings.get(orgUrlAttr));

        for (UserExtSource ues : userExtSources) {
            if ("cz.metacentrum.perun.core.impl.ExtSourceIdp".equals(ues.getExtSource().getType())) {
                Map<String, PerunAttributeValue> uesAttrValues = getUserExtSourceAttributeValues(ues.getId(), attrMappings);

                long asserted = ues.getLastAccess().getTime() / 1000L;

                String orgUrl = uesAttrValues.get(affiliationsAttr).valueAsString();
                String affs = uesAttrValues.get(orgUrlAttr).valueAsString();

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
        }

        return affiliations;
    }

    @Override
    public Set<Long> getUserIdsByAttributeValue(AttributeMapping attrName, String attrValue) {
        if (!connectorRpc.isEnabled()) {
            return new HashSet<>();
        }

        Set<Long> result = new HashSet<>();

        if (!StringUtils.hasText(attrName.getRpcName())) {
            return result;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(ATTRIBUTE_NAME, attrName.getRpcName());
        map.put(ATTRIBUTE_VALUE, attrValue);

        JsonNode res = connectorRpc.post(USERS_MANAGER, GET_USERS_BY_ATTRIBUTE_VALUE, map);

        if (res != null) {
            for (int i = 0; i < res.size(); i++) {
                result.add(res.get(i).get(ID).asLong());
            }
        }

        return result;
    }

    private List<Member> getMembersByUser(Long userId) {
        if (!this.connectorRpc.isEnabled()) {
            return new ArrayList<>();
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put(USER, userId);
        JsonNode jsonNode = connectorRpc.post(MEMBERS_MANAGER, GET_MEMBERS_BY_USER, params);

        return RpcMapper.mapMembers(jsonNode);
    }

    private List<Group> getMemberGroups(Long memberId) {
        if (!this.connectorRpc.isEnabled()) {
            return new ArrayList<>();
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MEMBER, memberId);

        JsonNode response = connectorRpc.post(GROUPS_MANAGER, GET_MEMBER_GROUPS, map);
        return RpcMapper.mapGroups(response);
    }

    private PerunAttributeValue getGroupAttributeValue(Group group, String attrToFetch) {
        if (attributeMappings.get(attrToFetch) == null) {
            return null;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(GROUP, group.getId());
        map.put(ATTRIBUTE_NAME, attributeMappings.get(attrToFetch).getRpcName());
        JsonNode res = connectorRpc.post(ATTRIBUTES_MANAGER, GET_ATTRIBUTE, map);

        return RpcMapper.mapAttributeValue(res);
    }

    private List<UserExtSource> getUserExtSources(Long userId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(USER, userId);

        JsonNode response = connectorRpc.post(USERS_MANAGER, GET_USER_EXT_SOURCES, map);
        return RpcMapper.mapUserExtSources(response);
    }

    private Map<String, PerunAttributeValue> getUserExtSourceAttributeValues(Long uesId, Map<String, AttributeMapping> attrMappings) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(USER_EXT_SOURCE, uesId);
        map.put(ATTR_NAMES, attrMappings.values().stream().map(AttributeMapping::getRpcName).collect(Collectors.toList()));

        JsonNode response = connectorRpc.post(ATTRIBUTES_MANAGER, GET_ATTRIBUTES, map);

        return RpcMapper.mapAttributes(response, attrMappings);
    }
}
