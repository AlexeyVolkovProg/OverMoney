package com.override.orchestrator_service.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TelegramAuthRequest {

    private Long id;

    private String first_name;

    private String last_name;

    private String username;

    private String photo_url;

    private String auth_date;

    private String hash;
}