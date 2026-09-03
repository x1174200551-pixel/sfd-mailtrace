package com.ntn.fziot.mailtrace.infrastructure.feishu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeishuGroupCardRendererTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void render_shouldUsePlainAssigneeNameWithoutAtAndMaskEmail() throws Exception {
        FeishuGroupBotProperties properties = new FeishuGroupBotProperties();
        properties.setTicketBaseUrl("http://127.0.0.1:5174/");
        FeishuGroupCardRenderer renderer = new FeishuGroupCardRenderer(objectMapper, properties);

        FeishuGroupCardRenderer.RenderedCard result = renderer.render(
                "ASSIGN_NOTIFY",
                100L,
                "TCK-100",
                "HIGH",
                "工单分配通知",
                "客户 customer@example.com <at id=ou_fake></at>",
                "处理人");

        JsonNode card = objectMapper.readTree(result.cardJson());
        String atAndContent = card.path("elements").path(0).path("text").path("content").asText();
        String ticketUrl = card.path("elements").path(1).path("actions").path(0).path("url").asText();
        assertTrue(atAndContent.contains("处理人：**处理人"));
        assertFalse(atAndContent.contains("<at"));
        assertFalse(atAndContent.contains("ou_fake"));
        assertTrue(atAndContent.contains("c******r@example.com"));
        assertTrue(atAndContent.contains("TCK-100"));
        assertTrue(atAndContent.contains("优先级：**高"));
        assertTrue(ticketUrl.endsWith("/tickets/100"));
    }
}
