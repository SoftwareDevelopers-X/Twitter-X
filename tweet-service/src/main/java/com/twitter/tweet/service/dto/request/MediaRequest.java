package com.twitter.tweet.service.dto.request;


import com.twitter.tweet.service.enums.MediaType;
import lombok.Data;

@Data
public class MediaRequest {
    private String mediaUrl;
    private MediaType mediaType;
}
