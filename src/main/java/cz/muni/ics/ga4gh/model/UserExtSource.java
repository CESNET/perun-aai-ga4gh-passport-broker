package cz.muni.ics.ga4gh.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class UserExtSource {

    private Long id;

    private ExtSource extSource;

    private String login;

    private int loa = 0;

    private boolean persistent;

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
