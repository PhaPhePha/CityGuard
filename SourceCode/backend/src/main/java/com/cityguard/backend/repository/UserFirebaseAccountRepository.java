package com.cityguard.backend.repository;

import com.cityguard.backend.entity.UserFirebaseAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserFirebaseAccountRepository extends JpaRepository<UserFirebaseAccount, Long> {
    Optional<UserFirebaseAccount> findByFirebaseUid(String firebaseUid);
    Optional<UserFirebaseAccount> findByUserId(Long userId);
}

