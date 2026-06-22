package com.twitter.social.service.feignDto;

import lombok.*;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class TweetMediaDto {
    private Long mediaId;
    private String mediaUrl;
    private String mediaType;
}