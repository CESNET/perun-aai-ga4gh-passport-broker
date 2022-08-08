package cz.muni.ics.ga4gh.facade.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import cz.muni.ics.ga4gh.base.exceptions.UserNotFoundException;
import cz.muni.ics.ga4gh.base.exceptions.UserNotUniqueException;
import cz.muni.ics.ga4gh.base.model.Ga4ghPassport;
import cz.muni.ics.ga4gh.facade.Ga4ghBrokerFacade;
import cz.muni.ics.ga4gh.service.Ga4ghBrokerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class Ga4GhBrokerFacadeImpl implements Ga4ghBrokerFacade {

    private final Ga4ghBrokerService ga4ghBrokerService;

    @Autowired
    public Ga4GhBrokerFacadeImpl(Ga4ghBrokerService ga4ghBrokerService) {
        this.ga4ghBrokerService = ga4ghBrokerService;
    }

    @Override
    public JsonNode getGa4ghPassport(String userIdentifier)
        throws UserNotFoundException, UserNotUniqueException
    {
        if (!StringUtils.hasText(userIdentifier)) {
            throw new IllegalArgumentException("User identifier cannot be empty");
        }

        Long perunUserId = ga4ghBrokerService.identifyUser(userIdentifier);
        if (perunUserId == null) {
            throw new UserNotFoundException("No user found for given identifier");
        }
        Ga4ghPassport passport = ga4ghBrokerService.getGa4ghPassport(perunUserId);
        if (passport == null) {
            return JsonNodeFactory.instance.nullNode();
        } else {
            return passport.toJsonObject();
        }
    }

}
