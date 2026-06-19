package com.twitter.media.service.repository;

import com.twitter.media.service.Model.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<MediaFile, Long> {

}
