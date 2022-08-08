package cz.muni.ics.ga4gh.base.properties;

import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

@Getter
@Slf4j

@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "perun.connector.ldap")
@ConditionalOnProperty("perun.connector.ldap.host")
public class PerunLdapConnectorProperties {

    @NotBlank
    private final String host;

    private final String user;

    private final String password;

    @NotBlank
    private final String baseDn;

    private final boolean useTls;

    private final boolean useSsl;

    private final boolean allowUntrustedSsl;

    @Min(1)
    private final long timeoutSecs;

    private final int port;

    public PerunLdapConnectorProperties(String host,
                                        String user,
                                        String password,
                                        String baseDn,
                                        Boolean useTls,
                                        Boolean useSsl,
                                        Boolean allowUntrustedSsl,
                                        Long timeoutSecs,
                                        Integer port)
    {
        this.host = host;
        this.user = user;
        this.password = password;
        this.baseDn = baseDn;
        this.useTls = Objects.requireNonNullElse(useTls, false);
        this.useSsl = Objects.requireNonNullElse(useSsl, false);
        this.allowUntrustedSsl = Objects.requireNonNullElse(allowUntrustedSsl, false);
        this.timeoutSecs = Objects.requireNonNullElse(timeoutSecs, 5L);
        this.port = Objects.requireNonNullElse(port, 336);
    }

    @PostConstruct
    public void init() {
        log.info("Initialized '{}' properties", this.getClass().getSimpleName());
        log.debug("{}", this);
    }

    @Override
    public String toString() {
        return "LdapProperties{" +
            "host='" + host + '\'' +
            ", user='" + user + '\'' +
            ", password='PROTECTED_STRING'" +
            ", baseDn='" + baseDn + '\'' +
            ", useTls=" + useTls +
            ", useSsl=" + useSsl +
            ", allowUntrustedSsl=" + allowUntrustedSsl +
            ", timeoutSecs=" + timeoutSecs +
            ", port=" + port +
            '}';
    }

}
