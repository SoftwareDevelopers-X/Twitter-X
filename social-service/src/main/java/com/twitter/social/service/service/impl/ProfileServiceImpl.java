package com.twitter.social.service.service.impl;

import com.twitter.social.service.Model.Like;
import com.twitter.social.service.Model.Profile;
import com.twitter.social.service.Model.Reply;
import com.twitter.social.service.client.AuthServiceClient;
import com.twitter.social.service.client.MediaServiceClient;
import com.twitter.social.service.client.TweetServiceClient;
import com.twitter.social.service.dto.PagedResponse;
import com.twitter.social.service.dto.ProfileResponse;
import com.twitter.social.service.dto.ReplyDto;
import com.twitter.social.service.dto.UpdateProfileRequest;
import com.twitter.social.service.exception.ProfileNotFoundException;
import com.twitter.social.service.exception.SocialException;
import com.twitter.social.service.feignDto.MediaResponse;
import com.twitter.social.service.feignDto.TweetDto;
import com.twitter.social.service.feignDto.UserDto;
import com.twitter.social.service.repository.FollowRepository;
import com.twitter.social.service.repository.LikeRepository;
import com.twitter.social.service.repository.ProfileRepository;
import com.twitter.social.service.repository.ReplyRepository;
import com.twitter.social.service.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * !!! DEPENDS ON FollowRepository and LikeRepository which I'm assuming exist
 * already (you have Follow.java and Like.java models in your Model package,
 * but didn't show me their repositories). If your existing repositories have
 * different method names than findByUserId / findByFollowerId etc, adjust
 * the calls below to match. I've documented exactly what each call assumes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final FollowRepository followRepository;
    private final LikeRepository likeRepository;
    private final ReplyRepository replyRepository;
    private final TweetServiceClient tweetServiceClient;
    private final AuthServiceClient authServiceClient;
    private final MediaServiceClient mediaServiceClient;

    @Override
    public ProfileResponse getProfile(Long userId, Long currentUserId) {
        ProfileResponse cached = getCachedProfileCore(userId);
        boolean isOwn = currentUserId != null && currentUserId.equals(userId);
        boolean isFollowed = !isOwn && currentUserId != null
                && followRepository.existsByFollowerIdAndFollowingId(currentUserId, userId);
        cached.setIsOwnProfile(isOwn);
        cached.setIsFollowedByCurrentUser(isFollowed);
        return cached;
    }

    @Cacheable(value = "profiles", key = "#userId")
    public ProfileResponse getCachedProfileCore(Long userId) {
        Profile profile = getOrCreateProfile(userId);
        return buildProfileResponse(profile, null);
    }

    @Override
    @Transactional
    @CacheEvict(value = "profiles", key = "#userId")
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        Profile profile = getOrCreateProfile(userId);

        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getLocation() != null) {
            profile.setLocation(request.getLocation());
        }
        if (request.getWebsite() != null) {
            profile.setWebsite(request.getWebsite());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getIsPrivate() != null) {
            profile.setIsPrivate(request.getIsPrivate());
        }

        Profile saved = profileRepository.save(profile);
        log.info("Profile updated for userId={}", userId);
        return buildProfileResponse(saved, userId);
    }

    // ── AVATAR ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = "profiles", key = "#userId")
    public ProfileResponse uploadAvatar(Long userId, MultipartFile file) {
        validateImageFile(file);
        Profile profile = getOrCreateProfile(userId);

        if (profile.getAvatarMediaId() != null) {
            throw new SocialException(
                    "Avatar already exists. Use PUT /{userId}/avatar to replace it.");
        }

        MediaResponse mediaResponse = mediaServiceClient.upload(file, userId);
        profile.setAvatarUrl(mediaResponse.getUrl());
        profile.setAvatarMediaId(mediaResponse.getMediaId());

        Profile saved = profileRepository.save(profile);
        log.info("Avatar uploaded for userId={}, mediaId={}", userId, mediaResponse.getMediaId());
        return buildProfileResponse(saved, userId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "profiles", key = "#userId")
    public ProfileResponse updateAvatar(Long userId, MultipartFile file) {
        validateImageFile(file);
        Profile profile = getOrCreateProfile(userId);

        if (profile.getAvatarMediaId() == null) {
            // No existing avatar — treat as first upload
            return uploadAvatar(userId, file);
        }

        MediaResponse mediaResponse = mediaServiceClient.update(profile.getAvatarMediaId(), file);
        profile.setAvatarUrl(mediaResponse.getUrl());
        profile.setAvatarMediaId(mediaResponse.getMediaId());

        Profile saved = profileRepository.save(profile);
        log.info("Avatar updated for userId={}, mediaId={}", userId, mediaResponse.getMediaId());
        return buildProfileResponse(saved, userId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "profiles", key = "#userId")
    public void deleteAvatar(Long userId) {
        Profile profile = getOrCreateProfile(userId);

        if (profile.getAvatarMediaId() == null) {
            throw new ProfileNotFoundException("No avatar found for userId: " + userId);
        }

        mediaServiceClient.delete(profile.getAvatarMediaId());
        profile.setAvatarUrl(null);
        profile.setAvatarMediaId(null);
        profileRepository.save(profile);
        log.info("Avatar deleted for userId={}", userId);
    }

// ── BANNER ───────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = "profiles", key = "#userId")
    public ProfileResponse uploadBanner(Long userId, MultipartFile file) {
        validateImageFile(file);
        Profile profile = getOrCreateProfile(userId);

        if (profile.getBannerMediaId() != null) {
            throw new SocialException(
                    "Banner already exists. Use PUT /{userId}/banner to replace it.");
        }

        MediaResponse mediaResponse = mediaServiceClient.upload(file, userId);
        profile.setBannerUrl(mediaResponse.getUrl());
        profile.setBannerMediaId(mediaResponse.getMediaId());

        Profile saved = profileRepository.save(profile);
        log.info("Banner uploaded for userId={}, mediaId={}", userId, mediaResponse.getMediaId());
        return buildProfileResponse(saved, userId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "profiles", key = "#userId")
    public ProfileResponse updateBanner(Long userId, MultipartFile file) {
        validateImageFile(file);
        Profile profile = getOrCreateProfile(userId);

        if (profile.getBannerMediaId() == null) {
            // No existing banner — treat as first upload
            return uploadBanner(userId, file);
        }

        MediaResponse mediaResponse = mediaServiceClient.update(profile.getBannerMediaId(), file);
        profile.setBannerUrl(mediaResponse.getUrl());
        profile.setBannerMediaId(mediaResponse.getMediaId());

        Profile saved = profileRepository.save(profile);
        log.info("Banner updated for userId={}, mediaId={}", userId, mediaResponse.getMediaId());
        return buildProfileResponse(saved, userId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "profiles", key = "#userId")
    public void deleteBanner(Long userId) {
        Profile profile = getOrCreateProfile(userId);

        if (profile.getBannerMediaId() == null) {
            throw new ProfileNotFoundException("No banner found for userId: " + userId);
        }

        mediaServiceClient.delete(profile.getBannerMediaId());
        profile.setBannerUrl(null);
        profile.setBannerMediaId(null);
        profileRepository.save(profile);
        log.info("Banner deleted for userId={}", userId);
    }

    @Override
    public PagedResponse<TweetDto> getPosts(Long userId, int page, int size) {
        List<TweetDto> all = getAllTweetsCached(userId);
        return paginate(all, page, size);
    }

    @Override
    public PagedResponse<TweetDto> getMedia(Long userId, int page, int size) {
        List<TweetDto> mediaOnly = getAllTweetsCached(userId).stream()
                .filter(t -> t.getMediaList() != null && !t.getMediaList().isEmpty())
                .collect(Collectors.toList());
        return paginate(mediaOnly, page, size);
    }

    /**
     * Single cached fetch of a user's FULL tweet list (tweet-service has no
     * pagination/media-filter support, see TweetController — no page/size
     * params on GET /api/tweets/user/{userId}). Cached so that flipping
     * through Posts/Media pages doesn't re-hit tweet-service every time —
     * only paginate()/the media filter run on each call, both in-memory.
     */

    @Cacheable(value = "profileTabs", key = "'allTweets:' + #userId")
    public List<TweetDto> getAllTweetsCached(Long userId) {
        return tweetServiceClient.getAllTweetsByUser(userId);
    }

    @Override
    @Cacheable(value = "profileTabs", key = "'replies:' + #userId + ':' + #page + ':' + #size")
    public PagedResponse<ReplyDto> getReplies(Long userId, int page, int size) {
        Page<Reply> replyPage = replyRepository.findByUserId(
                userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "repliedAt"))
        );

        List<ReplyDto> replies = replyPage.getContent().stream()
                .map(r -> ReplyDto.builder()
                        .replyId(r.getReplyId())
                        .userId(r.getUserId())
                        .tweetId(r.getTweetId())
                        .content(r.getContent())
                        .repliedAt(r.getRepliedAt())
                        .build())
                .collect(Collectors.toList());

        return PagedResponse.<ReplyDto>builder()
                .content(replies)
                .page(page)
                .size(size)
                .totalElements(replyPage.getTotalElements())
                .hasNext(replyPage.hasNext())
                .build();
    }

    @Override
    public PagedResponse<TweetDto> getLikedTweets(Long userId, int page, int size) {
        // Likes live in social-service's own table -> paginate locally first,
        // then fetch each liked tweet's content from tweet-service.
        // ASSUMES LikeRepository has findByUserId(Long, Pageable) returning
        // Page<Like>, ordered by likedAt.
        Page<Like> likePage = likeRepository.findByUserId(
                userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "likedAt"))
        );

        // No batch-by-ids endpoint on tweet-service, so we call getTweetById
        // per liked tweet. Fine for normal page sizes (10-20); if this tab
        // gets slow, ask for a GET /api/tweets/batch?ids=... on tweet-service.
        List<TweetDto> tweets = likePage.getContent().stream()
                .map(like -> {
                    try {
                        return tweetServiceClient.getTweetById(like.getTweetId());
                    } catch (Exception ex) {
                        log.warn("Tweet {} not found (likely deleted), skipping", like.getTweetId());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return PagedResponse.<TweetDto>builder()
                .content(tweets)
                .page(page)
                .size(size)
                .totalElements(likePage.getTotalElements())
                .hasNext(likePage.hasNext())
                .build();
    }

    // ---------- helpers ----------

    /**
     * Lazy-creation fallback: if no profile row exists yet for this userId
     * (e.g. the Kafka user-registered event hasn't been processed yet, or
     * was missed), create an empty one on the fly instead of 404-ing. This
     * makes profile existence self-healing. We deliberately do NOT verify
     * userId actually exists in auth-service here (no security/gateway yet
     * per your current setup) — once auth is wired in, you may want to
     * validate userId via AuthServiceClient before creating.
     */
    private Profile getOrCreateProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.warn("No profile found for userId={}, creating one lazily", userId);
                    Profile newProfile = Profile.builder()
                            .userId(userId)
                            .isVerified(false)
                            .isPrivate(false)
                            .build();
                    return profileRepository.save(newProfile);
                });
    }

    private ProfileResponse buildProfileResponse(Profile profile, Long currentUserId) {
        UserDto userDto = fetchUserSafely(profile.getUserId());

        // ASSUMES FollowRepository has countByFollowingId / countByFollowerId
        // (matching whatever Follow.java's actual field names are — I'm
        // guessing followerId/followingId since that's the standard pattern;
        // adjust to your real field names).
        long followersCount = followRepository.countByFollowingId(profile.getUserId());
        long followingCount = followRepository.countByFollowerId(profile.getUserId());

        Long postsCount = fetchPostCountSafely(profile.getUserId());

        boolean isOwnProfile = currentUserId != null && currentUserId.equals(profile.getUserId());
        boolean isFollowedByCurrentUser = !isOwnProfile && currentUserId != null
                && followRepository.existsByFollowerIdAndFollowingId(currentUserId, profile.getUserId());

        return ProfileResponse.builder()
                .userId(profile.getUserId())
                .username(userDto != null ? userDto.getUsername() : null)
                .displayName(userDto != null ? userDto.getDisplayName() : null)
                .bio(profile.getBio())
                .location(profile.getLocation())
                .website(profile.getWebsite())
                .avatarUrl(profile.getAvatarUrl())
                .bannerUrl(profile.getBannerUrl())
                .dateOfBirth(profile.getDateOfBirth())
                .isVerified(profile.getIsVerified())
                .isPrivate(profile.getIsPrivate())
                .joinedAt(profile.getCreatedAt())
                .followersCount(followersCount)
                .followingCount(followingCount)
                .postsCount(postsCount)
                .isFollowedByCurrentUser(isFollowedByCurrentUser)
                .isOwnProfile(isOwnProfile)
                .build();
    }

    /**
     * auth-service being briefly down shouldn't take down profile viewing
     * entirely (graceful degradation) — log and return null, frontend can
     * show a blank/fallback name. Tighten this if you'd rather fail loudly.
     */
    private UserDto fetchUserSafely(Long userId) {
        try {
            return authServiceClient.getUserById(userId);
        } catch (Exception ex) {
            log.error("Failed to fetch user details from auth-service for userId={}: {}", userId, ex.getMessage());
            return null;
        }
    }

    /**
     * tweet-service has no dedicated count endpoint (see TweetController —
     * no GET /api/tweets/count/user/{userId}). Deriving the count from the
     * same cached full-list fetch used by getPosts/getMedia avoids a second
     * network call entirely.
     */
    private Long fetchPostCountSafely(Long userId) {
        try {
            return (long) getAllTweetsCached(userId).size();
        } catch (Exception ex) {
            log.error("Failed to fetch post count from tweet-service for userId={}: {}", userId, ex.getMessage());
            return 0L;
        }
    }

    private PagedResponse<TweetDto> paginate(List<TweetDto> all, int page, int size) {
        List<TweetDto> sorted = all.stream()
                .sorted(Comparator.comparing(TweetDto::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        int from = Math.min(page * size, sorted.size());
        int to = Math.min(from + size, sorted.size());
        List<TweetDto> slice = sorted.subList(from, to);

        return PagedResponse.<TweetDto>builder()
                .content(slice)
                .page(page)
                .size(size)
                .totalElements(sorted.size())
                .hasNext(to < sorted.size())
                .build();
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }
        long maxSizeBytes = 5L * 1024 * 1024; // 5MB - adjust to your needs
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("File size must not exceed 5MB");
        }
    }
}