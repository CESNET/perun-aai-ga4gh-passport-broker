package cz.muni.ics.ga4gh.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "adapter")
@Getter
@Setter
public class AdapterConfig {

    private String adapterPrimary;

    private Boolean callFallback;
}
