package io.metersphere.node.controller.handler;


import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@RestControllerAdvice
public class RestControllerExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResultHolder msExceptionHandler(HttpServletRequest request, HttpServletResponse response, Exception e) {
        return ResultHolder.error(e.getMessage());
    }
}
