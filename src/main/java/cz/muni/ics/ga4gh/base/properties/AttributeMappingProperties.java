package cz.muni.ics.ga4gh.base.properties;

import cz.muni.ics.ga4gh.base.model.AttributeMapping;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

@Getter
@ToString
@Slf4j

@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "attributes")
public class AttributeMappingProperties {

    @NotEmpty
    private final Map<String, AttributeMapping> attributeMappings = new HashMap<>();

    public AttributeMappingProperties(@NotEmpty Map<String, AttributeMapping> attributeMappings) {

        this.attributeMappings.putAll(attributeMappings);
    }

    @PostConstruct
    public void init() {
        log.info("Initialized '{}' properties", this.getClass().getSimpleName());
        log.debug("{}", this);
    }

}
