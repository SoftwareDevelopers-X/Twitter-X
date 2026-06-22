package com.twitter.social.service.service;

import com.twitter.social.service.dto.PagedResponse;
import com.twitter.social.service.dto.ProfileResponse;
import com.twitter.social.service.dto.ReplyDto;
import com.twitter.social.service.dto.UpdateProfileRequest;
import com.twitter.social.service.feignDto.TweetDto;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {

    ProfileResponse getProfile(Long userId, Long currentUserId);

    ProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    ProfileResponse uploadAvatar(Long userId, MultipartFile file);

    ProfileResponse updateAvatar(Long userId, MultipartFile file);
    
    void deleteAvatar(Long userId);

    ProfileResponse uploadBanner(Long userId, MultipartFile file);

    ProfileResponse updateBanner(Long userId, MultipartFile file);

    void deleteBanner(Long userId);

    PagedResponse<TweetDto> getPosts(Long userId, int page, int size);

    PagedResponse<ReplyDto> getReplies(Long userId, int page, int size);

    PagedResponse<TweetDto> getMedia(Long userId, int page, int size);

    PagedResponse<TweetDto> getLikedTweets(Long userId, int page, int size);
}
