package cz.muni.ics.ga4gh.mappers;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.muni.ics.ga4gh.enums.MemberStatus;
import cz.muni.ics.ga4gh.exceptions.MissingFieldException;
import cz.muni.ics.ga4gh.model.AttributeMapping;
import cz.muni.ics.ga4gh.model.ExtSource;
import cz.muni.ics.ga4gh.model.Group;
import cz.muni.ics.ga4gh.model.Member;
import cz.muni.ics.ga4gh.model.PerunAttributeValue;
import cz.muni.ics.ga4gh.model.UserExtSource;


/**
 * This class is mapping JsonNodes to object models.
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class RpcMapper {

    public static final String ID = "id";
    public static final String UUID = "uuid";
    public static final String PARENT_GROUP_ID = "parentGroupId";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String VO_ID = "voId";
    public static final String USER_ID = "userId";
    public static final String STATUS = "status";
    public static final String TYPE = "type";
    public static final String LOGIN = "login";
    public static final String EXT_SOURCE = "extSource";
    public static final String LOA = "loa";
    public static final String LAST_ACCESS = "lastAccess";
    public static final String PERSISTENT = "persistent";
    public static final String FRIENDLY_NAME = "friendlyName";
    public static final String NAMESPACE = "namespace";
    public static final String VALUE = "value";

    /**
     * Maps JsonNode to Group model.
     *
     * @param json Group in JSON format from Perun to be mapped.
     * @return Mapped Group object.
     */
    public static Group mapGroup(JsonNode json) {
        if (json == null || json.isNull()) {
            return null;
        }

        Long id = getRequiredFieldAsLong(json, ID);
        Long parentGroupId = getFieldAsLong(json, PARENT_GROUP_ID);
        String name = getFieldAsString(json, NAME);
        String description = getFieldAsString(json, DESCRIPTION);
        Long voId = getRequiredFieldAsLong(json, VO_ID);
        String uuid = getRequiredFieldAsString(json, UUID);

        return new Group(id, parentGroupId, name, description, null, uuid,  voId);
    }

    /**
     * Maps JsonNode to List of Groups.
     *
     * @param jsonArray JSON array of groups in JSON format from Perun to be mapped.
     * @return List of groups.
     */
    public static List<Group> mapGroups(JsonNode jsonArray) {
        if (jsonArray.isNull()) {
            return new ArrayList<>();
        }

        List<Group> result = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonNode groupNode = jsonArray.get(i);
            Group mappedGroup = RpcMapper.mapGroup(groupNode);
            result.add(mappedGroup);
        }

        return result;
    }

    /**
     * Maps JsonNode to Member model.
     *
     * @param json Member in JSON format from Perun to be mapped.
     * @return Mapped Member object.
     */
    public static Member mapMember(JsonNode json) {
        if (json == null || json.isNull()) {
            return null;
        }

        Long id = getRequiredFieldAsLong(json, ID);
        Long userId = getRequiredFieldAsLong(json, USER_ID);
        Long voId = getRequiredFieldAsLong(json, VO_ID);
        MemberStatus status = MemberStatus.fromString(getRequiredFieldAsString(json, STATUS));

        return new Member(id, userId, voId, status);
    }

    /**
     * Maps JsonNode to List of Members.
     *
     * @param jsonArray JSON array of members in JSON format from Perun to be mapped.
     * @return List of members.
     */
    public static List<Member> mapMembers(JsonNode jsonArray) {
        if (jsonArray.isNull()) {
            return new ArrayList<>();
        }

        List<Member> members = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonNode memberNode = jsonArray.get(i);
            Member mappedMember = RpcMapper.mapMember(memberNode);
            members.add(mappedMember);
        }

        return members;
    }

    /**
     * Maps JsonNode to ExtSource model.
     *
     * @param json ExtSource in JSON format from Perun to be mapped.
     * @return Mapped ExtSource object.
     */
    public static ExtSource mapExtSource(JsonNode json) {
        if (json == null || json.isNull()) {
            return null;
        }

        Long id = getRequiredFieldAsLong(json, ID);
        String name = getRequiredFieldAsString(json, NAME);
        String type = getRequiredFieldAsString(json, TYPE);

        return new ExtSource(id, name, type);
    }

    /**
     * Maps JsonNode to UserExtSource model.
     *
     * @param json UserExtSource in JSON format from Perun to be mapped.
     * @return Mapped UserExtSource object.
     */
    public static UserExtSource mapUserExtSource(JsonNode json) {
        if (json == null || json.isNull()) {
            return null;
        }

        Long id = getRequiredFieldAsLong(json, ID);
        String login = getRequiredFieldAsString(json, LOGIN);
        ExtSource extSource = RpcMapper.mapExtSource(getRequiredFieldAsJsonNode(json, EXT_SOURCE));
        int loa = getRequiredFieldAsInt(json, LOA);
        boolean persistent = getRequiredFieldAsBoolean(json, PERSISTENT);
        Timestamp lastAccess = Timestamp.valueOf(getRequiredFieldAsString(json, LAST_ACCESS));

        return new UserExtSource(id, extSource, login, loa, persistent, lastAccess);
    }

    /**
     * Maps JsonNode to List of UserExtSources.
     *
     * @param jsonArray JSON array of userExtSources in JSON format from Perun to be mapped.
     * @return List of userExtSources.
     */
    public static List<UserExtSource> mapUserExtSources(JsonNode jsonArray) {
        if (jsonArray.isNull()) {
            return new ArrayList<>();
        }

        List<UserExtSource> userExtSources = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonNode userExtSource = jsonArray.get(i);
            UserExtSource mappedUes = RpcMapper.mapUserExtSource(userExtSource);
            userExtSources.add(mappedUes);
        }

        return userExtSources;
    }

    public static PerunAttributeValue mapAttributeValue(JsonNode json) {
        if (json == null || json.isNull()) {
            return null;
        }

        String friendlyName = getRequiredFieldAsString(json, FRIENDLY_NAME);
        String namespace = getRequiredFieldAsString(json, NAMESPACE);
        String type = getRequiredFieldAsString(json, TYPE);
        JsonNode value = getFieldAsJsonNode(json, VALUE);

        return new PerunAttributeValue(namespace + ':' + friendlyName, type, value);
    }

    public static Map<String, PerunAttributeValue> mapAttributes(JsonNode jsonNode, Map<String, AttributeMapping> attrMappings) {
        Map<String, PerunAttributeValue> attributesAsMap = new HashMap<>();

        for (int i = 0; i < jsonNode.size(); i++) {
            JsonNode attribute = jsonNode.get(i);
            PerunAttributeValue mappedAttributeValue = mapAttributeValue(attribute);

            for (Map.Entry<String, AttributeMapping> entry : attrMappings.entrySet()) {
                if (entry.getValue().getRpcName().equals(mappedAttributeValue.getAttrName())) {
                    attributesAsMap.put(entry.getKey(), mappedAttributeValue);
                }
            }
        }

        return attributesAsMap;
    }

    private static Long getRequiredFieldAsLong(JsonNode json, String name) {
        if (!json.hasNonNull(name)) {
            throw new MissingFieldException();
        }
        return json.get(name).asLong();
    }

    private static Long getFieldAsLong(JsonNode json, String name) {
        if (!json.hasNonNull(name)) {
            return 0L;
        }
        return json.get(name).asLong();
    }

    private static int getRequiredFieldAsInt(JsonNode json, String name) {
        if (!json.hasNonNull(name)) {
            throw new MissingFieldException();
        }
        return json.get(name).asInt();
    }

    private static int getFieldAsInt(JsonNode json, String name) {
        if (!json.hasNonNull(name)) {
            return 0;
        }
        return json.get(name).asInt();
    }

    private static boolean getRequiredFieldAsBoolean(JsonNode json, String name) {
        if (!json.hasNonNull(name)) {
            throw new MissingFieldException();
        }
        return json.get(name).asBoolean();
    }

    private static String getRequiredFieldAsString(JsonNode json, String name) {
        if (!json.hasNonNull(name)) {
            throw new MissingFieldException();
        }
        return json.get(name).asText();
    }

    private static String getFieldAsString(JsonNode json, String name) {
        if (!json.hasNonNull(name)) {
            return "";
        }
        return json.get(name).asText();
    }

    private static JsonNode getRequiredFieldAsJsonNode(JsonNode json, String name) {
        if (!json.hasNonNull(name)) {
            throw new MissingFieldException();
        }
        return json.get(name);
    }

    private static JsonNode getFieldAsJsonNode(JsonNode json, String name) {
        return json.get(name);
    }
}
