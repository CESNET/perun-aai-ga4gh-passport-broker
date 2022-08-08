package cz.muni.ics.ga4gh.base.properties;

import cz.muni.ics.ga4gh.base.exceptions.ConfigurationException;
import cz.muni.ics.ga4gh.base.model.BasicAuthCredentials;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Getter
@ToString
@Slf4j

@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "basic-auth")
public class BasicAuthProperties {

    @NotEmpty
    private final List<BasicAuthCredentials> credentials = new ArrayList<>();

    public BasicAuthProperties(@NotEmpty List<BasicAuthCredentials> credentials)
        throws ConfigurationException
    {
        for (BasicAuthCredentials c: credentials) {
            if (!StringUtils.hasText(c.getUsername()) || !StringUtils.hasText(c.getPassword())) {
                throw new ConfigurationException("Invalid basic-auth credentials configured - empty username or password. Check your configuration.");
            }
        }

        this.credentials.addAll(credentials);
    }

    @PostConstruct
    public void init() {
        log.info("Initialized '{}' properties", this.getClass().getSimpleName());
        log.debug("{}", this);
    }

}
