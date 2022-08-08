package cz.muni.ics.ga4gh.base.adapters;

import cz.muni.ics.ga4gh.base.adapters.impl.PerunAdapterLdap;
import cz.muni.ics.ga4gh.base.adapters.impl.PerunAdapterRpc;
import cz.muni.ics.ga4gh.base.exceptions.ConfigurationException;
import cz.muni.ics.ga4gh.base.properties.PerunAdapterProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public abstract class PerunAdapter implements PerunAdapterMethods {

    public static final String RPC = "RPC";
    public static final String LDAP = "LDAP";

    private final List<PerunAdapterMethods> adaptersChain = new ArrayList<>();
    private final PerunAdapterMethodsRpc adapterRpc;
    private final PerunAdapterMethodsLdap adapterLdap;
    private final boolean callFallback;

    public PerunAdapter(PerunAdapterProperties config,
                        PerunAdapterRpc adapterRpc,
                        PerunAdapterLdap adapterLdap)
        throws ConfigurationException
    {
        if (adapterRpc == null) {
            throw new ConfigurationException("No Perun RPC adapter configured");
        }

        if (RPC.equalsIgnoreCase(config.getAdapterPrimary())) {
            this.adaptersChain.add(adapterRpc);
            if (adapterLdap != null) {
                this.adaptersChain.add(adapterLdap);
            }
            this.adaptersChain.add(adapterRpc);
            this.adaptersChain.add(adapterLdap);
        } else if (LDAP.equalsIgnoreCase(config.getAdapterPrimary())) {
            if (adapterLdap == null) {
                throw new ConfigurationException("LDAP adapter specified as primary, but not defined");
            }
            this.adaptersChain.add(adapterLdap);
        } else {
            throw new ConfigurationException("Unrecognized primary adapter set");
        }

        this.adapterRpc = adapterRpc;
        this.adapterLdap = adapterLdap;
        this.callFallback = config.isCallFallback();
    }

}
