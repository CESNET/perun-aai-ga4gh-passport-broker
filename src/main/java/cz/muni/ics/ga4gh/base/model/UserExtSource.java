package cz.muni.ics.ga4gh.base.model;

import java.sql.Timestamp;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@Validated
public class UserExtSource {

    @NotNull
    private Long id;

    private ExtSource extSource;

    @NotBlank
    private String login;

    @Min(0)
    private int loa;

    private boolean persistent;

    @NotNull
    private Timestamp lastAccess;

    public void setExtSource(ExtSource extSource) {
        if (extSource == null) {
            throw new IllegalArgumentException("extSource can't be null");
        }

        this.extSource = extSource;
    }

    public void setLogin(String login) {
        if (!StringUtils.hasText(login)) {
            throw new IllegalArgumentException("login can't be null or empty");
        }

        this.login = login;
    }

    public void setLoa(int loa) {
        if (loa < 0) {
            throw new IllegalArgumentException("loa has to be 0 or higher");
        }

        this.loa = loa;
    }
}
