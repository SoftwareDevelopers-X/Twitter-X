package com.twitter.social.service.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 160, message = "Bio cannot exceed 160 characters")
    private String bio;

    @Size(max = 100, message = "Location cannot exceed 100 characters")
    private String location;

    @Size(max = 100, message = "Website cannot exceed 100 characters")
    private String website;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private Boolean isPrivate;
}
