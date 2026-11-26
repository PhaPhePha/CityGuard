package com.cityguard.backend.security;

import com.cityguard.backend.entity.User;
import com.cityguard.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service tải thông tin người dùng cho Spring Security theo username hoặc email.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String account) throws UsernameNotFoundException {
        String trimmed = account.trim();
        User user;
        if (trimmed.contains("@")) {
            user = userRepository.findByEmailIgnoreCase(trimmed)
                    .orElseGet(() -> userRepository.findByUsernameIgnoreCase(trimmed)
                            .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + trimmed)));
        } else {
            user = userRepository.findByUsernameIgnoreCase(trimmed)
                    .orElseGet(() -> userRepository.findByEmailIgnoreCase(trimmed)
                            .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với tài khoản: " + trimmed)));
        }
        return new CustomUserDetails(user);
    }
}

