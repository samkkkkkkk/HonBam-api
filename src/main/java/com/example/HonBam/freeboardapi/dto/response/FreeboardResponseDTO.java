package com.example.HonBam.freeboardapi.dto.response;


import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreeboardResponseDTO {

    private List<FreeboardDetailResponseDTO> posts;
    private int count;

}
