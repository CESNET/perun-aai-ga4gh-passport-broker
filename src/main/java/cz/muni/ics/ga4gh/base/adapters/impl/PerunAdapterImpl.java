package cz.muni.ics.ga4gh.base.adapters.impl;

import cz.muni.ics.ga4gh.base.adapters.PerunAdapter;
import cz.muni.ics.ga4gh.base.adapters.PerunAdapterMethods;
import cz.muni.ics.ga4gh.base.exceptions.ConfigurationException;
import cz.muni.ics.ga4gh.base.exceptions.PerunAdapterOperationException;
import cz.muni.ics.ga4gh.base.model.Affiliation;
import cz.muni.ics.ga4gh.base.properties.PerunAdapterProperties;
import java.util.List;
import java.util.Set;

public class PerunAdapterImpl extends PerunAdapter {


    public PerunAdapterImpl(PerunAdapterProperties config,
                            PerunAdapterRpc adapterRpc,
                            PerunAdapterLdap adapterLdap)
        throws ConfigurationException
    {
        super(config, adapterRpc, adapterLdap);
    }

    public PerunAdapterImpl(PerunAdapterProperties config,
                            PerunAdapterRpc adapterRpc)
        throws ConfigurationException
    {
        super(config, adapterRpc, null);
    }

    @Override
    public Long getPerunUserId(String extLogin, String extSourceName) {
        try {
            for (PerunAdapterMethods adapter: getAdaptersChain()) {
                return adapter.getPerunUserId(extLogin, extSourceName);
            }
        } catch (UnsupportedOperationException e) {
            if (!this.isCallFallback()) {
                throw e;
            }
        }
        throw new PerunAdapterOperationException("No adapter able to perform call");
    }

    @Override
    public boolean isUserInGroup(Long userId, Long groupId) {
        try {
            for (PerunAdapterMethods adapter: getAdaptersChain()) {
                return adapter.isUserInGroup(userId, groupId);
            }
        } catch (UnsupportedOperationException e) {
            if (!this.isCallFallback()) {
                throw e;
            }
        }
        throw new PerunAdapterOperationException("No adapter able to perform call");
    }

    @Override
    public List<Affiliation> getGroupAffiliations(Long userId, String groupAffiliationsAttr) {
        try {
            for (PerunAdapterMethods adapter: getAdaptersChain()) {
                return adapter.getGroupAffiliations(userId, groupAffiliationsAttr);
            }
        } catch (UnsupportedOperationException e) {
            if (!this.isCallFallback()) {
                throw e;
            }
        }
        throw new PerunAdapterOperationException("No adapter able to perform call");
    }

    @Override
    public List<Affiliation> getGroupAffiliations(Long userId, Long voId, String groupAffiliationsAttr) {
        try {
            for (PerunAdapterMethods adapter: getAdaptersChain()) {
                return adapter.getGroupAffiliations(userId, voId, groupAffiliationsAttr);
            }
        } catch (UnsupportedOperationException e) {
            if (!this.isCallFallback()) {
                throw e;
            }
        }
        throw new PerunAdapterOperationException("No adapter able to perform call");
    }

    @Override
    public Set<Long> getUserIdsByAttributeValue(String attrName, String attrValue) {
        try {
            for (PerunAdapterMethods adapter: getAdaptersChain()) {
                return adapter.getUserIdsByAttributeValue(attrName, attrValue);
            }
        } catch (UnsupportedOperationException e) {
            if (!this.isCallFallback()) {
                throw e;
            }
        }
        throw new PerunAdapterOperationException("No adapter able to perform call");
    }

    @Override
    public String getUserSub(Long userId, String subAttribute) {
        try {
            for (PerunAdapterMethods adapter: getAdaptersChain()) {
                return adapter.getUserSub(userId, subAttribute);
            }
        } catch (UnsupportedOperationException e) {
            if (!this.isCallFallback()) {
                throw e;
            }
        }
        throw new PerunAdapterOperationException("No adapter able to perform call");
    }

    @Override
    public boolean isUserInVo(Long userId, Long voId) {
        try {
            for (PerunAdapterMethods adapter: getAdaptersChain()) {
                return adapter.isUserInVo(userId, voId);
            }
        } catch (UnsupportedOperationException e) {
            if (!this.isCallFallback()) {
                throw e;
            }
        }
        throw new PerunAdapterOperationException("No adapter able to perform call");
    }

}
