package com.twitter.media.service.service;

import com.twitter.media.service.Enum.MediaTypes;
import com.twitter.media.service.Model.MediaFile;
import com.twitter.media.service.dto.MediaResponse;
import com.twitter.media.service.repository.MediaRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MinioClient minioClient;
    private final MediaRepository mediaRepository;

    public MediaResponse upload( MultipartFile file, Long userId ) throws Exception {

        String objectName =
                UUID.randomUUID()
                        + "-"
                        + file.getOriginalFilename()
                        .replace(" ", "_");

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket("twitter-media")
                        .object(objectName)
                        .stream(  file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        String url = "http://localhost:9000/twitter-media/" + objectName;

        MediaFile mediaFile =
                MediaFile.builder()
                        .userId(userId)
                        .fileName(file.getOriginalFilename())
                        .url(url)
                        .size(file.getSize())
                        .mediaType(determineType(file))
                        .build();

        mediaFile = mediaRepository.save(mediaFile);

        return new MediaResponse(
                mediaFile.getMediaId(),
                mediaFile.getUrl()
        );
    }

    private MediaTypes determineType(MultipartFile file) {

        String contentType = file.getContentType();

        if (contentType == null) {
            throw new RuntimeException( "Unable to determine file type" );
        }

        if (contentType.startsWith("image/")) {
            if (contentType.contains("gif")) {
                return MediaTypes.GIF;
            }
            return MediaTypes.IMAGE;
        }

        if (contentType.startsWith("video/")) {
            return MediaTypes.VIDEO;
        }

        throw new RuntimeException("Unsupported media type");
    }
}