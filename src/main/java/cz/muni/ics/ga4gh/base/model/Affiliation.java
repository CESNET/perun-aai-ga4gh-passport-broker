package cz.muni.ics.ga4gh.base.model;

import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@Validated
public class Affiliation {

    private final String source;

    @NotBlank
    private final String value;

    private final long asserted;

}
