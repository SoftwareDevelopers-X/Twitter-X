package com.twitter.media.service.service;

import com.twitter.media.service.Enum.MediaTypes;
import com.twitter.media.service.Model.MediaFile;
import com.twitter.media.service.dto.MediaResponse;
import com.twitter.media.service.exception.MediaNotFoundWithId;
import com.twitter.media.service.exception.UnexpectedUrl;
import com.twitter.media.service.exception.UnsupportedFileType;
import com.twitter.media.service.repository.MediaRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MinioClient minioClient;
    private final MediaRepository mediaRepository;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.bucket-name}")
    private String bucketName;

    // ─── Upload ───────────────────────────────────────────────────────────────

    public MediaResponse upload(MultipartFile file, Long userId) throws Exception {
        String objectName = generateObjectName(file);

        putToMinio(objectName, file);

        String url = buildUrl(objectName);

        MediaFile mediaFile = MediaFile.builder()
                .userId(userId)
                .fileName(file.getOriginalFilename())
                .url(url)
                .size(file.getSize())
                .mediaType(determineType(file))
                .build();

        mediaFile = mediaRepository.save(mediaFile);
        return new MediaResponse(mediaFile.getMediaId(), mediaFile.getUrl());
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    public MediaResponse update(Long mediaId, MultipartFile newFile) throws Exception {
        MediaFile mediaFile = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundWithId("Media not found with id: " + mediaId));

        // Delete the old object from MinIO
        String oldObjectName = extractObjectName(mediaFile.getUrl());
        deleteFromMinio(oldObjectName);

        // Upload the new object
        String newObjectName = generateObjectName(newFile);
        putToMinio(newObjectName, newFile);

        // Update the DB record
        mediaFile.setFileName(newFile.getOriginalFilename());
        mediaFile.setUrl(buildUrl(newObjectName));
        mediaFile.setSize(newFile.getSize());
        mediaFile.setMediaType(determineType(newFile));

        mediaFile = mediaRepository.save(mediaFile);
        return new MediaResponse(mediaFile.getMediaId(), mediaFile.getUrl());
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    public void delete(Long mediaId) throws Exception {
        MediaFile mediaFile = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundWithId("Media not found with id: " + mediaId));

        String objectName = extractObjectName(mediaFile.getUrl());
        deleteFromMinio(objectName);

        mediaRepository.delete(mediaFile);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String generateObjectName(MultipartFile file) {
        return UUID.randomUUID() + "-" + file.getOriginalFilename().replace(" ", "_");
    }

    private String buildUrl(String objectName) {
        return minioUrl + "/" + bucketName + "/" + objectName;
    }

    /**
     * Extracts the object name from a full MinIO URL.
     * e.g. "http://192.168.x.x:9000/twitter-media/uuid-file.jpg" → "uuid-file.jpg"
     */
    private String extractObjectName(String url) {
        // strip everything up to and including the bucket name
        String prefix = minioUrl + "/" + bucketName + "/";
        if (!url.startsWith(prefix)) {
            throw new UnexpectedUrl("Unexpected URL format, cannot extract object name: " + url);
        }
        return url.substring(prefix.length());
    }

    private void putToMinio(String objectName, MultipartFile file) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
    }

    private void deleteFromMinio(String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    private MediaTypes determineType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new UnsupportedFileType("Unable to determine file type");
        }
        if (contentType.startsWith("image/")) {
            if (contentType.contains("gif")) {
                return MediaTypes.GIF;
            } else {
                return MediaTypes.IMAGE;
            }
        }
        if (contentType.startsWith("video/")) {
            return MediaTypes.VIDEO;
        }
        throw new UnsupportedFileType("Unsupported media type: " + contentType);
    }
}