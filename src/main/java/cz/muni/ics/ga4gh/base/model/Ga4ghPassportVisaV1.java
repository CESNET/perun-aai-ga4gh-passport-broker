package cz.muni.ics.ga4gh.base.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class Ga4ghPassportVisaV1 {
    public static final String GA4GH_VISA_V1 = "ga4gh_visa_v1";

    // === mandatory ===
    private long asserted;

    private String source;

    private String type;

    private String value;


    // === optional ===
    private String by;

    private JsonNode conditions;

}
