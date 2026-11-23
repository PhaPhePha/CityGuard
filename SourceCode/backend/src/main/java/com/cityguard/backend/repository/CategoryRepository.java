package com.cityguard.backend.repository;

import com.cityguard.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByCode(String code);
    Optional<Category> findByCodeIgnoreCase(String code);
}
