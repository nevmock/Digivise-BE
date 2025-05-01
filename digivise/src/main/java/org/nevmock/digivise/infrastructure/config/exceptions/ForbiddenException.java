package org.nevmock.digivise.infrastructure.config.exceptions;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
    super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
