package cz.muni.ics.ga4gh.service;

import cz.muni.ics.ga4gh.base.exceptions.UserNotFoundException;
import cz.muni.ics.ga4gh.base.exceptions.UserNotUniqueException;
import cz.muni.ics.ga4gh.base.model.Ga4ghPassport;

public interface Ga4ghBrokerService {

    Ga4ghPassport getGa4ghPassport(Long userId) throws UserNotFoundException, UserNotUniqueException;

    Long identifyUser(String userIdentifier)  throws UserNotFoundException, UserNotUniqueException;

}
