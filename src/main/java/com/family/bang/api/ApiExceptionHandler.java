package com.family.bang.api;

import com.family.bang.game.GameException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(GameException.class)
    ProblemDetail gameError(GameException exception) {
        return ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validationError(MethodArgumentNotValidException exception) {
        return ProblemDetail.forStatusAndDetail(400, "playerName must contain 1 through 40 characters");
    }
}
