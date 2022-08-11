package cz.muni.ics.ga4gh.service;

import cz.muni.ics.ga4gh.base.adapters.PerunAdapter;
import cz.muni.ics.ga4gh.base.exceptions.ConfigurationException;
import cz.muni.ics.ga4gh.base.properties.BrokerInstanceProperties;
import cz.muni.ics.ga4gh.base.properties.Ga4ghBrokersProperties;
import cz.muni.ics.ga4gh.service.impl.brokers.BbmriGa4ghBroker;
import cz.muni.ics.ga4gh.service.impl.brokers.ElixirGa4ghBroker;
import cz.muni.ics.ga4gh.service.impl.brokers.Ga4ghBroker;
import cz.muni.ics.ga4gh.service.impl.brokers.LifescienceRiGa4ghBroker;
import cz.muni.ics.ga4gh.service.impl.brokers.PerunGa4ghBroker;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class Ga4ghPassportBrokerBeans {

    @Autowired
    @Bean
    public List<Ga4ghBroker> passportBrokers(Ga4ghBrokersProperties ga4ghBrokersProperties,
                                             PerunAdapter perunAdapter,
                                             JWTSigningAndValidationService jwtService)
        throws ConfigurationException
    {
        List<Ga4ghBroker> brokers = new ArrayList<>();
        List<BrokerInstanceProperties> properties = ga4ghBrokersProperties.getBrokersProperties();
        for (BrokerInstanceProperties instanceProperties: properties) {
            brokers.add(initializeBroker(instanceProperties, ga4ghBrokersProperties, perunAdapter, jwtService));
        }
        return brokers;
    }

    private Ga4ghBroker initializeBroker(BrokerInstanceProperties instanceProperties,
                                         Ga4ghBrokersProperties ga4ghBrokersProperties,
                                         PerunAdapter perunAdapter,
                                         JWTSigningAndValidationService jwtService)
        throws ConfigurationException
    {
        String implementationClass = instanceProperties.getBrokerClass();
        switch (implementationClass) {
            case "BbmriGa4ghBroker":
                return new BbmriGa4ghBroker(instanceProperties, ga4ghBrokersProperties, perunAdapter, jwtService);
            case "ElixirGa4ghBroker":
                return new ElixirGa4ghBroker(instanceProperties, ga4ghBrokersProperties, perunAdapter, jwtService);
            case "LifescienceRiGa4ghBroker":
                return new LifescienceRiGa4ghBroker(instanceProperties, ga4ghBrokersProperties, perunAdapter, jwtService);
            case "PerunGa4ghBroker":
                return new PerunGa4ghBroker(instanceProperties, ga4ghBrokersProperties, perunAdapter, jwtService);
            default:
                throw new ConfigurationException("Invalid broker class name specified");
        }
    }
}
