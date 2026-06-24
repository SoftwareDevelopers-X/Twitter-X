package com.twitterx.chatservice.dto;

import com.twitterx.chatservice.enums.ConversationType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    @NotNull
    private ConversationType type;

    /**
     * For ONE_TO_ONE: must contain exactly 1 other userId.
     * For GROUP: must contain 2+ other userIds.
     * The caller (taken from X-User-Id) is added automatically - don't include it here.
     */
    @NotEmpty
    private List<Long> participantIds;

    @Size(max = 150)
    private String groupName; // required if type == GROUP
}
