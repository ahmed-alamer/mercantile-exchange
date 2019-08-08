package com.hydra.merc.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Created By aalamer on 08-08-2019
 */
@ControllerAdvice
public class GlobalExceptionsHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = AccountNotFound.class)
    public ResponseEntity<ExceptionBody> accountNotFoundHandler(AccountNotFound ex) {
        return handleSingleError(ex);
    }

    @ExceptionHandler(value = PositionNotFound.class)
    public ResponseEntity<ExceptionBody> positionNotFoundHandler(PositionNotFound ex) {
        return handleSingleError(ex);
    }

    private ResponseEntity<ExceptionBody> handleSingleError(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionBody.singleError(ex.getMessage()));
    }

}
