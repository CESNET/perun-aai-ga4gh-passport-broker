package cz.muni.ics.ga4gh.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "broker")
@Getter
@Setter
public class BrokerConfig {

    private String bonaFideStatusAttr;

    private String bonaFideStatusRemsAttr;

    private String groupAffiliationsAttr;

    private Long termsAndPoliciesGroupId;

    private String affiliationsAttr;

    private String orgUrlAttr;

    private List<String> attributesToSearch;

    private String issuer;

    private String jku;

    private String pathToJwkFile;
}
