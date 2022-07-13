package cz.muni.ics.ga4gh;

import cz.muni.ics.ga4gh.config.BrokerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.net.MalformedURLException;

@Configuration
public class ApplicationContext {

    private final String pathToJwkFile;

    public ApplicationContext(BrokerConfig config) {
        this.pathToJwkFile = config.getPathToJwkFile();
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Resource jwks() throws MalformedURLException {
        return new FileUrlResource(pathToJwkFile);
    }
}
