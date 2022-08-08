package cz.muni.ics.ga4gh.facade;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.ics.ga4gh.base.exceptions.UserNotFoundException;
import cz.muni.ics.ga4gh.base.exceptions.UserNotUniqueException;

public interface Ga4ghBrokerFacade {

    JsonNode getGa4ghPassport(String userIdentifier)
        throws UserNotFoundException, UserNotUniqueException;

}
