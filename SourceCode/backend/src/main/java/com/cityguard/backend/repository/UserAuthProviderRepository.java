package com.cityguard.backend.repository;

import com.cityguard.backend.entity.UserAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAuthProviderRepository extends JpaRepository<UserAuthProvider, Long> {
    Optional<UserAuthProvider> findByProviderAndProviderUid(String provider, String providerUid);
    List<UserAuthProvider> findByUserId(Long userId);
    Optional<UserAuthProvider> findByUserIdAndProvider(Long userId, String provider);
    boolean existsByProviderAndProviderUid(String provider, String providerUid);
}

