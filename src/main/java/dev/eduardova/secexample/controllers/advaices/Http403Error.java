package dev.eduardova.secexample.controllers.advaices;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class Http403Error {

    @ExceptionHandler(AccessDeniedException.class)
    public String userNotFoundException(final AccessDeniedException ex) {
        return "403";
    }

}
