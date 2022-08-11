package cz.muni.ics.ga4gh.web.controllers;

import cz.muni.ics.ga4gh.base.exceptions.InvalidRequestParametersException;
import cz.muni.ics.ga4gh.base.exceptions.UserNotFoundException;
import cz.muni.ics.ga4gh.base.exceptions.UserNotUniqueException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionTranslator {

    @ExceptionHandler({InvalidRequestParametersException.class, UserNotUniqueException.class})
    public ResponseEntity<Object> badRequest(Exception e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({UserNotFoundException.class})
    public ResponseEntity<Object> notFound(Exception e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> exception(Exception e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
