package com.sthree.file.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.UUID;

/*
    Aspect for logging file service operations
    Provides structured logging in JSON format (Logstash) for all
    controller and service operations
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /**
     * Pointcut for all controller methods
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerPointcut() {}

    /**
     * Pointcut for all service methods
     */
    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void servicePointcut() {}

    /**
     * Around advice for controller methods
     */
    @Around("controllerPointcut()")
    public Object logControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String traceId = generateTraceId();

        HttpServletRequest request = getCurrentRequest();
        String method = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : "UNKNOWN";
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        // Log request
        log.info("{\"timestamp\":\"{}\",\"level\":\"INFO\",\"service_name\":\"file-service\"," +
                "\"logger\":\"{}\",\"message\":\"API request\",\"trace_id\":\"{}\",\"http_method\":\"{}\"," +
                "\"api_endpoint\":\"{}\",\"method\":\"{}.{}\"}",
                java.time.Instant.now().toString(),
                className,
                traceId,
                method,
                uri,
                className,
                methodName);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            // Log response
            log.info("{\"timestamp\":\"{}\",\"level\":\"INFO\",\"service_name\":\"file-service\"," +
                    "\"logger\":\"{}\",\"message\":\"API response\",\"trace_id\":\"{}\"," +
                    "\"http_method\":\"{}\",\"api_endpoint\":\"{}\",\"duration_ms\":{},\"response_code\":200}",
                    java.time.Instant.now().toString(),
                    className,
                    traceId,
                    method,
                    uri,
                    duration);

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            // Log error
            log.error("{\"timestamp\":\"{}\",\"level\":\"ERROR\",\"service_name\":\"file-service\"," +
                    "\"logger\":\"{}\",\"message\":\"API error\",\"trace_id\":\"{}\"," +
                    "\"http_method\":\"{}\",\"api_endpoint\":\"{}\",\"duration_ms\":{},\"exception\":\"{}\",\"error_message\":\"{}\"}",
                    java.time.Instant.now().toString(),
                    className,
                    traceId,
                    method,
                    uri,
                    duration,
                    e.getClass().getName(),
                    e.getMessage());

            throw e;
        }
    }

    /**
     * Around advice for service methods
     */
    @Around("servicePointcut() && execution(* com.sthree.file.service..*(..))")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        // Extract relevant parameters for logging
        Object[] args = joinPoint.getArgs();
        String userId = extractUserId(args);
        String fileId = extractFileId(args);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            // Log success
            log.debug("{\"timestamp\":\"{}\",\"level\":\"DEBUG\",\"service_name\":\"file-service\"," +
                    "\"logger\":\"{}\",\"message\":\"Service method completed\",\"method\":\"{}\",\"duration_ms\":{}{}}",
                    java.time.Instant.now().toString(),
                    className,
                    methodName,
                    duration,
                    formatContext(userId, fileId));

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            // Log error
            log.error("{\"timestamp\":\"{}\",\"level\":\"ERROR\",\"service_name\":\"file-service\"," +
                    "\"logger\":\"{}\",\"message\":\"Service method failed\",\"method\":\"{}\",\"duration_ms\":{}," +
                    "\"exception\":\"{}\",\"error_message\":\"{}\"{}}",
                    java.time.Instant.now().toString(),
                    className,
                    methodName,
                    duration,
                    e.getClass().getName(),
                    e.getMessage(),
                    formatContext(userId, fileId));

            throw e;
        }
    }

    /**
     * Get the current HTTP request
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * Generate a trace ID for request tracking
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Extract user ID from method arguments
     */
    private String extractUserId(Object[] args) {
        return Arrays.stream(args)
                .filter(arg -> arg instanceof UUID)
                .map(arg -> ((UUID) arg).toString())
                .findFirst()
                .orElse(null);
    }

    /**
     * Extract file ID from method arguments
     */
    private String extractFileId(Object[] args) {
        return Arrays.stream(args)
                .filter(arg -> arg instanceof UUID)
                .skip(1)
                .map(arg -> ((UUID) arg).toString())
                .findFirst()
                .orElse(null);
    }

    /**
     * Format context for logging
     */
    private String formatContext(String userId, String fileId) {
        StringBuilder sb = new StringBuilder();
        if (userId != null) {
            sb.append(",\"user_id\":\"").append(userId).append("\"");
        }
        if (fileId != null) {
            sb.append(",\"file_id\":\"").append(fileId).append("\"");
        }
        return sb.toString();
    }
}
