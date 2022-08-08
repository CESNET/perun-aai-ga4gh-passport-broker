package cz.muni.ics.ga4gh.base.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@Validated
public class Group {

    @NotNull
    private Long id;

    private Long parentGroupId;

    @NotBlank
    private String name;

    private String description;

    private String uniqueGroupName;

    private String uuid;

    @NotNull
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
