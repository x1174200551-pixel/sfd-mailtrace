-- 扩展工单邮件消息，保留来信原始内容与完整收件人信息。
ALTER TABLE `mt_ticket_message`
    MODIFY COLUMN `content_text` LONGTEXT DEFAULT NULL COMMENT '纯文本正文',
    MODIFY COLUMN `content_html` LONGTEXT DEFAULT NULL COMMENT 'HTML正文',
    ADD COLUMN `to_addresses` TEXT DEFAULT NULL COMMENT '原始收件人列表（逗号分隔）' AFTER `to_address`,
    ADD COLUMN `cc_addresses` TEXT DEFAULT NULL COMMENT '原始抄送列表（逗号分隔）' AFTER `to_addresses`,
    ADD COLUMN `bcc_addresses` TEXT DEFAULT NULL COMMENT '原始密送列表（逗号分隔）' AFTER `cc_addresses`,
    ADD COLUMN `raw_headers` LONGTEXT DEFAULT NULL COMMENT '原始邮件头',
    ADD COLUMN `raw_eml_object_key` VARCHAR(512) DEFAULT NULL COMMENT '原始EML文件对象键',
    ADD COLUMN `raw_eml_size` BIGINT NOT NULL DEFAULT 0 COMMENT '原始EML大小（字节）';

-- 扩展附件元数据，用于识别内嵌资源并支持 HTML 中 cid: 图片替换。
ALTER TABLE `mt_ticket_attachment`
    ADD COLUMN `is_inline` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否内嵌资源',
    ADD COLUMN `content_id` VARCHAR(255) DEFAULT NULL COMMENT 'Content-ID（内嵌资源引用）',
    ADD KEY `idx_mt_ticket_attachment_content_id` (`content_id`);
