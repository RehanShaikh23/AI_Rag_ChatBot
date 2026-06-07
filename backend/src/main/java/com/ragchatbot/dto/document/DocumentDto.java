package com.ragchatbot.dto.document;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentDto {

    private Long id;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private Integer chunkCount;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
}
