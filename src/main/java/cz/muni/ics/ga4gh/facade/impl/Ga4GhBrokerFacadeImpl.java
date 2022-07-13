package cz.muni.ics.ga4gh.facade.impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import cz.muni.ics.ga4gh.facade.Ga4ghBrokerFacade;
import cz.muni.ics.ga4gh.service.Ga4ghBrokerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Ga4GhBrokerFacadeImpl implements Ga4ghBrokerFacade {

    Ga4ghBrokerService ga4GhBrokerService;

    @Autowired
    public Ga4GhBrokerFacadeImpl(Ga4ghBrokerService ga4GhBrokerService) {
        this.ga4GhBrokerService = ga4GhBrokerService;
    }

    @Override
    public ArrayNode getGa4ghPassport(String eppn) {
        return ga4GhBrokerService.getGa4ghPassport(eppn);
    }
}
