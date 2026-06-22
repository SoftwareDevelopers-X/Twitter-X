package com.twitter.social.service.feignDto;

import lombok.*;

/**
 * Mirrors media-service's MediaResponse EXACTLY (from the MediaController
 * you shared: returns userId + url after upload). If media-service's
 * MediaResponse has more fields (e.g. mediaId, fileType, fileSize), add them
 * here too — Jackson will just ignore extra JSON fields we don't map, so
 * this is safe to under-specify but not over-specify.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MediaResponse {
    private Long mediaId;
    private String url;
}
