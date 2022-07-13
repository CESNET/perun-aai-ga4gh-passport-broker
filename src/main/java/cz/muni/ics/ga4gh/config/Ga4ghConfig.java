package cz.muni.ics.ga4gh.config;

import cz.muni.ics.ga4gh.model.Repo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "ga4gh")
@Getter
@Setter
public class Ga4ghConfig {

    private List<Repo> repos;
}
