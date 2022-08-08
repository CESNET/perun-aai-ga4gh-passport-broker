package cz.muni.ics.ga4gh.service.impl;

import cz.muni.ics.ga4gh.base.adapters.PerunAdapter;
import cz.muni.ics.ga4gh.base.exceptions.UserNotFoundException;
import cz.muni.ics.ga4gh.base.exceptions.UserNotUniqueException;
import cz.muni.ics.ga4gh.base.model.Ga4ghPassport;
import cz.muni.ics.ga4gh.base.model.Ga4ghPassportVisa;
import cz.muni.ics.ga4gh.base.properties.Ga4ghBrokersProperties;
import cz.muni.ics.ga4gh.service.Ga4ghBrokerService;
import cz.muni.ics.ga4gh.service.impl.brokers.Ga4ghBroker;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class Ga4GhBrokerBrokerServiceImpl implements Ga4ghBrokerService {

    private final List<Ga4ghBroker> brokers = new ArrayList<>();

    private final PerunAdapter adapter;

    private final List<String> userIdentificationAttributes = new ArrayList<>();

    @Autowired
    public Ga4GhBrokerBrokerServiceImpl(List<Ga4ghBroker> brokers,
                                        PerunAdapter adapter,
                                        Ga4ghBrokersProperties brokersProperties)
    {
        this.brokers.addAll(brokers);
        this.adapter = adapter;
        this.userIdentificationAttributes.addAll(brokersProperties.getUserIdentificationAttributes());
    }

    @Override
    public Ga4ghPassport getGa4ghPassport(Long userId) {
        Ga4ghPassport passport = new Ga4ghPassport();
        long startTime = System.currentTimeMillis();
        for (Ga4ghBroker broker: brokers) {
            long localStartTime = System.currentTimeMillis();
            List<Ga4ghPassportVisa> visas = broker.constructGa4ghPassportVisas(userId);
            if (visas != null) {
                passport.addVisas(visas);
            }
            long localEndTime = System.currentTimeMillis();
            log.info("Generating for user '{}' in broker '{}({})' took {}ms", userId, broker.getBrokerName(), broker.getClass().getSimpleName(), localEndTime - localStartTime);
        }
        long endTime = System.currentTimeMillis();
        log.info("Generating for user '{}' in total took {}ms", userId, endTime - startTime);
        return passport;
    }

    @Override
    public Long identifyUser(String userIdentifier)
        throws UserNotFoundException, UserNotUniqueException
    {
        if (!StringUtils.hasText(userIdentifier)) {
            throw new IllegalArgumentException("User identifier cannot be empty");
        }

        Set<Long> perunUserIds = new HashSet<>();

        for (String attrName : userIdentificationAttributes) {
            Set<Long> foundUserIds = adapter.getUserIdsByAttributeValue(attrName, userIdentifier);
            if (foundUserIds == null || foundUserIds.isEmpty()) {
                log.debug("No user IDs found for identifier '{}' and attribute '{}'",
                    userIdentifier, attrName);
                continue;
            }
            perunUserIds.addAll(foundUserIds);
        }

        if (perunUserIds.isEmpty()) {
            log.debug("User with identifier '{}' not found", userIdentifier);
            throw new UserNotFoundException("User with identifier '" + userIdentifier + "' not found");
        }

        if (perunUserIds.size() > 1) {
            log.warn("Multiple users found with identifier '{}' - user IDS: '{}'", userIdentifier, perunUserIds);
            throw new UserNotUniqueException("More than one user found for identifier '" +
                userIdentifier + '\'');
        }
        return perunUserIds.iterator().next();
    }

}
