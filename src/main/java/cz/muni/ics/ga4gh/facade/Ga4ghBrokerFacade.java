package cz.muni.ics.ga4gh.facade;

import com.fasterxml.jackson.databind.node.ArrayNode;

public interface Ga4ghBrokerFacade {

    ArrayNode getGa4ghPassport(String eppn);
}
