package cz.muni.ics.ga4gh.base.properties;

import cz.muni.ics.ga4gh.base.adapters.PerunAdapter;
import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.validation.constraints.NotBlank;
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
@ConfigurationProperties(prefix = "perun.adapter")
public class PerunAdapterProperties {

    @NotBlank
    private final String adapterPrimary;

    private final boolean callFallback;

    public PerunAdapterProperties(String primary, Boolean callFallback) {
        if (StringUtils.hasText(primary)) {
            this.adapterPrimary = primary;
        } else {
            this.adapterPrimary = PerunAdapter.RPC;
        }

        this.callFallback = Objects.requireNonNullElse(callFallback, true);
    }

    @PostConstruct
    public void init() {
        log.info("Initialized '{}' properties", this.getClass().getSimpleName());
        log.debug("{}", this);
    }

}
