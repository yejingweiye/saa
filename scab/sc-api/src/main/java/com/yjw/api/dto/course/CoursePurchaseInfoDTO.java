package com.yjw.api.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Schema(description = "课程购买信息")
@NoArgsConstructor
@AllArgsConstructor
public class CoursePurchaseInfoDTO {
    @Schema(description = "报名人数")
    private Integer enrollNum;
    @Schema(description = "退款人数")
    private Integer refundNum;
    @Schema(description = "实付总金额")
    private Integer realPayAmount;
}
