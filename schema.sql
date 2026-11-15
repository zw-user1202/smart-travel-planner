-- 旅行规划助手工作台 数据库初始化脚本（MySQL 5.7+）
CREATE DATABASE IF NOT EXISTS travel_planner DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE travel_planner;

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码(BCrypt风格简化为SHA-256加盐)',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `daily_quota` INT NOT NULL DEFAULT 5 COMMENT '每日AI规划额度',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

DROP TABLE IF EXISTS `trip`;
CREATE TABLE `trip` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '行程ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户',
  `title` VARCHAR(100) NOT NULL COMMENT '行程标题',
  `destination` VARCHAR(100) NOT NULL COMMENT '目的地',
  `start_date` DATE NOT NULL COMMENT '出发日期',
  `end_date` DATE NOT NULL COMMENT '返回日期',
  `people_count` INT NOT NULL DEFAULT 1 COMMENT '出行人数',
  `budget` DECIMAL(12,2) DEFAULT NULL COMMENT '总预算(元)',
  `preferences` VARCHAR(500) DEFAULT NULL COMMENT '偏好标签,逗号分隔',
  `status` VARCHAR(20) NOT NULL DEFAULT 'planning' COMMENT '状态: planned待规划 planned已规划 ongoing进行中 done已完成',
  `plan_json` LONGTEXT COMMENT '智能规划结果JSON',
  `advice` VARCHAR(1000) DEFAULT NULL COMMENT '总览建议',
  `estimated_cost` DECIMAL(12,2) DEFAULT NULL COMMENT '预算估算(元)',
  `ai_provider` VARCHAR(50) DEFAULT NULL COMMENT 'AI供应商',
  `ai_model` VARCHAR(50) DEFAULT NULL COMMENT 'AI模型',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='旅行行程表';

DROP TABLE IF EXISTS `ai_usage_log`;
CREATE TABLE `ai_usage_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `trip_id` BIGINT DEFAULT NULL,
  `use_date` DATE NOT NULL COMMENT '使用日期',
  `tokens` INT DEFAULT 0,
  `success` TINYINT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`, `use_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI额度使用日志';
