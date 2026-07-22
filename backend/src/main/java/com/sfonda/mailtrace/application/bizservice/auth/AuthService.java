package com.sfonda.mailtrace.application.bizservice.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sfonda.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.sfonda.mailtrace.infrastructure.security.JwtTokenService;
import com.sfonda.mailtrace.interfaces.vo.auth.CurrentUserVO;
import com.sfonda.mailtrace.interfaces.vo.auth.LoginRequest;
import com.sfonda.mailtrace.interfaces.vo.auth.LoginResponse;
import com.sfonda.mailtrace.repox.mysql.entity.UserEntity;
import com.sfonda.mailtrace.repox.mysql.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_BAD_CREDENTIALS = 40101;
    private static final int CODE_ACCOUNT_DISABLED = 40301;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String account = normalize(request.getAccount());
        String password = normalize(request.getPassword());
        if (account.isEmpty() || password.isEmpty()) {
            throw new AuthBusinessException(CODE_BAD_REQUEST, "请输入账号和密码后再登录");
        }

        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getAccount, account)
                .last("LIMIT 1"));
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthBusinessException(CODE_BAD_CREDENTIALS, "账号或密码错误，请检查后重试");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new AuthBusinessException(CODE_ACCOUNT_DISABLED, "该账号已停用，请联系管理员处理");
        }

        userMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getId, user.getId())
                .set(UserEntity::getLastLoginAt, LocalDateTime.now())
                .set(UserEntity::getUpdatedBy, account));

        String token = jwtTokenService.createToken(user);
        return new LoginResponse(token, "Bearer", jwtTokenService.getExpiresInSeconds(), toCurrentUser(user));
    }

    public CurrentUserVO currentUser(CurrentUserPrincipal principal) {
        return new CurrentUserVO(
                principal.id(),
                principal.account(),
                principal.displayName(),
                principal.email(),
                principal.roleCode()
        );
    }

    private CurrentUserVO toCurrentUser(UserEntity user) {
        return new CurrentUserVO(
                user.getId(),
                user.getAccount(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRoleCode()
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
