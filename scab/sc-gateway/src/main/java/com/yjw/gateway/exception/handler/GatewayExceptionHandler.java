package com.yjw.gateway.exception.handler;

import com.yjw.common.constants.Constant;
import com.yjw.common.domain.R;
import com.yjw.common.exceptions.CommonException;
import com.yjw.common.exceptions.UnauthorizedException;
import com.yjw.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.yjw.common.constants.ErrorInfo.Code.FAILED;
import static com.yjw.common.constants.ErrorInfo.Msg.SERVER_INTER_ERROR;

/**
 * 实现 ErrorWebExceptionHandler：网关专属响应式全局异常捕获接口（Gateway 是 WebFlux 响应式，不能用 SpringMVC 的 @ControllerAdvice）；
 * Ordered + HIGHEST_PRECEDENCE：最高优先级，优先拦截网关所有异常；
 * 作用：捕获网关层面所有报错，统一格式化 JSON 返回给前端，区分不同异常做差异化处理。
 */
@Slf4j
@Component
public class GatewayExceptionHandler implements ErrorWebExceptionHandler, Ordered {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // 1.获取响应
        ServerHttpResponse response = exchange.getResponse();
        // 2.判断是否已处理
        if (response.isCommitted()) {
            // 如果已经提交，直接结束，避免重复处理
            return Mono.error(ex);
        }

        // 3.按照异常类型进行翻译处理，翻译的结果易于前端理解
        String message;
        int code = FAILED;
        if (ex instanceof UnauthorizedException) {
            // 登录异常，直接返回状态码
            UnauthorizedException e = (UnauthorizedException) ex;
            return Mono.error(new ResponseStatusException(e.getStatus(), e.getMessage(), e));
        } else if (ex instanceof CommonException) {
            CommonException e = (CommonException) ex;
            code = e.getCode();
            message = e.getMessage();
        } else if (ex instanceof NotFoundException) {
            message = "服务不存在";
        } else if (ex instanceof ResponseStatusException) {
            message = ex.getMessage();
        } else {
            message = SERVER_INTER_ERROR;
            // 4.记录日志
            writeLog(exchange, ex);
        }
        // 5.设置响应结果为 JSON
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // 6.封装响应结果并写出
        R<Object> r = R.error(code, message);
        List<String> requestIds = response.getHeaders().get(Constant.REQUEST_ID_HEADER);
        if (requestIds != null) {
            r.requestId(requestIds.get(0));
        }
        byte[] resp = JsonUtils.toJsonStr(r).getBytes(StandardCharsets.UTF_8);
        return response.writeWith(
                Mono.fromSupplier(
                        () -> response.bufferFactory().wrap(resp)
                ));
    }

    private void writeLog(ServerWebExchange exchange, Throwable ex) {
        ServerHttpRequest request = exchange.getRequest();
        URI uri = request.getURI();
        String host = uri.getHost();
        int port = uri.getPort();
        log.error("网关路由异常-host:{} ,port:{}，uri:{},  errormessage:",
                host, port, request.getPath(), ex);
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}