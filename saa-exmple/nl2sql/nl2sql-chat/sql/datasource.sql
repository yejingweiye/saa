SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for datasource
-- ----------------------------
DROP TABLE IF EXISTS `datasource`;
CREATE TABLE `datasource`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '数据源名称',
  `type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '数据库类型 mysql/postgresql',
  `host` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '主机',
  `port` int NULL DEFAULT NULL COMMENT '端口',
  `database_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '数据库名',
  `username` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '密码',
  `connection_url` varchar(1024) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'JDBC连接串',
  `status` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '状态',
  `test_status` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '测试状态',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '描述',
  `creator_id` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for agent_datasource
-- ----------------------------
DROP TABLE IF EXISTS `agent_datasource`;
CREATE TABLE `agent_datasource`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `agent_id` int NULL DEFAULT NULL COMMENT '智能体id',
  `datasource_id` int NULL DEFAULT NULL COMMENT '数据源id',
  `is_active` int NULL DEFAULT NULL COMMENT '是否启用 1启用 0停用',
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records
-- ----------------------------
INSERT INTO `datasource` (`name`, `type`, `host`, `port`, `database_name`, `username`, `password`, `connection_url`, `status`, `test_status`, `description`, `creator_id`, `create_time`, `update_time`)
VALUES ('nl2sql_demo', 'mysql', '127.0.0.1', 3306, 'nl2sql_demo', 'root', '123456',
        'jdbc:mysql://127.0.0.1:3306/nl2sql_demo?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai',
        'active', 'success', 'nl2sql demo mysql', 1, NOW(), NOW());

-- agent 999999 绑定上面的数据源（SqlExecuteNode 查不到 agent 数据源时会兜底用 999999）
INSERT INTO `agent_datasource` (`agent_id`, `datasource_id`, `is_active`, `create_time`, `update_time`)
VALUES (999999, LAST_INSERT_ID(), 1, NOW(), NOW());

SET FOREIGN_KEY_CHECKS = 1;
