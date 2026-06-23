package com.twitter.social.service.controller;

import com.twitter.social.service.dto.PagedResponse;
import com.twitter.social.service.dto.ProfileResponse;
import com.twitter.social.service.dto.ReplyDto;
import com.twitter.social.service.dto.UpdateProfileRequest;
import com.twitter.social.service.feignDto.TweetDto;
import com.twitter.social.service.response.ApiResponse;
import com.twitter.social.service.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    // ── PROFILE ───────────────────────────────────────────────────────────────

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @PathVariable Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId
    ) {
        ProfileResponse response = profileService.getProfile(userId, currentUserId);
        return ResponseEntity.ok(new ApiResponse<>("success", "Profile fetched successfully", response));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        ProfileResponse response = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(new ApiResponse<>("success", "Profile updated successfully", response));
    }

    // ── AVATAR ────────────────────────────────────────────────────────────────

    // First-time upload (no avatar exists yet)
    @PostMapping(value = "/{userId}/avatar", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ProfileResponse>> uploadAvatar(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file
    ) {
        ProfileResponse response = profileService.uploadAvatar(userId, file);
        return ResponseEntity.ok(new ApiResponse<>("success", "Avatar uploaded successfully", response));
    }

    // Replace existing avatar (calls media-service PUT /update/{mediaId})
    @PutMapping(value = "/{userId}/avatar", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateAvatar(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file
    ) {
        ProfileResponse response = profileService.updateAvatar(userId, file);
        return ResponseEntity.ok(new ApiResponse<>("success", "Avatar updated successfully", response));
    }

    // Remove avatar entirely
    @DeleteMapping("/{userId}/avatar")
    public ResponseEntity<ApiResponse<Void>> deleteAvatar(@PathVariable Long userId) {
        profileService.deleteAvatar(userId);
        return ResponseEntity.ok(new ApiResponse<>("success", "Avatar deleted successfully", null));
    }

    // ── BANNER ────────────────────────────────────────────────────────────────

    @PostMapping(value = "/{userId}/banner", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ProfileResponse>> uploadBanner(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file
    ) {
        ProfileResponse response = profileService.uploadBanner(userId, file);
        return ResponseEntity.ok(new ApiResponse<>("success", "Banner uploaded successfully", response));
    }

    @PutMapping(value = "/{userId}/banner", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateBanner(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file
    ) {
        ProfileResponse response = profileService.updateBanner(userId, file);
        return ResponseEntity.ok(new ApiResponse<>("success", "Banner updated successfully", response));
    }

    @DeleteMapping("/{userId}/banner")
    public ResponseEntity<ApiResponse<Void>> deleteBanner(@PathVariable Long userId) {
        profileService.deleteBanner(userId);
        return ResponseEntity.ok(new ApiResponse<>("success", "Banner deleted successfully", null));
    }

    // ── TABS ──────────────────────────────────────────────────────────────────

    @GetMapping("/{userId}/posts")
    public ResponseEntity<ApiResponse<PagedResponse<TweetDto>>> getPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(new ApiResponse<>("success", "Posts fetched",
                profileService.getPosts(userId, page, size)));
    }

    @GetMapping("/{userId}/replies")
    public ResponseEntity<ApiResponse<PagedResponse<ReplyDto>>> getReplies(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(new ApiResponse<>("success", "Replies fetched",
                profileService.getReplies(userId, page, size)));
    }

    @GetMapping("/{userId}/media")
    public ResponseEntity<ApiResponse<PagedResponse<TweetDto>>> getMedia(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(new ApiResponse<>("success", "Media fetched",
                profileService.getMedia(userId, page, size)));
    }

    @GetMapping("/{userId}/likes")
    public ResponseEntity<ApiResponse<PagedResponse<TweetDto>>> getLikedTweets(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(new ApiResponse<>("success", "Liked tweets fetched",
                profileService.getLikedTweets(userId, page, size)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<java.util.List<ProfileResponse>>> searchProfiles(
            @RequestParam String query,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId
    ) {
        java.util.List<ProfileResponse> responses = profileService.searchProfiles(query, currentUserId);
        return ResponseEntity.ok(new ApiResponse<>("success", "Profiles searched successfully", responses));
    }
}