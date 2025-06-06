package org.example.apigateway.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.example.apigateway.dto.common.ApiResponse;
import org.example.apigateway.service.IdentityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;


@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AuthenticationFilter implements GlobalFilter, Ordered {
    IdentityService identityService;
    ObjectMapper objectMapper;

    @NonFinal
//    private final String[] publicEndpoints = new String[]{".*"};
    private String[] publicEndpoints = {
            "/profile/info/.*",
            "/identity/auth/.*",
            "/identity/users/registration",
            "/notification/email/send",
            "/book/books/.*",
            "/search/search/.*",
            "/chapter/chapters/.*"

    };

    @Value("${app.api-prefix}")
    @NonFinal
    private String apiPrefix;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("Enter authentication filter....");

        if (isPublicEndpoint(exchange.getRequest())){
            log.info("this is a public endpoint: " + exchange.getRequest());
            return chain.filter(exchange);}
        else {
            log.info("this is not a public endpoint "+ exchange.getRequest());
        }

        // Get token from authorization header
        List<String> authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (CollectionUtils.isEmpty(authHeader)) {
            log.info("lỗi đính kèm header");
            return unauthenticated(exchange.getResponse());
        }

        String token = authHeader.get(0).replace("Bearer ", "");
        log.info("Token: {}", token);

        return identityService.introspect(token).flatMap(introspectResponse -> {
            if (introspectResponse.getResult().isValid()){
                log.info(token);
                return chain.filter(exchange);}
            else{
                log.info("lỗi introspect");
                return unauthenticated(exchange.getResponse());}
        }).onErrorResume(throwable ->{
            log.info("introspect đang bị throwable" );
            throwable.printStackTrace();
            return unauthenticated(exchange.getResponse());
        });
    }

    @Override
    public int getOrder() {
        return -1;
    }
    private boolean isPublicEndpoint(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        log.info("Checking path: {}", path);

        return Arrays.stream(publicEndpoints)
                .anyMatch(pattern -> {
                    String regex = "^" + apiPrefix + pattern;  // bắt đầu chuỗi
                    log.info("Matching regex: {}", regex);
                    return path.matches(regex);
                });
    }


    Mono<Void> unauthenticated(ServerHttpResponse response){
        log.info("gateway lỗi");
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(1401)
                .message("Unauthenticated, gateway")
                .build();

        String body = null;
        try {
            body = objectMapper.writeValueAsString(apiResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);


        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }
}