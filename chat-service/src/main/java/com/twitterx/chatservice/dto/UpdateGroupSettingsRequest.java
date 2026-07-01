package com.twitterx.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGroupSettingsRequest {
    private String name;
    private String groupImageUrl;
}
