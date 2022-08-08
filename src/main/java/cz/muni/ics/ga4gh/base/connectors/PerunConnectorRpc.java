package cz.muni.ics.ga4gh.base.connectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import cz.muni.ics.ga4gh.base.properties.PerunRpcConnectorProperties;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Getter
@Slf4j
@Validated
public class PerunConnectorRpc {

    @NotBlank
    private final String url;

    private final boolean enabled;

    @NotBlank
    private final String serializer;

    @NotNull
    private RestTemplate restTemplate;

    private final long connectionTimeout;
    private final long connectionRequestTimeout;
    private final long requestTimeout;

    @Autowired
    public PerunConnectorRpc(PerunRpcConnectorProperties rpcProperties) {
        this.url = setUrl(rpcProperties.getUrl());
        this.enabled = rpcProperties.isEnabled();
        this.serializer = setSerializer(rpcProperties.getSerializer());
        this.connectionTimeout = rpcProperties.getConnectionTimeout();
        this.connectionRequestTimeout = rpcProperties.getConnectionRequestTimeout();
        this.requestTimeout = rpcProperties.getRequestTimeout();
        initRestTemplate(rpcProperties);
    }

    private String setUrl(String url) {
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("Perun URL cannot be null or empty");
        } else if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return url;
    }

    private String setSerializer(String serializer) {
        if (!StringUtils.hasText(serializer)) {
            serializer = "json";
        }
        return serializer;
    }

    public void initRestTemplate(PerunRpcConnectorProperties rpcProperties) {
        restTemplate = new RestTemplate();
        //HTTP connection pooling, see https://howtodoinjava.com/spring-restful/resttemplate-httpclient-java-config/
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(rpcProperties.getConnectionRequestTimeout()) // The timeout when requesting a connection from the connection manager
                .setConnectTimeout(rpcProperties.getConnectionTimeout()) // Determines the timeout in milliseconds until a connection is established
                .setSocketTimeout(rpcProperties.getRequestTimeout()) // The timeout for waiting for data
                .build();

        PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager();
        poolingConnectionManager.setMaxTotal(20); // maximum connections total
        poolingConnectionManager.setDefaultMaxPerRoute(18);

        ConnectionKeepAliveStrategy connectionKeepAliveStrategy = (response, context) -> {
            HeaderElementIterator it = new BasicHeaderElementIterator(
                response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();

                if (value != null && param.equalsIgnoreCase("timeout")) {
                    return Long.parseLong(value) * 1000;
                }
            }

            return 20000L;
        };

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(poolingConnectionManager)
                .setKeepAliveStrategy(connectionKeepAliveStrategy)
                .build();

        HttpComponentsClientHttpRequestFactory poolingRequestFactory = new HttpComponentsClientHttpRequestFactory();
        poolingRequestFactory.setHttpClient(httpClient);
        //basic authentication
        List<ClientHttpRequestInterceptor> interceptors = Collections.singletonList(
            new BasicAuthenticationInterceptor(
                rpcProperties.getUsername(),
                rpcProperties.getPassword()
            ));
        InterceptingClientHttpRequestFactory authenticatingRequestFactory = new InterceptingClientHttpRequestFactory(poolingRequestFactory, interceptors);
        restTemplate.setRequestFactory(authenticatingRequestFactory);
    }

    /**
     * Make post call to Perun RPC
     * @param manager String value representing manager to be called. Use constants from this class.
     * @param method Method to be called (i.e. getUserById)
     * @param map Map of parameters to be passed as request body
     * @return Response from Perun
     */
    public JsonNode post(String manager, String method, Map<String, Object> map) {
        if (!this.enabled) {
            return JsonNodeFactory.instance.nullNode();
        }

        String actionUrl = url + '/' + serializer + '/' + manager + '/' + method;
        //make the call
        try {
            log.debug("Calling Perun - URL '{}' with parameters '{}'", actionUrl, map);
            return restTemplate.postForObject(actionUrl, map, JsonNode.class);
        } catch (HttpClientErrorException ex) {
            MediaType contentType = null;
            if (ex.getResponseHeaders() != null) {
                contentType = ex.getResponseHeaders().getContentType();
            }
            String body = ex.getResponseBodyAsString();
            log.error("HTTP ERROR when calling Perun RPC - {}, {}, {}",
                ex.getRawStatusCode(), contentType, actionUrl);

            if (contentType != null && "json".equals(contentType.getSubtype())) {
                try {
                    log.error(new ObjectMapper().readValue(body, JsonNode.class)
                        .path("message").asText());
                } catch (IOException e) {
                    log.error("cannot parse error message from JSON", e);
                }
            } else {
                log.error(ex.getMessage());
            }

            throw new RuntimeException("cannot connect to Perun RPC", ex);
        }
    }

}
