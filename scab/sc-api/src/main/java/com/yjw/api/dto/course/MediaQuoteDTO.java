package com.yjw.api.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "媒资被引用情况")
@AllArgsConstructor
@NoArgsConstructor
public class MediaQuoteDTO {
    @Schema(description = "媒资id")
    private Long mediaId;
    @Schema(description = "引用数")
    private Integer quoteNum;
}
