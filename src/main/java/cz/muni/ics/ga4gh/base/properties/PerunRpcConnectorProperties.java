package cz.muni.ics.ga4gh.base.properties;

import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Getter
@Slf4j

@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "perun.connector.rpc")
public class PerunRpcConnectorProperties {

    @NotBlank
    private final String url;
    @NotBlank
    private final String username;
    @NotBlank
    private final String password;
    private final String serializer;
    @Min(1)
    private final int connectionTimeout;
    @Min(1)
    private final int connectionRequestTimeout;
    @Min(1)
    private final int requestTimeout;
    private final boolean enabled;

    public PerunRpcConnectorProperties(String url,
                                       String username,
                                       String password,
                                       String serializer,
                                       Integer connectionTimeout,
                                       Integer connectionRequestTimeout,
                                       Integer requestTimeout,
                                       Boolean enabled)
    {
        if (!StringUtils.hasText(serializer)) {
            serializer = "jsonlite";
        }
        this.url = url;
        this.username = username;
        this.password = password;
        this.serializer = serializer;
        this.connectionTimeout = Objects.requireNonNullElse(connectionTimeout, 30000);
        this.connectionRequestTimeout = Objects.requireNonNullElse(connectionRequestTimeout, 30000);
        this.requestTimeout = Objects.requireNonNullElse(requestTimeout, 60000);
        this.enabled = Objects.requireNonNullElse(enabled, true);
    }

    @PostConstruct
    public void init() {
        log.info("Initialized '{}' properties", this.getClass().getSimpleName());
        log.debug("{}", this);
    }

    @Override
    public String toString() {
        return "RpcAdapterProperties{" +
            "url='" + url + '\'' +
            ", username='" + username + '\'' +
            ", password='PROTECTED_STRING'" +
            ", serializer='" + serializer + '\'' +
            ", connectionTimeout='" + connectionTimeout + '\'' +
            ", connectionRequestTimeout='" + connectionRequestTimeout + '\'' +
            ", requestTimeout='" + requestTimeout + '\'' +
            ", enabled='" + enabled + '\'' +
            '}';
    }

}
