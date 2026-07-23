package com.ntn.fziot.mailtrace.application.bizservice.mailfetch;

import com.ntn.fziot.mailtrace.infrastructure.mail.ParsedMail;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageIdDedupServiceTest {

    @Mock
    private TicketMessageMapper ticketMessageMapper;

    private MessageIdDedupService dedupService;

    @BeforeEach
    void setUp() {
        dedupService = new MessageIdDedupService(ticketMessageMapper);
    }

    @Test
    void isDuplicate_whenExists_shouldReturnTrue() {
        when(ticketMessageMapper.countExistingByMessageId("<existing@test.com>")).thenReturn(1);
        assertTrue(dedupService.isDuplicate("<existing@test.com>"));
    }

    @Test
    void isDuplicate_whenNotExists_shouldReturnFalse() {
        when(ticketMessageMapper.countExistingByMessageId("<new@test.com>")).thenReturn(0);
        assertFalse(dedupService.isDuplicate("<new@test.com>"));
    }

    @Test
    void isDuplicate_whenNullMessageId_shouldReturnFalse() {
        assertFalse(dedupService.isDuplicate((String) null));
    }

    @Test
    void isDuplicate_whenBlankMessageId_shouldReturnFalse() {
        assertFalse(dedupService.isDuplicate(""));
        assertFalse(dedupService.isDuplicate("   "));
    }

    // ==================== isDuplicate(ParsedMail) ====================

    @Test
    void isDuplicate_whenMailExists_shouldReturnTrue() {
        when(ticketMessageMapper.countExistingByMessageId("<dup@test.com>")).thenReturn(1);
        ParsedMail mail = createMail("<dup@test.com>");
        assertTrue(dedupService.isDuplicate(mail));
    }

    @Test
    void isDuplicate_whenMailNullMessageId_shouldReturnFalse() {
        ParsedMail mail = createMail(null);
        assertFalse(dedupService.isDuplicate(mail));
    }

    @Test
    void isDuplicate_whenMailIsNull_shouldReturnFalse() {
        assertFalse(dedupService.isDuplicate((ParsedMail) null));
    }

    // ==================== filterNew(List) ====================

    @Test
    void filterNew_whenAllNew_shouldReturnAll() {
        when(ticketMessageMapper.countExistingByMessageId(any())).thenReturn(0);
        List<ParsedMail> mails = List.of(
                createMail("<a@test.com>"),
                createMail("<b@test.com>"),
                createMail("<c@test.com>")
        );
        List<ParsedMail> result = dedupService.filterNew(mails);
        assertEquals(3, result.size());
    }

    @Test
    void filterNew_whenSomeDuplicate_shouldFilterOut() {
        when(ticketMessageMapper.countExistingByMessageId("<a@test.com>")).thenReturn(0);
        when(ticketMessageMapper.countExistingByMessageId("<b@test.com>")).thenReturn(1);
        when(ticketMessageMapper.countExistingByMessageId("<c@test.com>")).thenReturn(0);

        List<ParsedMail> mails = List.of(
                createMail("<a@test.com>"),
                createMail("<b@test.com>"),
                createMail("<c@test.com>")
        );
        List<ParsedMail> result = dedupService.filterNew(mails);
        assertEquals(2, result.size());
        assertEquals("<a@test.com>", result.get(0).messageId());
        assertEquals("<c@test.com>", result.get(1).messageId());
    }

    @Test
    void filterNew_whenAllDuplicate_shouldReturnEmpty() {
        when(ticketMessageMapper.countExistingByMessageId(any())).thenReturn(1);
        List<ParsedMail> mails = List.of(
                createMail("<dup1@test.com>"),
                createMail("<dup2@test.com>")
        );
        List<ParsedMail> result = dedupService.filterNew(mails);
        assertTrue(result.isEmpty());
    }

    @Test
    void filterNew_whenEmptyList_shouldReturnEmpty() {
        List<ParsedMail> result = dedupService.filterNew(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void filterNew_whenNullList_shouldReturnEmpty() {
        List<ParsedMail> result = dedupService.filterNew(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void filterNew_whenNullMessageId_shouldNotBeTreatedAsDuplicate() {
        // 有 null messageId → 不走 Mapper 查询，直接放行
        when(ticketMessageMapper.countExistingByMessageId("<real@test.com>")).thenReturn(0);
        List<ParsedMail> mails = List.of(
                createMail(null),
                createMail("<real@test.com>")
        );
        List<ParsedMail> result = dedupService.filterNew(mails);
        assertEquals(2, result.size());
    }

    // ==================== 辅助方法 ====================

    private static ParsedMail createMail(String messageId) {
        return new ParsedMail(
                messageId, null, null,
                "from@test.com", "Test Sender",
                List.of("to@test.com"), List.of(),
                "Test Subject", "Test body", null,
                null, null, List.of(), 0
        );
    }
}
