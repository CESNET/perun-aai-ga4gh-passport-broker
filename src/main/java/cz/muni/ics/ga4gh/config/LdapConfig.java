package cz.muni.ics.ga4gh.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "ldap")
@Getter
@Setter
public class LdapConfig {

    private String host;

    private String user;

    private String password;

    private String baseDn;

    private Boolean useTls;

    private Boolean useSsl;

    private Boolean allowUntrustedSsl;

    private Long timeoutSecs;

    private int port;
}
