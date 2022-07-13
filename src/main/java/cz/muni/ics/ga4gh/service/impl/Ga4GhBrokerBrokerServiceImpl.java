package cz.muni.ics.ga4gh.service.impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import cz.muni.ics.ga4gh.adapters.PerunAdapter;
import cz.muni.ics.ga4gh.config.AttributesConfig;
import cz.muni.ics.ga4gh.config.BrokerConfig;
import cz.muni.ics.ga4gh.exceptions.UserNotUniqueException;
import cz.muni.ics.ga4gh.model.AttributeMapping;
import cz.muni.ics.ga4gh.service.impl.brokers.Ga4ghBroker;
import cz.muni.ics.ga4gh.service.Ga4ghBrokerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class Ga4GhBrokerBrokerServiceImpl implements Ga4ghBrokerService {

    private final Ga4ghBroker broker;

    private final PerunAdapter adapter;

    private final List<String> attributesToSearch;

    private final Map<String, AttributeMapping> attributes;

    @Autowired
    public Ga4GhBrokerBrokerServiceImpl(Ga4ghBroker broker, PerunAdapter adapter, BrokerConfig brokerConfig, AttributesConfig attributesConfig) {
        this.broker = broker;
        this.adapter = adapter;
        this.attributesToSearch = brokerConfig.getAttributesToSearch();
        this.attributes = attributesConfig.getAttributeMappings();
    }

    @Override
    public ArrayNode getGa4ghPassport(String eppn) {
        Set<Long> userIds = new HashSet<>();

        for (String attrName : attributesToSearch) {
            if (attributes.get(attrName) != null) {
                userIds.addAll(adapter.getAdapterPrimary().getUserIdsByAttributeValue(attributes.get(attrName), eppn));
            }
        }

        if (userIds.isEmpty()) {
            log.debug("User {} not found", eppn);
            return null;
        }

        if (userIds.size() > 1) {
            throw new UserNotUniqueException("There are more users found by " + eppn + " - " + String.join(", ", userIds.toString()));
        }

        return broker.constructGa4ghPassportVisa(userIds.iterator().next(), eppn);
    }
}
