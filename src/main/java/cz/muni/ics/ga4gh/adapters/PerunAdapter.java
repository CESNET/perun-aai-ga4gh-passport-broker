package cz.muni.ics.ga4gh.adapters;

import cz.muni.ics.ga4gh.adapters.impl.PerunAdapterLdap;
import cz.muni.ics.ga4gh.adapters.impl.PerunAdapterRpc;
import cz.muni.ics.ga4gh.config.AdapterConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public abstract class PerunAdapter implements PerunAdapterMethods {

    private final String RPC = "rpc";

    private PerunAdapterMethods adapterPrimary;
    private PerunAdapterMethods adapterFallback;

    private PerunAdapterMethodsRpc adapterRpc;
    private PerunAdapterMethodsLdap adapterLdap;

    private boolean callFallback;

    public PerunAdapter(AdapterConfig config, PerunAdapterRpc adapterRpc, PerunAdapterLdap adapterLdap) {
        if (config.getAdapterPrimary() != null && config.getAdapterPrimary().equalsIgnoreCase(RPC)) {
            this.adapterPrimary = adapterRpc;
            this.adapterFallback = adapterLdap;
        } else {
            this.adapterPrimary = adapterLdap;
            this.adapterFallback = adapterRpc;
        }

        this.adapterRpc = adapterRpc;
        this.adapterLdap = adapterLdap;

        this.callFallback = Objects.requireNonNullElse(config.getCallFallback(), false);
    }
}
