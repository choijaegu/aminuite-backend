package com.example.political_chat_backend;

import com.example.political_chat_backend.User;
import com.example.political_chat_backend.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component; // Spring이 이 클래스를 관리하도록 @Component 추가

import java.util.Set;

@Component // 이 어노테이션을 통해 Spring 컨테이너가 이 클래스의 빈을 관리하게 됩니다.
public class DataInitializer {

    // CommandLineRunner를 @Bean으로 등록하면 애플리케이션 시작 시 자동으로 실행됩니다.
    // 테스트용으로만 사용하고, 실제 회원가입 API 구현 후에는 이 부분을 제거하거나 주석 처리하세요.
    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // 기존 사용자가 없을 경우에만 생성 (선택적)
            if (!userRepository.existsByUsername("dbuser")) {
                User user = new User("dbuser", passwordEncoder.encode("dbpassword"));
                user.setRoles(Set.of("ROLE_USER")); // 기본 역할 설정
                userRepository.save(user);
                System.out.println("Created test user: dbuser / dbpassword");
            }

            if (!userRepository.existsByUsername("dbadmin")) {
                User admin = new User("dbadmin", passwordEncoder.encode("dbadminpass"));
                admin.setRoles(Set.of("ROLE_ADMIN", "ROLE_USER")); // 관리자 및 사용자 역할
                userRepository.save(admin);
                System.out.println("Created test admin: dbadmin / dbadminpass");
            }

            // 필요하다면 여기에 다른 초기 데이터(예: 기본 커뮤니티 카테고리) 생성 로직 추가 가능
        };
    }
}
