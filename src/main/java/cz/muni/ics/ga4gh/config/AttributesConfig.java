package cz.muni.ics.ga4gh.config;

import cz.muni.ics.ga4gh.model.AttributeMapping;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "attributes")
@Getter
@Setter
public class AttributesConfig {

    private Map<String, AttributeMapping> attributeMappings;
}
