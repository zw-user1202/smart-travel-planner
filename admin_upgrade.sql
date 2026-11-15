-- 后台管理扩展：用户角色 + 系统配置
-- 在已有 travel_planner 库执行

USE travel_planner;

ALTER TABLE `user`
  ADD COLUMN `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色:0普通 1管理员' AFTER `daily_quota`,
  ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1正常' AFTER `role`;


-- 如需将其他演示账号提为管理员：UPDATE `user` SET `role` = 1 WHERE `username` = 'xxx';


CREATE TABLE IF NOT EXISTS `sys_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cfg_key` VARCHAR(50) NOT NULL COMMENT '配置键',
  `cfg_value` VARCHAR(500) DEFAULT NULL COMMENT '配置值',
  `remark` VARCHAR(200) DEFAULT NULL,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`cfg_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

INSERT INTO `sys_config` (`cfg_key`, `cfg_value`, `remark`) VALUES
  ('deepseek.api-key', '', 'DeepSeek API Key'),
  ('deepseek.model', 'deepseek-chat', 'DeepSeek 模型名'),
  ('amap.key', '', '高德 Web 服务 Key'),
  ('amap.web-js-key', '', '高德 JS API Key'),
  ('amap.security-js-code', '', '高德 JS 安全密钥')
ON DUPLICATE KEY UPDATE `cfg_key` = `cfg_key`;
