package cz.muni.ics.ga4gh.base.model;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class Ga4ghPassport {

    private final List<Ga4ghPassportVisa> visas = new ArrayList<>();

    public void addVisas(List<Ga4ghPassportVisa> visas) {
        if (visas == null || visas.isEmpty()) {
            return;
        }
        this.visas.addAll(visas);
    }

    public ArrayNode toJsonObject() {
        ArrayNode passport = JsonNodeFactory.instance.arrayNode();
        if (!visas.isEmpty()) {
            for (Ga4ghPassportVisa visa: visas) {
                passport.add(visa.serialize());
            }
        }
        return passport;
    }
}
