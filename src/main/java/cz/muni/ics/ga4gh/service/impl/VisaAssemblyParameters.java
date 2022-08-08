package cz.muni.ics.ga4gh.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.ics.ga4gh.service.JWTSigningAndValidationService;
import java.net.URI;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class VisaAssemblyParameters {

    private String issuer;
    private URI jku;
    private String type;
    private String sub;
    private Long userId;
    private String value;
    private String source;
    private String by;
    private long asserted;
    private long expires;
    private JsonNode conditions;
    private String signer;
    private JWTSigningAndValidationService jwtService;

}
