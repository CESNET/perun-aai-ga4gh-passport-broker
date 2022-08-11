package cz.muni.ics.ga4gh.base.properties;

import cz.muni.ics.ga4gh.base.exceptions.ConfigurationException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.core.io.FileUrlResource;
import org.springframework.validation.annotation.Validated;

@Getter
@ToString
@Slf4j

@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "broker")
public class Ga4ghBrokersProperties {

    @NotEmpty
    private final List<String> userIdentificationAttributes = new ArrayList<>();

    @NotBlank
    private final String issuer;

    @NotNull
    private final URI jku;
    @NotNull
    private final FileUrlResource jwkKeystoreFile;

    @NotEmpty
    private final List<BrokerInstanceProperties> brokersProperties = new ArrayList<>();

    public Ga4ghBrokersProperties(@NotEmpty List<String> userIdentificationAttributes,
                                  @NotBlank String issuer,
                                  @URL String jku,
                                  @NotBlank String pathToJwkFile,
                                  @NotEmpty List<BrokerInstanceProperties> brokers)
        throws ConfigurationException, MalformedURLException, URISyntaxException
    {
        try {
            jwkKeystoreFile = new FileUrlResource(pathToJwkFile);
            if (!this.jwkKeystoreFile.exists()) {
                throw new Exception("JWK file does not exist");
            } else if (!this.jwkKeystoreFile.isReadable()) {
                throw new Exception("JWK file is not readable");
            }
            this.jwkKeystoreFile.getFile();
        } catch (Exception e) {
            throw new ConfigurationException("Error when loading JWK keystore file: " + e.getMessage());
        }
        this.userIdentificationAttributes.addAll(userIdentificationAttributes);
        this.issuer = issuer;
        this.jku = new java.net.URL(jku).toURI();
        this.brokersProperties.addAll(brokers);
    }

    @PostConstruct
    public void init() {
        log.info("Initialized '{}' properties", this.getClass().getSimpleName());
        log.debug("{}", this);
    }

}
