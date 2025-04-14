package org.nevmock.digivise.infrastructure.config.exceptions;

import org.springframework.http.HttpStatusCode;

public class AlreadyExistException extends RuntimeException {
    public AlreadyExistException (String message) {
        super(message);
    }

    public AlreadyExistException (String message, Throwable cause) {
        super(message, cause);
    }
}
