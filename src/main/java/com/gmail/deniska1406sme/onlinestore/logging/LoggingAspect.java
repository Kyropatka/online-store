package com.gmail.deniska1406sme.onlinestore.logging;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    @Before("execution(* com.gmail.deniska1406sme.onlinestore..*(..))")
    public void logBefore(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        Object[] filteredArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            filteredArgs[i] = checkSensitiveData(args[i]);
        }
        logger.info("Method call: {} with arguments: {}", joinPoint.getSignature().getName(), Arrays.toString(filteredArgs));
    }

    @AfterReturning(pointcut = "execution(* com.gmail.deniska1406sme.onlinestore..*(..))",returning = "result")
    public void logAfter(JoinPoint joinPoint, Object result) {
        Object filteredResult = checkSensitiveData(result);
        if ("createToken".equals(joinPoint.getSignature().getName()) || "authenticate".equals(joinPoint.getSignature().getName())
                || "login".equals(joinPoint.getSignature().getName())) {
            logger.info("Method {} completed successfully, but result is hidden.", joinPoint.getSignature().getName());
            return;
        }
        logger.info("Method {} completed successfully, with result: {}", joinPoint.getSignature().getName(), filteredResult);
    }

    @AfterThrowing(pointcut = "execution(* com.gmail.deniska1406sme.onlinestore..*(..))", throwing = "exception")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable exception) {
        logger.error("Method {} threw an exception: {}", joinPoint.getSignature().getName(), exception.getMessage());//, exception) for extended stack trace
    }

    private Object checkSensitiveData(Object arg){
        if (arg instanceof String) {
            String strArg = (String) arg;
            if (strArg.matches(".*password.*") || strArg.matches(".*token.*")) {
                return "****";
            }
        }
        return arg;
    }
}
