package com.twitter.media.service.exception;

public class UnexpectedUrl extends RuntimeException{

    public UnexpectedUrl(String message){
        super(message);
    }

    public UnexpectedUrl(){
        super();
    }
}
