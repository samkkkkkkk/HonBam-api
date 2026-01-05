package com.example.HonBam.chatapi.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InviteUserRequest {
    private List<String> targetUserIds;
}
