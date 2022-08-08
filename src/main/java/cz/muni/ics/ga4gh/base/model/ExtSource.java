package cz.muni.ics.ga4gh.base.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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
public class ExtSource {

    @NotNull
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String type;
}
