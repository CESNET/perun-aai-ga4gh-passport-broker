package cz.muni.ics.ga4gh.controllers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import cz.muni.ics.ga4gh.facade.Ga4ghBrokerFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/ga4gh")
public class Ga4ghBrokerController {

    private final Ga4ghBrokerFacade ga4GhBrokerFacade;

    @Autowired
    public Ga4ghBrokerController(Ga4ghBrokerFacade ga4GhBrokerFacade) {
        this.ga4GhBrokerFacade = ga4GhBrokerFacade;
    }

    @GetMapping(value = "/{eppn}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ArrayNode getGa4ghPassport(@PathVariable String eppn, HttpServletResponse response) {
        ArrayNode result = ga4GhBrokerFacade.getGa4ghPassport(eppn);

        if (result == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        return result;
    }
}
