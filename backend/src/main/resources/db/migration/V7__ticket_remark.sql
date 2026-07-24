-- 补充 mt_ticket 的 remark 字段
ALTER TABLE mt_ticket ADD COLUMN `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注' AFTER `last_agent_reply_at`;
