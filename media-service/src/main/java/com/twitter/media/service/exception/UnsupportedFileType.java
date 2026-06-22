package com.twitter.media.service.exception;

public class UnsupportedFileType extends RuntimeException{

    public UnsupportedFileType(String message){
        super(message);
    }

    public UnsupportedFileType(){
        super();
    }

}
