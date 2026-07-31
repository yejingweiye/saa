package com.tianji.aigc.tools;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.tools.result.PrePlaceOrder;
import com.tianji.api.client.trade.TradeClient;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderTools {

    private final TradeClient tradeClient;

    @Tool(description = Constant.Tools.PRE_PLACE_ORDER)
    public PrePlaceOrder prePlaceOrder(@ToolParam(description = Constant.ToolParams.COURSE_IDS) List<Number> ids,
                                       ToolContext toolContext) {

//        // 设置用户ID，用于身份验证，否在在Feign调用时会出现401错误
//        UserContext.setUser(Convert.toLong(toolContext.getContext().get(Constant.USER_ID)));
//        // 大模型传入的ids，可能是int类型，所以转化为long类型，再调用Feign
//        var orderConfirmVO = this.tradeClient.prePlaceOrder(CollStreamUtil.toList(ids, Number::longValue));
//
//        return Optional.ofNullable(orderConfirmVO)
//                .map(PrePlaceOrder::of)
//                .map(prePlaceOrder -> {
//                    var field = StrUtil.lowerFirst(prePlaceOrder.getClass().getSimpleName());
//                    var requestId = Convert.toStr(toolContext.getContext().get(Constant.REQUEST_ID));
//                    ToolResultHolder.put(requestId, field, prePlaceOrder);
//                    return prePlaceOrder;
//                })
//                .orElse(null);

        PrePlaceOrder prePlaceOrder = PrePlaceOrder.builder()
                .count(1)
                .totalAmount(199.0)
                .discountAmount(6.0)
                .couponName("叠加6券：【优惠6.0元】")
                .payAmount(193.0)
                .courseIds(List.of(1880521847886917634L))
                .orderId(1904459544722419714L)
                .couponId(1901825343409999874L)
                .build();
        var field = StrUtil.lowerFirst(prePlaceOrder.getClass().getSimpleName());
        var requestId = Convert.toStr(toolContext.getContext().get(Constant.REQUEST_ID));
        ToolResultHolder.put(requestId, field, prePlaceOrder);
        log.info("课程id：{}，预下单信息：{}", ids, prePlaceOrder);

        return prePlaceOrder;

    }

    /**
     * 生成10条预下单伪造测试数据
     * <p>
     * "prePlaceOrder": {
     * "count": 1,
     * "totalAmount": 199.0,
     * "discountAmount": 6.0,
     * "couponName": "叠加6券：【优惠6.0元】",
     * "payAmount": 193.0,
     * "courseIds": [
     * "1589905661084430337"
     * ],
     * "orderId": "1904459544722419714",
     * "couponId": "1901825343409999874"
     * }
     *
     * @return
     */
    private Map<Long, PrePlaceOrder> fakePrePlaceOrderData() {
        Random random = new Random();
        Map<Long, PrePlaceOrder> prePlaceOrderMap = new HashMap<>(10);
        long baseId = 1880521847886917634L;
        for (int i = 0; i < 10; i++) {
            Long courseId = baseId + i;
            PrePlaceOrder prePlaceOrder = PrePlaceOrder.builder()
                    //重新生成随机，下面不符合
                    .count(1)
                    .totalAmount(199.0)
                    .discountAmount(6.0)
                    .couponName("叠加6券：【优惠6.0元】")
                    .payAmount(193.0)
                    .courseIds(List.of(courseId))
                    .orderId(1904459544722419714L)
                    .couponId(1901825343409999874L)
                    .build();
            prePlaceOrderMap.put(courseId, prePlaceOrder);
        }
        return prePlaceOrderMap;
    }

}
