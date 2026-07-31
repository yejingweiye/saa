package com.tianji.aigc.tools;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.tools.result.CourseInfo;
import com.tianji.api.client.course.CourseClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import com.tianji.api.dto.course.CourseBaseInfoDTO;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseTools {

    private final CourseClient courseClient;
    private static final String FIELD_NAME_FORMAT = "{}_{}";  // 提取格式字符串常量

    // 1589905661084430337造数据


    /**
     * 根据课程id查询课程信息
     *
     * @param courseId 课程id
     * @return 课程信息
     * <p>
     * 查询课程，课程id：1880521847886917634
     */
    @Tool(description = Constant.Tools.QUERY_COURSE_BY_ID)
    public CourseInfo queryCourseById(@ToolParam(description = Constant.ToolParams.COURSE_ID) Long courseId, ToolContext toolContext) {
        // 设备配置跑不了那么服务，智只能造数据了
        CourseInfo courseInfo = CourseInfo.builder()
                .id(1880521847886917634L)
                .name("互联网产品运营实战")
                .price(91.99)
                .validDuration(3)
                .usePeople("本课程适合20至35岁之间的学员，要求具备大专及以上学历，并且需要有一定的市场营销或产品管理基础知识。学员应对互联网行业有浓厚的兴趣，愿意深入学习如何管理和优化互联网产品。")
                .detail("本课程聚焦全流程产品运营实战，从用户需求挖掘、用户分层运营、线上活动策划、数据指标复盘到产品版本迭代全覆盖。课程额外增设技术联动模块，结合Java后端开发逻辑讲解需求落地、接口对接、数据埋点等内容，帮助学员同时掌握运营方法论与基础技术思维，实现产品运营高效落地。")
                .build();
        String field = StrUtil.format(FIELD_NAME_FORMAT,
                StrUtil.lowerFirst(CourseInfo.class.getSimpleName()),
                courseInfo.getId());
        // 获取传过来的requestId，将数据存储到ToolResultHolder中
        var requestId = Convert.toStr(toolContext.getContext().get(Constant.REQUEST_ID));
        ToolResultHolder.put(requestId, field, courseInfo);
        log.info("查询课程，课程id：{}，工具课程信息：{}", courseId, courseInfo);

        return courseInfo;

//  卡片的数据格式
//  private static final Map<String, Map<String, Object>> HANDLER_MAP = new ConcurrentHashMap<>();
//        {
//            requestId:{
//                courseInfo_1880524734406930434:{
//                    "id": 1880524734406930434,
//                    "name": "机器学习"
//                }
//            }
//        }
//        {
//            "eventData": {
//            "courseInfo_1880532172006830082": {
//                "id": 1880532172006830082,
//                        "name": "Java大数据处理与分析",
//                        "price": 12.99,
//                        "validDuration": 24,
//                        "usePeople": "年龄22岁以上，本科及以上学历，具备扎实的Java编程基础，并对大数据技术有兴趣或需求的人士。特别适合那些希望在数据密集型应用场景下利用Java进行高效数据处理的工程师。",
//                        "detail": "课程首先回顾了必要的Java基础知识，随后深入探讨了Hadoop生态系统中的MapReduce编程模型、HDFS存储机制及其应用案例。接着，课程会详细介绍Apache Spark的核心概念、RDD操作、DataFrame API以及Spark Streaming实时处理功能。最后，通过一系列真实世界的大数据项目案例，学员将学会如何使用Java结合这些工具和技术来解决实际的数据挑战。完成课程后，学员将有能力参与或领导大数据项目的设计与实施。"
//            }
//        },
//            "eventType": 1003
//        }

// 真实数据，能跑起来多服务，可放开
//        return Optional.ofNullable(courseId)
//                .map(id -> CourseInfo.of(this.courseClient.baseInfo(id, true)))
//                .map(courseInfo -> {
//                    // field的格式：courseInfo_1880524734406930434
//                    String field = StrUtil.format(FIELD_NAME_FORMAT,
//                            StrUtil.lowerFirst(CourseInfo.class.getSimpleName()),
//                            courseInfo.getId());
//                    // 获取传过来的requestId，将数据存储到ToolResultHolder中
//                    var requestId = Convert.toStr(toolContext.getContext().get(Constant.REQUEST_ID));
//                    ToolResultHolder.put(requestId, field, courseInfo);
//                    return courseInfo;
//                })
//                .orElse(null);
    }

    /**
     * 生成10条课程伪造测试数据
     * @return List<单条课程Map>
     */
    private Map<Long, CourseInfo> fakeCourseData() {
        // 固定随机实例，全局复用
        Random random = new Random();
        List<String> eduList = Arrays.asList("本科", "大专", "硕士", "博士");
        List<String> techList = Arrays.asList("Java", "大数据", "机器学习", "Spark");

        Map<Long, CourseInfo> mapData = new HashMap<>(10);
        // 正确循环：生成10条数据，id自增
        long baseId = 1880521847886917634L;
        for (int i = 0; i < 10; i++) {
            Long courseId = baseId + i;
            String edu = eduList.get(random.nextInt(eduList.size()));
            String tech = techList.get(random.nextInt(techList.size()));

            // 价格1~100，有效期1~100，规避0值
            int price = random.nextInt(99) + 1;
            int validDuration = random.nextInt(99) + 1;

            CourseInfo courseInfo = CourseInfo.builder()
                    .id(courseId)
                    .name(tech + "课程")
                    .price(price)
                    .validDuration(validDuration)
                    .usePeople("年龄" + random.nextInt(100) + "岁以上，" + edu + "及以上学历，具备扎实的Java编程基础，并对" + tech + "技术有兴趣或需求.")
                    .detail("课程首先回顾了必要的 " + edu + "基础知识")
                    .build();
            mapData.put(courseId, courseInfo);

        }
        return mapData;
    }


}







