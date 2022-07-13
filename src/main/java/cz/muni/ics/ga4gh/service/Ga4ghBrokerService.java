package cz.muni.ics.ga4gh.service;

import com.fasterxml.jackson.databind.node.ArrayNode;

public interface Ga4ghBrokerService {

    ArrayNode getGa4ghPassport(String eppn);
}
