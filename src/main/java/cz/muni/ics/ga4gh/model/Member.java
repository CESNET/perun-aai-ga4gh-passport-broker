package cz.muni.ics.ga4gh.model;

import cz.muni.ics.ga4gh.enums.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    private Long id;

    private Long userId;

    private Long voId;

    private MemberStatus status;
}
