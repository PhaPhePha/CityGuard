package com.cityguard.backend.service.impl;

import com.cityguard.backend.dto.auth.AuthResponse;
import com.cityguard.backend.dto.auth.LoginRequest;
import com.cityguard.backend.entity.User;
import com.cityguard.backend.enums.ErrorCode;
import com.cityguard.backend.exception.AppException;
import com.cityguard.backend.repository.UserRepository;
import com.cityguard.backend.security.AuthTokenIssuer;
import com.cityguard.backend.service.AdminAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenIssuer authTokenIssuer;

    @Override
    @Transactional
    public AuthResponse adminLogin(LoginRequest request){
        String account = request.getAccount().trim();

        User user = account.contains("@")
                ? userRepository.findByEmailIgnoreCase(account)
                  .orElseThrow(()-> new AppException(
                ErrorCode.UNAUTHORIZED,
                "Tài khoản hoặc mật khẩy không chính xác"))
                :userRepository.findByUsernameIgnoreCase(account)
                 .orElseThrow(()-> new AppException(
                         ErrorCode.UNAUTHORIZED,
                         "Tài khoản hoặc mật khẩu không chính xác"
                 ));

        if(!passwordEncoder.matches(
                request.getPassword(), user.getPasswordHash()
        )){
            throw new AppException(
                    ErrorCode.UNAUTHORIZED,
                    "Tảì khoản hoặc mật khấu không chính xác"
            );
        }
        if(!"ACTIVE".equalsIgnoreCase(user.getStatus())){
            throw new AppException(
                    ErrorCode.FORBIDDEN,
                    "Tài khoản đã bị khóa hoặc ngừng họat động"
            );
        }

        String role = user.getRole();

        if(!"ADMIN".equalsIgnoreCase(role)
        && !"MODERATOR".equalsIgnoreCase(role)){
            throw new AppException(
                    ErrorCode.FORBIDDEN,
                    "Bạn không có quyền truy cập trang quản trị"
            );
        }
        return authTokenIssuer.issueAuthResponse(user);
    }
}
