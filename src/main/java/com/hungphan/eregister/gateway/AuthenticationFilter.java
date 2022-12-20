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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

@Component
public class AuthenticationFilter implements GatewayFilter, Ordered {

    private static final String CLIENT_SESSION_ID_NAME = "CLIENT_SESSION_ID";

    @Value("${eregister.name}")
    private String eregisterServiceName;

    @Value("${eregister.http.port}")
    private String eregisterHttpPort;

    @Value("${eregisterUserService.name}")
    private String userServiceName;

    @Value("${eregisterUserService.port}")
    private String userServicePort;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Override
    public int getOrder() {
        return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        System.out.println(token);

        List<ServiceInstance> list = discoveryClient.getInstances(userServiceName);
        WebClient webClient = WebClient.create("http://" + list.get(0).getHost() + ":" + list.get(0).getPort());

        return webClient.get().uri("/user/check-authentication")
                .header(HttpHeaders.AUTHORIZATION, token)
                .exchangeToMono(webClientResponse -> {
                    if (webClientResponse.statusCode().equals(HttpStatus.OK)) {
                        String newUrl = "lb://" + userServiceName;
                        URI uri = null;
                        try {
                            uri = new URI(newUrl);
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, uri);
                        return chain.filter(exchange);
                    }
//                    return response.createException().flatMap(Mono::error);
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return response.setComplete();
                });
    }
}
