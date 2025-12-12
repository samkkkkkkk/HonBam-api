package com.example.HonBam.upload.dto;

import com.google.errorprone.annotations.NoAllocation;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class FileUploadRequest {
    private String fileName;
    private String contentType;
}
