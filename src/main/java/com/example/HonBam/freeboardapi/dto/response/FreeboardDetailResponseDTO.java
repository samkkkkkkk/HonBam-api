package com.example.HonBam.freeboardapi.dto.response;

import com.example.HonBam.freeboardapi.entity.Freeboard;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@ToString @EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreeboardDetailResponseDTO {
    private Long id;
    private String title;
    private String content;
    private String userName;
    private String userId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime date;

    public FreeboardDetailResponseDTO(Freeboard freeboard) {
        this.id = freeboard.getId();
        this.title = freeboard.getTitle();
        this.content = freeboard.getContent();
        this.userName = freeboard.getUser().getUserName();
        this.userId = freeboard.getUser().getUserId();
        if(freeboard.getUpdateDate() != null){
            this.date = freeboard.getUpdateDate();
        }else {
            this.date = freeboard.getCreateDate();
        }
    }


}
