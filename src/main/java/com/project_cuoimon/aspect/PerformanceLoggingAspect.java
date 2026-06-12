package com.project_cuoimon.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PerformanceLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceLoggingAspect.class);

    // Ghi log thời gian thực hiện cho tất cả các phương thức trong Controller và Service
    @Around("execution(* com.project_cuoimon.controller..*(..)) || execution(* com.project_cuoimon.service..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        // Thực thi phương thức gốc
        Object proceed = joinPoint.proceed();

        long executionTime = System.currentTimeMillis() - start;

        // Log ra thông tin signature ngắn gọn và thời gian chạy (ms)
        logger.info("[PERFORMANCE] Method [{}] executed in {} ms", 
                joinPoint.getSignature().toShortString(), executionTime);

        return proceed;
    }
}
