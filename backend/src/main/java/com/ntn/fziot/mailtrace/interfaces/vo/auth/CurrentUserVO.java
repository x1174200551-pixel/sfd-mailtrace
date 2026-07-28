package com.ntn.fziot.mailtrace.interfaces.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "当前用户信息")
public class CurrentUserVO {

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "账号")
    private String account;

    @Schema(description = "显示名称")
    private String displayName;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "角色码")
    private String roleCode;

    @Schema(description = "角色编码列表")
    private Set<String> roles;

    @Schema(description = "权限编码列表")
    private Set<String> permissions;

    @Schema(description = "数据范围，key 为资源类型，value 为范围编码列表")
    private Map<String, Set<String>> dataScopes;

    public CurrentUserVO(Long id, String account, String displayName, String email, String roleCode) {
        this(id, account, displayName, email, roleCode, Set.of(), Set.of(), Map.of());
    }
}
