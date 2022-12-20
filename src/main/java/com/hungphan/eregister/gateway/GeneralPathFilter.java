package com.hungphan.eregister.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

@Component
public class GeneralPathFilter implements GatewayFilter, Ordered {

    private static final String CLIENT_SESSION_ID_NAME = "CLIENT_SESSION_ID";

    @Value("${eregister.name}")
    private String eregisterName;

    @Value("${eregister.http.port}")
    private String eregisterHttpPort;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Override
    public int getOrder() {
        return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        List<ServiceInstance> list = discoveryClient.getInstances(eregisterName);
        String host = null;
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(CLIENT_SESSION_ID_NAME);
        if (cookie != null) {
            String sessionId = cookie.getValue();
            for (ServiceInstance serviceInstance : list) {
                if (serviceInstance.getHost().equals(sessionId)) {
                    host = serviceInstance.getHost();
                }
            }
        }
        if (host == null) {
            host = list.get((Commons.ipSelectCount)%list.size()).getHost();
            if(exchange.getRequest().getPath().toString().contains("get-client-session-id")) Commons.ipSelectCount++;
        }

        String newUrl = "http://" + host + ":" + eregisterHttpPort + exchange.getRequest().getPath();
        URI uri = null;
        try {
            uri = new URI(newUrl);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, uri);
        return chain.filter(exchange);
    }
}
