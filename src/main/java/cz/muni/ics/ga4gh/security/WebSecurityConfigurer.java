package cz.muni.ics.ga4gh.security;

import cz.muni.ics.ga4gh.config.BasicAuthConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfigurer {

    private static final String ROLE_USER = "ROLE_USER";
    private static final String PUBLIC_ENDPOINTS_PREFIX = "/public/**";

    private final String username;
    private final String password;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public WebSecurityConfigurer(BasicAuthConfig config, PasswordEncoder passwordEncoder) {
        this.username = config.getUsername();
        this.password = config.getPassword();
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser(username).password(passwordEncoder.encode(password))
                .authorities(ROLE_USER);
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
