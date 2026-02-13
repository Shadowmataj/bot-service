package com.portability.bot_service.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class ServiceLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLoggingAspect.class);

    /**
     * Pointcut for all methods in service layer
     */
    @Pointcut("execution(* com.portability.bot_service.service.*.*(..))")
    public void serviceLayerMethods() {}

    /**
     * Log before method execution
     */
    @Before("serviceLayerMethods()")
    public void logBefore(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        
        logger.info("Executing method: {} with arguments: {}", methodName, Arrays.toString(args));
    }

    /**
     * Log after method execution (successful return)
     */
    @AfterReturning(pointcut = "serviceLayerMethods()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().toShortString();
        logger.info("Method {} executed successfully with result: {}", methodName, result);
    }

    /**
     * Log after method throws an exception
     */
    @AfterThrowing(pointcut = "serviceLayerMethods()", throwing = "exception")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable exception) {
        String methodName = joinPoint.getSignature().toShortString();
        logger.error("Method {} threw exception: {}", methodName, exception.getMessage(), exception);
    }

    /**
     * Log execution time of methods
     */
    @Around("serviceLayerMethods()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            logger.info("Method {} executed in {} ms", methodName, executionTime);
            return result;
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Method {} failed after {} ms", methodName, executionTime);
            throw throwable;
        }
    }
}
