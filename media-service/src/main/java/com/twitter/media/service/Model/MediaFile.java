package com.twitter.media.service.Model;

import com.twitter.media.service.Enum.MediaTypes;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "media_files")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer mediaId;

    private Integer userId;

    private String fileName;

    private String url;

    private Long size;

    @Enumerated(EnumType.STRING)
    private MediaTypes mediaType;

    @CreationTimestamp
    private LocalDateTime uploadedAt;
}