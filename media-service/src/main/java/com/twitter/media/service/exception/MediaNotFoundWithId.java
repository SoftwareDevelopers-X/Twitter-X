package com.twitter.media.service.exception;

public class MediaNotFoundWithId extends RuntimeException{
    public MediaNotFoundWithId(String message){
        super(message);
    }

    public MediaNotFoundWithId(){
        super();
    }
}
