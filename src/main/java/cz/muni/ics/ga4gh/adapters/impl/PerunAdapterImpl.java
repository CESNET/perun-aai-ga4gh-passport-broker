package cz.muni.ics.ga4gh.adapters.impl;

import cz.muni.ics.ga4gh.adapters.PerunAdapter;
import cz.muni.ics.ga4gh.config.AdapterConfig;
import cz.muni.ics.ga4gh.model.Affiliation;
import cz.muni.ics.ga4gh.model.AttributeMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class PerunAdapterImpl extends PerunAdapter {

    @Autowired
    public PerunAdapterImpl(AdapterConfig config, PerunAdapterRpc adapterRpc, PerunAdapterLdap adapterLdap) {
        super(config, adapterRpc, adapterLdap);
    }

    @Override
    public Long getPreauthenticatedUserId(String extLogin, String extSourceName) {
        try {
            return this.getAdapterPrimary().getPreauthenticatedUserId(extLogin, extSourceName);
        } catch (UnsupportedOperationException e) {
            if (this.isCallFallback()) {
                return this.getAdapterFallback().getPreauthenticatedUserId(extLogin, extSourceName);
            } else {
                throw e;
            }
        }
    }

    @Override
    public boolean isUserInGroup(Long userId, Long groupId) {
        try {
            return this.getAdapterPrimary().isUserInGroup(userId, groupId);
        } catch (UnsupportedOperationException e) {
            if (this.isCallFallback()) {
                return this.getAdapterFallback().isUserInGroup(userId, groupId);
            } else {
                throw e;
            }
        }
    }

    @Override
    public List<Affiliation> getGroupAffiliations(Long userId, String groupAffiliationsAttr) {
        try {
            return this.getAdapterPrimary().getGroupAffiliations(userId, groupAffiliationsAttr);
        } catch (UnsupportedOperationException e) {
            if (this.isCallFallback()) {
                return this.getAdapterFallback().getGroupAffiliations(userId, groupAffiliationsAttr);
            } else {
                throw e;
            }
        }
    }

    @Override
    public Set<Long> getUserIdsByAttributeValue(AttributeMapping attrName, String attrValue) {
        try {
            return this.getAdapterPrimary().getUserIdsByAttributeValue(attrName, attrValue);
        } catch (UnsupportedOperationException e) {
            if (this.isCallFallback()) {
                return this.getAdapterFallback().getUserIdsByAttributeValue(attrName, attrValue);
            } else {
                throw e;
            }
        }
    }
}
