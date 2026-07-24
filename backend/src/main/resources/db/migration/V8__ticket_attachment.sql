-- 工单附件表
CREATE TABLE IF NOT EXISTS `mt_ticket_attachment` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `ticket_id`      BIGINT       NOT NULL            COMMENT '工单ID',
    `message_id`     BIGINT       DEFAULT NULL         COMMENT '关联消息ID（为空则为回复编辑器上传）',
    `file_name`      VARCHAR(255) NOT NULL            COMMENT '原始文件名',
    `file_size`      BIGINT       NOT NULL DEFAULT 0  COMMENT '文件大小（字节）',
    `content_type`   VARCHAR(128) DEFAULT NULL         COMMENT 'MIME 类型',
    `object_key`     VARCHAR(512) NOT NULL            COMMENT 'MinIO 对象键',
    `uploaded_by`    VARCHAR(64)  NOT NULL            COMMENT '上传人',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_message_id` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单附件';
