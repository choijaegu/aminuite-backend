package com.example.political_chat_backend;

import com.example.political_chat_backend.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // username으로 사용자를 찾는 메소드 (로그인 시 사용)
    Optional<User> findByUsername(String username);

    // username이 이미 존재하는지 확인하는 메소드 (회원가입 시 중복 체크용)
    Boolean existsByUsername(String username);

    // 필요하다면 email로 사용자를 찾거나, email 존재 여부 확인 메소드 등 추가 가능
    // Optional<User> findByEmail(String email);
    // Boolean existsByEmail(String email);
}
