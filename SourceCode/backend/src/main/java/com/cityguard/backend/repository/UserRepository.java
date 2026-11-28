package com.cityguard.backend.repository;

import com.cityguard.backend.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);


    @Query("""
            select u from User u
            where u.role = coalesce(:role, u.role)
              and u.status = coalesce(:status, u.status)
              and (
                  coalesce(:search, '') = ''
                  or locate(lower(:search), lower(coalesce(u.username, ''))) > 0
                  or locate(lower(:search), lower(coalesce(u.email, ''))) > 0
                  or locate(lower(:search), lower(u.displayName)) > 0
              )
            """)
    Page<User> findAdminUsers(
            @Param("search") String search,
            @Param("role") String role,
            @Param("status") String status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
