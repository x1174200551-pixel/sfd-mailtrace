package com.ntn.fziot.mailtrace.infrastructure.cache;

import com.ntn.fziot.mailtrace.application.bizservice.enterprise.EnterpriseService;
import com.ntn.fziot.mailtrace.application.bizservice.mailbox.MailboxService;
import com.ntn.fziot.mailtrace.application.bizservice.role.RoleManagementService;
import com.ntn.fziot.mailtrace.application.bizservice.user.UserService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.enterprise.EnterpriseEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.enterprise.EnterpriseSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.role.RoleEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.role.RoleSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserUpdateRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CacheInvalidationCoverageTest {

    @Test
    void authorizationWrites_shouldInvalidateMatchingCacheKeys() throws Exception {
        assertDoubleDelete(UserService.class, "updateUser", "user-primary-role-id", "#id",
                CurrentUserPrincipal.class, Long.class, UserUpdateRequest.class);
        assertDoubleDelete(RoleManagementService.class, "updateRole", "role-authorization", "#id",
                CurrentUserPrincipal.class, Long.class, RoleSaveRequest.class);
        assertDoubleDelete(RoleManagementService.class, "updateEnabled", "role-authorization", "#id",
                CurrentUserPrincipal.class, Long.class, RoleEnabledRequest.class);
    }

    @Test
    void enterpriseAndMailboxWrites_shouldInvalidateAccessCatalog() throws Exception {
        assertDoubleDelete(EnterpriseService.class, "createEnterprise", "access-catalog", "'all'",
                CurrentUserPrincipal.class, EnterpriseSaveRequest.class);
        assertDoubleDelete(EnterpriseService.class, "updateEnterprise", "access-catalog", "'all'",
                CurrentUserPrincipal.class, Long.class, EnterpriseSaveRequest.class);
        assertDoubleDelete(EnterpriseService.class, "updateEnabled", "access-catalog", "'all'",
                CurrentUserPrincipal.class, Long.class, EnterpriseEnabledRequest.class);
        assertDoubleDelete(MailboxService.class, "createMailbox", "access-catalog", "'all'",
                CurrentUserPrincipal.class, MailboxSaveRequest.class);
        assertDoubleDelete(MailboxService.class, "updateMailbox", "access-catalog", "'all'",
                CurrentUserPrincipal.class, Long.class, MailboxSaveRequest.class);
        assertDoubleDelete(MailboxService.class, "updateEnabled", "access-catalog", "'all'",
                CurrentUserPrincipal.class, Long.class, MailboxEnabledRequest.class);
        assertDoubleDelete(MailboxService.class, "deleteMailbox", "access-catalog", "'all'",
                CurrentUserPrincipal.class, Long.class);
    }

    private void assertDoubleDelete(Class<?> type, String methodName, String cacheName, String key,
                                    Class<?>... parameterTypes) throws Exception {
        Method method = type.getMethod(methodName, parameterTypes);
        MtRedisCacheDoubleDelete annotation = method.getAnnotation(MtRedisCacheDoubleDelete.class);
        assertEquals(cacheName, annotation.cacheName());
        assertEquals(key, annotation.key());
        assertEquals(500, annotation.delayMillis());
    }
}
