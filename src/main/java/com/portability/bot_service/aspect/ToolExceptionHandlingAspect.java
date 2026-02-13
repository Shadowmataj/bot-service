package com.portability.bot_service.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.portability.bot_service.exception.ToolExecutionException;

/**
 * Aspect that intercepts all @Tool methods to provide consistent exception handling
 * and logging. This ensures that tool failures are properly logged with technical
 * details while the LLM receives user-friendly error messages.
 * 
 * Enhanced to support retry mechanisms by providing clear error context.
 * Order(1) ensures this runs before ContextStorageAspect
 */
@Aspect
@Component
@Order(1)
public class ToolExceptionHandlingAspect {

    private static final Logger logger = LoggerFactory.getLogger(ToolExceptionHandlingAspect.class);

    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object handleToolExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        try {
            logger.info("Executing tool: {}.{}", className, methodName);
            Object result = joinPoint.proceed();
            logger.info("Tool executed successfully: {}.{}", className, methodName);
            return result;
            
        } catch (ToolExecutionException e) {
            // Log technical details for debugging
            logger.error("Tool execution failed - Tool: {}.{}, User Message: {}, Technical Details: {}", 
                    className, methodName, e.getUserFriendlyMessage(), e.getTechnicalDetails(), e);
            
            // Re-throw with LLM-friendly message
            // The orchestrator will handle recording this error for retry detection
            throw new ToolExecutionException(
                    e.getToolName(),
                    e.getUserFriendlyMessage(),
                    e.getTechnicalDetails()
            );
            
        } catch (Exception e) {
            // Catch any unexpected exceptions and wrap them
            logger.error("Unexpected error in tool: {}.{}", className, methodName, e);
            throw new ToolExecutionException(
                    methodName,
                    "Ocurrió un error inesperado. Por favor, intenta nuevamente",
                    e.getMessage()
            );
        }
    }
}
