package cz.muni.ics.ga4gh.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.StringUtils;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class ExtSource {

    @Setter
    private Long id;

    private String name;

    private String type;

    public void setName(String name) {
        if (StringUtils.hasText(name)) {
            throw new IllegalArgumentException("name cannot be null nor empty");
        }

        this.name = name;
    }

    public void setType(String type) {
        if (!StringUtils.hasText(type)) {
            throw new IllegalArgumentException("type cannot be null nor empty");
        }

        this.type = type;
    }
}
