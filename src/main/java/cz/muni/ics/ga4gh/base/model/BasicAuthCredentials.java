package cz.muni.ics.ga4gh.base.model;

import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@Validated
public class BasicAuthCredentials {
    @NotBlank
    private String username;
    @NotBlank
    private String password;

    @Override
    public String toString() {
        return "BasicAuthCredentials{" +
            "username='" + username + '\'' +
            ", password='PROTECTED_STRING'" +
            '}';
    }
}