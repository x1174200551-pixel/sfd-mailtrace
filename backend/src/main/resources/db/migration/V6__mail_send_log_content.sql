-- P1-W2-BE-08 发送日志增加正文存储，用于详情展示

ALTER TABLE `mt_mail_send_log`
    ADD COLUMN `content_body` TEXT COMMENT '邮件正文内容（纯文本）' AFTER `subject`;
