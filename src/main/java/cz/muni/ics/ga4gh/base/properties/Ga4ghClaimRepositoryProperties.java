package cz.muni.ics.ga4gh.base.properties;

import cz.muni.ics.ga4gh.base.model.ClaimRepositoryHeader;
import java.util.List;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@Validated
public class Ga4ghClaimRepositoryProperties {

    @NotBlank
    private String name;

    @NotBlank
    private String url;

    @NotBlank
    private String jwks;

    private List<ClaimRepositoryHeader> headers;

}
