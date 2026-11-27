package com.bookit.shared.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessRuleException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BusinessRuleException(String code, String message) {
        this(HttpStatus.CONFLICT, code, message);
    }

    public BusinessRuleException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
