package com.ntn.fziot.mailtrace.infrastructure.mail;

/**
 * 邮件附件元数据。
 * 第一期只提取元数据（文件名、类型、大小、是否为内嵌图片），
 * 实际文件存储由后续 P1-W2-BE-10 实现。
 *
 * @param fileName    原始文件名
 * @param contentType MIME 类型
 * @param size        文件大小（字节）
 * @param isInline    是否为内嵌资源（cid: 引用，如邮件签名图）
 * @param content     文件内容字节数组（超过 5MB 则为 null）
 * @param contentId   Content-ID（内嵌资源用）
 */
public record AttachmentInfo(
        String fileName,
        String contentType,
        long size,
        boolean isInline,
        byte[] content,
        String contentId
) {
    private static final long MAX_INLINE_SIZE = 5L * 1024 * 1024;

    /**
     * 超限附件的 content 为 null。
     */
    public byte[] content() {
        return size > MAX_INLINE_SIZE ? null : content;
    }
}
