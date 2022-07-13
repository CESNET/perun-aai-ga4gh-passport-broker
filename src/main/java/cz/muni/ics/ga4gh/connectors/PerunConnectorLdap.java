package cz.muni.ics.ga4gh.connectors;

import cz.muni.ics.ga4gh.aop.LogTimes;
import cz.muni.ics.ga4gh.config.LdapConfig;
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
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Repository
@Slf4j
@Getter
public class PerunConnectorLdap implements DisposableBean {

    private final String baseDN;
    private final LdapConnectionPool pool;
    private final LdapConnectionTemplate ldap;

    @Autowired
    public PerunConnectorLdap(LdapConfig config) {
        if (config.getHost() == null || config.getHost().trim().isEmpty()) {
            throw new IllegalArgumentException("Host cannot be null or empty");
        } else if (config.getBaseDn() == null || config.getBaseDn().trim().isEmpty()) {
            throw new IllegalArgumentException("baseDN cannot be null or empty");
        }

        boolean useTLS = Objects.requireNonNullElse(config.getUseTls(), false);
        boolean useSSL = Objects.requireNonNullElse(config.getUseSsl(), false);
        boolean allowUntrustedSsl = Objects.requireNonNullElse(config.getAllowUntrustedSsl(), false);
        long timeoutSecs = Objects.requireNonNullElse(config.getTimeoutSecs(), 5L);


        this.baseDN = config.getBaseDn();

        LdapConnectionConfig ldapConnectionConfig = getLdapConnectionConfig(config.getHost(), config.getPort(), useTLS, useSSL, allowUntrustedSsl);
        if (config.getUser() != null && !config.getUser().isEmpty()) {
            log.debug("setting ldap user to {}", config.getUser());
            ldapConnectionConfig.setName(config.getUser());
        }
        if (config.getPassword() != null && !config.getPassword().isEmpty()) {
            log.debug("setting ldap password");
            ldapConnectionConfig.setCredentials(config.getPassword());
        }
        DefaultLdapConnectionFactory factory = new DefaultLdapConnectionFactory(ldapConnectionConfig);
        factory.setTimeOut(timeoutSecs * 1000L);

        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setTestOnBorrow(true);

        pool = new LdapConnectionPool(new DefaultPoolableLdapConnectionFactory(factory), poolConfig);
        ldap = new LdapConnectionTemplate(pool);
        log.debug("initialized LDAP connector");
    }

    public String getBaseDN() {
        return baseDN;
    }

    private LdapConnectionConfig getLdapConnectionConfig(String host, int port, boolean useTLS, boolean useSSL,
                                           boolean allowUntrustedSsl) {
        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost(host);
        config.setLdapPort(port);
        config.setUseSsl(useSSL);
        config.setUseTls(useTLS);
        if (allowUntrustedSsl) {
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
     * @param attributes Attributes to be fetch for entry
     * @param entryMapper Mapper of entries to the target class T
     * @param <T> Class that the result should be mapped to.
     * @return Found entry mapped to target class
     */
    @LogTimes
    public <T> T searchFirst(String dnPrefix, FilterBuilder filter, SearchScope scope, String[] attributes,
                             EntryMapper<T> entryMapper)
    {
        Dn fullDn = getFullDn(dnPrefix);
        return ldap.searchFirst(fullDn, filter, scope, attributes, entryMapper);
    }

    /**
     * Perform lookup for the entry that satisfies criteria.
     * @param dnPrefix Prefix to be added to the base DN. (i.e. ou=People) !DO NOT END WITH A COMMA!
     * @param attributes Attributes to be fetch for entry
     * @param entryMapper Mapper of entries to the target class T
     * @param <T> Class that the result should be mapped to.
     * @return Found entry mapped to target class
     */
    @LogTimes
    public <T> T lookup(String dnPrefix, String[] attributes, EntryMapper<T> entryMapper) {
        Dn fullDn = getFullDn(dnPrefix);
        return ldap.lookup(fullDn, attributes, entryMapper);
    }

    /**
     * Search for the entries satisfy criteria.
     * @param dnPrefix Prefix to be added to the base DN. (i.e. ou=People) !DO NOT END WITH A COMMA!
     * @param filter Filter for entries
     * @param scope Search scope
     * @param attributes Attributes to be fetch for entry
     * @param entryMapper Mapper of entries to the target class T
     * @param <T> Class that the result should be mapped to.
     * @return List of found entries mapped to target class
     */
    @LogTimes
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
