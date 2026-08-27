package com.ntn.fziot.mailtrace.application.bizservice.mailfetch;

import com.ntn.fziot.mailtrace.application.bizservice.ticket.MessageThreadService;
import com.ntn.fziot.mailtrace.application.bizservice.ticket.TicketBizService;
import com.ntn.fziot.mailtrace.application.bizservice.security.EnterpriseMailboxAccessService;
import com.ntn.fziot.mailtrace.infrastructure.crypto.MailPasswordCipher;
import com.ntn.fziot.mailtrace.infrastructure.mail.ImapFetchClient;
import com.ntn.fziot.mailtrace.infrastructure.mail.ImapFetchConfig;
import com.ntn.fziot.mailtrace.infrastructure.mail.ParsedMail;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailFetchLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailFetchLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailFetchBizServiceTest {

    @Mock
    private MailboxMapper mailboxMapper;
    @Mock
    private MailFetchLogMapper mailFetchLogMapper;
    @Mock
    private MailPasswordCipher mailPasswordCipher;
    @Mock
    private ImapFetchClient imapFetchClient;
    @Mock
    private MessageIdDedupService messageIdDedupService;
    @Mock
    private MessageThreadService messageThreadService;
    @Mock
    private TicketBizService ticketBizService;
    @Mock
    private EnterpriseMailboxAccessService enterpriseMailboxAccessService;

    @InjectMocks
    private MailFetchBizService mailFetchBizService;

    private MailboxEntity mailbox;

    @BeforeEach
    void setUp() {
        mailbox = new MailboxEntity();
        mailbox.setId(11L);
        mailbox.setEnabled(true);
        mailbox.setEmailAddress("support@example.com");
        mailbox.setImapHost("imap.example.com");
        mailbox.setImapPort(993);
        mailbox.setImapSslEnabled(true);
        mailbox.setImapUsername("support@example.com");
        mailbox.setImapPasswordEnc("enc");
        mailbox.setImapFolder("INBOX");
        mailbox.setFetchIntervalSec(120);
    }

    @Test
    void isDue_whenNeverFetched_shouldReturnTrue() {
        mailbox.setLastFetchAt(null);
        assertTrue(mailFetchBizService.isDue(mailbox, LocalDateTime.now()));
    }

    @Test
    void isDue_whenWithinInterval_shouldReturnFalse() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 23, 11, 30, 0);
        mailbox.setLastFetchAt(now.minusSeconds(30));
        assertFalse(mailFetchBizService.isDue(mailbox, now));
    }

    @Test
    void isDue_whenIntervalElapsed_shouldReturnTrue() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 23, 11, 30, 0);
        mailbox.setLastFetchAt(now.minusSeconds(120));
        assertTrue(mailFetchBizService.isDue(mailbox, now));
    }

    @Test
    void fetchMailbox_whenImapSuccess_shouldWriteSuccessLog() throws Exception {
        when(mailboxMapper.selectById(11L)).thenReturn(mailbox);
        when(mailPasswordCipher.decrypt("enc")).thenReturn("secret");
        when(imapFetchClient.fetchUnseenMessages(any(ImapFetchConfig.class))).thenReturn(List.of());
        when(messageIdDedupService.filterNew(any())).thenReturn(List.of());
        when(mailFetchLogMapper.insert(any(MailFetchLogEntity.class))).thenAnswer(invocation -> {
            MailFetchLogEntity entity = invocation.getArgument(0);
            entity.setId(1001L);
            return 1;
        });

        Long logId = mailFetchBizService.fetchMailbox(11L, MailFetchBizService.TRIGGER_SCHEDULED);

        assertEquals(1001L, logId);
        ArgumentCaptor<MailFetchLogEntity> logCaptor = ArgumentCaptor.forClass(MailFetchLogEntity.class);
        verify(mailFetchLogMapper).insert(logCaptor.capture());
        MailFetchLogEntity saved = logCaptor.getValue();
        assertTrue(Boolean.TRUE.equals(saved.getSuccess()));
        assertEquals(0, saved.getFetchedCount());
        assertEquals(MailFetchBizService.TRIGGER_SCHEDULED, saved.getTriggerType());
        assertEquals(0, saved.getCreatedTicketCount());
        verify(mailboxMapper).updateById(any(MailboxEntity.class));
    }

    @Test
    void fetchMailbox_whenImapFails_shouldWriteFailureLog() throws Exception {
        when(mailboxMapper.selectById(11L)).thenReturn(mailbox);
        when(mailPasswordCipher.decrypt("enc")).thenReturn("secret");
        when(imapFetchClient.fetchUnseenMessages(any(ImapFetchConfig.class)))
                .thenThrow(new IllegalStateException("Invalid credentials"));
        when(mailFetchLogMapper.insert(any(MailFetchLogEntity.class))).thenAnswer(invocation -> {
            MailFetchLogEntity entity = invocation.getArgument(0);
            entity.setId(1002L);
            return 1;
        });

        Long logId = mailFetchBizService.fetchMailbox(11L, MailFetchBizService.TRIGGER_MANUAL);

        assertEquals(1002L, logId);
        ArgumentCaptor<MailFetchLogEntity> logCaptor = ArgumentCaptor.forClass(MailFetchLogEntity.class);
        verify(mailFetchLogMapper).insert(logCaptor.capture());
        MailFetchLogEntity saved = logCaptor.getValue();
        assertFalse(Boolean.TRUE.equals(saved.getSuccess()));
        assertEquals(0, saved.getFetchedCount());
        assertEquals(MailFetchBizService.TRIGGER_MANUAL, saved.getTriggerType());
        assertTrue(saved.getErrorMessage().contains("Invalid credentials"));
    }
}
