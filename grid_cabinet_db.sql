/*
 Navicat Premium Data Transfer

 Source Server         : 127.0.0.1
 Source Server Type    : MySQL
 Source Server Version : 50717
 Source Host           : localhost:3306
 Source Schema         : grid_cabinet_db

 Target Server Type    : MySQL
 Target Server Version : 50717
 File Encoding         : 65001

 Date: 05/06/2026 16:33:06
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for cabinet_config
-- ----------------------------
DROP TABLE IF EXISTS `cabinet_config`;
CREATE TABLE `cabinet_config`  (
  `id` int(13) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '柜子名称',
  `width` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '页面显示的柜子宽度默认270px',
  `height` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '柜子高度 默认auto',
  `isDefault` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '这个柜子将作为初始展示  true/false',
  `dehumidifier_comm_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '除湿机通讯方式 485 或 TCP',
  `dehumidifier_comm_port` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '通讯端口  485 时存 COM1 等；TCP 时存 IP 地址除湿机',
  `dehumidifier_addr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '除湿机地址 除湿设备地址/标识',
  `humidity_max` decimal(10, 0) NULL DEFAULT NULL COMMENT '湿度上限 单位：%RH',
  `humidity_min` decimal(10, 0) NULL DEFAULT NULL COMMENT '湿度下限 单位：%RH',
  `temperature_max` decimal(10, 0) NULL DEFAULT NULL COMMENT '温度上限 单位：℃',
  `temperature_min` decimal(10, 0) NULL DEFAULT NULL COMMENT '温度下限 单位：℃',
  `lock_comm_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '锁板通讯方式 c485 或 TCP',
  `lock_comm_port` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '锁板通讯端口  485 时存 COM1 等；TCP 时存 IP 地址除湿机',
  `lock_board_addr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '锁板地址',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '柜子配置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cabinet_config
-- ----------------------------
INSERT INTO `cabinet_config` VALUES (1, '格口柜1#', '270px', 'auto', 'false', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `cabinet_config` VALUES (2, '格口柜2#', '270px', 'auto', 'false', '485', 'com2', '5', 70, 30, 25, 15, NULL, NULL, NULL);
INSERT INTO `cabinet_config` VALUES (3, '格口柜3#', '150px', 'auto', 'true', 'TCP', '192.168.1.2:8456', '2', 70, 30, 25, 15, '485', 'com1@115200', '');
INSERT INTO `cabinet_config` VALUES (4, '格口柜4#', '270px', 'auto', 'false', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `cabinet_config` VALUES (5, '格口柜5#', '270px', 'auto', 'false', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for cabinet_config_copy1
-- ----------------------------
DROP TABLE IF EXISTS `cabinet_config_copy1`;
CREATE TABLE `cabinet_config_copy1`  (
  `id` int(13) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '柜子名称',
  `width` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '页面显示的柜子宽度默认270px',
  `height` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '柜子高度 默认auto',
  `isDefault` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '这个柜子将作为初始展示  true/false',
  `dehumidifier_comm_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '除湿机通讯方式 485 或 TCP',
  `dehumidifier_comm_port` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '通讯端口  485 时存 COM1 等；TCP 时存 IP 地址除湿机',
  `dehumidifier_addr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '除湿机地址 除湿设备地址/标识',
  `humidity_max` decimal(10, 0) NULL DEFAULT NULL COMMENT '湿度上限 单位：%RH',
  `humidity_min` decimal(10, 0) NULL DEFAULT NULL COMMENT '湿度下限 单位：%RH',
  `temperature_max` decimal(10, 0) NULL DEFAULT NULL COMMENT '温度上限 单位：℃',
  `temperature_min` decimal(10, 0) NULL DEFAULT NULL COMMENT '温度下限 单位：℃',
  `lock_comm_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '锁板通讯方式 c485 或 TCP',
  `lock_comm_port` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '锁板通讯端口  485 时存 COM1 等；TCP 时存 IP 地址除湿机',
  `lock_board_addr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '锁板地址',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '柜子配置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cabinet_config_copy1
-- ----------------------------
INSERT INTO `cabinet_config_copy1` VALUES (1, '格口柜1#', '270px', 'auto', 'false', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `cabinet_config_copy1` VALUES (2, '格口柜2#', '270px', 'auto', 'false', '485', 'com2', '5', 70, 30, 25, 15, NULL, NULL, NULL);
INSERT INTO `cabinet_config_copy1` VALUES (3, '格口柜3#', '150px', 'auto', 'true', 'TCP', '192.168.1.2', '2', 70, 30, 25, 15, 'TCP', '192.168.1.3', '');
INSERT INTO `cabinet_config_copy1` VALUES (4, '格口柜4#', '270px', 'auto', 'false', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `cabinet_config_copy1` VALUES (5, '格口柜5#', '270px', 'auto', 'false', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for cell_config
-- ----------------------------
DROP TABLE IF EXISTS `cell_config`;
CREATE TABLE `cell_config`  (
  `id` int(13) NOT NULL AUTO_INCREMENT,
  `cabinet_id` int(13) NOT NULL COMMENT '所属柜子',
  `row_num` int(255) NOT NULL COMMENT '第几行',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '类型 cell格口/image 图片',
  `columns` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '单元格占据的列宽 1fr',
  `height` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '单元格占据的行高 \'60px\'',
  `colSpan` int(13) NULL DEFAULT NULL COMMENT '跨越的列数（整数）',
  `rowSpan` int(13) NULL DEFAULT NULL COMMENT '跨越的行数（整数）',
  `number` int(13) NULL DEFAULT NULL COMMENT '普通格子 (type=\'cell\') 额外字段：格子编号',
  `toolName` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '普通格子 (type=\'cell\') 额外字段：工具名称',
  `isEmpty` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '普通格子 (type=\'cell\') 额外字段：是否空格子',
  `imageUrl` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '图片格子 (type=\'image\') 额外字段：图片地址',
  `label` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '图片格子 (type=\'image\') 额外字段：图片标签',
  `mac_address` varchar(17) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '硬件地址',
  `qrcode_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '二维码内容',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 58 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '格口配置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cell_config
-- ----------------------------
INSERT INTO `cell_config` VALUES (1, 1, 1, 'cell', '1fr', '72px', 1, 1, 1, '1骄傲', 'false', NULL, NULL, '1', '0879ccc53393e14');
INSERT INTO `cell_config` VALUES (2, 1, 1, 'cell', '1fr', '72px', 1, 1, 2, '精密天平2#', 'false', NULL, NULL, '2', 'a5a94bcc277d7e9');
INSERT INTO `cell_config` VALUES (3, 1, 1, 'cell', '1fr', '72px', 1, 1, 3, '精密天平3#', 'false', NULL, NULL, '3', '85315c1e22a3ef1');
INSERT INTO `cell_config` VALUES (4, 1, 2, 'cell', '1fr', '72px', 1, 1, 4, '精密天平4#', 'false', NULL, NULL, '4', '7188de66ffa9d7f');
INSERT INTO `cell_config` VALUES (5, 1, 2, 'cell', '1fr', '72px', 1, 1, 5, '精密天平5#', 'false', NULL, NULL, '5', 'd619947eb6afc5c');
INSERT INTO `cell_config` VALUES (6, 1, 2, 'cell', '1fr', '72px', 1, 1, 6, '精密天平6#', 'false', NULL, NULL, '6', '7660a89f1da8feb');
INSERT INTO `cell_config` VALUES (7, 1, 3, 'cell', '1fr', '72px', 1, 1, 7, '精密天平7#', 'false', NULL, NULL, '7', 'cc7c0023f642144');
INSERT INTO `cell_config` VALUES (8, 1, 3, 'cell', '1fr', '72px', 1, 1, 8, '精密天平8#', 'false', NULL, NULL, '8', 'ad1d50945c98a57');
INSERT INTO `cell_config` VALUES (9, 1, 3, 'cell', '1fr', '72px', 1, 1, 9, '精密天平9#', 'false', NULL, NULL, '9', '13752530eee07db');
INSERT INTO `cell_config` VALUES (10, 1, 4, 'cell', '1fr', '60px', 3, 1, 10, '精密天平10#', 'false', NULL, NULL, '10', '90a4216f0f7f379');
INSERT INTO `cell_config` VALUES (11, 1, 5, 'cell', '1fr', '60px', 3, 1, 11, '精密天平11#', 'false', NULL, NULL, '11', 'c6cf16a5126f9d9');
INSERT INTO `cell_config` VALUES (12, 1, 6, 'cell', '1fr', '60px', 3, 1, 12, '精密天平12#', 'false', NULL, NULL, '12', 'b59afe0b1b63343');
INSERT INTO `cell_config` VALUES (13, 1, 7, 'cell', '1fr', '100px', 3, 1, 13, '精密天平13#', 'false', NULL, NULL, '13', '5589798b8f0af80');
INSERT INTO `cell_config` VALUES (14, 2, 1, 'cell', '1fr', '72px', 1, 1, 14, '精密天平14#', 'false', NULL, NULL, '14', '6b4e904e755d994');
INSERT INTO `cell_config` VALUES (15, 2, 1, 'cell', '1fr', '72px', 1, 1, 15, '精密天平15#', 'false', NULL, NULL, '15', 'b0c9b01024f8d93');
INSERT INTO `cell_config` VALUES (16, 2, 1, 'cell', '1fr', '72px', 1, 1, 16, '精密天平16#', 'false', NULL, NULL, '16', '968d316620617e0');
INSERT INTO `cell_config` VALUES (17, 2, 2, 'cell', '1fr', '72px', 1, 1, 17, '精密天平17#', 'false', NULL, NULL, '17', '60ae5410fcdd0f0');
INSERT INTO `cell_config` VALUES (18, 2, 2, 'cell', '1fr', '72px', 1, 1, 18, '精密天平18#', 'false', NULL, NULL, '18', '5d42f64275a48bb');
INSERT INTO `cell_config` VALUES (19, 2, 2, 'cell', '1fr', '72px', 1, 1, 19, '精密天平19#', 'false', NULL, NULL, '19', 'e6a9d941cf31c5d');
INSERT INTO `cell_config` VALUES (20, 2, 3, 'cell', '1fr', '72px', 1, 1, 20, '精密天平20#', 'false', NULL, NULL, '20', '71fd95ebbe4aae7');
INSERT INTO `cell_config` VALUES (21, 2, 3, 'cell', '1fr', '72px', 1, 1, 21, '精密天平21#', 'true', NULL, NULL, '21', '86f4516166d536f');
INSERT INTO `cell_config` VALUES (22, 2, 3, 'cell', '1fr', '72px', 1, 1, 22, '精密天平22#', 'false', NULL, NULL, '22', '12080b8a5fd6059');
INSERT INTO `cell_config` VALUES (23, 2, 4, 'cell', '1fr', '60px', 3, 1, 23, '精密天平23#', 'false', NULL, NULL, '23', 'f572b90748e8b59');
INSERT INTO `cell_config` VALUES (24, 2, 5, 'cell', '1fr', '60px', 3, 1, 24, '精密天平24#', 'false', NULL, NULL, '24', 'fcd03a5eaa49ccc');
INSERT INTO `cell_config` VALUES (25, 2, 6, 'cell', '1fr', '60px', 3, 1, 25, 'z222', 'false', NULL, NULL, '25', '03a3c3c6a030377');
INSERT INTO `cell_config` VALUES (26, 2, 7, 'cell', '1fr', '100px', 3, 1, 26, '精密天平26#', 'false', NULL, NULL, '26', '471e8919988abd1');
INSERT INTO `cell_config` VALUES (27, 3, 1, 'image', '1fr', '72px', 3, 3, NULL, NULL, '', '/uploads/21cbfae1-d608-40d2-bf98-1db2f0d83f93.jpeg', '主控电脑', '', NULL);
INSERT INTO `cell_config` VALUES (28, 3, 2, 'cell', '1fr', '60px', 3, 1, 27, '精密天平27#', 'true', NULL, NULL, '27', '227371cf620a09d');
INSERT INTO `cell_config` VALUES (29, 3, 3, 'cell', '1fr', '60px', 3, 1, 28, '精密天平28#', 'true', NULL, NULL, '28', '595f5079f58093b');
INSERT INTO `cell_config` VALUES (30, 3, 4, 'cell', '1fr', '60px', 3, 1, 29, '精密天平29#', 'true', NULL, NULL, '29', '3069590e96fc91d');
INSERT INTO `cell_config` VALUES (31, 3, 5, 'cell', '1fr', '100px', 3, 1, 30, '精密天平30#', 'true', NULL, NULL, '30', '5abc4375c5108e2');
INSERT INTO `cell_config` VALUES (32, 4, 1, 'cell', '1fr', '72px', 1, 1, 31, '精密天平31#', 'false', NULL, NULL, '31', '5c6e11f9a33d9b7');
INSERT INTO `cell_config` VALUES (33, 4, 1, 'cell', '1fr', '72px', 1, 1, 32, '精密天平32#', 'false', NULL, NULL, '32', '87fd2760f3b00a9');
INSERT INTO `cell_config` VALUES (34, 4, 1, 'cell', '1fr', '72px', 1, 1, 33, '精密天平33#', 'false', NULL, NULL, '33', 'ec09a2ab1f52e5a');
INSERT INTO `cell_config` VALUES (35, 4, 2, 'cell', '1fr', '72px', 1, 1, 34, '精密天平34#', 'false', NULL, NULL, '34', '2237c351e54a8b9');
INSERT INTO `cell_config` VALUES (36, 4, 2, 'cell', '1fr', '72px', 1, 1, 35, '精密天平35#', 'false', NULL, NULL, '35', '224052ab6f44408');
INSERT INTO `cell_config` VALUES (37, 4, 2, 'cell', '1fr', '72px', 1, 1, 36, '精密天平36#', 'false', NULL, NULL, '36', '7899160bbb4e7b3');
INSERT INTO `cell_config` VALUES (38, 4, 3, 'cell', '1fr', '72px', 1, 1, 37, '精密天平37#', 'false', NULL, NULL, '37', '2f18715b3fbbeb3');
INSERT INTO `cell_config` VALUES (39, 4, 3, 'cell', '1fr', '72px', 1, 1, 38, '精密天平38#', 'false', NULL, NULL, '38', 'a4ecbb10cb3c99d');
INSERT INTO `cell_config` VALUES (40, 4, 3, 'cell', '1fr', '72px', 1, 1, 39, '精密天平39#', 'false', NULL, NULL, '39', '929c3c7995b39d4');
INSERT INTO `cell_config` VALUES (41, 4, 4, 'cell', '1fr', '60px', 3, 1, 40, '精密天平40#', 'false', NULL, NULL, '40', '01c7559fe739517');
INSERT INTO `cell_config` VALUES (42, 4, 5, 'cell', '1fr', '60px', 3, 1, 41, '精密天平41#', 'true', NULL, NULL, '41', '400973d38449b48');
INSERT INTO `cell_config` VALUES (43, 4, 6, 'cell', '1fr', '60px', 3, 1, 42, '精密天平42#', 'false', NULL, NULL, '42', '467bf4065d66cad');
INSERT INTO `cell_config` VALUES (44, 4, 7, 'cell', '1fr', '100px', 3, 1, 43, '精密天平43#', 'false', NULL, NULL, '43', '9f51dad11dc5f98');
INSERT INTO `cell_config` VALUES (45, 5, 1, 'cell', '1fr', '72px', 1, 1, 44, '精密天平44#', 'false', NULL, NULL, '44', 'c9c3be1f06122a1');
INSERT INTO `cell_config` VALUES (46, 5, 1, 'cell', '1fr', '72px', 1, 1, 45, '精密天平45#', 'false', NULL, NULL, '45', '0878fa1b1a3a22f');
INSERT INTO `cell_config` VALUES (47, 5, 1, 'cell', '1fr', '72px', 1, 1, 46, '精密天平46#', 'false', NULL, NULL, '46', 'bde0e09e816be7b');
INSERT INTO `cell_config` VALUES (48, 5, 2, 'cell', '1fr', '72px', 1, 1, 47, '精密天平47#', 'false', NULL, NULL, '47', '4dbac131257373f');
INSERT INTO `cell_config` VALUES (49, 5, 2, 'cell', '1fr', '72px', 1, 1, 48, '精密天平48#', 'false', NULL, NULL, '48', 'd85a2955f528efa');
INSERT INTO `cell_config` VALUES (50, 5, 2, 'cell', '1fr', '72px', 1, 1, 49, '精密天平49#', 'false', NULL, NULL, '49', '86e53667df63182');
INSERT INTO `cell_config` VALUES (51, 5, 3, 'cell', '1fr', '72px', 1, 1, 50, '精密天平50#', 'false', NULL, NULL, '50', '2c4c566bcc55a37');
INSERT INTO `cell_config` VALUES (52, 5, 3, 'cell', '1fr', '72px', 1, 1, 51, '精密天平51#', 'false', NULL, NULL, '51', 'a67daf76dcaa1d8');
INSERT INTO `cell_config` VALUES (53, 5, 3, 'cell', '1fr', '72px', 1, 1, 52, '精密天平52#', 'false', NULL, NULL, '52', '96c8788a5db7a92');
INSERT INTO `cell_config` VALUES (54, 5, 4, 'cell', '1fr', '60px', 3, 1, 53, '精密天平53#', 'false', NULL, NULL, '53', 'aefb221b6cf7b3c');
INSERT INTO `cell_config` VALUES (55, 5, 5, 'cell', '1fr', '60px', 3, 1, 54, '精密天平54#', 'false', NULL, NULL, '54', '2708e81f56e8966');
INSERT INTO `cell_config` VALUES (56, 5, 6, 'cell', '1fr', '60px', 3, 1, 55, '精密天平55#', 'false', NULL, NULL, '55', '6eed3a274a96c46');
INSERT INTO `cell_config` VALUES (57, 5, 7, 'cell', '1fr', '100px', 3, 1, 56, '精密天平56#', 'false', NULL, NULL, '56', '8858520ae2f1fe3');

-- ----------------------------
-- Table structure for cell_config_copy1
-- ----------------------------
DROP TABLE IF EXISTS `cell_config_copy1`;
CREATE TABLE `cell_config_copy1`  (
  `id` int(13) NOT NULL AUTO_INCREMENT,
  `cabinet_id` int(13) NOT NULL COMMENT '所属柜子',
  `row_num` int(255) NOT NULL COMMENT '第几行',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '类型 cell格口/image 图片',
  `columns` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '单元格占据的列宽 1fr',
  `height` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '单元格占据的行高 \'60px\'',
  `colSpan` int(13) NULL DEFAULT NULL COMMENT '跨越的列数（整数）',
  `rowSpan` int(13) NULL DEFAULT NULL COMMENT '跨越的行数（整数）',
  `number` int(13) NULL DEFAULT NULL COMMENT '普通格子 (type=\'cell\') 额外字段：格子编号',
  `toolName` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '普通格子 (type=\'cell\') 额外字段：工具名称',
  `isEmpty` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '普通格子 (type=\'cell\') 额外字段：是否空格子',
  `imageUrl` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '图片格子 (type=\'image\') 额外字段：图片地址',
  `label` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '图片格子 (type=\'image\') 额外字段：图片标签',
  `mac_address` varchar(17) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '硬件地址',
  `qrcode_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '二维码内容',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 58 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '格口配置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cell_config_copy1
-- ----------------------------
INSERT INTO `cell_config_copy1` VALUES (1, 1, 1, 'cell', '1fr', '72px', 1, 1, 1, '1骄傲', 'false', NULL, NULL, '1', '0879ccc53393e14');
INSERT INTO `cell_config_copy1` VALUES (2, 1, 1, 'cell', '1fr', '72px', 1, 1, 2, '精密天平2#', 'false', NULL, NULL, '2', 'a5a94bcc277d7e9');
INSERT INTO `cell_config_copy1` VALUES (3, 1, 1, 'cell', '1fr', '72px', 1, 1, 3, '精密天平3#', 'false', NULL, NULL, '3', NULL);
INSERT INTO `cell_config_copy1` VALUES (4, 1, 2, 'cell', '1fr', '72px', 1, 1, 4, '精密天平4#', 'false', NULL, NULL, '4', NULL);
INSERT INTO `cell_config_copy1` VALUES (5, 1, 2, 'cell', '1fr', '72px', 1, 1, 5, '精密天平5#', 'false', NULL, NULL, '5', 'd619947eb6afc5c');
INSERT INTO `cell_config_copy1` VALUES (6, 1, 2, 'cell', '1fr', '72px', 1, 1, 6, '精密天平6#', 'false', NULL, NULL, '6', NULL);
INSERT INTO `cell_config_copy1` VALUES (7, 1, 3, 'cell', '1fr', '72px', 1, 1, 7, '精密天平7#', 'false', NULL, NULL, '7', 'cc7c0023f642144');
INSERT INTO `cell_config_copy1` VALUES (8, 1, 3, 'cell', '1fr', '72px', 1, 1, 8, '精密天平8#', 'false', NULL, NULL, '8', 'ad1d50945c98a57');
INSERT INTO `cell_config_copy1` VALUES (9, 1, 3, 'cell', '1fr', '72px', 1, 1, 9, '精密天平9#', 'false', NULL, NULL, '9', '13752530eee07db');
INSERT INTO `cell_config_copy1` VALUES (10, 1, 4, 'cell', '1fr', '60px', 3, 1, 10, '精密天平10#', 'false', NULL, NULL, '10', '90a4216f0f7f379');
INSERT INTO `cell_config_copy1` VALUES (11, 1, 5, 'cell', '1fr', '60px', 3, 1, 11, '精密天平11#', 'false', NULL, NULL, '11', 'c6cf16a5126f9d9');
INSERT INTO `cell_config_copy1` VALUES (12, 1, 6, 'cell', '1fr', '60px', 3, 1, 12, '精密天平12#', 'false', NULL, NULL, '12', NULL);
INSERT INTO `cell_config_copy1` VALUES (13, 1, 7, 'cell', '1fr', '100px', 3, 1, 13, '精密天平13#', 'false', NULL, NULL, '13', NULL);
INSERT INTO `cell_config_copy1` VALUES (14, 2, 1, 'cell', '1fr', '72px', 1, 1, 14, '精密天平14#', 'false', NULL, NULL, '14', '6b4e904e755d994');
INSERT INTO `cell_config_copy1` VALUES (15, 2, 1, 'cell', '1fr', '72px', 1, 1, 15, '精密天平15#', 'false', NULL, NULL, '15', NULL);
INSERT INTO `cell_config_copy1` VALUES (16, 2, 1, 'cell', '1fr', '72px', 1, 1, 16, '精密天平16#', 'false', NULL, NULL, '16', NULL);
INSERT INTO `cell_config_copy1` VALUES (17, 2, 2, 'cell', '1fr', '72px', 1, 1, 17, '精密天平17#', 'false', NULL, NULL, '17', NULL);
INSERT INTO `cell_config_copy1` VALUES (18, 2, 2, 'cell', '1fr', '72px', 1, 1, 18, '精密天平18#', 'false', NULL, NULL, '18', NULL);
INSERT INTO `cell_config_copy1` VALUES (19, 2, 2, 'cell', '1fr', '72px', 1, 1, 19, '精密天平19#', 'false', NULL, NULL, '19', NULL);
INSERT INTO `cell_config_copy1` VALUES (20, 2, 3, 'cell', '1fr', '72px', 1, 1, 20, '精密天平20#', 'false', NULL, NULL, '20', '71fd95ebbe4aae7');
INSERT INTO `cell_config_copy1` VALUES (21, 2, 3, 'cell', '1fr', '72px', 1, 1, 21, '精密天平21#', 'true', NULL, NULL, '21', NULL);
INSERT INTO `cell_config_copy1` VALUES (22, 2, 3, 'cell', '1fr', '72px', 1, 1, 22, '精密天平22#', 'true', NULL, NULL, '22', NULL);
INSERT INTO `cell_config_copy1` VALUES (23, 2, 4, 'cell', '1fr', '60px', 3, 1, 23, '精密天平23#', 'false', NULL, NULL, '23', 'f572b90748e8b59');
INSERT INTO `cell_config_copy1` VALUES (24, 2, 5, 'cell', '1fr', '60px', 3, 1, 24, '精密天平24#', 'false', NULL, NULL, '24', 'fcd03a5eaa49ccc');
INSERT INTO `cell_config_copy1` VALUES (25, 2, 6, 'cell', '1fr', '60px', 3, 1, 25, 'z222', 'true', NULL, NULL, '25', '03a3c3c6a030377');
INSERT INTO `cell_config_copy1` VALUES (26, 2, 7, 'cell', '1fr', '100px', 3, 1, 26, '精密天平26#', 'false', NULL, NULL, '26', '471e8919988abd1');
INSERT INTO `cell_config_copy1` VALUES (27, 3, 1, 'image', '1fr', '72px', 3, 3, NULL, NULL, '', '/uploads/21cbfae1-d608-40d2-bf98-1db2f0d83f93.jpeg', '主控电脑', '', NULL);
INSERT INTO `cell_config_copy1` VALUES (28, 3, 2, 'cell', '1fr', '60px', 3, 1, 27, '精密天平27#', 'false', NULL, NULL, '27', '227371cf620a09d');
INSERT INTO `cell_config_copy1` VALUES (29, 3, 3, 'cell', '1fr', '60px', 3, 1, 28, '精密天平28#', 'false', NULL, NULL, '28', '595f5079f58093b');
INSERT INTO `cell_config_copy1` VALUES (30, 3, 4, 'cell', '1fr', '60px', 3, 1, 29, '精密天平29#', 'false', NULL, NULL, '29', '3069590e96fc91d');
INSERT INTO `cell_config_copy1` VALUES (31, 3, 5, 'cell', '1fr', '100px', 3, 1, 30, '精密天平30#', 'false', NULL, NULL, '30', '5abc4375c5108e2');
INSERT INTO `cell_config_copy1` VALUES (32, 4, 1, 'cell', '1fr', '72px', 1, 1, 31, '精密天平31#', 'false', NULL, NULL, '31', NULL);
INSERT INTO `cell_config_copy1` VALUES (33, 4, 1, 'cell', '1fr', '72px', 1, 1, 32, '精密天平32#', 'false', NULL, NULL, '32', NULL);
INSERT INTO `cell_config_copy1` VALUES (34, 4, 1, 'cell', '1fr', '72px', 1, 1, 33, '精密天平33#', 'false', NULL, NULL, '33', NULL);
INSERT INTO `cell_config_copy1` VALUES (35, 4, 2, 'cell', '1fr', '72px', 1, 1, 34, '精密天平34#', 'false', NULL, NULL, '34', NULL);
INSERT INTO `cell_config_copy1` VALUES (36, 4, 2, 'cell', '1fr', '72px', 1, 1, 35, '精密天平35#', 'false', NULL, NULL, '35', NULL);
INSERT INTO `cell_config_copy1` VALUES (37, 4, 2, 'cell', '1fr', '72px', 1, 1, 36, '精密天平36#', 'false', NULL, NULL, '36', NULL);
INSERT INTO `cell_config_copy1` VALUES (38, 4, 3, 'cell', '1fr', '72px', 1, 1, 37, '精密天平37#', 'false', NULL, NULL, '37', NULL);
INSERT INTO `cell_config_copy1` VALUES (39, 4, 3, 'cell', '1fr', '72px', 1, 1, 38, '精密天平38#', 'false', NULL, NULL, '38', NULL);
INSERT INTO `cell_config_copy1` VALUES (40, 4, 3, 'cell', '1fr', '72px', 1, 1, 39, '精密天平39#', 'false', NULL, NULL, '39', NULL);
INSERT INTO `cell_config_copy1` VALUES (41, 4, 4, 'cell', '1fr', '60px', 3, 1, 40, '精密天平40#', 'false', NULL, NULL, '40', NULL);
INSERT INTO `cell_config_copy1` VALUES (42, 4, 5, 'cell', '1fr', '60px', 3, 1, 41, '精密天平41#', 'false', NULL, NULL, '41', NULL);
INSERT INTO `cell_config_copy1` VALUES (43, 4, 6, 'cell', '1fr', '60px', 3, 1, 42, '精密天平42#', 'false', NULL, NULL, '42', NULL);
INSERT INTO `cell_config_copy1` VALUES (44, 4, 7, 'cell', '1fr', '100px', 3, 1, 43, '精密天平43#', 'false', NULL, NULL, '43', NULL);
INSERT INTO `cell_config_copy1` VALUES (45, 5, 1, 'cell', '1fr', '72px', 1, 1, 44, '精密天平44#', 'false', NULL, NULL, '44', NULL);
INSERT INTO `cell_config_copy1` VALUES (46, 5, 1, 'cell', '1fr', '72px', 1, 1, 45, '精密天平45#', 'false', NULL, NULL, '45', NULL);
INSERT INTO `cell_config_copy1` VALUES (47, 5, 1, 'cell', '1fr', '72px', 1, 1, 46, '精密天平46#', 'false', NULL, NULL, '46', NULL);
INSERT INTO `cell_config_copy1` VALUES (48, 5, 2, 'cell', '1fr', '72px', 1, 1, 47, '精密天平47#', 'false', NULL, NULL, '47', NULL);
INSERT INTO `cell_config_copy1` VALUES (49, 5, 2, 'cell', '1fr', '72px', 1, 1, 48, '精密天平48#', 'false', NULL, NULL, '48', NULL);
INSERT INTO `cell_config_copy1` VALUES (50, 5, 2, 'cell', '1fr', '72px', 1, 1, 49, '精密天平49#', 'false', NULL, NULL, '49', NULL);
INSERT INTO `cell_config_copy1` VALUES (51, 5, 3, 'cell', '1fr', '72px', 1, 1, 50, '精密天平50#', 'false', NULL, NULL, '50', NULL);
INSERT INTO `cell_config_copy1` VALUES (52, 5, 3, 'cell', '1fr', '72px', 1, 1, 51, '精密天平51#', 'false', NULL, NULL, '51', NULL);
INSERT INTO `cell_config_copy1` VALUES (53, 5, 3, 'cell', '1fr', '72px', 1, 1, 52, '精密天平52#', 'false', NULL, NULL, '52', NULL);
INSERT INTO `cell_config_copy1` VALUES (54, 5, 4, 'cell', '1fr', '60px', 3, 1, 53, '精密天平53#', 'false', NULL, NULL, '53', 'aefb221b6cf7b3c');
INSERT INTO `cell_config_copy1` VALUES (55, 5, 5, 'cell', '1fr', '60px', 3, 1, 54, '精密天平54#', 'false', NULL, NULL, '54', '2708e81f56e8966');
INSERT INTO `cell_config_copy1` VALUES (56, 5, 6, 'cell', '1fr', '60px', 3, 1, 55, '精密天平55#', 'false', NULL, NULL, '55', '6eed3a274a96c46');
INSERT INTO `cell_config_copy1` VALUES (57, 5, 7, 'cell', '1fr', '100px', 3, 1, 56, '精密天平56#', 'false', NULL, NULL, '56', NULL);

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log`  (
  `id` int(13) NOT NULL AUTO_INCREMENT,
  `cabinet_id` int(13) NULL DEFAULT NULL COMMENT '柜子ID',
  `cabinet_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '柜子名称 ',
  `cell_id` int(13) NULL DEFAULT NULL COMMENT '格口id',
  `cell_number` int(13) NULL DEFAULT NULL COMMENT '格口编号',
  `tool_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '工具名称',
  `borrower_photo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '领用照片',
  `borrower_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '领用人姓名',
  `borrower_number` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '工号/卡号',
  `borrow_time` datetime(0) NULL DEFAULT NULL COMMENT '领用时间',
  `borrow_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '领用说明',
  `return_photo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '归还照片',
  `return_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '领用人姓名',
  `return_number` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '工号/卡号',
  `return_time` datetime(0) NULL DEFAULT NULL COMMENT '归还时间',
  `return_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '归还说明',
  `expected_return_time` datetime(0) NULL DEFAULT NULL COMMENT '预计归还时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_oper_log
-- ----------------------------
INSERT INTO `sys_oper_log` VALUES (1, 2, '格口柜2#', 22, 22, '精密天平22#', NULL, NULL, NULL, NULL, NULL, '/uploads/f9a56775-dcaa-4c91-9d90-3744c6ceff7b.jpg', '111111111', '1111111111111', '2026-06-05 03:13:25', '333333333333333333333333333', NULL);
INSERT INTO `sys_oper_log` VALUES (2, 3, '格口柜3#', 29, 28, '精密天平28#', '/uploads/f8eeb44d-57b8-47a3-8702-2f689b508aa6.jpg', '4545', '额外热舞', '2026-06-05 03:20:57', '243241414', '/uploads/f41326c4-1201-4a1d-9c65-2618128f6f74.jpg', '', '', '2026-06-05 03:27:09', '', '2026-06-12 20:00:00');
INSERT INTO `sys_oper_log` VALUES (3, 3, '格口柜3#', 28, 27, '精密天平27#', '/uploads/f8eeb44d-57b8-47a3-8702-2f689b508aa6.jpg', '4545', '额外热舞', '2026-06-05 03:20:57', '243241414', '/uploads/f41326c4-1201-4a1d-9c65-2618128f6f74.jpg', '', '', '2026-06-05 03:27:04', '', '2026-06-12 20:00:00');
INSERT INTO `sys_oper_log` VALUES (4, 3, '格口柜3#', 30, 29, '精密天平29#', '/uploads/f8eeb44d-57b8-47a3-8702-2f689b508aa6.jpg', '4545', '额外热舞', '2026-06-05 03:20:56', '243241414', '/uploads/f41326c4-1201-4a1d-9c65-2618128f6f74.jpg', '', '', '2026-06-05 03:27:20', '', '2026-06-12 20:00:00');
INSERT INTO `sys_oper_log` VALUES (5, 3, '格口柜3#', 31, 30, '精密天平30#', '/uploads/f8eeb44d-57b8-47a3-8702-2f689b508aa6.jpg', '4545', '额外热舞', '2026-06-05 03:20:56', '243241414', '/uploads/f41326c4-1201-4a1d-9c65-2618128f6f74.jpg', '', '', '2026-06-05 03:27:26', '', '2026-06-12 20:00:00');
INSERT INTO `sys_oper_log` VALUES (6, 2, '格口柜2#', 25, 25, 'z222', NULL, NULL, NULL, NULL, NULL, '/uploads/07ca7e3f-448c-48ab-8af6-9d2ce9a65bde.jpg', '42141252', '5215125', '2026-06-05 03:25:30', '51512515', NULL);
INSERT INTO `sys_oper_log` VALUES (7, 2, '格口柜2#', 22, 22, '精密天平22#', NULL, NULL, NULL, NULL, NULL, '/uploads/3b8190b0-bba2-406c-9ed4-80f7b518d0cf.jpg', 'rwqrqw', '242412', '2026-06-05 03:42:37', '41241241', NULL);
INSERT INTO `sys_oper_log` VALUES (8, 3, '格口柜3#', 28, 27, '精密天平27#', NULL, '42421412', '4124214214', '2026-06-05 14:01:48', '1241241241241', NULL, NULL, NULL, NULL, NULL, '2026-07-05 14:01:00');
INSERT INTO `sys_oper_log` VALUES (9, 3, '格口柜3#', 29, 28, '精密天平28#', NULL, '42421412', '4124214214', '2026-06-05 14:01:48', '1241241241241', NULL, NULL, NULL, NULL, NULL, '2026-07-05 14:01:00');
INSERT INTO `sys_oper_log` VALUES (10, 3, '格口柜3#', 30, 29, '精密天平29#', NULL, '42421412', '4124214214', '2026-06-05 14:01:40', '1241241241241', NULL, NULL, NULL, NULL, NULL, '2026-07-05 14:01:00');
INSERT INTO `sys_oper_log` VALUES (11, 3, '格口柜3#', 31, 30, '精密天平30#', NULL, '42421412', '4124214214', '2026-06-05 14:01:33', '1241241241241', NULL, NULL, NULL, NULL, NULL, '2026-07-05 14:01:00');

-- ----------------------------
-- Table structure for sys_oper_log_copy1
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log_copy1`;
CREATE TABLE `sys_oper_log_copy1`  (
  `id` int(13) NOT NULL AUTO_INCREMENT,
  `cabinet_id` int(13) NULL DEFAULT NULL COMMENT '柜子ID',
  `cabinet_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '柜子名称 ',
  `cell_id` int(13) NULL DEFAULT NULL COMMENT '格口id',
  `cell_number` int(13) NULL DEFAULT NULL COMMENT '格口编号',
  `tool_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '工具名称',
  `borrower_photo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '领用照片',
  `borrower_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '领用人姓名',
  `borrower_number` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '工号/卡号',
  `borrow_time` datetime(0) NULL DEFAULT NULL COMMENT '领用时间',
  `borrow_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '领用说明',
  `return_photo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '归还照片',
  `return_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '领用人姓名',
  `return_number` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '工号/卡号',
  `return_time` datetime(0) NULL DEFAULT NULL COMMENT '归还时间',
  `return_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '归还说明',
  `expected_return_time` datetime(0) NULL DEFAULT NULL COMMENT '预计归还时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_oper_log_copy1
-- ----------------------------
INSERT INTO `sys_oper_log_copy1` VALUES (1, 3, '格口柜3#', 31, 30, '精密天平30#', '/uploads/bac30812-6991-484a-807c-ba005c48e384.jpg', '333', '222222222', '2026-05-22 17:11:46', '1111111', NULL, NULL, NULL, NULL, NULL, '2026-05-29 20:00:00');
INSERT INTO `sys_oper_log_copy1` VALUES (2, 3, '格口柜3#', 30, 29, '精密天平29#', '/uploads/bac30812-6991-484a-807c-ba005c48e384.jpg', '333', '222222222', '2026-05-22 17:12:45', '1111111', NULL, NULL, NULL, NULL, NULL, '2026-05-29 20:00:00');
INSERT INTO `sys_oper_log_copy1` VALUES (3, 3, '格口柜3#', 29, 28, '精密天平28#', '/uploads/bac30812-6991-484a-807c-ba005c48e384.jpg', '333', '222222222', '2026-05-22 14:11:44', '1111111', NULL, NULL, NULL, NULL, NULL, '2026-05-29 20:00:00');
INSERT INTO `sys_oper_log_copy1` VALUES (4, 3, '格口柜3#', 28, 27, '精密天平27#', '/uploads/c77c703d-f8bd-48e6-86e7-7a50732852a5.jpg', '22', '3332312', '2026-05-31 23:51:06', '23123123', NULL, NULL, NULL, NULL, NULL, '2026-06-07 20:00:00');
INSERT INTO `sys_oper_log_copy1` VALUES (5, 1, '格口柜1#', 10, 10, '精密天平10#', '/uploads/c77c703d-f8bd-48e6-86e7-7a50732852a5.jpg', '22', '3332312', '2026-05-30 23:55:54', '23123123', '/uploads/bac30812-6991-484a-807c-ba005c48e384.jpg', '333', '222222222', '2026-05-21 17:11:46', NULL, '2026-06-07 20:00:00');
INSERT INTO `sys_oper_log_copy1` VALUES (6, 2, '格口柜2#', 25, 25, 'z', '/uploads/c77c703d-f8bd-48e6-86e7-7a50732852a5.jpg', '22', '3332312', '2026-05-31 23:51:48', '23123123', '/uploads/bac30812-6991-484a-807c-ba005c48e384.jpg', '333', '222222222', '2026-05-31 23:50:48', NULL, '2026-05-22 17:11:45');
INSERT INTO `sys_oper_log_copy1` VALUES (7, 2, '格口柜2#', 24, 24, '精密天平24#', '/uploads/c77c703d-f8bd-48e6-86e7-7a50732852a5.jpg', '22', '3332312', '2026-05-31 23:52:47', '23123123', '/uploads/bac30812-6991-484a-807c-ba005c48e384.jpg', '333', '222222222', '2026-05-22 17:11:44', '333', '2026-06-07 20:00:00');
INSERT INTO `sys_oper_log_copy1` VALUES (8, 2, '格口柜2#', 23, 23, '精密天平23#', '/uploads/c77c703d-f8bd-48e6-86e7-7a50732852a5.jpg', '22', '3332312', '2026-05-31 23:50:47', '23123123', '/uploads/c77c703d-f8bd-48e6-86e7-7a50732852a5.jpg', NULL, NULL, NULL, NULL, '2026-06-07 20:00:00');
INSERT INTO `sys_oper_log_copy1` VALUES (9, 2, '格口柜2#', 20, 20, '精密天平20#', '/uploads/c77c703d-f8bd-48e6-86e7-7a50732852a5.jpg', '22', '3332312', '2026-05-31 23:50:47', '23123123', NULL, NULL, NULL, NULL, NULL, '2026-06-07 20:00:00');
INSERT INTO `sys_oper_log_copy1` VALUES (10, 2, '格口柜2#', 21, 21, '精密天平21#', '/uploads/c77c703d-f8bd-48e6-86e7-7a50732852a5.jpg', '22', '3332312', '2026-05-31 23:50:46', '23123123', NULL, NULL, NULL, NULL, NULL, '2026-06-07 20:00:00');
INSERT INTO `sys_oper_log_copy1` VALUES (11, 2, '格口柜2#', 22, 22, '精密天平22#', '/uploads/c77c703d-f8bd-48e6-86e7-7a50732852a5.jpg', '22', '3332312', '2026-05-31 23:50:46', '23123123', NULL, NULL, NULL, NULL, NULL, '2026-06-07 20:00:00');

-- ----------------------------
-- Table structure for system_config
-- ----------------------------
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config`  (
  `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `system_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '系统名称',
  `eng_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '英文名称',
  `system_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '系统编号',
  `location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '所属位置',
  `admin_pwd` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '管理密码（建议存储哈希值）',
  `borrow_period` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '使用周期（如：30天）',
  `auto_return_timeout_minutes` int(11) NOT NULL DEFAULT 30 COMMENT '长时间不操作返回主页（分钟）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of system_config
-- ----------------------------
INSERT INTO `system_config` VALUES (1, '智能工具柜系统', 'Smart Cabinet System', 'SC-001', 'A区 工具库', 'admin', '30', 5);

-- ----------------------------
-- Table structure for system_config_copy1
-- ----------------------------
DROP TABLE IF EXISTS `system_config_copy1`;
CREATE TABLE `system_config_copy1`  (
  `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `system_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '系统名称',
  `eng_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '英文名称',
  `system_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '系统编号',
  `location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '所属位置',
  `admin_pwd` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '管理密码（建议存储哈希值）',
  `borrow_period` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '使用周期（如：30天）',
  `auto_return_timeout_minutes` int(11) NOT NULL DEFAULT 30 COMMENT '长时间不操作返回主页（分钟）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of system_config_copy1
-- ----------------------------
INSERT INTO `system_config_copy1` VALUES (1, '智能工具柜系统', 'Smart Cabinet System', 'SC-001', 'A区 工具库', 'admin', '7', 5);

SET FOREIGN_KEY_CHECKS = 1;
