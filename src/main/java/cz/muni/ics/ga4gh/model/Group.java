package cz.muni.ics.ga4gh.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class Group {

    private Long id;

    private Long parentGroupId;

    private String name;

    private String description;

    private String uniqueGroupName;

    private String uuid;

    private Long voId;

    private Map<String, JsonNode> attributes = new LinkedHashMap<>();

    public Group(Long id, Long parentGroupId, String name, String description, String uniqueGroupName, String uuid, Long voId) {
        this.id = id;
        this.parentGroupId = parentGroupId;
        this.name = name;
        this.description = description;
        this.uniqueGroupName = uniqueGroupName;
        this.uuid = uuid;
        this.voId = voId;
    }
}
