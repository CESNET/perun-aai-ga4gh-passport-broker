package cz.muni.ics.ga4gh.web.controllers;

import static cz.muni.ics.ga4gh.web.security.WebSecurityConfigurer.GA4GH_ENDPOINTS_PATH;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.ics.ga4gh.base.exceptions.InvalidRequestParametersException;
import cz.muni.ics.ga4gh.base.exceptions.UserNotFoundException;
import cz.muni.ics.ga4gh.base.exceptions.UserNotUniqueException;
import cz.muni.ics.ga4gh.facade.Ga4ghBrokerFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(GA4GH_ENDPOINTS_PATH)
public class Ga4ghBrokerController {

    private final Ga4ghBrokerFacade ga4GhBrokerFacade;

    @Autowired
    public Ga4ghBrokerController(Ga4ghBrokerFacade ga4ghBrokerFacade) {
        this.ga4GhBrokerFacade = ga4ghBrokerFacade;
    }

    @GetMapping(value = "/{user_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode getGa4ghPassport(@PathVariable(name = "user_id") String userIdentifier)
        throws UserNotFoundException, UserNotUniqueException, InvalidRequestParametersException
    {
        if (!StringUtils.hasText(userIdentifier)) {
            throw new InvalidRequestParametersException("No user identifier specified");
        }
        return ga4GhBrokerFacade.getGa4ghPassport(userIdentifier);
    }

}
