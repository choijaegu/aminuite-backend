package com.example.political_chat_backend;

import com.example.political_chat_backend.SignupRequestDto;
import com.example.political_chat_backend.User;
import com.example.political_chat_backend.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 추가

import java.util.Set; // 추가

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // SecurityConfig에 빈으로 등록된 PasswordEncoder 주입

    @Transactional // 데이터 변경이 있으므로 트랜잭션 처리
    public User registerUser(SignupRequestDto signupRequest) {
        // 사용자 이름 중복 확인
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            // 실제 서비스에서는 좀 더 구체적인 예외를 정의하고 처리하는 것이 좋습니다.
            throw new IllegalArgumentException("오류: 이미 사용 중인 사용자 이름입니다.");
        }

        // (선택 사항) 이메일 중복 확인 (SignupRequestDto에 email 필드가 있다면)
        // if (signupRequest.getEmail() != null && userRepository.existsByEmail(signupRequest.getEmail())) {
        //     throw new IllegalArgumentException("오류: 이미 사용 중인 이메일입니다.");
        // }

        // 새 사용자 객체 생성
        User user = new User(
                signupRequest.getUsername(),
                passwordEncoder.encode(signupRequest.getPassword()) // 비밀번호 암호화
        );

        // 기본 역할 설정 (예: ROLE_USER)
        // User 엔티티의 roles 필드 타입에 맞게 설정 (여기서는 Set<String>)
        user.setRoles(Set.of("ROLE_USER"));

        // 사용자 저장
        return userRepository.save(user);
    }
}
