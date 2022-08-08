package cz.muni.ics.ga4gh.base.connectors;

import cz.muni.ics.ga4gh.base.properties.PerunLdapConnectorProperties;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.DefaultLdapConnectionFactory;
import org.apache.directory.ldap.client.api.DefaultPoolableLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.NoVerificationTrustManager;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.apache.directory.ldap.client.template.EntryMapper;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

@Slf4j
public class PerunConnectorLdap implements DisposableBean {

    @Getter
    private final String baseDN;
    private final LdapConnectionPool pool;
    private final LdapConnectionTemplate ldap;

    @Autowired
    public PerunConnectorLdap(PerunLdapConnectorProperties perunLdapConnectorProperties) {
        if (!StringUtils.hasText(perunLdapConnectorProperties.getHost())) {
            throw new IllegalArgumentException("Host cannot be null or empty");
        } else if (!StringUtils.hasText(perunLdapConnectorProperties.getBaseDn())) {
            throw new IllegalArgumentException("baseDN cannot be null or empty");
        }

        this.baseDN = perunLdapConnectorProperties.getBaseDn();

        LdapConnectionConfig ldapConnectionConfig = getLdapConnectionConfig(
            perunLdapConnectorProperties);
        if (StringUtils.hasText(perunLdapConnectorProperties.getUser())) {
            log.debug("Setting ldap user to '{}'", perunLdapConnectorProperties.getUser());
            ldapConnectionConfig.setName(perunLdapConnectorProperties.getUser());
        }

        if (StringUtils.hasText(perunLdapConnectorProperties.getPassword())) {
            log.debug("Setting ldap password");
            ldapConnectionConfig.setCredentials(perunLdapConnectorProperties.getPassword());
        }

        DefaultLdapConnectionFactory factory = new DefaultLdapConnectionFactory(ldapConnectionConfig);
        factory.setTimeOut(perunLdapConnectorProperties.getTimeoutSecs() * 1000L);

        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setTestOnBorrow(true);

        this.pool = new LdapConnectionPool(new DefaultPoolableLdapConnectionFactory(factory), poolConfig);
        this.ldap = new LdapConnectionTemplate(pool);
        log.debug("initialized LDAP connector");
    }

    private LdapConnectionConfig getLdapConnectionConfig(
        PerunLdapConnectorProperties perunLdapConnectorProperties)
    {
        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost(perunLdapConnectorProperties.getHost());
        config.setLdapPort(perunLdapConnectorProperties.getPort());
        config.setUseSsl(perunLdapConnectorProperties.isUseSsl());
        config.setUseTls(perunLdapConnectorProperties.isUseTls());
        if (perunLdapConnectorProperties.isAllowUntrustedSsl()) {
            config.setTrustManagers(new NoVerificationTrustManager());
        }

        return config;
    }

    @Override
    public void destroy() {
        if (!pool.isClosed()) {
            pool.close();
        }
    }

    /**
     * Search for the first entry that satisfies criteria.
     * @param dnPrefix Prefix to be added to the base DN. (i.e. ou=People) !DO NOT END WITH A COMMA!
     * @param filter Filter for entries
     * @param scope Search scope
     * @param attributes Attributes to be fetched for entry
     * @param entryMapper Mapper of entries to the target class T
     * @param <T> Class that the result should be mapped to.
     * @return Found entry mapped to target class
     */
    public <T> T searchFirst(String dnPrefix, FilterBuilder filter, SearchScope scope, String[] attributes,
                             EntryMapper<T> entryMapper)
    {
        Dn fullDn = getFullDn(dnPrefix);
        return ldap.searchFirst(fullDn, filter, scope, attributes, entryMapper);
    }

    /**
     * Perform lookup for the entry that satisfies criteria.
     * @param dnPrefix Prefix to be added to the base DN. (i.e. ou=People) !DO NOT END WITH A COMMA!
     * @param attributes Attributes to be fetched for entry
     * @param entryMapper Mapper of entries to the target class T
     * @param <T> Class that the result should be mapped to.
     * @return Found entry mapped to target class
     */
    public <T> T lookup(String dnPrefix, String[] attributes, EntryMapper<T> entryMapper) {
        Dn fullDn = getFullDn(dnPrefix);
        return ldap.lookup(fullDn, attributes, entryMapper);
    }

    /**
     * Search for the entries satisfy criteria.
     * @param dnPrefix Prefix to be added to the base DN. (i.e. ou=People) !DO NOT END WITH A COMMA!
     * @param filter Filter for entries
     * @param scope Search scope
     * @param attributes Attributes to be fetched for entry
     * @param entryMapper Mapper of entries to the target class T
     * @param <T> Class that the result should be mapped to.
     * @return List of found entries mapped to target class
     */
    public <T> List<T> search(String dnPrefix, FilterBuilder filter, SearchScope scope, String[] attributes,
                              EntryMapper<T> entryMapper)
    {
        Dn fullDn = getFullDn(dnPrefix);
        return ldap.search(fullDn, filter, scope, attributes, entryMapper);
    }

    private Dn getFullDn(String prefix) {
        String dn = baseDN;
        if (StringUtils.hasText(prefix)) {
            dn = prefix + "," + baseDN;
        }

        return ldap.newDn(dn);
    }

}
