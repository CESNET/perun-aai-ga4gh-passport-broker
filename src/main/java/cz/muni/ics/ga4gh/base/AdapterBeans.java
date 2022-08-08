package cz.muni.ics.ga4gh.base;

import cz.muni.ics.ga4gh.base.adapters.PerunAdapter;
import cz.muni.ics.ga4gh.base.adapters.impl.PerunAdapterImpl;
import cz.muni.ics.ga4gh.base.adapters.impl.PerunAdapterLdap;
import cz.muni.ics.ga4gh.base.adapters.impl.PerunAdapterRpc;
import cz.muni.ics.ga4gh.base.connectors.PerunConnectorLdap;
import cz.muni.ics.ga4gh.base.connectors.PerunConnectorRpc;
import cz.muni.ics.ga4gh.base.exceptions.ConfigurationException;
import cz.muni.ics.ga4gh.base.properties.AttributeMappingProperties;
import cz.muni.ics.ga4gh.base.properties.PerunAdapterProperties;
import cz.muni.ics.ga4gh.base.properties.PerunLdapConnectorProperties;
import cz.muni.ics.ga4gh.base.properties.PerunRpcConnectorProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class AdapterBeans {

    @Bean
    public PerunAdapterRpc perunAdapterRpc(PerunConnectorRpc connectorRpc,
                                           AttributeMappingProperties attributeMappingProperties)
    {
        return new PerunAdapterRpc(connectorRpc, attributeMappingProperties);
    }

    @Bean
    public PerunConnectorRpc perunConnectorRpc(PerunRpcConnectorProperties rpcConnectorProperties) {
        return new PerunConnectorRpc(rpcConnectorProperties);
    }

    @Bean
    @ConditionalOnBean(PerunLdapConnectorProperties.class)
    public PerunConnectorLdap perunConnectorLdap(PerunLdapConnectorProperties ldapConnectorProperties)
    {
        return new PerunConnectorLdap(ldapConnectorProperties);
    }

    @Bean
    @ConditionalOnBean(PerunConnectorLdap.class)
    public PerunAdapterLdap perunAdapterLdap(PerunConnectorLdap connectorLdap,
                                             AttributeMappingProperties attributeMappingProperties)
    {
        return new PerunAdapterLdap(connectorLdap, attributeMappingProperties);
    }

    @Bean
    @ConditionalOnBean(PerunAdapterLdap.class)
    public PerunAdapter perunAdapterLdapRpc(PerunAdapterProperties adapterProperties,
                                            PerunAdapterRpc adapterRpc,
                                            PerunAdapterLdap adapterLdap)
        throws ConfigurationException
    {
        return new PerunAdapterImpl(adapterProperties, adapterRpc, adapterLdap);
    }

    @Bean
    @ConditionalOnMissingBean(PerunAdapterLdap.class)
    public PerunAdapter perunAdapterRpcOnly(PerunAdapterProperties adapterProperties,
                                            PerunAdapterRpc adapterRpc)
        throws ConfigurationException
    {
        return new PerunAdapterImpl(adapterProperties, adapterRpc);
    }

}
