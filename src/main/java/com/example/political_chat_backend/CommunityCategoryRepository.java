package com.example.political_chat_backend;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CommunityCategoryRepository extends JpaRepository<CommunityCategory, String> {
    Optional<CommunityCategory> findByCategoryId(String categoryId);
}
