package com.example.HonBam.upload.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class JoinUploadRequest {
    private String fileName;
    private String contentType;
}
