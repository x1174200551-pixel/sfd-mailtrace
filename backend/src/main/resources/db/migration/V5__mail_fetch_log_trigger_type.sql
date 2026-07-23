-- P1-W2-BE-04: 拉取日志补充触发方式，对齐接口契约 triggerType
ALTER TABLE `mt_mail_fetch_log`
  ADD COLUMN `trigger_type` VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED'
    COMMENT '触发方式：SCHEDULED/MANUAL'
    AFTER `mailbox_id`;
