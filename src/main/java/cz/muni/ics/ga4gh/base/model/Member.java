package cz.muni.ics.ga4gh.base.model;

import cz.muni.ics.ga4gh.base.enums.MemberStatus;
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
public class Member {

    @NotNull
    private Long id;

    @NotNull
    private Long userId;

    @NotNull
    private Long voId;

    @NotNull
    private MemberStatus status;

}
