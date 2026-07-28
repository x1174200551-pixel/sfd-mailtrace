package com.ntn.fziot.mailtrace.application.bizservice.holiday;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

class NationalHolidayPresetServiceTest {

    private final CurrentUserPrincipal admin = new CurrentUserPrincipal(
            1L, "admin", "系统管理员", "admin@example.com", "ADMIN");
    private final CurrentUserPrincipal agent = new CurrentUserPrincipal(
            2L, "agent", "处理人", "agent@example.com", "AGENT");

    @Test
    void getPreset_shouldFetchProviderAndKeepOnlyOffDays() {
        NationalHolidayPresetService service = serviceWithJson("""
                {
                  "2026-01-01": {"date": "2026-01-01", "name": "元旦", "isOffDay": true},
                  "2026-01-02": {"date": "2026-01-02", "name": "元旦", "isOffDay": true},
                  "2026-01-04": {"date": "2026-01-04", "name": "元旦", "isOffDay": false},
                  "2026-02-10": {"date": "2026-02-10", "name": "北小年", "isOffDay": false}
                }
                """);

        var response = service.getPreset(admin, 2026);

        assertEquals(2026, response.year());
        assertEquals("https://provider.test/holidays/2026", response.sourceUrl());
        assertEquals(2, response.records().size());
        assertEquals("2026-01-01", response.records().get(0).holidayDate().toString());
        assertEquals("元旦", response.records().get(0).holidayName());
        assertEquals(1, response.makeupWorkdayDates().size());
        assertEquals("2026-01-04", response.makeupWorkdayDates().get(0).toString());
    }

    @Test
    void getPreset_whenProviderReturnsDataWrapper_shouldParseDataNode() {
        NationalHolidayPresetService service = serviceWithJson("""
                {
                  "data": [
                    {"date": "2026-10-01", "name": "国庆节", "isOffDay": true},
                    {"date": "2026-10-10", "name": "国庆节", "isOffDay": false}
                  ]
                }
                """);

        var response = service.getPreset(admin, 2026);

        assertEquals(1, response.records().size());
        assertEquals("国庆节", response.records().get(0).holidayName());
        assertEquals("2026-10-10", response.makeupWorkdayDates().get(0).toString());
    }

    @Test
    void getPreset_whenNotAdmin_shouldReject() {
        NationalHolidayPresetService service = serviceWithJson("{}");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getPreset(agent, 2026));

        assertTrue(ex.getMessage().contains("无权导入法定节假日模板"));
    }

    @Test
    void getPreset_whenProviderUnavailable_shouldReturnBusinessError() {
        NationalHolidayPresetService service = new NationalHolidayPresetService(
                new ObjectMapper(), permissionService(), "https://provider.test/holidays/{year}", 1000) {
            @Override
            String fetchProviderJson(URI providerUri) throws IOException {
                throw new IOException("provider down");
            }
        };

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getPreset(admin, 2026));

        assertTrue(ex.getMessage().contains("三方节假日接口暂不可用"));
    }

    private NationalHolidayPresetService serviceWithJson(String json) {
        return new NationalHolidayPresetService(new ObjectMapper(), permissionService(), "https://provider.test/holidays/{year}", 1000) {
            @Override
            String fetchProviderJson(URI providerUri) {
                return json;
            }
        };
    }

    private PermissionService permissionService() {
        PermissionService permissionService = mock(PermissionService.class);
        lenient().doAnswer(invocation -> {
            CurrentUserPrincipal principal = invocation.getArgument(0);
            String message = invocation.getArgument(2);
            if (principal == null) {
                throw new BusinessException(40302, "未登录");
            }
            if ("ADMIN".equals(principal.roleCode())) {
                return null;
            }
            throw new BusinessException(40302, message);
        }).when(permissionService).assertPermission(any(), any(), any());
        return permissionService;
    }
}
