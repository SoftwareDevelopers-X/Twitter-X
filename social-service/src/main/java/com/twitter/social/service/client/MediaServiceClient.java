package com.twitter.social.service.client;

import com.twitter.social.service.feignDto.MediaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


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
