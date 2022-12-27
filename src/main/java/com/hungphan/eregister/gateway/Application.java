package com.hungphan.eregister.gateway;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.Collections;
import java.util.Set;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

@SpringBootApplication
public class Application {

    @Autowired
    AuthenticationFilter authenticationFilter;

    @Autowired
    private GeneralPathFilter generalPathFilter;

    @Autowired
    private WebsocketPathFilter websocketPathFilter;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    void init() {
    }

    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(p -> p.path("/user/login").uri("lb://eregister-user-service"))
                .route(p -> p.path("/user/check-authentication").uri("lb://eregister-user-service"))
                .route(p -> p.path("/user/**").uri("lb://eregister-user-service"))
                .route(p -> p.path("/eregister-service/get-client-session-id").filters(f -> f.filter(generalPathFilter)).uri("no://op"))
                .route(p -> p.path("/eregister-service/websocket/**").filters(f -> f.filter(websocketPathFilter)).uri("no://op"))
    //                .route(p -> p.path("/**").uri("http://localhost:9998"))
                .route(p -> p.path("/eregister-service/**").filters(f -> f.filters(authenticationFilter, generalPathFilter)).uri("no://op"))
                .route(p -> p.path("/**").uri("http://eregister-frontend:3000"))
                .build();
    }

}

@Component
class LoggingFilter implements GlobalFilter {
    Log log = LogFactory.getLog(getClass());

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Set<URI> uris = exchange.getAttributeOrDefault(GATEWAY_ORIGINAL_REQUEST_URL_ATTR, Collections.emptySet());
        String originalUri = (uris.isEmpty()) ? "Unknown" : uris.iterator().next().toString();
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        URI routeUri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        log.info("Incoming request " + originalUri + " is routed to id: " + route.getId()
                + ", uri:" + routeUri);
        return chain.filter(exchange);
    }
}
