package com.twitter.media.service.controller;

import com.twitter.media.service.dto.MediaResponse;
import com.twitter.media.service.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/media")
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    public ResponseEntity<MediaResponse> upload(@RequestParam("file") MultipartFile file, @RequestParam("userId") Long userId) throws Exception {

        return ResponseEntity.ok(
                mediaService.upload(file, userId));
    }
}