package com.manish.smartcart.shared.exception;

public class TokenExpiredException extends RuntimeException{
    public TokenExpiredException(String message){
        super(message);
    }
}
