package cz.muni.ics.ga4gh.web.security;

import cz.muni.ics.ga4gh.base.model.BasicAuthCredentials;
import cz.muni.ics.ga4gh.base.properties.BasicAuthProperties;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfigurer {

    private static final String ROLE_USER = "ROLE_USER";

    public static final String PUBLIC_ENDPOINTS_PATH = "/public";
    public static final String GA4GH_ENDPOINTS_PATH = "/ga4gh";
    private static final String PUBLIC_ENDPOINTS_PREFIX = PUBLIC_ENDPOINTS_PATH + "/**";

    private final List<BasicAuthCredentials> basicAuthCredentialsList;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public WebSecurityConfigurer(BasicAuthProperties basicAuthProperties,
                                 PasswordEncoder passwordEncoder)
    {
        this.basicAuthCredentialsList = basicAuthProperties.getCredentials();
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> configurer = auth.inMemoryAuthentication();
        for (BasicAuthCredentials credentials: basicAuthCredentialsList) {
            configurer.withUser(credentials.getUsername())
                .password(passwordEncoder.encode(credentials.getPassword()))
                .authorities(ROLE_USER);
        }
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers(PUBLIC_ENDPOINTS_PREFIX).permitAll()
                .anyRequest().authenticated()
                .and()
                .httpBasic();

        http.headers().frameOptions().sameOrigin();

        return http.build();
    }
}
