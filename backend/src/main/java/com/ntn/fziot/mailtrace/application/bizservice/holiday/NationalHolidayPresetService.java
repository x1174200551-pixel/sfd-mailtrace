package com.ntn.fziot.mailtrace.application.bizservice.holiday;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.holiday.NationalHolidayPresetItemVO;
import com.ntn.fziot.mailtrace.interfaces.vo.holiday.NationalHolidayPresetResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class NationalHolidayPresetService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_PROVIDER_FAILED = 50201;
    private static final String SOURCE_NAME = "节假日API";

    private final ObjectMapper objectMapper;
    private final PermissionService permissionService;
    private final HttpClient httpClient;
    private final String apiUrlTemplate;
    private final Duration requestTimeout;

    public NationalHolidayPresetService(
            ObjectMapper objectMapper,
            PermissionService permissionService,
            @Value("${mailtrace.holiday.national-api-url-template:https://api.jiejiariapi.com/v1/holidays/{year}}")
            String apiUrlTemplate,
            @Value("${mailtrace.holiday.national-api-timeout-ms:5000}") long timeoutMs) {
        this.objectMapper = objectMapper;
        this.permissionService = permissionService;
        this.apiUrlTemplate = apiUrlTemplate;
        this.requestTimeout = Duration.ofMillis(timeoutMs);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.requestTimeout)
                .build();
    }

    public NationalHolidayPresetResponse getPreset(CurrentUserPrincipal principal, Integer year) {
        permissionService.assertPermission(principal, "holiday:import", "无权导入法定节假日模板");
        if (year == null || year < 2000 || year > 2100) {
            throw new BusinessException(CODE_BAD_REQUEST, "请选择有效年份");
        }

        URI providerUri = URI.create(resolveProviderUrl(year));
        ProviderPreset preset = fetchProviderPreset(providerUri);
        if (preset.holidays().isEmpty()) {
            throw new BusinessException(CODE_PROVIDER_FAILED, "三方节假日接口未返回" + year + "年放假日期");
        }

        return new NationalHolidayPresetResponse(
                year,
                SOURCE_NAME,
                providerUri.toString(),
                List.of(year),
                preset.holidays(),
                preset.makeupWorkdays()
        );
    }

    private ProviderPreset fetchProviderPreset(URI providerUri) {
        try {
            return parseProviderResponse(fetchProviderJson(providerUri));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new BusinessException(CODE_PROVIDER_FAILED, "三方节假日接口请求被中断");
        } catch (IOException | RuntimeException error) {
            throw new BusinessException(CODE_PROVIDER_FAILED, "三方节假日接口暂不可用，请稍后重试");
        }
    }

    String fetchProviderJson(URI providerUri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(providerUri)
                .GET()
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("holiday provider status=" + response.statusCode());
        }
        return response.body();
    }

    ProviderPreset parseProviderResponse(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode payload = root.has("data") && (root.get("data").isObject() || root.get("data").isArray())
                ? root.get("data")
                : root;

        List<NationalHolidayPresetItemVO> holidays = new ArrayList<>();
        List<LocalDate> makeupWorkdays = new ArrayList<>();
        if (payload.isObject()) {
            payload.fields().forEachRemaining(entry -> collectProviderItem(entry.getValue(), holidays, makeupWorkdays));
        } else if (payload.isArray()) {
            payload.forEach(item -> collectProviderItem(item, holidays, makeupWorkdays));
        }

        holidays.sort(Comparator.comparing(NationalHolidayPresetItemVO::holidayDate));
        makeupWorkdays.sort(Comparator.naturalOrder());
        return new ProviderPreset(List.copyOf(holidays), List.copyOf(makeupWorkdays));
    }

    private void collectProviderItem(JsonNode item, List<NationalHolidayPresetItemVO> holidays,
                                     List<LocalDate> makeupWorkdays) {
        if (item == null || !item.hasNonNull("date")) {
            return;
        }
        LocalDate date = LocalDate.parse(item.get("date").asText());
        boolean offDay = item.has("isOffDay") ? item.get("isOffDay").asBoolean() : item.path("holiday").asBoolean(false);
        String name = normalize(item.hasNonNull("name") ? item.get("name").asText() : item.path("holidayName").asText());
        if (offDay) {
            holidays.add(new NationalHolidayPresetItemVO(date, name.isEmpty() ? "法定节假日" : name));
            return;
        }
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            makeupWorkdays.add(date);
        }
    }

    private String resolveProviderUrl(Integer year) {
        if (apiUrlTemplate.contains("{year}")) {
            return apiUrlTemplate.replace("{year}", String.valueOf(year));
        }
        return String.format(apiUrlTemplate, year);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    record ProviderPreset(
            List<NationalHolidayPresetItemVO> holidays,
            List<LocalDate> makeupWorkdays
    ) {
    }
}
