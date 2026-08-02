package com.yjw.api.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@Schema(description = "目录简单信息")
public class CataSimpleInfoDTO {
    @Schema(description = "目录id")
    private Long id;
    @Schema(description = "目录名称")
    private String name;
    @Schema(description = "数字序号，不包含章序号")
    private Integer cIndex;
}
