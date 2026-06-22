package com.twitter.social.service.client;

import com.twitter.social.service.feignDto.MediaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Talks to media-service's existing /media/upload endpoint:
 *
 *   POST /media/upload
 *   @RequestParam("file") MultipartFile file
 *   @RequestParam("userId") Long userId
 *
 * "media-service" below is the Eureka application name (spring.application.name
 * in media-service's application.yml) — confirm it matches exactly, Feign
 * resolves it via Eureka + Ribbon/LoadBalancer, no hardcoded host:port needed.
 *
 * IMPORTANT: Multipart Feign calls require the `feign-form` + `feign-form-spring`
 * dependencies (see pom.xml snippet in the writeup) and a configuration class
 * registering SpringFormEncoder, otherwise you'll get
 * "Content-Type is not multipart/form-data" failures at runtime. See
 * config/FeignMultipartSupportConfig.java.
 */
@FeignClient(name = "media-service", configuration = com.twitter.social.service.config.FeignMultipartSupportConfig.class)
public interface MediaServiceClient {

    @PostMapping(value = "/media/upload", consumes = "multipart/form-data")
    MediaResponse upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("userId") Long userId
    );

    @PutMapping(value = "/media/update/{mediaId}", consumes = "multipart/form-data")
    MediaResponse update(
            @PathVariable("mediaId") Long mediaId,
            @RequestPart("file") MultipartFile newFile
    );

    @DeleteMapping("/media/delete/{mediaId}")
    void delete(@PathVariable("mediaId") Long mediaId);
}
