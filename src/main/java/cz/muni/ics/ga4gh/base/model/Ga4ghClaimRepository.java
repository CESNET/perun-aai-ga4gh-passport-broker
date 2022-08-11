package cz.muni.ics.ga4gh.base.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@Validated
public class Ga4ghClaimRepository {

    @NotBlank
    private final String name;

    @NotBlank
    private final String actionURL;

    @NotNull
    private final RestTemplate restTemplate;

}
