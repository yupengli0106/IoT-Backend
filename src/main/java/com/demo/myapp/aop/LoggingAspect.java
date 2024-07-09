package com.demo.myapp.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @Author: Yupeng Li
 * @Date: 9/7/2024 15:14
 * @Description: AOP for logging the method entry, exit and exception
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * Log the method entry with arguments before the method is executed (service layer)
     * @param joinPoint the join point
     */
    @Before("execution(* com.demo.myapp.service..*(..))")
    public void logBefore(JoinPoint joinPoint) {
        logger.info("Entering method: {} with arguments: {}", joinPoint.getSignature().toShortString(), joinPoint.getArgs());
    }

    /**
     * Log the method exit with result after the method is executed
     * @param joinPoint the join point
     * @param result the result
     */
    @AfterReturning(pointcut = "execution(* com.demo.myapp.service..*(..))", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        logger.info("Exiting method: {} with result: {}", joinPoint.getSignature().toShortString(), result);
    }

    /**
     * Log the exception after the method is executed
     * @param joinPoint the join point
     * @param error the exception
     */
    @AfterThrowing(pointcut = "execution(* com.demo.myapp.service..*(..))", throwing = "error")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable error) {
        logger.error("Exception in method: {} with cause: {}", joinPoint.getSignature().toShortString(), error.getMessage());
    }
}
